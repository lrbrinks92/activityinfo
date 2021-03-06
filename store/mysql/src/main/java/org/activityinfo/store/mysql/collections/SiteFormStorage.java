package org.activityinfo.store.mysql.collections;

import com.google.common.base.Optional;
import com.googlecode.objectify.VoidWork;
import com.vividsolutions.jts.geom.Geometry;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.model.form.FormRecord;
import org.activityinfo.model.legacy.CuidAdapter;
import org.activityinfo.model.resource.RecordUpdate;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.type.FieldValue;
import org.activityinfo.model.type.RecordRef;
import org.activityinfo.model.type.ReferenceValue;
import org.activityinfo.service.store.*;
import org.activityinfo.store.hrd.entity.FormEntity;
import org.activityinfo.store.hrd.entity.FormRecordEntity;
import org.activityinfo.store.hrd.entity.FormRecordSnapshotEntity;
import org.activityinfo.store.hrd.op.QueryVersions;
import org.activityinfo.store.mysql.cursor.QueryExecutor;
import org.activityinfo.store.mysql.cursor.RecordFetcher;
import org.activityinfo.store.mysql.mapping.TableMapping;
import org.activityinfo.store.mysql.metadata.Activity;
import org.activityinfo.store.mysql.metadata.PermissionsCache;
import org.activityinfo.store.mysql.metadata.UserPermission;
import org.activityinfo.store.mysql.update.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Collection of Sites
 */
public class SiteFormStorage implements FormStorage {
    
    private final Activity activity;
    private final TableMapping baseMapping;
    private final QueryExecutor queryExecutor;
    private final PermissionsCache permissionsCache;

    public SiteFormStorage(Activity activity, TableMapping baseMapping,
                           QueryExecutor queryExecutor,
                           PermissionsCache permissionsCache) {
        this.activity = activity;
        this.baseMapping = baseMapping;
        this.queryExecutor = queryExecutor;
        this.permissionsCache = permissionsCache;
    }

    @Override
    public FormPermissions getPermissions(int userId) {
        if(activity.getOwnerUserId() == userId) {
           return FormPermissions.full(); 
        } else {

            UserPermission databasePermission = permissionsCache.getPermission(userId, activity.getDatabaseId());

            FormPermissions permissions = new FormPermissions();

            String partnerFilter = String.format("%s=%s",
                    CuidAdapter.partnerField(activity.getId()),
                    CuidAdapter.partnerRecordId(databasePermission.getPartnerId()));

            if(databasePermission.isViewAll()) {
                permissions.setVisible(true);

            } else if(databasePermission.isView()) {
                permissions.setVisible(true);
                permissions.setVisibilityFilter(partnerFilter);

            }
            if(databasePermission.isEditAll()) {
                permissions.setEditAllowed(true);
            } else if(databasePermission.isEdit()) {
                permissions.setEditAllowed(true);
                permissions.setEditFilter(partnerFilter);
            }
       
            // published property of activity overrides user permissions
            if(activity.isPublished()) {
                permissions.setVisible(true);
                permissions.setVisibilityFilter(null);
            }

            return permissions;

        }
    }

    @Override
    public Optional<FormRecord> get(ResourceId resourceId) {
        RecordFetcher fetcher = new RecordFetcher(this);
        return fetcher.get(resourceId);
    }

    @Override
    public List<RecordVersion> getVersions(ResourceId recordId) {

        List<RecordVersion> versions = new ArrayList<>();
        
        // Read first from legacy sitehistory table
        SiteHistoryReader tableReader = new SiteHistoryReader(queryExecutor, activity, getFormClass(),
                CuidAdapter.getLegacyIdFromCuid(recordId));
        try {
            versions.addAll(tableReader.read());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        
        // Now read additional entries from HRD
        versions.addAll(ofy().transact(QueryVersions.of(getFormClass(), recordId)));
        
        return versions;
    }

    @Override
    public List<RecordVersion> getVersionsForParent(ResourceId parentRecordId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FormClass getFormClass() {
        FormClass formClass = baseMapping.getFormClass();
        return formClass;
    }

    @Override
    public void updateFormClass(FormClass formClass) {
        ActivityUpdater updater = new ActivityUpdater(activity, queryExecutor);
        updater.update(formClass);
    }

    @Override
    public void add(RecordUpdate update) {
        ResourceId formClassId = getFormClass().getId();
        BaseTableInserter baseTable = new BaseTableInserter(baseMapping, update.getRecordId());
        baseTable.addValue("ActivityId", activity.getId());
        baseTable.addValue("DateCreated", new Date());
        baseTable.addValue("DateEdited", new Date());

        if(!activity.hasLocationType()) {
            baseTable.addValue("locationId", activity.getNullaryLocationId());
        }
        
        IndicatorValueTableUpdater indicatorValues = new IndicatorValueTableUpdater(update.getRecordId());
        AttributeValueTableUpdater attributeValues = new AttributeValueTableUpdater(activity, update.getRecordId());

        for (Map.Entry<ResourceId, FieldValue> change : update.getChangedFieldValues().entrySet()) {
            if(change.getKey().getDomain() == CuidAdapter.INDICATOR_DOMAIN) {
                indicatorValues.update(change.getKey(), change.getValue());
            } else if(change.getKey().getDomain() == CuidAdapter.ATTRIBUTE_GROUP_FIELD_DOMAIN) {
                attributeValues.update(change.getKey(), change.getValue());
            } else if(change.getKey().equals(CuidAdapter.locationField(activity.getId()))) {
                ReferenceValue value = (ReferenceValue) change.getValue();
                if(value.getOnlyReference().getRecordId().getDomain() == CuidAdapter.LOCATION_DOMAIN) {
                    baseTable.set(change.getKey(), change.getValue());
                } else {
                    baseTable.set(change.getKey(), dummyLocationReference(value.getOnlyReference()));
                }
            } else {
                baseTable.set(change.getKey(), change.getValue());
            }
            if(change.getKey().equals(CuidAdapter.field(formClassId, CuidAdapter.START_DATE_FIELD))) {
                indicatorValues.setDate1(change.getValue());
            } else if(change.getKey().equals(CuidAdapter.field(formClassId, CuidAdapter.END_DATE_FIELD))) {
                indicatorValues.setDate2(change.getValue());
            }
        }
        long newVersion = incrementSiteVersion();
        baseTable.executeInsert(queryExecutor);
        attributeValues.executeUpdates(queryExecutor);
        indicatorValues.insert(queryExecutor);
        
        // Write the snapshot to HRD as a first step in the transition
        dualWriteToHrd(RecordChangeType.CREATED, update, newVersion, update.getChangedFieldValues());
    }

    private void dualWriteToHrd(final RecordChangeType changeType, final RecordUpdate update, final long newVersion, final Map<ResourceId, FieldValue> values) {

        ofy().transact(new VoidWork() {
            @Override
            public void vrun() {

                FormEntity rootEntity = new FormEntity();
                rootEntity.setId(activity.getSiteFormClassId());
                rootEntity.setVersion(activity.getVersion());
                rootEntity.setSchemaVersion(activity.getActivityVersion().getSchemaVersion());

                FormRecordEntity recordEntity = new FormRecordEntity(activity.getSiteFormClassId(), update.getRecordId());
                recordEntity.setVersion(newVersion);
                recordEntity.setSchemaVersion(activity.getActivityVersion().getSchemaVersion());
                recordEntity.setFieldValues(getFormClass(), values);

                FormRecordSnapshotEntity snapshot = new FormRecordSnapshotEntity(update.getUserId(), changeType, recordEntity);

                if (changeType == RecordChangeType.DELETED) {
                    ofy().save().entities(rootEntity, snapshot);
                    ofy().delete().entities(recordEntity);
                } else {
                    ofy().save().entities(rootEntity, recordEntity, snapshot);
                }
            }
        });
    }


    private FieldValue dummyLocationReference(RecordRef ref)  {
        if(activity.getAdminLevelId() == null) {
            throw new IllegalStateException("Location type is not bound, but value is admin entity");
        }
        
        int adminEntityId = CuidAdapter.getLegacyIdFromCuid(ref.getRecordId());
        
        try {

            String sql = "SELECT l.locationId FROM location l " +
                    "LEFT JOIN locationadminlink k ON (k.locationId = l.locationId) " +
                    "WHERE l.locationTypeId = " + activity.getLocationTypeId() + " AND k.adminEntityId = " + adminEntityId;


            try (ResultSet rs = queryExecutor.query(sql)) {
                if (rs.next()) {
                    return new ReferenceValue(
                            new RecordRef(
                                    CuidAdapter.locationFormClass(activity.getLocationTypeId()),
                                    CuidAdapter.locationInstanceId(rs.getInt(1))));
                }
            }

            // No existing dummy location entry, create one :-(

            ResourceId locationId = CuidAdapter.generateLocationCuid();
            SqlInsert locationInsert = SqlInsert.insertInto("location");
            locationInsert.value("locationId", CuidAdapter.getLegacyIdFromCuid(locationId));
            locationInsert.value("locationTypeId", activity.getLocationTypeId());
            locationInsert.value("name", queryAdminName(adminEntityId));
            locationInsert.execute(queryExecutor);

            while (adminEntityId > 0) {
                SqlInsert linkInsert = SqlInsert.insertInto("locationadminlink");
                linkInsert.value("locationId", CuidAdapter.getLegacyIdFromCuid(locationId));
                linkInsert.value("adminEntityId", adminEntityId);
                linkInsert.execute(queryExecutor);
                adminEntityId = queryAdminParent(adminEntityId);
            }

            return new ReferenceValue(
                    new RecordRef(
                            CuidAdapter.locationFormClass(activity.getLocationTypeId()),
                            locationId));

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create dummy location row", e);
        }
    }

    private String queryAdminName(int adminEntityId) throws SQLException {
        String sql;
        sql = "SELECT name FROM adminentity WHERE adminEntityId = " + adminEntityId;
        try(ResultSet rs = queryExecutor.query(sql)) {
            if(!rs.next()) {
                throw new IllegalStateException("AdminEntity " + adminEntityId + " does not exist");
            }
            return rs.getString(1);
        }
    }


    private int queryAdminParent(int adminEntityId) throws SQLException {
        String sql;
        sql = "SELECT adminEntityParentId FROM adminentity WHERE adminEntityId = " + adminEntityId;
        try(ResultSet rs = queryExecutor.query(sql)) {
            if(!rs.next()) {
                throw new IllegalStateException("AdminEntity " + adminEntityId + " does not exist");
            }
            int parentId = rs.getInt(1);
            if(rs.wasNull()) {
                return -1;
            } else {
                return parentId;
            }
        }
    }


    @Override
    public void update(RecordUpdate update) {

        FormRecord formRecord = get(update.getRecordId()).get();
        FormInstance formInstance = FormInstance.toFormInstance(getFormClass(), formRecord);

        BaseTableUpdater baseTable = new BaseTableUpdater(baseMapping, update.getRecordId());
        IndicatorValueTableUpdater indicatorValues = new IndicatorValueTableUpdater(update.getRecordId());
        AttributeValueTableUpdater attributeValues = new AttributeValueTableUpdater(activity, update.getRecordId());

        
        if(update.isDeleted()) {
            baseTable.delete();
        } else {

            for (Map.Entry<ResourceId, FieldValue> change : update.getChangedFieldValues().entrySet()) {
                if (change.getKey().getDomain() == CuidAdapter.INDICATOR_DOMAIN) {
                    indicatorValues.update(change.getKey(), change.getValue());
                } else if (change.getKey().getDomain() == CuidAdapter.ATTRIBUTE_GROUP_FIELD_DOMAIN) {
                    attributeValues.update(change.getKey(), change.getValue());
                } else {
                    baseTable.update(change.getKey(), change.getValue());
                }
            }
        }
        long newVersion;
        try {
            newVersion = incrementSiteVersion();
            baseTable.executeUpdates(queryExecutor);
            indicatorValues.execute(queryExecutor);
            attributeValues.executeUpdates(queryExecutor);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        Map<ResourceId, FieldValue> fieldValues = new HashMap<>();
        fieldValues.putAll(formInstance.getFieldValueMap());
        fieldValues.putAll(update.getChangedFieldValues());

        RecordChangeType changeType = update.isDeleted() ? RecordChangeType.DELETED : RecordChangeType.UPDATED;

        dualWriteToHrd(changeType, update, newVersion, fieldValues);
    }

    @Override
    public ColumnQueryBuilder newColumnQuery() {
        return new SiteColumnQueryBuilder(activity, baseMapping, queryExecutor);
    }
    
    public long incrementSiteVersion() {
        long newVersion = activity.getVersion() + 1;
        SqlUpdate.update("activity")
                .set("version",  newVersion)
                .set("siteVersion", newVersion)
                .where("activityId", activity.getId())
                .execute(queryExecutor);
        
        return newVersion;
    }

    @Override
    public long cacheVersion() {
        return activity.getVersion();
    }

    @Override
    public void updateGeometry(ResourceId recordId, ResourceId fieldId, Geometry value) {
        throw new UnsupportedOperationException();
    }
}
