package org.activityinfo.server.endpoint.jsonrpc;

import com.bedatadriven.rebar.time.calendar.LocalDate;
import com.extjs.gxt.ui.client.data.RpcMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.activityinfo.legacy.shared.command.Command;
import org.activityinfo.legacy.shared.command.result.CommandResult;
import org.activityinfo.legacy.shared.command.result.HttpStatusCode;
import org.activityinfo.legacy.shared.command.result.VoidResult;
import org.activityinfo.legacy.shared.exception.CommandException;
import org.activityinfo.legacy.shared.exception.IllegalAccessCommandException;
import org.activityinfo.model.auth.AuthenticatedUser;
import org.activityinfo.server.command.DispatcherSync;
import org.activityinfo.server.util.monitoring.Count;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.module.SimpleModule;

import javax.inject.Provider;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Path("/command")
public class JsonRpcServlet {

    private static final Logger LOGGER = Logger.getLogger(JsonRpcServlet.class.getName());

    private final DispatcherSync dispatcher;
    private final ObjectMapper objectMapper;
    private final CommandValidator validator = new CommandValidator();
    
    private final Provider<AuthenticatedUser> user;

    @Inject
    public JsonRpcServlet(DispatcherSync dispatcher, Provider<AuthenticatedUser> user) {
        this.dispatcher = dispatcher;
        this.user = user;

        SimpleModule module = new SimpleModule("Command", new Version(1, 0, 0, null));
        module.addDeserializer(Command.class, new CommandDeserializer());
        module.addDeserializer(RpcMap.class, new RpcMapDeserializer());
        module.addSerializer(LocalDate.class, new LocalDateSerializer());
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer());

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(module);

        // to ensure that VoidResult is handled without error
        objectMapper.disable(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS);
        
        // Don't write out 'null' properties
        objectMapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);

    }
    
    @POST
    @Count("api.rpc")
    public Response execute(String json) {
        
        // All RPC Commands require authentication
        if(user.get().isAnonymous()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        
        Command command = deserialize(json);
        CommandResult result = execute(command);
        return serializeResult(result);
    }

    private Command deserialize(String json) {
        Command command;
        try {
            command = objectMapper.readValue(json, Command.class);
        } catch (BadRpcRequest e) {
            LOGGER.log(Level.SEVERE, "Failed to deserialize command", e);
            throw e;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to deserialize command", e);
            throw new BadRpcRequest("Unexpected exception deserializing the command.");
        }
        
        validator.assertValid(command);
        
        return command;
    }

    private CommandResult execute(Command command) {
        CommandResult result;
        try {
            result = dispatcher.execute(command);
            
        } catch (IllegalAccessCommandException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            throw new WebApplicationException(Response.Status.FORBIDDEN);
            
        } catch (CommandException e) {
            LOGGER.log(Level.SEVERE, "Command exception", e);
            throw new BadRpcRequest(e.getMessage());
        }
        return result;
    }

    private Response serializeResult(CommandResult result) {
        if(result == null || result instanceof VoidResult) {
            
            // No content for VoidResult and friends
            return Response.noContent().build();

        } else {
            
            // Serialize CommandResult object as JSON
            try {
                return Response.status(statusForCommandResult(result))
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .entity(objectMapper.writeValueAsString(result))
                        .build();
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Command exception", e);
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
            }
        }
    }


    private int statusForCommandResult(CommandResult result) {
        HttpStatusCode code = result.getClass().getAnnotation(HttpStatusCode.class);
        if(code != null) {
            return code.value().getStatusCode();
        }
        
        return Response.Status.OK.getStatusCode();
    }
}
