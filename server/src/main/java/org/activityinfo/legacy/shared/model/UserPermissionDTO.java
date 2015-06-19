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
import org.activityinfo.server.endpoint.jsonrpc.Required;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonSetter;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.Length;

/**
 * Projection DTO of the
 * {@link org.activityinfo.server.database.hibernate.entity.UserPermission
 * UserPermission} domain object
 *
 * @author Alex Bertram
 */
@JsonAutoDetect(JsonMethod.NONE)
public final class UserPermissionDTO extends BaseModelData implements DTO {

    public UserPermissionDTO() {
        setAllowView(true);
        setAllowViewAll(false);
        setAllowEdit(false);
        setAllowEditAll(false);
        setAllowManageUsers(false);
        setAllowManageAllUsers(false);
        setAllowDesign(false);
    }

    public void setName(String value) {
        set("name", value);
    }

    /**
     * Returns the User's name.
     *
     * @return the user's name
     */
    @Required
    @Length(max = 50)
    @JsonProperty
    public String getName() {
        return get("name");
    }

    @JsonProperty
    @Length(max = 100)
    public String getOrganization() {
        return get("organization");
    }

    public void setOrganization(String organization) {
        set("organization", organization);
    }

    @JsonProperty
    @Length(max = 100)
    public String getJobtitle() {
        return get("jobtitle");
    }

    public void setJobtitle(String jobtitle) {
        set("jobtitle", jobtitle);
    }

    public void setEmail(String value) {
        set("email", value);
    }

    /**
     * Returns the User's email
     *
     * @return the User's email
     */
    @Required
    @Email
    @JsonProperty
    @Length(max = 75)
    public String getEmail() {
        return get("email");
    }

    public void setAllowView(boolean value) {
        set("allowView", value);
    }

    public void setAllowDesign(boolean value) {
        set("allowDesign", value);
    }

    @JsonProperty
    public boolean getAllowDesign() {
        return (Boolean) get("allowDesign");
    }

    @JsonProperty
    public boolean getAllowView() {
        return (Boolean) get("allowView");
    }

    public void setAllowViewAll(boolean value) {
        set("allowViewAll", value);
    }

    @JsonProperty
    public boolean getAllowViewAll() {
        return (Boolean) get("allowViewAll");
    }

    public void setAllowEdit(boolean value) {
        set("allowEdit", value);
    }

    @JsonProperty
    public boolean getAllowEdit() {
        return (Boolean) get("allowEdit");
    }

    public void setAllowEditAll(boolean value) {
        set("allowEditAll", value);
    }

    @JsonProperty
    public boolean getAllowEditAll() {
        return (Boolean) get("allowEditAll");
    }

    @JsonProperty
    public boolean getAllowManageUsers() {
        return (Boolean) get("allowManageUsers");
    }

    public void setAllowManageUsers(boolean allowManageUsers) {
        set("allowManageUsers", allowManageUsers);
    }

    @JsonProperty
    public boolean getAllowManageAllUsers() {
        return (Boolean) get("allowManageAllUsers");
    }

    public void setAllowManageAllUsers(boolean allowManageAll) {
        set("allowManageAllUsers", allowManageAll);
    }

    @JsonProperty
    public PartnerDTO getPartner() {
        return get("partner");
    }

    public void setPartner(PartnerDTO value) {
        set("partner", value);
    }
    

    @JsonSetter
    public void setPartnerId(int partnerId) {
        PartnerDTO partner = new PartnerDTO();
        partner.setId(partnerId);
        setPartner(partner);
    }
    
}
