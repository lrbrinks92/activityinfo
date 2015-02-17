package org.activityinfo.legacy.shared.adapter.bindings;

import com.google.common.base.Function;
import com.google.common.collect.Sets;
import org.activityinfo.legacy.shared.model.*;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.model.legacy.CuidAdapter;
import org.activityinfo.model.legacy.KeyGenerator;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.type.enumerated.EnumValue;

import java.util.Map;
import java.util.Set;

import static org.activityinfo.model.legacy.CuidAdapter.*;

/**
 * Creates a SiteBinding from a SchemaDTO
 */
public class SiteBindingFactory implements Function<ActivityFormDTO, SiteBinding> {

    private final KeyGenerator keyGenerator = new KeyGenerator();

    public SiteBindingFactory() {
    }

    @Override
    public SiteBinding apply(ActivityFormDTO activity) {

        ResourceId formClassId = activity.getResourceId();

        SiteBinding binding = new SiteBinding(activity);
        binding.addNestedField(partnerField(activity.getId()), PARTNER_DOMAIN, "partner");
        binding.addNestedField(projectField(activity.getId()), PROJECT_DOMAIN, "project");

        binding.addField(field(formClassId, START_DATE_FIELD), "date1");
        binding.addField(field(formClassId, END_DATE_FIELD), "date2");
        binding.addField(field(formClassId, GUID_FIELD), "siteGuid");

        if (activity.getLocationType().isAdminLevel()) {
            binding.addField(new AdminLevelLocationBinding(formClassId, activity.getLocationType().getBoundAdminLevelId()));
        } else {
            binding.addNestedField(locationField(activity.getId()), LOCATION_DOMAIN, "location");
        }

        for (AttributeGroupDTO group : activity.getAttributeGroups()) {
            binding.addField(new AttributeGroupBinding(group));
        }

        for (IndicatorDTO indicator : activity.getIndicators()) {
            binding.addField(indicatorField(indicator.getId()), IndicatorDTO.getPropertyName(indicator.getId()));
        }

        binding.addField(field(formClassId, COMMENT_FIELD), "comments");

        return binding;
    }

    private ResourceId partnerField(int id) {
        return CuidAdapter.field(activityFormClass(id), PARTNER_FIELD);
    }

    private ResourceId projectField(int id) {
        return CuidAdapter.field(activityFormClass(id), PROJECT_FIELD);
    }

    private class AttributeGroupBinding implements FieldBinding<SiteDTO> {

        private final AttributeGroupDTO group;
        private ResourceId fieldId;

        private AttributeGroupBinding(AttributeGroupDTO group) {
            this.group = group;
            fieldId = CuidAdapter.attributeGroupField(group.getId());
        }

        @Override
        public void updateInstanceFromModel(FormInstance instance, SiteDTO model) {
            Set<ResourceId> references = Sets.newHashSet();
            for (AttributeDTO attribute : group.getAttributes()) {
                int id = attribute.getId();
                if (model.getAttributeValue(id)) {
                    references.add(CuidAdapter.attributeId(id));
                }
            }
            if (!references.isEmpty()) {
                instance.set(fieldId, new EnumValue(references));
            }
        }

        @Override
        public void populateChangeMap(FormInstance instance, Map<String, Object> changeMap) {
            Set<ResourceId> references = instance.getReferences(fieldId);
            for (ResourceId attributeResourceId : references) {
                changeMap.put(AttributeDTO.getPropertyName(getLegacyIdFromCuid(attributeResourceId)), true);
            }
        }
    }

    private class AdminLevelLocationBinding implements FieldBinding<SiteDTO> {

        private ResourceId formClassId;
        private final int levelId;

        private AdminLevelLocationBinding(ResourceId formClassId, int levelId) {
            this.formClassId = formClassId;
            this.levelId = levelId;
        }

        @Override
        public void updateInstanceFromModel(FormInstance instance, SiteDTO model) {
            LocationDTO dummyLocation = model.getLocation();
            final AdminEntityDTO adminEntity = dummyLocation.getAdminEntity(levelId);
            instance.set(field(formClassId, LOCATION_FIELD), Sets.newHashSet(entity(adminEntity.getId())));
        }

        @Override
        public void populateChangeMap(FormInstance instance, Map<String, Object> changeMap) {
            changeMap.put("locationId", keyGenerator.generateInt());
        }
    }

}
