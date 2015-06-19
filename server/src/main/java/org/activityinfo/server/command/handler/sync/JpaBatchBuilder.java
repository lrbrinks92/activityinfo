package org.activityinfo.server.command.handler.sync;

import com.bedatadriven.rebar.sql.client.query.SqlQuery;
import com.google.common.collect.Lists;
import org.activityinfo.legacy.shared.command.result.SyncRegion;
import org.activityinfo.legacy.shared.command.result.SyncRegionUpdate;
import org.activityinfo.server.database.hibernate.entity.Offline;

import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.SingularAttribute;
import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class JpaBatchBuilder {

    private static final Logger LOGGER = Logger.getLogger(JpaBatchBuilder.class.getName());

    private EntityManager entityManager;
    private SqliteBatchBuilder batch;
    private long version;
    private String regionPath;
    private boolean complete = true;

    public JpaBatchBuilder(SqliteBatchBuilder batch, EntityManager entityManager) {
        this.batch = batch;
        this.entityManager = entityManager;
    }

    public JpaBatchBuilder(EntityManager entityManager, String regionPath) throws IOException {
        this.regionPath = regionPath;
        this.batch = new SqliteBatchBuilder();
        this.entityManager = entityManager;
    }

    /**
     * Appends a delete statement to the update list, deleting all entities in the 
     * local browser database that match criteria.
     * @param entity a JPA entity
     * @param criteria Sqlite criteria
     */
    public void delete(Class<?> entity, String criteria) throws IOException {
        batch.addStatement("DELETE FROM " + tableName(entity) + " WHERE " + criteria);
    }


    private String tableName(Class<?> entity) {
        Table tableAnnotation = entity.getAnnotation(Table.class);
        String tableName;
        if(tableAnnotation != null) {
            tableName = tableAnnotation.name();
        } else {
            tableName = entity.getSimpleName();
        }

        return tableName.toLowerCase();
    }

    public void insert(Class<?> entity, String criteria) {
        SqlQuery query = SqlQuery
                .select(columnsToSync(entity))
                .from(tableName(entity))
                .whereTrue(criteria);
        batch.insert().into(tableName(entity))
                .from(query)
                .execute(entityManager);
    }
    
    public InsertBuilder insert(Class<?> entity) {
        return new InsertBuilder(entity);
    }

    public boolean isComplete() {
        return complete;
    }

    public JpaBatchBuilder setComplete(boolean complete) {
        this.complete = complete;
        return this;
    }

    private List<SingularAttribute<?, ?>> attributesToSync(Class<?> entity) {
        List<SingularAttribute<?, ?>> attributes = new ArrayList<>();
        EntityType<?> entityType = entityManager.getMetamodel().entity(entity);
        for (SingularAttribute<?,?> attribute : entityType.getSingularAttributes()) {
            if(isSynced(attribute)) {
                if(attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.EMBEDDED) {
                    addEmbeddedAttributes(attributes, attribute);
                } else {
                    attributes.add(attribute);
                }
            }
        }
        return attributes;
    }
    
    public JpaBatchBuilder setVersion(long version) {
        this.version = version;
        return this;
    }


    public void addStatement(String sql) throws IOException {
        batch.addStatement(sql);
    }

    private void addEmbeddedAttributes(List<SingularAttribute<?, ?>> columns, SingularAttribute<?, ?> attribute) {
        EmbeddableType<?> embeddedType = entityManager.getMetamodel().embeddable(attribute.getJavaType());
        for (SingularAttribute<?,?> embeddedAttribute : embeddedType.getSingularAttributes()) {
            if(isSynced(embeddedAttribute)) {
                columns.add(embeddedAttribute);
            }
        }
    }

    private String[] columnsToSync(Class<?> entity) {
        List<String> columns = Lists.newArrayList();
        for (SingularAttribute<?,?> attribute : attributesToSync(entity)) {
            if(isSynced(attribute)) {
                columns.add(columnName(attribute));
            }
        }
        return columns.toArray(new String[columns.size()]);
    }

    private String columnName(SingularAttribute<?, ?> attribute) {
        AccessibleObject member = (AccessibleObject) attribute.getJavaMember();
        String columnName = attribute.getName();
        Column column = member.getAnnotation(Column.class);
        if(column != null) {
            if(!column.name().isEmpty()) {
                columnName = column.name();
            }
        }
        JoinColumn joinColumn = member.getAnnotation(JoinColumn.class);
        if(joinColumn != null && joinColumn.name() != null) {
            columnName = joinColumn.name();
        }
        return columnName;
    }

    private boolean isSynced(SingularAttribute<?, ?> attribute) {
        AccessibleObject member = (AccessibleObject) attribute.getJavaMember();
        Offline offline = member.getAnnotation(Offline.class);
        return offline == null || offline.sync();
    }

    private String sqliteType(SingularAttribute<?,?> attribute) {

        if(attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.MANY_TO_ONE) {

            return findIdType(attribute.getJavaType());

        } else {

            return SqliteTypes.getSqliteType(attribute.getJavaType());

        }
    }

    private String findIdType(Class<?> javaType) {
        EntityType<?> entityType = entityManager.getMetamodel().entity(javaType);
        for (SingularAttribute<?,?> attribute : entityType.getSingularAttributes()) {
            if(attribute.isId()) {
                return sqliteType(attribute);
            }
        }
        throw new IllegalArgumentException(javaType + " has no @Id field!");

    }

    public String build() throws IOException {
        return batch.build();
    }

    public SqliteInsertBuilder insert() {
        return batch.insert();
    }

    public SyncRegionUpdate buildUpdate() throws IOException {
        SyncRegionUpdate update = new SyncRegionUpdate();
        update.setVersion(version);
        update.setComplete(complete);
        update.setSql(batch.build());
        return update;
    }


    public class InsertBuilder {

        private SqlQuery query;
        private String targetTable;
        private String lastTable;
        
        public InsertBuilder(Class<?> entity) {
            targetTable = tableName(entity);
            query = SqlQuery.select().from(targetTable);
            
            for(String column : columnsToSync(entity)) {
               query.appendColumn(targetTable + "." + column); 
            }
            lastTable = targetTable;
        }

        public InsertBuilder join(Class<?> entityClass) {
            String joinTable = tableName(entityClass);
            String idField = joinTable + "Id";
            query.innerJoin(joinTable).on(String.format("%s.%s = %s.%s",
                    lastTable, idField,
                    joinTable, idField));
            
            lastTable = joinTable;
            return this;
        }

        public InsertBuilder join(String joinTable, String idField) {
            query.innerJoin(joinTable).on(String.format("%s.%s = %s.%s",
                    lastTable, idField,
                    joinTable, idField));

            lastTable = joinTable;
            return this;
        }
        
        public InsertBuilder whereNotDeleted(Class<?> entityClass) {
            query.whereTrue(String.format("%s.dateDeleted IS NULL", tableName(entityClass)));
            return this;
        }
        
        public InsertBuilder where(String criteria) {
            query.whereTrue(criteria);
            return this;
        }
        
        public void execute() {
            if(LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(query.sql());
            }
            batch.insert().from(query).into(targetTable).execute(entityManager);
        }

    }
}
