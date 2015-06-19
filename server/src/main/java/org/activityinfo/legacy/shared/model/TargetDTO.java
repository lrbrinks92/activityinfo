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

import com.bedatadriven.rebar.time.calendar.LocalDate;
import com.extjs.gxt.ui.client.data.BaseModelData;
import org.activityinfo.server.endpoint.jsonrpc.Required;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonSetter;
import org.hibernate.validator.constraints.Length;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

@JsonAutoDetect(JsonMethod.NONE)
public class TargetDTO extends BaseModelData implements EntityDTO {

    private UserDatabaseDTO userDatabase;
    private List<TargetValueDTO> targetValues;

    public static final String entityName = "Target";

    public TargetDTO() {
        super();
    }

    public TargetDTO(int id, String name) {
        super();

        set("id", id);
        set("name", name);
    }

    @Override
    @JsonProperty
    public int getId() {
        return get("id", 0);
    }

    public void setId(int id) {
        set("id", id);
    }

    @Override
    public Map<String, Object> getProperties() {
        return super.getProperties();
    }

    @Override
    public Collection<String> getPropertyNames() {
        return super.getPropertyNames();
    }

    @Override
    @Required
    @JsonProperty
    @Length(max = 255)
    public String getName() {
        return (String) get("name");
    }

    public void setName(String name) {
        set("name", name);
    }

    public void setDescription(String description) {
        set("description", description);
    }

    @JsonProperty
    public String getDescription() {
        return (String) get("description");
    }

    public void setUserDatabase(UserDatabaseDTO database) {
        this.userDatabase = database;
    }

    public UserDatabaseDTO getUserDatabase() {
        return userDatabase;
    }


    @JsonProperty
    public ProjectDTO getProject() {
        return get("project");
    }
    
    
    @JsonSetter
    public void setProjectId(Integer id) {
        if(id == null) {
            setProject(null);
        } else {
            ProjectDTO project = new ProjectDTO();
            project.setId(id);
            setProject(project);
        }
    }
    
    public void setProject(ProjectDTO value) {
        set("project", value);
    }

    @JsonProperty
    public PartnerDTO getPartner() {
        return get("partner");
    }
        
    @JsonSetter
    public void setPartnerId(Integer id) {
        if(id == null) {
            setPartner(null);
        } else {
            PartnerDTO partner = new PartnerDTO();
            partner.setId(id);
            setPartner(partner);
        }
    }
    
    public void setPartner(PartnerDTO value) {
        set("partner", value);
    }

    public AdminEntityDTO getAdminEntity() {
        return get("adminEntity");
    }

    public void setAdminEntity(AdminEntityDTO value) {
        set("adminEntity", value);
    }


    
    @Required
    @JsonProperty
    @SuppressWarnings("deprecation")
    public LocalDate getFromDate() {
        if(getDate1() == null) {
            return null;
        } else {
            return new LocalDate(getDate1());
        }
    }
    
    public void setFromDate(LocalDate date) {
        setDate1(date.atMidnightInMyTimezone());
    }

    @Required
    @JsonProperty
    @SuppressWarnings("deprecation")
    public LocalDate getToDate() {
        if(getDate2() == null) {
            return null;
        } else {
            return new LocalDate(getDate2());
        }
    }
    
    @SuppressWarnings("deprecation")
    public void setToDate(LocalDate date) {
        setDate2(date.atMidnightInMyTimezone());
    }
    
    @Deprecated
    public Date getDate1() {
        return get("date1");
    }

    @Deprecated
    public void setDate1(Date date1) {
        set("date1", date1);
    }

    @Deprecated
    public void setDate2(Date date2) {
        set("date2", date2);
    }

    @Deprecated
    public Date getDate2() {
        return get("date2");
    }

    public void setArea(String area) {
        set("area", area);
    }

    public String getArea() {
        return get("area");
    }

    @JsonProperty
    public List<TargetValueDTO> getTargetValues() {
        return targetValues;
    }

    public void setTargetValues(List<TargetValueDTO> targetValues) {
        this.targetValues = targetValues;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public String getEntityName() {
        return entityName;
    }
}
