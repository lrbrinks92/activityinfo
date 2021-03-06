package org.activityinfo.store.mysql.metadata;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.*;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.legacy.CuidAdapter;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.store.mysql.collections.BETA;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;

public class Activity implements Serializable {

    public static final int REPORT_ONCE = 0;
    public static final int REPORT_MONTHLY = 1;

    int activityId;
    int databaseId;
    String databaseName;
    int reportingFrequency;
    int locationTypeId;
    
    int sortOrder;

    /**
     * Because it currently possible to change location type, it's possible that a single
     * activity references *multiple* location types
     */
    Set<Integer> locationTypeIds = Sets.newHashSet();
    List<ResourceId> locationRange = new ArrayList<>();
    String category;
    String locationTypeName;
    Integer adminLevelId;
    String name;
    int ownerUserId;
    boolean published;
    long schemaVersion;
    long siteVersion;
    
    boolean deleted;

    boolean classicView;

    FormClassHolder serializedFormClass = new FormClassHolder();


    List<ActivityField> fields = Lists.newArrayList();
    
    
    Map<ResourceId, Integer> fieldsOrder = Maps.newHashMap();

    /**
     * Map from destination indicator to it source indicators
     */
    Multimap<Integer, Integer> linkedIndicators = HashMultimap.create();
    
    Map<Integer, LinkedActivity> linkedActivities = Maps.newHashMap();

    public int getId() {
        return activityId;
    }

    public int getDatabaseId() {
        return databaseId;
    }

    public int getReportingFrequency() {
        return reportingFrequency;
    }

    public int getLocationTypeId() {
        return locationTypeId;
    }

    public String getCategory() {
        return category;
    }

    public boolean isClassicView() {
        return classicView;
    }

    public String getLocationTypeName() {
        return locationTypeName;
    }

    public Integer getAdminLevelId() {
        return adminLevelId;
    }

    public List<ActivityField> getFields() {
        return fields;
    }

    public Map<ResourceId, Integer> getFieldsOrder() {
        return fieldsOrder;
    }

    public String getName() {
        return name;
    }

    public Iterable<ActivityField> getAttributeAndIndicatorFields() {
        if(reportingFrequency == REPORT_ONCE) {
            return fields;
        } else {
            return Iterables.filter(fields, new Predicate<ActivityField>() {
                @Override
                public boolean apply(ActivityField input) {
                    return input.isAttributeGroup();
                }
            });
        }
    }

    public long getVersion() {
        return Math.max(siteVersion, schemaVersion);
    }

    public Iterable<ActivityField> getIndicatorFields() {
        return Iterables.filter(fields, new Predicate<ActivityField>() {
            @Override
            public boolean apply(ActivityField input) {
                return !input.isAttributeGroup();
            }
        });
    }
    
    public boolean hasLocationType() {
        // hack!!
        return !isNullLocationType();
    }

    public int getNullaryLocationId() {
        // This is nasty hack to allow for activities without location types.
        // Each country has one "nullary" location type called "Country"
        // Each of these location types has exactly one location instance, with the same id.
        return locationTypeId;
    }


    private boolean isNullLocationType() {
        return "Country".equals(locationTypeName) && locationTypeId != 20301;
    }

    public ResourceId getProjectFormClassId() {
        return CuidAdapter.projectFormClass(databaseId);
    }
    
    public ResourceId getPartnerFormClassId() {
        return CuidAdapter.partnerFormId(databaseId);
    }

    public ResourceId getLocationFormClassId() {
        return CuidAdapter.locationFormClass(locationTypeId);
    }


    public int getOwnerUserId() {
        return ownerUserId;
    }

    public boolean isPublished() {
        return published;
    }

    public Collection<ResourceId> getLocationFormClassIds() {
        if(BETA.ENABLE_LOCATION_UNION_FIELDS) {
            return locationRange;

        } else if(adminLevelId != null) {
            return Collections.singleton(CuidAdapter.adminLevelFormClass(adminLevelId));

        } else {
            return Collections.singleton(CuidAdapter.locationFormClass(locationTypeId));
        }
    }

    public int getSortOrder() {
        return sortOrder;
    }


    public boolean hasCategory() {
        return !Strings.isNullOrEmpty(category);
    }

    public ResourceId getSiteFormClassId() {
        return CuidAdapter.activityFormClass(activityId);
    }

    public ResourceId getLeafFormClassId() {
        if(reportingFrequency == 0) {
            return getSiteFormClassId();
        } else {
            return CuidAdapter.reportingPeriodFormClass(activityId);
        }
    }

    public Collection<LinkedActivity> getLinkedActivities() {
        return linkedActivities.values();
    }

    public boolean isMonthly() {
        return reportingFrequency == 1;
    }

    public String getDatabaseName() {
        return databaseName;
    }


    public ActivityField getIndicatorField(Integer indicatorId) {
        for (ActivityField field : fields) {
            if(field.getId() == indicatorId) {
                return field;
            }
        }

        throw new IllegalArgumentException("No such indicator " + indicatorId + " in activity " + activityId);
    }
    
    public ActivityVersion getActivityVersion() {
        return new ActivityVersion(this.getId(), schemaVersion, siteVersion);
    }
    
    void addLink(int destinationIndicatorId, int sourceActivityId, int sourceReportingFrequency, int sourceIndicatorId) {
        if(sourceActivityId == this.activityId) {
            linkedIndicators.put(destinationIndicatorId, sourceIndicatorId);
        } else {
            LinkedActivity linkedActivity = linkedActivities.get(sourceActivityId);
            if(linkedActivity == null) {
                linkedActivity = new LinkedActivity();
                linkedActivity.activityId = sourceActivityId;
                linkedActivity.reportingFrequency = sourceReportingFrequency;
                linkedActivities.put(sourceActivityId, linkedActivity);
            }
            linkedActivity.linkMap.put(destinationIndicatorId, sourceIndicatorId);

        }
    }
    
    public boolean isDeleted() {
        return deleted;
    }
    
    public LinkedActivity getSelfLink() {
        LinkedActivity linked = new LinkedActivity();
        linked.activityId = this.activityId;
        linked.reportingFrequency = this.reportingFrequency;
        for (ActivityField indicatorField : getIndicatorFields()) {
            linked.linkMap.put(indicatorField.getId(), indicatorField.getId());
        }
        linked.linkMap.putAll(linkedIndicators);
        return linked;
    }

    public FormClass getSerializedFormClass() {
        return serializedFormClass.value;
    }

    /**
     * Helper class to allow the FormClass value to be serialized with JSON instead of Java Serialization.
     */
    static class FormClassHolder implements Serializable {

        FormClass value;

        private void writeObject(ObjectOutputStream out) throws IOException {
            if(value == null) {
                out.writeBoolean(false);
            } else {
                out.writeBoolean(true);
                out.writeUTF(value.toJsonString());
            }
        }
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            if(in.readBoolean()) {
                this.value = FormClass.fromJson(in.readUTF());
            } else {
                this.value = null;
            }
        }
    }
}
