package org.activityinfo.ui.client.page.entry.sitehistory;

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

import com.google.common.base.Objects;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.legacy.shared.command.Month;
import org.activityinfo.legacy.shared.model.*;

import java.util.Map;

class ItemDetail {
    private String stringValue;

    static ItemDetail create(RenderContext ctx, Map.Entry<String, Object> entry) {

        Map<String, Object> state = ctx.getState();
        ActivityFormDTO form = ctx.getForm();

        String key = entry.getKey();
        final Object oldValue = state.get(key);
        final Object newValue = entry.getValue();
        state.put(key, newValue);

        final StringBuilder sb = new StringBuilder();

        // basic
        if (key.equals("date1")) {
            addValues(sb, I18N.CONSTANTS.startDate(), oldValue, newValue);

        } else if (key.equals("date2")) {
            addValues(sb, I18N.CONSTANTS.endDate(), oldValue, newValue);

        } else if (key.equals("comments")) {
            addValues(sb, I18N.CONSTANTS.comments(), oldValue, newValue);

        } else if (key.equals("locationId")) { // schema loookups
            String oldName = null;
            if (oldValue != null) {
                LocationDTO location = ctx.getLocation(toInt(oldValue));
                if (location != null) {
                    oldName = location.getName();
                }
            }
            String newName = ctx.getLocation(toInt(newValue)).getName();
            addValues(sb, I18N.CONSTANTS.location(), oldName, newName);

        } else if (key.equals("projectId")) {
            String oldName = null;
            if (oldValue != null) {
                ProjectDTO project = form.getProjectById(toInt(oldValue));
                if (project != null) {
                    oldName = project.getName();
                }
            }
            String newName = form.getProjectById(toInt(newValue)).getName();
            addValues(sb, I18N.CONSTANTS.project(), oldName, newName);

        } else if (key.equals("partnerId")) {
            String oldName = null;
            if (oldValue != null) {
                PartnerDTO oldPartner = form.getPartnerById(toInt(oldValue));
                if (oldPartner != null) {
                    oldName = oldPartner.getName();
                }
            }
            PartnerDTO newPartner = form.getPartnerById(toInt(newValue));
            if (newPartner != null) {
                String newName = newPartner.getName();
                addValues(sb, I18N.CONSTANTS.partner(), oldName, newName);
            }

        } else if (key.startsWith(IndicatorDTO.PROPERTY_PREFIX)) {
            // custom
            int id = IndicatorDTO.indicatorIdForPropertyName(key);
            IndicatorDTO dto = form.getIndicatorById(id);
            if (dto != null) {
                String name = dto.getName();

                Month m = IndicatorDTO.monthForPropertyName(key);
                if (m != null) {
                    name = I18N.MESSAGES.siteHistoryIndicatorName(name, m.toLocalDate().atMidnightInMyTimezone());
                }

                addValues(sb, name, oldValue, newValue, dto.getUnits());
            }

        } else if (key.startsWith(AttributeDTO.PROPERTY_PREFIX)) {
            if(toBool(oldValue) != toBool(newValue)) {
                int id = AttributeDTO.idForPropertyName(key);
                AttributeDTO dto = form.getAttributeById(id);
                if (dto != null) {
                    if (toBool(newValue)) {
                        sb.append(I18N.MESSAGES.siteHistoryAttrAdd(dto.getName()));
                    } else {
                        sb.append(I18N.MESSAGES.siteHistoryAttrRemove(dto.getName()));
                    }
                }
            }

        } else {
            // fallback
            addValues(sb, key, oldValue, newValue);
        }

        if(sb.length() > 0) {

            ItemDetail d = new ItemDetail();
            d.stringValue = sb.toString();

            return d;
        } else {
            return null;
        }
    }

    private static boolean toBool(Object value) {
        if(value == null) {
            return false;
        } else if(value == Boolean.TRUE) {
            return true;
        } else if(Boolean.parseBoolean(value.toString())) {
            return true;
        }
        return false;
    }

    private static void addValues(StringBuilder sb, String key, Object oldValue, Object newValue) {
        addValues(sb, key, oldValue, newValue, null);
    }

    private static void addValues(StringBuilder sb, String key, Object oldValue, Object newValue, String units) {
        sb.append(key);
        sb.append(": ");
        sb.append(newValue);

        if (units != null) {
            sb.append(" ");
            sb.append(units);
        }

        if (!Objects.equal(oldValue, newValue)) {
            sb.append(" (");
            if (oldValue == null) {
                sb.append(I18N.MESSAGES.siteHistoryOldValueBlank());
            } else {
                sb.append(I18N.MESSAGES.siteHistoryOldValue(oldValue));
            }
            sb.append(")");
        }
    }

    private static int toInt(Object val) {
        return val != null ? Integer.parseInt(val.toString()) : -1;
    }

    @Override
    public String toString() {
        return stringValue;
    }
}
