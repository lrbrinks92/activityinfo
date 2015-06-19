package org.activityinfo.legacy.shared.model;

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

import com.extjs.gxt.ui.client.data.BaseModelData;
import com.google.common.collect.Lists;
import org.activityinfo.model.legacy.CuidAdapter;
import org.activityinfo.model.form.FormField;
import org.activityinfo.model.type.Cardinality;
import org.activityinfo.model.type.enumerated.EnumItem;
import org.activityinfo.model.type.enumerated.EnumType;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonView;

import java.util.ArrayList;
import java.util.List;

/**
 * One-to-one DTO for the
 * {@link org.activityinfo.server.database.hibernate.entity.AttributeGroup}
 * domain object
 */
@JsonAutoDetect(JsonMethod.NONE)
public final class AttributeGroupDTO extends BaseModelData implements EntityDTO, IsFormField {
    private static final long serialVersionUID = 7927425202152761370L;
    public static final String PROPERTY_PREFIX = "AG";

    public static final int NAME_MAX_LENGTH = 255;

    private List<AttributeDTO> attributes = new ArrayList<AttributeDTO>(0);

    public AttributeGroupDTO() {
    }

    @Override
    public String getLabel() {
        return getName();
    }

    /**
     * Creates a shallow clone
     *
     * @param model
     */
    public AttributeGroupDTO(AttributeGroupDTO model) {
        super(model.getProperties());
        setAttributes(model.getAttributes());
    }

    public boolean isEmpty() {
        return this.attributes == null || attributes.isEmpty();
    }

    public AttributeGroupDTO(int id) {
        this.setId(id);
    }

    @Override @JsonProperty @JsonView(DTOViews.Schema.class)
    public int getId() {
        return (Integer) get("id");
    }

    public void setId(int id) {
        set("id", id);
    }

    public void setName(String name) {
        set("name", name);
    }

    @Override @JsonProperty @JsonView(DTOViews.Schema.class)
    public String getName() {
        return get("name");
    }

    public void setMandatory(boolean mandatory) {
        set("mandatory", mandatory);
    }

    @JsonProperty @JsonView(DTOViews.Schema.class)
    public boolean isMandatory() {
        return get("mandatory", false);
    }

    public Integer getDefaultValue() {
        return get("defaultValue", null);
    }

    public void setDefaultValue(Integer defaultValue) {
        set("defaultValue", defaultValue);
    }

    public void setWorkflow(boolean workflow) {
        set("workflow", workflow);
    }

    @JsonProperty @JsonView(DTOViews.Schema.class)
    public boolean isWorkflow() {
        return get("workflow", false);
    }

    @JsonProperty @JsonView(DTOViews.Schema.class)
    public List<AttributeDTO> getAttributes() {
        return attributes;
    }

    public List<Integer> getAttributeIds() {
        List<Integer> result = new ArrayList<Integer>();
        for (AttributeDTO attr : getAttributes()) {
            result.add(attr.getId());
        }
        return result;
    }

    public void setAttributes(List<AttributeDTO> attributes) {
        this.attributes = attributes;
    }

    public AttributeDTO getAttributeById(int id) {
        for (AttributeDTO attr : getAttributes()) {
            if (attr.getId() == id) {
                return attr;
            }
        }
        return null;
    }

    @JsonProperty @JsonView(DTOViews.Schema.class)
    public boolean isMultipleAllowed() {
        return get("multipleAllowed", false);
    }

    public void setMultipleAllowed(boolean allowed) {
        set("multipleAllowed", allowed);
    }

    @Override
    public String getEntityName() {
        return "AttributeGroup";
    }

    public static String getPropertyName(int attributeGroupId) {
        return PROPERTY_PREFIX + attributeGroupId;
    }



    @Override
    public int getSortOrder() {
        return get("sortOrder", 0);
    }

    @Override
    public FormField asFormField() {
        Cardinality cardinality = isMultipleAllowed() ? Cardinality.MULTIPLE : Cardinality.SINGLE;
        List<EnumItem> values = Lists.newArrayList();
        for(AttributeDTO attribute : getAttributes()) {
            values.add(new EnumItem(CuidAdapter.attributeId(attribute.getId()), attribute.getName()));
        }

        return new FormField(CuidAdapter.attributeGroupField(getId()))
                .setLabel(getName())
                .setType(new EnumType(cardinality, values))
                .setRequired(isMandatory());
    }

    public void setSortOrder(int sortOrder) {
        set("sortOrder", sortOrder);
    }

    public String getPropertyName() {
        return getPropertyName(getId());
    }

    public static int idForPropertyName(String property) {
        return Integer.parseInt(property.substring(PROPERTY_PREFIX.length()));
    }

    @Override
    public String toString() {
        return getName() + "-" + getAttributes();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof AttributeGroupDTO)) {
            return false;
        }

        return getId() == ((AttributeGroupDTO) obj).getId();
    }

    @Override
    public int hashCode() {
        return getId();
    }
}
