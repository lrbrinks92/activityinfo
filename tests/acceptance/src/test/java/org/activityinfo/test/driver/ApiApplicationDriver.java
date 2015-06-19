package org.activityinfo.test.driver;


import com.codahale.metrics.Meter;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.mysql.jdbc.StringUtils;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import cucumber.runtime.java.guice.ScenarioScoped;
import org.activityinfo.model.calc.AggregationMethod;
import org.activityinfo.model.type.enumerated.EnumType;
import org.activityinfo.test.capacity.Metrics;
import org.activityinfo.test.sut.Accounts;
import org.activityinfo.test.sut.Server;
import org.activityinfo.test.sut.UserAccount;
import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static java.lang.String.format;

@ScenarioScoped
public class ApiApplicationDriver extends ApplicationDriver {

    public static final Logger LOGGER = Logger.getLogger(ApiApplicationDriver.class.getName());

    public static final Meter COMMAND_RATE = Metrics.REGISTRY.meter("COMMAND_RATE");
    public static final ApiErrorRate ERROR_RATE = new ApiErrorRate();
    


    private boolean flushing = false;

    private int retryCount = 0;

    public UserAccount getCurrentUser() {
        return currentUser;
    }


    private class Command {
        private final JSONObject request;
        private final ResponseHandler handler;

        public Command(JSONObject object, ResponseHandler handler) {
            this.request = object;
            this.handler = handler;
        }
    }
    
    private interface ResponseHandler {
        void response(JSONObject response) throws JSONException;
    }
    
    private class PendingId implements ResponseHandler, Supplier<Integer> {

        private Integer id;

        public PendingId() {
        }

        @Override
        public void response(JSONObject response) throws JSONException {
            this.id = response.getInt("newId");
        }
        
        public boolean isResolved() {
            return id != null;
        }

        @Override
        public Integer get() {
            if(id == null) {
                try {
                    flush();
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
            if(id == null) {
                throw new IllegalStateException();
            }
            return id;
        }
    }
    
    private static class PendingChild {
        PendingId parentId;
        String name;

        public PendingChild(PendingId parentId, String name) {
            this.parentId = parentId;
            this.name = name;
        }
    }

    private static final int RDC = 1;

    private final Server server;
    private final Accounts accounts;
    
    private final AliasTable aliases;
    
    private UserAccount currentUser = null;
    
    private List<String> createdDatabases = Lists.newArrayList();

    private Cache<Integer, Integer> nullaryLocationCache = CacheBuilder.newBuilder().build();

    private boolean batchingEnabled = false;
    private LinkedList<Command> pendingBatch = new LinkedList<>();
    private LinkedList<PendingChild> pendingChildren = new LinkedList<>();

    
    
    @Inject
    public ApiApplicationDriver(Server server, Accounts accounts, AliasTable aliases) {
        super(aliases);
        this.server = server;
        this.accounts = accounts;
        this.aliases = aliases;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public ApiApplicationDriver setRetryCount(int retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    private WebResource commandEndpoint() {
        return root().path("command");
    }

    private WebResource root() {
        Client client = new Client();
        if(currentUser != null) {
            client.addFilter(new HTTPBasicAuthFilter(currentUser.getEmail(), currentUser.getPassword()));
        }
        return client.resource(server.getRootUrl());
    }

    /**
     * Enables batching of commands for higher performance
     */
    public void startBatch() {
        batchingEnabled = true;
    }

    @Override
    public void login() {
        currentUser = accounts.any();
    }

    @Override
    public void login(UserAccount account) {
        currentUser = account;
    }

    @Override
    public ApplicationDriver setup() {
        return this;
    }

    @Override
    public void createDatabase(TestObject map) throws Exception {

        JSONObject properties = new JSONObject();
        properties.put("name", map.getAlias());
        properties.put("countryId", 1);

        createEntityAndBindId("UserDatabase", properties);
        
        createdDatabases.add(map.getName());
    }


    @Override
    public void createForm(TestObject form) throws Exception {
        JSONObject properties = new JSONObject();
        properties.put("name", form.getAlias());
        properties.put("databaseId", form.getId("database")); 
        properties.put("locationTypeId", form.getId("locationType", queryNullaryLocationType(RDC)));
        properties.put("published", form.getInteger("published", 0)); // not published
        properties.put("classicView", form.getBoolean("classicView", true));

        switch (form.getString("reportingFrequency", "once")) {
            case "monthly":
                properties.put("reportingFrequency", 1);
                break;
            default:
                properties.put("reportingFrequency", 0);
                break;
        }
        
        createEntityAndBindId("Activity", properties);
    }
    
    @Override
    public void createField(TestObject field) throws Exception {

        if(field.getString("type").equals("enumerated")) {
            JSONObject properties = new JSONObject();
            properties.put("name", aliases.createAliasIfNotExists(field.getName()));
            properties.put("activityId", field.getId("form"));

            PendingId groupId = createEntityAndBindId("AttributeGroup", properties);

            for (String item : field.getStringList("items")) {
                
                String alias = aliases.createAliasIfNotExists(item);
        
                if(batchingEnabled) {
                    pendingChildren.add(new PendingChild(groupId, alias));
                } else {
                    createAttribute(groupId, alias);
                }
            }

        } else {
            JSONObject properties = new JSONObject();
            properties.put("name", field.getAlias());
            properties.put("activityId", field.getId("form"));
            properties.put("type", field.getString("type"));
            properties.put("units", field.getString("units", "parsects"));
            properties.put("aggregation", field.getInteger("aggregation", AggregationMethod.Sum.code()));

            // switch also server nameInExpression -> code
            properties.put("nameInExpression", field.getString("code", field.getAlias()));

            if (field.getBoolean("calculatedAutomatically", false)) {
                properties.put("calculatedAutomatically", true);
                properties.put("expression", field.getString("expression"));
            }

            createEntityAndBindId("Indicator", properties);
        }
    }

    @Override
    public void submitForm(String formName, List<FieldValue> values) throws Exception {
        submitForm(formName, values, Lists.<String>newArrayList());
    }

    @Override
    public void submitForm(String formName, List<FieldValue> values, List<String> headers) throws Exception {
        int activityId = aliases.getId(formName);

        JSONObject properties = new JSONObject();
        properties.put("activityId", activityId);
        properties.put("locationId", queryNullaryLocationType(RDC));
        properties.put("id", aliases.generateId());
        properties.put("reportingPeriodId", aliases.generateId());
        properties.put("date1", "2014-01-01");
        properties.put("date2", "2014-02-01");  
        
        for(int i = 0; i < values.size(); i++) {
            FieldValue value = values.get(i);
            switch (value.getField().toLowerCase()) {
                case "partner":
                    properties.put("partnerId", aliases.getId(value.getValue()));
                    break;
                case "project":
                    properties.put("projectId", aliases.getId(value.getValue()));
                    break;
                case "location":
                    properties.put("locationId", aliases.getId(value.getValue()));
                    break;
                case "start date":
                case "fromdate":
                    properties.put("date1", value.getValue());
                    break;
                case "end date":
                case "todate":
                    properties.put("date2", value.getValue());
                    break;
                case "comments":
                    properties.put("comments", value.getValue());
                    break;
                default:

                    if (value.getType() != null && value.getType().isPresent() && value.getType().get() == EnumType.TYPE_CLASS) {
                        for (String item : StringUtils.split(value.getValue(), ",", true)) {
                            int attributeId = aliases.getId(new AliasTable.TestHandle(item, aliases.getId(headers.get(i))));
                            properties.put("ATTRIB" + attributeId, true);
                        }
                    } else {
                        int indicatorId = aliases.getId(value.getField());
                        properties.put("I" + indicatorId, value.maybeNumberValue());
                    }
                    break;
            }
        }

        executeCreateSite(properties);
    }

    @Override
    public void submitForm(String formName, String partner, List<MonthlyFieldValue> fieldValues) throws Exception {
        int activityId = aliases.getId(formName);
        int siteId = aliases.generateId();

        JSONObject properties = new JSONObject();
        properties.put("activityId", activityId);
        properties.put("id", siteId);
        properties.put("locationId", queryNullaryLocationType(RDC));
        properties.put("partnerId", aliases.getId(partner));
        
        executeCreateSite(properties);
        
        JSONArray changes = new JSONArray();
        for (MonthlyFieldValue fieldValue : fieldValues) {
            JSONObject month = new JSONObject();
            month.put("month", fieldValue.getMonth());
            month.put("year", fieldValue.getYear());
            
            JSONObject change = new JSONObject();
            change.put("month", month);
            change.put("indicatorId", aliases.getId(fieldValue.getField()));
            change.put("value", Double.parseDouble(fieldValue.getValue()));
            changes.put(changes.length(), change);
        }
        
        JSONObject updateCommand = new JSONObject();
        updateCommand.put("changes", changes);
        updateCommand.put("siteId", siteId);
        
        executeCommand("UpdateMonthlyReports", updateCommand);
    }


    private void executeCreateSite(JSONObject properties) throws JSONException {
        JSONObject command = new JSONObject();
        command.put("properties", properties);

        executeCommand("CreateSite", command);
    }


    @Override
    public void delete(ObjectType objectType, String name) throws Exception {
        
        switch(objectType) {
            case DATABASE:
                executeDelete("UserDatabase", aliases.getId(name));
                break;
            default:
                throw new IllegalArgumentException(format("Invalid object type '%s'", objectType));
        }
    }

    public List<String> getForms(String database) throws JSONException {
        flush();

        int databaseId = aliases.getId(database);
        JSONObject schema = new JSONObject(root().path("resources/database/" + databaseId + "/schema").get(String.class));
        
        List<String> forms = Lists.newArrayList();
        JSONArray activities = schema.getJSONArray("activities");
        for(int i=0;i!=activities.length();++i) {
            forms.add(aliases.getTestHandleForAlias(activities.getJSONObject(i).getString("name")));
        }

        return forms;
    }
    
    private void executeDelete(String objectType, int objectId) throws Exception {
        JSONObject command = new JSONObject();
        command.put("entityName", objectType);
        command.put("id", objectId);

        executeCommand("Delete", command);
    }

    @Override
    public void addPartner(String partnerName, String databaseName) throws Exception {
        
        // AddPartner will be called multiple times by different members of the scenario
        // and concurrency introduces a tricky problem with concurrency here, so we 
        // have to flush the queue here and block on the call to get the id
        
        flush();
        
        int databaseId = aliases.getId(databaseName);
        String partnerAlias = aliases.createAliasIfNotExists(partnerName);

        JSONObject partner = new JSONObject();
        partner.put("name", partnerAlias);
        
        JSONObject command = new JSONObject();
        command.put("databaseId", databaseId);
        command.put("partner", partner);

        JSONObject response = new JSONObject(doCommand("AddPartner", command).getEntity(String.class));
        
        aliases.bindTestHandleToIdIfAbsent(partnerName, Suppliers.ofInstance(response.getInt("newId")));
    }

    @Override
    public void createProject(TestObject project) throws Exception {

        JSONObject properties = new JSONObject();
        properties.put("name", project.getAlias());

        JSONObject command = new JSONObject();
        command.put("databaseId", project.getId("database"));
        command.put("project", properties);

        PendingId pendingId = new PendingId();
        
        executeCommand("AddProject", command, pendingId);
        
        aliases.bindTestHandleToId(project.getName(), pendingId);
    }

    @Override
    public void grantPermission(TestObject properties) throws Exception {
        
        UserAccount email = accounts.ensureAccountExists(properties.getString("user"));
        
        JSONObject model = new JSONObject();
        model.put("name", "A User");
        model.put("email", email.getEmail());
        model.put("partnerId", aliases.getId(properties.getString("partner")));
        
        for(String permission : properties.getStringList("permissions")) {
            switch (permission) {
                case "View":
                    model.put("allowView", true);
                    break;
                case "View All":
                    model.put("allowViewAll", true);
                    break;
                case "Edit":
                    model.put("allowEdit", true);
                    break;
                case "Edit All":
                    model.put("allowEditAll", true);
                    break;
                case "Manage Users":
                    model.put("allowManageUsers", true);
                    break;
                case "Manage All Users":
                    model.put("allowManageAllUsers", true);
                    break;
                case "Design":
                    model.put("allowDesign", true);
                    break;
            }
        }
        
        JSONObject command = new JSONObject();
        command.put("databaseId", aliases.getId(properties.getString("database")));
        command.put("model", model);

        LOGGER.fine(format("Granting access to database '%s' [%d] to %s [%s] within %s [%d]",
                properties.getString("database"), command.getInt("databaseId"),
                properties.getString("user"), email.getEmail(),
                properties.getString("partner"), model.getInt("partnerId")));
        
        executeCommand("UpdateUserPermissions", command);
    }

    @Override
    public void cleanup() throws Exception {
    }

    @Override
    public void createLocationType(TestObject locationType) throws Exception {
        
        JSONObject properties = new JSONObject();
        properties.put("name", locationType.getAlias());
        properties.put("databaseId", locationType.getId("database"));
        properties.put("workflowId", locationType.getString("workflowId", "closed"));
        createEntityAndBindId("LocationType", properties);
    }

    @Override
    public void createLocation(TestObject map) throws Exception {
        
        JSONObject properties = new JSONObject();
        properties.put("id", aliases.generateIdFor(map.getName()));
        properties.put("name", map.getAlias());
        properties.put("axe", map.getString("code"));
        properties.put("locationTypeId", map.getId("locationType"));
        
        JSONObject command = new JSONObject();
        command.put("properties", properties);
        
        executeCommand("CreateLocation", command);
    }

    @Override
    public void createTarget(TestObject properties) throws Exception {

        JSONObject target = new JSONObject();
        target.put("name", properties.getAlias());
        target.put("fromDate", properties.getDate("fromDate", new LocalDate(1900, 1, 1)));
        target.put("toDate", properties.getDate("toDate", new LocalDate(2050, 1, 1)));

        JSONObject addTarget = new JSONObject();
        addTarget.put("databaseId", properties.getId("database"));
        addTarget.put("target", target);

        PendingId pendingId = new PendingId();
        
        executeCommand("AddTarget", addTarget, pendingId);
        
        aliases.bindAliasToId(properties.getAlias(), pendingId);
    }

    @Override
    public void setTargetValues(String targetName, List<FieldValue> values) throws Exception {
  
        for(FieldValue value : values) {

            JSONObject changes = new JSONObject();
            changes.put("value", value.asDouble());
            
            JSONObject command = new JSONObject();
            command.put("targetId", aliases.getId(targetName));
            command.put("indicatorId", aliases.getId(value.getField()));
            command.put("changes", changes);
            
            executeCommand("UpdateTargetValue", command);
        }
    }

    @Override
    public File exportForm(String formName) throws Exception {
        return export("filter=Activity+" + aliases.getId(formName));
    }

    @Override
    public File exportDatabase(String databaseName) throws Exception {
        return export("filter=Database+" + aliases.getId(databaseName));
    }

    private File export(String exportModel) throws Exception {
        WebResource root = root();
        String id = root.path("ActivityInfo").path("export")
                .entity(exportModel, MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                .post(String.class);

        Stopwatch stopwatch = Stopwatch.createStarted();
        String downloadUri;
        while(true) {
            JSONObject status = new JSONObject(root.path("generated").path("status").path(id).get(String.class));
            if(status.has("downloadUri")) {
                downloadUri = status.getString("downloadUri");
                break;
            }
            if(stopwatch.elapsed(TimeUnit.MINUTES) > 5) {
                throw new AssertionError("Download timed out.");
            }
            
            Thread.sleep(1000);
        }

        File file = File.createTempFile("export", ".xls");
        try(InputStream inputStream = new URI(downloadUri).toURL().openStream()) {
            ByteStreams.copy(inputStream, Files.asByteSink(file));
        }

        return file;
    }

    private PendingId createEntityAndBindId(String entityType, JSONObject properties) throws JSONException {

        PendingId pendingId = createEntity(entityType, properties);

        aliases.bindAliasToId(properties.getString("name"), pendingId);
        
        return pendingId;
    }

    private PendingId createEntity(String entityType, JSONObject properties) throws JSONException {
        JSONObject object = new JSONObject();
        object.put("entityName", entityType);
        object.put("properties", properties);

        if(properties.has("name")) {
            aliases.getTestHandleForAlias(properties.getString("name"));
        }
        
        PendingId pendingId = new PendingId();

        executeCommand("CreateEntity", object, pendingId);
        return pendingId;
    }

    private int queryNullaryLocationType(final int countryId) throws ExecutionException {
    
        return nullaryLocationCache.get(countryId, new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {

                String json = root().path("resources")
                        .path("country").path(Integer.toString(countryId))
                        .path("locationTypes")
                        .get(String.class);

                try {
                    JSONArray array = new JSONArray(json);

                    for (int i = 0; i != array.length(); ++i) {
                        JSONObject locationType = array.getJSONObject(i);
                        if (locationType.getString("name").equals("Country")) {
                            return locationType.getInt("id");
                        }
                    }
                } catch (JSONException e) {
                    throw new RuntimeException("Exception parsing locationType list", e);
                }

                throw new IllegalStateException(format("Country %d has no nullary location type: expected" +
                        " location type with name 'Country'", countryId));
            }
        });
    }


    private void executeCommand(String type, JSONObject command) throws JSONException {
        executeCommand(type, command, null);
    }

    private void executeCommand(String type, JSONObject command, ResponseHandler handler) throws JSONException {
        JSONObject request = new JSONObject();
        request.put("type", type);
        request.put("command", command);

        Command pendingCommand = new Command(request, handler);

        if(batchingEnabled) {

            pendingBatch.add(pendingCommand);
            if(!flushing && pendingBatch.size() > 1000) {
                flush();
            }
        } else {
            executeImmediately(pendingCommand, retryCount);
        }
    }
    
    private void executeImmediately(Command command, int retriesRemaining) throws JSONException {

        ClientResponse response = doCommand(command.request, retriesRemaining);
        if(command.handler != null) {
            command.handler.response(new JSONObject(response.getEntity(String.class)));
        }
    }
    
    private ClientResponse doCommand(String type, JSONObject command) throws Exception {
        JSONObject request = new JSONObject();
        request.put("type", type);
        request.put("command", command);
        
        return doCommand(request, getRetryCount());

    }
    
    private ClientResponse doCommand(JSONObject json, int retriesRemaining) throws JSONException {
        COMMAND_RATE.mark();
        ERROR_RATE.markSubmitted();

        ClientResponse response;
        try {
            response = commandEndpoint()
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .post(ClientResponse.class, json.toString());
        } catch (ClientHandlerException e) {
            LOGGER.fine(format("Command %s failed: %s", json.getString("type"), e.getMessage()));
            ERROR_RATE.markError();

            if(retriesRemaining > 0) {
                backoff();
                return doCommand(json, retriesRemaining-1);
            }
            
            throw new RuntimeException(format("Command %s failed: %s", json.getString("type"), e.getMessage()));
        }
        
        ClientResponse.Status status = response.getClientResponseStatus();

        if (status.getFamily() == Response.Status.Family.SUCCESSFUL) {
            return response;

        } else {
            ERROR_RATE.markError();

            LOGGER.fine(format("Command %s failed: %d %s",
                    json.getString("type"), status.getStatusCode(), status.getReasonPhrase()));
            
            if (retriesRemaining > 0 &&
                    status == ClientResponse.Status.INTERNAL_SERVER_ERROR ||
                    status == ClientResponse.Status.SERVICE_UNAVAILABLE) {
                return doCommand(json, retriesRemaining-1);
            }
            
        
            String message = status.getStatusCode() + " " + status.getReasonPhrase();
            if(MediaType.TEXT_PLAIN_TYPE.equals(response.getType())) {
                message += ": " + response.getEntity(String.class);
            }
            
            throw new RuntimeException(message);
        }
    }

    private void backoff() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 2000));
        } catch (InterruptedException ignored) {
            throw new RuntimeException("Interrupted");
        }
    }


    public List<SyncRegion> getSyncRegions() throws Exception {
        Preconditions.checkState(currentUser != null, "Authentication required");
        
        flush();

        JSONObject request = new JSONObject();
        request.put("type", "GetSyncRegions");
        request.put("command", new JSONObject());
        
        String responseJson = doCommand(request, getRetryCount()).getEntity(String.class);

        JSONObject response = new JSONObject(responseJson);
        
        JSONArray array = response.getJSONArray("list");
        List<SyncRegion> regions = Lists.newArrayList();
        for(int i=0;i<array.length();++i) {
            regions.add(new SyncRegion(
                    array.getJSONObject(i).getString("id"),
                    array.getJSONObject(i).getString("currentVersion")));
        }
        
        return regions;
    }
    
    public SyncUpdate fetchSyncRegion(String id, String version) throws Exception {
        Preconditions.checkState(currentUser != null, "Authentication required");

        JSONObject command = new JSONObject();
        command.put("regionPath", id);
        
        if(version != null) {
            command.put("localVersion", version);
        }
        
        JSONObject request = new JSONObject();
        request.put("type", "GetSyncRegionUpdates");
        request.put("command", command);

        ClientResponse response = doCommand(request, getRetryCount());
        
        // Read response to exercise server.. not sure if necessary
        String json = response.getEntity(String.class);
        JSONObject update = new JSONObject(json);
        
        SyncUpdate syncUpdate = new SyncUpdate();
        syncUpdate.setByteCount(json.length());
        syncUpdate.setComplete(update.getBoolean("complete"));
        syncUpdate.setVersion(update.getString("version"));
        
        return syncUpdate;
    }
    
    public void flush() throws JSONException {

        Preconditions.checkState(!flushing, "Re-entrant flushing");


        this.flushing = true;
        try {
            if (!pendingBatch.isEmpty()) {

                List<JSONObject> requests = new ArrayList<>();
                for (Command command : pendingBatch) {
                    requests.add(command.request);
                }

                JSONObject command = new JSONObject();
                command.put("commands", requests);

                JSONObject request = new JSONObject();
                request.put("type", "BatchCommand");
                request.put("command", command);


                String json = doCommand(request, getRetryCount()).getEntity(String.class);

                JSONObject response = new JSONObject(json);
                JSONArray results = response.getJSONArray("results");

                for (int i = 0; i != results.length(); ++i) {
                    ResponseHandler responseHandler = pendingBatch.get(i).handler;
                    if (responseHandler != null) {
                        responseHandler.response(results.getJSONObject(i));
                    }
                }

                pendingBatch.clear();

            }

            Iterator<PendingChild> childIt = pendingChildren.listIterator();
            while (childIt.hasNext()) {
                PendingChild child = childIt.next();
                if (child.parentId.isResolved()) {
                    createAttribute(child.parentId, child.name);
                    childIt.remove();

                }
            }
        } finally {
            flushing = false;
        }
    }
    
    
    private void createAttribute(PendingId parentId, String name) throws JSONException {
        JSONObject itemProperties = new JSONObject();
        itemProperties.put("name", name);
        itemProperties.put("attributeGroupId", parentId.get());

        PendingId pendingId = createEntity("Attribute", itemProperties);
        String testHandle = aliases.getTestHandleForAlias(name);

        aliases.bindTestHandleToId(new AliasTable.TestHandle(testHandle, parentId.get()), pendingId);
    }


    public void submitBatch() throws JSONException {
        flush();
        batchingEnabled = false;
    }
}
