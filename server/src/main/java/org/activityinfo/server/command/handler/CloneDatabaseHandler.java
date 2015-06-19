package org.activityinfo.server.command.handler;
/*
 * #%L
 * ActivityInfo Server
 * %%
 * Copyright (C) 2009 - 2013 UNICEF
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.api.client.util.Strings;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.activityinfo.legacy.shared.command.CloneDatabase;
import org.activityinfo.legacy.shared.command.GetFormClass;
import org.activityinfo.legacy.shared.command.UpdateFormClass;
import org.activityinfo.legacy.shared.command.result.CreateResult;
import org.activityinfo.legacy.shared.command.result.FormClassResult;
import org.activityinfo.legacy.shared.command.result.VoidResult;
import org.activityinfo.legacy.shared.exception.IllegalAccessCommandException;
import org.activityinfo.legacy.shared.impl.CommandHandlerAsync;
import org.activityinfo.legacy.shared.impl.ExecutionContext;
import org.activityinfo.model.form.*;
import org.activityinfo.model.legacy.CuidAdapter;
import org.activityinfo.model.legacy.KeyGenerator;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.type.FieldType;
import org.activityinfo.model.type.ParametrizedFieldType;
import org.activityinfo.model.type.ReferenceType;
import org.activityinfo.model.type.enumerated.EnumItem;
import org.activityinfo.model.type.enumerated.EnumType;
import org.activityinfo.model.type.expr.CalculatedFieldType;
import org.activityinfo.model.type.number.QuantityType;
import org.activityinfo.model.type.time.LocalDateType;
import org.activityinfo.promise.Promise;
import org.activityinfo.server.database.hibernate.entity.*;
import org.activityinfo.server.endpoint.gwtrpc.RemoteExecutionContext;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.activityinfo.model.legacy.CuidAdapter.BUILTIN_FIELDS;

/**
 * @author yuriyz on 11/17/2014.
 */
public class CloneDatabaseHandler implements CommandHandlerAsync<CloneDatabase, CreateResult> {

    private static final Logger LOGGER = Logger.getLogger(CloneDatabaseHandler.class.getName());

    private final EntityManager em;
    private final PermissionOracle permissionOracle;
    private final KeyGenerator generator = new KeyGenerator();

    // Mappings old id (source db) -> new id (target/newly created db)
    private final Map<Integer, Partner> partnerMapping = Maps.newHashMap();
    private final Map<Integer, Activity> activityMapping = Maps.newHashMap();
    private final Map<Integer, AttributeGroup> attributeGroupMapping = Maps.newHashMap();

    private CloneDatabase command;
    private UserDatabase targetDb;
    private UserDatabase sourceDb;

    @Inject
    public CloneDatabaseHandler(Injector injector) {
        this.em = injector.getInstance(EntityManager.class);
        this.permissionOracle = injector.getInstance(PermissionOracle.class);
    }

    @Override
    public void execute(CloneDatabase command, ExecutionContext context, final AsyncCallback<CreateResult> callback) {

        final User user = ((RemoteExecutionContext) context).retrieveUserEntity();

        this.command = command;
        this.targetDb = createDatabase(command, user);
        this.sourceDb = em.find(UserDatabase.class, command.getSourceDatabaseId());

        if (!permissionOracle.isViewAllowed(sourceDb, user)) {
            throw new IllegalAccessCommandException();
        }

        // 1. copy partners and keep mapping between old and new partners
        if (command.isCopyPartners() || command.isCopyUserPermissions()) {
            copyPartners();
        }

        // 2. copy user permissions : without design privileges the user shouldn't be able to see the list of users.
        if (command.isCopyUserPermissions() && permissionOracle.isDesignAllowed(sourceDb, user)) {
            copyUserPermissions();
        }

        List<Promise<Void>> promises = new ArrayList<>();

        // 3. copy forms and form data
        promises.add(copyFormData(context));


        Promise.waitAll(promises).then(new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }

            @Override
            public void onSuccess(Void result) {
                callback.onSuccess(new CreateResult(targetDb.getId()));
            }
        });
    }

    private void copyUserPermissions() {

        for (UserPermission sourcePermission : sourceDb.getUserPermissions()) {
            UserPermission newPermission = new UserPermission(sourcePermission);
            newPermission.setDatabase(targetDb);
            newPermission.setLastSchemaUpdate(new Date());

            // set newly created partner
            if (sourcePermission.getPartner() != null) {
                Partner targetPartner = partnerMapping.get(sourcePermission.getPartner().getId());
                newPermission.setPartner(targetPartner != null ? targetPartner : null);
            }

            em.persist(newPermission);

        }
    }

    private void copyPartners() {
        for (Partner partner : sourceDb.getPartners()) {
            Partner newPartner = new Partner();
            newPartner.setName(partner.getName());
            newPartner.setFullName(partner.getFullName());

            em.persist(newPartner);

            partnerMapping.put(partner.getId(), newPartner);
            targetDb.getPartners().add(newPartner);
        }

        targetDb.setLastSchemaUpdate(new Date());
        em.persist(targetDb);
    }

    private Promise<Void> copyFormData(final ExecutionContext context) {

        // first copy all activities without payload (indicators, attributes)
        for (Activity activity : sourceDb.getActivities()) {
            copyActivity(activity);
        }

        final List<Promise<VoidResult>> copyPromises = new ArrayList<>();
        for (Activity activity : sourceDb.getActivities()) {
            final ResourceId sourceFormClass = CuidAdapter.activityFormClass(activity.getId());
            final ResourceId targetFormClass = CuidAdapter.activityFormClass(activityMapping.get(activity.getId()).getId());

            // copy form class
            copyPromises.add(copyFormClass(context, sourceFormClass, targetFormClass));

            // if the new countryId of the target database is different than the countryId of sourceDatabase,
            // copyData must be false -> skip data copy
            if (command.isCopyData() && sourceDb.getCountry().getId() == targetDb.getCountry().getId()) {
                // site form instances
                // todo : commenting it temporary until we have nice idea how to implement it in scalable manner. (AI-787)
                // copyPromises.add(copySiteFormInstances(context, activity, newActivity));
            }
        }
        return Promise.waitAll(copyPromises);
    }

//    private Promise<VoidResult> copySiteFormInstances(final ExecutionContext context, Activity sourceActivity, final Activity targetActivity) {
//        Filter filter = new Filter();
//        filter.addRestriction(DimensionType.Activity, sourceActivity.getId());
//
//        GetSites query = new GetSites();
//        query.setFilter(filter);
//
//        final Promise<ActivityFormDTO> activityForm = new Promise<>();
//        context.execute(new GetActivityForm(sourceActivity.getId()), activityForm);
//
//        final Promise<SiteResult> fetchSitesPromise = new Promise<>();
//        context.execute(query, fetchSitesPromise);
//
//        return Promise.waitAll(activityForm, fetchSitesPromise).join(new Function<Void, Promise<VoidResult>>() {
//            @Nullable
//            @Override
//            public Promise<VoidResult> apply(@Nullable Void input) {
//
//                for (SiteDTO site : fetchSitesPromise.get().getData()) {
//                    SiteBinding binding = new SiteBinding(activityForm.get());
//
//                    // adapt id and classId to targetActivity
//                    FormInstance formInstance = binding.newInstance(site)
//                            .setId(CuidAdapter.cuid(CuidAdapter.SITE_DOMAIN, targetActivity.getId()))
//                            .setClassId(CuidAdapter.activityFormClass(targetActivity.getId()));
//
//                    // persist
//                    new SitePersister(new DispatchAdapter(context)).persist(formInstance);
//                }
//                return Promise.resolved(VoidResult.INSTANCE);
//            }
//        });
//    }

    private Promise<VoidResult> copyFormClass(final ExecutionContext context, final ResourceId sourceFormClassId, final ResourceId targetFormClassId) {
        final Promise<VoidResult> promise = new Promise<>();
        context.execute(new GetFormClass(sourceFormClassId), new AsyncCallback<FormClassResult>() {
            @Override
            public void onFailure(Throwable caught) {
                LOGGER.log(Level.SEVERE, caught.getMessage(), caught);
                promise.onFailure(caught);
            }

            @Override
            public void onSuccess(FormClassResult sourceFormClass) {
                FormClass targetFormClass = cloneFormClass(sourceFormClass.getFormClass(), new FormClass(targetFormClassId));

                context.execute(new UpdateFormClass(targetFormClass), promise);
            }
        });
        return promise;
    }

    private FormClass cloneFormClass(FormClass sourceFormClass, FormClass targetFormClass) {
        targetFormClass.setLabel(sourceFormClass.getLabel());
        targetFormClass.setDescription(sourceFormClass.getDescription());
        targetFormClass.setParentId(CuidAdapter.databaseId(targetDb.getId()));

        copyFormElements(sourceFormClass, targetFormClass, sourceFormClass.getId(), targetFormClass.getId());

        return targetFormClass;
    }

    private void copyFormElements(FormElementContainer sourceContainer, FormElementContainer targetContainer, ResourceId sourceClassId, ResourceId targetClassId) {
        for (FormElement element : sourceContainer.getElements()) {
            if (element instanceof FormSection) {
                FormSection sourceSection = (FormSection) element;
                FormSection targetSection = new FormSection(ResourceId.generateId());
                targetSection.setLabel(sourceSection.getLabel());

                targetContainer.addElement(targetSection);

                copyFormElements(sourceSection, targetSection, sourceClassId, targetClassId);
            } else if (element instanceof FormField) {
                FormField sourceField = (FormField) element;
                FormField targetField = new FormField(targetFieldId(sourceField, sourceClassId, targetClassId));

                targetField.setType(targetFieldType(sourceField));
                targetField.setCode(sourceField.getCode());
                targetField.setRelevanceConditionExpression(sourceField.getRelevanceConditionExpression());
                targetField.setLabel(sourceField.getLabel());
                targetField.setDescription(sourceField.getDescription());
                targetField.setReadOnly(sourceField.isReadOnly());
                targetField.setRequired(sourceField.isRequired());
                targetField.setSuperProperties(sourceField.getSuperProperties());

                targetContainer.addElement(targetField);
            } else {
                throw new RuntimeException("Unsupported FormElement : " + element);
            }
        }
    }

    private FieldType targetFieldType(FormField sourceField) {
        FieldType fieldType = sourceField.getType();

        if (!(fieldType instanceof ParametrizedFieldType)) {
            return fieldType;
        }

        if (fieldType instanceof QuantityType ||
                fieldType instanceof CalculatedFieldType ||
                fieldType instanceof LocalDateType) {
            return fieldType;
        }

        if (fieldType instanceof EnumType) {
            if (sourceField.getId().getDomain() == CuidAdapter.ATTRIBUTE_GROUP_FIELD_DOMAIN) {
                EnumType sourceEnumType = (EnumType) fieldType;
                List<EnumItem> targetValues = Lists.newArrayList();

                for (EnumItem sourceValue : sourceEnumType.getValues()) {

                    ResourceId targetValueId = CuidAdapter.cuid(sourceValue.getId().getDomain(), generator.generateInt());
                    targetValues.add(new EnumItem(targetValueId, sourceValue.getLabel()));
                }
                return new EnumType(sourceEnumType.getCardinality(), targetValues);
            }
        }

        if (fieldType instanceof ReferenceType) {
            ReferenceType sourceType = (ReferenceType) fieldType;

            Set<ResourceId> sourceRange = sourceType.getRange();
            Set<ResourceId> targetRange = new HashSet<>();

            switch (sourceRange.iterator().next().getDomain()) {
                case CuidAdapter.PARTNER_FORM_CLASS_DOMAIN:
                    if (command.isCopyPartners()) {

                        // as defined in ActivityFormClassBuilder.build() in reference range we stick to db id
                        targetRange.add(CuidAdapter.partnerFormClass(targetDb.getId()));
                    }
                    break;
                case CuidAdapter.PROJECT_CLASS_DOMAIN:
                    targetRange.add(CuidAdapter.projectFormClass(targetDb.getId()));
                    break;
            }


            // fallback to source targetRange
            if (targetRange.isEmpty()) {
                targetRange = sourceRange;
            }

            return new ReferenceType()
                    .setCardinality(sourceType.getCardinality())
                    .setRange(targetRange);
        }

        throw new RuntimeException("Unable to generate field id for fieldType : " + fieldType);

    }

    private ResourceId targetFieldId(FormField sourceField, ResourceId sourceClassId, ResourceId targetClassId) {
        ResourceId sourceFieldId = sourceField.getId();
        for (int fieldIndex : BUILTIN_FIELDS) {
            if (sourceFieldId.equals(CuidAdapter.field(sourceClassId, fieldIndex))) {
                return CuidAdapter.field(targetClassId, fieldIndex);
            }
        }

        return CuidAdapter.cuid(sourceField.getId().getDomain(), generator.generateInt());
    }

    private Activity copyActivity(Activity sourceActivity) {
        Activity newActivity = new Activity(sourceActivity); // copy simple values : like name, category (but not Indicators, Attributes)
        newActivity.getAttributeGroups().clear();
        newActivity.getLockedPeriods().clear();
        newActivity.getIndicators().clear();

        // target db
        newActivity.setDatabase(targetDb);

        setLocationTypeForNewActivity(sourceActivity, newActivity);

        em.persist(newActivity); // persist to get id of new activity
        activityMapping.put(sourceActivity.getId(), newActivity);

        return newActivity;
    }

    private void setLocationTypeForNewActivity(Activity sourceActivity, Activity newActivity) {
        // location type -> change it only if sourceCountry != targetCountry
        if (sourceActivity.getLocationType() != null && sourceDb.getCountry().getId() != targetDb.getCountry().getId()) {

            boolean locationTypeCreated = false;

            //1. If there is a location type with the same name in the new country, use that location Type
            String sourceName = sourceActivity.getLocationType().getName();
            if (!Strings.isNullOrEmpty(sourceName)) {
                List<LocationType> locationTypes = em.createQuery("SELECT d FROM LocationType d WHERE Name = :activityName AND CountryId = :countryId")
                        .setParameter("activityName", sourceName)
                        .setParameter("countryId", targetDb.getCountry().getId())
                        .getResultList();
                if (!locationTypes.isEmpty()) {
                    newActivity.setLocationType(locationTypes.get(0));
                    locationTypeCreated = true;
                }
            }

            //2. if the source locationtype is bound to an adminlevel, choose the first root adminlevel in the new country
            if (!locationTypeCreated && sourceActivity.getLocationType().getBoundAdminLevel() != null) {
                List<LocationType> locationTypes = em.createQuery("SELECT d FROM LocationType d WHERE CountryId = :countryId")
                        .setParameter("countryId", targetDb.getCountry().getId())
                        .getResultList();
                if (!locationTypes.isEmpty()) {
                    newActivity.setLocationType(locationTypes.get(0));
                    locationTypeCreated = true;
                }
            }

            //3. Otherwise create new location type in the target country.
            if (!locationTypeCreated) {
                LocationType newLocationType = new LocationType();
                newLocationType.setName(sourceActivity.getLocationType().getName());
                newLocationType.setCountry(targetDb.getCountry());
                newLocationType.setWorkflowId(sourceActivity.getLocationType().getWorkflowId());
                newLocationType.setReuse(sourceActivity.getLocationType().isReuse());

                em.persist(newLocationType);

                newActivity.setLocationType(newLocationType);
            }
        }
    }

    private UserDatabase createDatabase(CloneDatabase command, User user) {
        UserDatabase db = new UserDatabase();
        db.setName(command.getName());
        db.setFullName(command.getDescription());
        db.setCountry(em.find(Country.class, command.getCountryId()));
        db.setOwner(user);

        em.persist(db);
        return db;
    }
}
