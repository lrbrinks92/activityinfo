package org.activityinfo.ui.client.component.form.field;
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

import com.google.common.base.Function;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import org.activityinfo.core.client.ResourceLocator;
import org.activityinfo.core.shared.application.ApplicationProperties;
import org.activityinfo.legacy.shared.Log;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.form.FormField;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.model.type.FieldType;
import org.activityinfo.model.type.NarrativeType;
import org.activityinfo.model.type.ReferenceType;
import org.activityinfo.model.type.barcode.BarcodeType;
import org.activityinfo.model.type.enumerated.EnumType;
import org.activityinfo.model.type.expr.CalculatedFieldType;
import org.activityinfo.model.type.expr.ExprFieldType;
import org.activityinfo.model.type.geo.GeoPointType;
import org.activityinfo.model.type.image.ImageType;
import org.activityinfo.model.type.number.QuantityType;
import org.activityinfo.model.type.primitive.BooleanType;
import org.activityinfo.model.type.primitive.TextType;
import org.activityinfo.model.type.time.LocalDateIntervalType;
import org.activityinfo.model.type.time.LocalDateType;
import org.activityinfo.promise.Promise;
import org.activityinfo.ui.client.component.form.field.hierarchy.HierarchyFieldWidget;
import org.activityinfo.ui.client.component.form.field.image.ImageUploadFieldWidget;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author yuriyz on 1/28/14.
 */
public class FormFieldWidgetFactory {

    public interface Template extends SafeHtmlTemplates {

        @SafeHtmlTemplates.Template("<span class='help-block'>{0}</span>")
        SafeHtml error(String error);
    }

    public static final Template TEMPLATE = GWT.create(Template.class);

    /**
     * Based on this numbers FormField Widget generates different widgets and layouts:
     * <p/>
     * 1. Single :
     * less SMALL_BALANCE_NUMBER -> Radio buttons
     * less MEDIUM_BALANCE_NUMBER -> Dropdown list
     * more MEDIUM_BALANCE_NUMBER -> Suggest box
     * 2. Multiple :
     * less SMALL_BALANCE_NUMBER -> Check boxes
     * less MEDIUM_BALANCE_NUMBER -> List of selected + add button
     * more MEDIUM_BALANCE_NUMBER -> List of selected + add button
     */
    public static final int SMALL_BALANCE_NUMBER = 10;
    public static final int MEDIUM_BALANCE_NUMBER = 20;

    private final ResourceLocator resourceLocator;
    private final FieldWidgetMode fieldWidgetMode;

    public FormFieldWidgetFactory(ResourceLocator resourceLocator, FieldWidgetMode fieldWidgetMode) {
        this.resourceLocator = resourceLocator;
        this.fieldWidgetMode = fieldWidgetMode;
    }

    public Promise<? extends FormFieldWidget> createWidget(FormClass formClass, FormField field, ValueUpdater valueUpdater) {
        return createWidget(null, formClass, field, valueUpdater, null, null);
    }

    public Promise<? extends FormFieldWidget> createWidget(FormClass formClass, FormField field, ValueUpdater valueUpdater, @Nullable EventBus eventBus) {
        return createWidget(null, formClass, field, valueUpdater, null, eventBus);
    }

    public Promise<? extends FormFieldWidget> createWidget(String resourceId, FormClass formClass, FormField field,
                                                           ValueUpdater valueUpdater, FormClass validationFormClass, @Nullable EventBus eventBus) {
        FieldType type = field.getType();

        if (type instanceof QuantityType) {
            return Promise.resolved(new QuantityFieldWidget((QuantityType) type, valueUpdater, eventBus, field.getId()));

        } else if (type instanceof NarrativeType) {
            return Promise.resolved(new NarrativeFieldWidget(valueUpdater));

        } else if (type instanceof TextType) {
            return Promise.resolved(new TextFieldWidget(valueUpdater));

        } else if (type instanceof ExprFieldType) {
            return Promise.resolved(new ExprFieldWidget(validationFormClass, valueUpdater));

        } else if (type instanceof CalculatedFieldType) {
            return Promise.resolved(new CalculatedFieldWidget(valueUpdater));

        } else if (type instanceof LocalDateType) {
            return Promise.resolved(new DateFieldWidget(valueUpdater, eventBus, field.getId()));

        } else if (type instanceof LocalDateIntervalType) {
            return Promise.resolved(new DateIntervalFieldWidget(valueUpdater));

        } else if (type instanceof GeoPointType) {
            return Promise.resolved(new GeographicPointWidget(valueUpdater));

        } else if (type instanceof EnumType) {
            return Promise.resolved(new EnumFieldWidget((EnumType) field.getType(), valueUpdater, fieldWidgetMode));

        } else if (type instanceof BooleanType) {
            return Promise.resolved(new BooleanFieldWidget(valueUpdater));

        } else if (type instanceof ImageType) {
            return Promise.resolved(new ImageUploadFieldWidget(resourceId, field, valueUpdater));

        } else if (type instanceof ReferenceType) {
            return createReferenceWidget(field, valueUpdater);

        } else if (type instanceof BarcodeType) {
            return Promise.resolved(new BarcodeFieldWidget(valueUpdater));
        }

        Log.error("Unexpected field type " + type.getTypeClass());
        throw new UnsupportedOperationException();
    }

    private Promise<? extends FormFieldWidget> createReferenceWidget(FormField field, ValueUpdater updater) {
        if (field.isSubPropertyOf(ApplicationProperties.HIERARCHIAL)) {
            return HierarchyFieldWidget.create(resourceLocator, (ReferenceType) field.getType(), updater);
        } else {
            return createSimpleListWidget((ReferenceType) field.getType(), updater);
        }
    }

    private Promise createSimpleListWidget(final ReferenceType type, final ValueUpdater valueUpdater) {
        return resourceLocator
                .queryInstances(type.getRange())
                .then(new Function<List<FormInstance>, FormFieldWidget>() {
                    @Override
                    public FormFieldWidget apply(List<FormInstance> input) {

                        if (input.size() < SMALL_BALANCE_NUMBER) {
                            // Radio buttons
                            return new CheckBoxFieldWidget(type, input, valueUpdater);

                        } else if (input.size() < MEDIUM_BALANCE_NUMBER) {
                            // Dropdown list
                            return new ComboBoxFieldWidget(input, valueUpdater);

                        } else {
                            // Suggest box
                            return new SuggestBoxWidget(input, valueUpdater);
                        }
                    }
                });
    }
}

