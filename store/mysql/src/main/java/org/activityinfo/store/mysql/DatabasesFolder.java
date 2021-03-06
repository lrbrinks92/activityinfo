package org.activityinfo.store.mysql;

import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.model.form.CatalogEntry;
import org.activityinfo.model.form.CatalogEntryType;
import org.activityinfo.model.legacy.CuidAdapter;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.store.mysql.cursor.QueryExecutor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by yuriyz on 8/17/2016.
 */
public class DatabasesFolder {

    public static final String ROOT_ID = "databases";

    private final QueryExecutor executor;

    public DatabasesFolder(QueryExecutor executor) {
        this.executor = executor;
    }

    public CatalogEntry getRootEntry() {
        return new CatalogEntry(ROOT_ID, I18N.CONSTANTS.databases(), CatalogEntryType.FOLDER);
    }

    public List<CatalogEntry> getChildren(String parentId, int userId) throws SQLException {
        if (parentId.equals(ROOT_ID)) {
            return queryDatabases(userId);
        } else if (ResourceId.valueOf(parentId).getDomain() == CuidAdapter.DATABASE_DOMAIN) {
            List<CatalogEntry> children = new ArrayList<>();
            children.addAll(queryForms(ResourceId.valueOf(parentId)));
            children.addAll(queryLocationTypes(ResourceId.valueOf(parentId)));
            return children;
        }
        return Collections.emptyList();
    }



    private List<CatalogEntry> queryDatabases(int userId) throws SQLException {
        List<CatalogEntry> entries = new ArrayList<>();

        try (ResultSet rs = executor.query("select d.name, d.databaseId " +
                "FROM userdatabase d " +
                "WHERE d.dateDeleted is NULL AND (d.ownerUserId = ? OR d.databaseId IN " +
                " (select databaseid from userpermission where userpermission.userId = ? and allowview=1))" +
                "ORDER BY d.name", userId, userId)) {
            while (rs.next()) {
                String label = rs.getString(1);
                String formId = CuidAdapter.databaseId(rs.getInt(2)).asString();
                entries.add(new CatalogEntry(formId, label, CatalogEntryType.FOLDER));
            }
        }
        return entries;
    }

    private List<CatalogEntry> queryForms(ResourceId databaseId) throws SQLException {
        List<CatalogEntry> entries = new ArrayList<>();
        try(ResultSet rs = executor.query("SELECT ActivityId, name FROM activity " +
                "WHERE dateDeleted IS NULL AND databaseId = ? ", CuidAdapter.getLegacyIdFromCuid(databaseId))) {
            while(rs.next()) {
                String formId = CuidAdapter.activityFormClass(rs.getInt(1)).asString();
                String label = rs.getString(2);
                entries.add(new CatalogEntry(formId, label, CatalogEntryType.FORM));
            }
        }
        return entries;
    }

    private List<CatalogEntry> queryLocationTypes(ResourceId databaseId) throws SQLException {
        List<CatalogEntry> entries = new ArrayList<>();
        try(ResultSet rs = executor.query("SELECT locationTypeId, name FROM locationtype " +
                "WHERE databaseId = ? ", CuidAdapter.getLegacyIdFromCuid(databaseId))) {
            while(rs.next()) {
                String formId = CuidAdapter.locationFormClass(rs.getInt(1)).asString();
                String label = rs.getString(2);
                entries.add(new CatalogEntry(formId, label, CatalogEntryType.FORM));
            }
        }

        return entries;
    }
}
