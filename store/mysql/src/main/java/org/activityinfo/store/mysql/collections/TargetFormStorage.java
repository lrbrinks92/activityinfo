package org.activityinfo.store.mysql.collections;

import com.google.common.base.Optional;
import com.vividsolutions.jts.geom.Geometry;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.form.FormRecord;
import org.activityinfo.model.resource.RecordUpdate;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.service.store.ColumnQueryBuilder;
import org.activityinfo.service.store.FormPermissions;
import org.activityinfo.service.store.FormStorage;
import org.activityinfo.service.store.RecordVersion;
import org.activityinfo.store.mysql.cursor.QueryExecutor;
import org.activityinfo.store.mysql.cursor.RecordFetcher;
import org.activityinfo.store.mysql.mapping.TableMapping;
import org.activityinfo.store.mysql.metadata.DatabaseTargetForm;

import java.util.List;


public class TargetFormStorage implements FormStorage {
    
    private final QueryExecutor executor;
    private final DatabaseTargetForm target;
    private final TableMapping mapping;
    
    public TargetFormStorage(QueryExecutor executor, DatabaseTargetForm target) {
        this.target = target;
        this.executor = executor;
        this.mapping = target.buildMapping();
    }

    @Override
    public FormPermissions getPermissions(int userId) {
        return FormPermissions.readonly();
    }

    @Override
    public Optional<FormRecord> get(ResourceId resourceId) {
        return RecordFetcher.fetch(this, resourceId);
    }

    @Override
    public List<RecordVersion> getVersions(ResourceId recordId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<RecordVersion> getVersionsForParent(ResourceId parentRecordId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FormClass getFormClass() {
        return mapping.getFormClass();
    }

    @Override
    public void updateFormClass(FormClass formClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(RecordUpdate update) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(RecordUpdate update) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ColumnQueryBuilder newColumnQuery() {
        return new TargetQueryBuilder(executor, target, mapping);
    }

    @Override
    public long cacheVersion() {
        return 0;
    }

    @Override
    public void updateGeometry(ResourceId recordId, ResourceId fieldId, Geometry value) {
        throw new UnsupportedOperationException();
    }
}
