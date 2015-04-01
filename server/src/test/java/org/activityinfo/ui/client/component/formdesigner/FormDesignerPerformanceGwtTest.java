package org.activityinfo.ui.client.component.formdesigner;
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
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.gwt.junit.client.GWTTestCase;
import org.activityinfo.core.client.ResourceLocatorStub;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.form.FormField;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.type.Cardinality;
import org.activityinfo.model.type.enumerated.EnumItem;
import org.activityinfo.model.type.enumerated.EnumType;
import org.activityinfo.model.type.number.QuantityType;
import org.activityinfo.model.type.primitive.TextType;
import org.activityinfo.ui.client.component.form.field.FormFieldWidget;
import org.activityinfo.ui.client.component.formdesigner.drop.NullValueUpdater;
import org.activityinfo.ui.client.widget.CheckBox;
import org.activityinfo.ui.client.widget.RadioButton;
import org.activityinfo.ui.client.widget.TextBox;

import javax.annotation.Nullable;
import java.util.List;

import static org.activityinfo.core.client.GwtPromiseMatchers.assertResolves;

/**
 * Main goal is to check time of creation of FormDesignerPanel.
 * <p/>
 * As reported by AI-1006, sometimes it takes more than 10 seconds which is not acceptable.
 * The goal is to keep FormDesignerPanel time less then 2 seconds.
 *
 * @author yuriyz on 03/26/2015.
 */
public class FormDesignerPerformanceGwtTest extends GWTTestCase {

    private static final FormClass DUMMY_FORM_CLASS = new FormClass(ResourceId.generateId());
    private static final int MAX_ALLOWED_BUILD_TIME_FOR_FORM_DESIGNER_PANEL_MS = 1000; // in milliseconds
    private static final int MAX_ALLOWED_BUILD_TIME_FOR_FORM_FIELD_WIDGET_MS = 30; // in milliseconds

    private static FormDesigner formDesigner;


    protected void gwtSetUp() throws Exception {
        formDesigner = getFormDesigner(DUMMY_FORM_CLASS);
    }

    @Override
    public String getModuleName() {
        return "org.activityinfo.ui.ActivityInfoSafariTest";
    }

    public void testShowWidgetCreationTime() {
        // TextBox
        show(new Function<Void, String>() {
            @Nullable
            @Override
            public String apply(@Nullable Void input) {
                new TextBox();
                return "TextBox";
            }
        });

        // RadioButton
        show(new Function<Void, String>() {
            @Nullable
            @Override
            public String apply(@Nullable Void input) {
                new RadioButton("test");
                return "RadioButton";
            }
        });

        // CheckBox
        show(new Function<Void, String>() {
            @Nullable
            @Override
            public String apply(@Nullable Void input) {
                new CheckBox("test");
                return "CheckBox";
            }
        });

    }

    public void testTextFieldCreation() {
        FormField field = new FormField(ResourceId.generateId())
                .setType(TextType.INSTANCE)
                .setLabel("Text field");

        assertWidgetBuildtime(field);
    }

    public void testQuantityFieldCreation() {
        FormField field = new FormField(ResourceId.generateId())
                .setType(new QuantityType())
                .setLabel("Quantity field");

        assertWidgetBuildtime(field);
    }

    public void testEnumSingleFieldCreation() {
        int count = 5;
        FormField field = new FormField(ResourceId.generateId())
                .setType(new EnumType(Cardinality.SINGLE, generateEnumItems(count)))
                .setLabel("Enum single field");

        // on each enumItem we spend ~6-7ms, please check testShowWidgetCreationTime() to see time of single widget creation
        assertWidgetBuildtime(field, MAX_ALLOWED_BUILD_TIME_FOR_FORM_FIELD_WIDGET_MS + count * 6);
    }

    public void testEnumMultipleFieldCreation() {
        int count = 5;
        FormField field = new FormField(ResourceId.generateId())
                .setType(new EnumType(Cardinality.MULTIPLE, generateEnumItems(count)))
                .setLabel("Enum single field");

        // on each enumItem we spend ~6-7ms, please check testShowWidgetCreationTime() to see time of single widget creation
        assertWidgetBuildtime(field, MAX_ALLOWED_BUILD_TIME_FOR_FORM_FIELD_WIDGET_MS + count * 6);
    }


    // we are forced to use JUnit3 because of GWTTestCase
    public void testTimeOfFormDesignerPanelBuild() {

        FormClass formClass = new FormClass(ResourceId.generateId());

        FormField textField = formClass.addField(ResourceId.generateId());
        textField.setType(TextType.INSTANCE);
        textField.setLabel("Text field");

        FormField quantityField = formClass.addField(ResourceId.generateId());
        quantityField.setType(new QuantityType());
        quantityField.setLabel("Quantity field");

        FormField enumSingleField = formClass.addField(ResourceId.generateId());
        enumSingleField.setType(new EnumType(Cardinality.SINGLE, Lists.newArrayList(
                new EnumItem(ResourceId.generateId(), "item1"),
                new EnumItem(ResourceId.generateId(), "item2"),
                new EnumItem(ResourceId.generateId(), "item3")
        )));
        enumSingleField.setLabel("Enum single field");

        FormField enumMultipleField = formClass.addField(ResourceId.generateId());
        enumMultipleField.setType(new EnumType(Cardinality.MULTIPLE, Lists.newArrayList(
                new EnumItem(ResourceId.generateId(), "item1"),
                new EnumItem(ResourceId.generateId(), "item2"),
                new EnumItem(ResourceId.generateId(), "item3")
        )));
        enumMultipleField.setLabel("Enum single field");

        final Stopwatch stopwatch = Stopwatch.createStarted();
        FormDesigner formDesigner = getFormDesigner(formClass);
        System.out.println("FormDesigner all together takes: " + stopwatch.elapsedMillis() + "ms");
        formDesigner.getFormDesignerPanelPresenter().buildWidgetContainers(formClass, 0);

        long buildTime = stopwatch.elapsedMillis();
        System.out.println("FormDesignerPanel creation takes: " + buildTime + "ms");

        if (buildTime > MAX_ALLOWED_BUILD_TIME_FOR_FORM_DESIGNER_PANEL_MS) {
            throw new AssertionError("FormDesignerPanel build time takes more then " +
                    MAX_ALLOWED_BUILD_TIME_FOR_FORM_DESIGNER_PANEL_MS + " ms.");
        }
    }

    private static void assertWidgetBuildtime(FormField formField) {
        assertWidgetBuildtime(formField, MAX_ALLOWED_BUILD_TIME_FOR_FORM_FIELD_WIDGET_MS);
    }

    private static void assertWidgetBuildtime(FormField formField, int maxTime) {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        createWidget(formField);

        long elapsed = stopwatch.elapsedMillis();
        System.out.println("creation field, type: " + formField.getType().getTypeClass().getId() + " takes: " + elapsed + "ms");

        if (elapsed > maxTime) {
            throw new AssertionError("FormFieldWidget build time takes more then " +
                    maxTime + " second(s).");
        }
    }

    private static FormFieldWidget createWidget(FormField formField) {
        return assertResolves(formDesigner.getFormFieldWidgetFactory().createWidget(formField, NullValueUpdater.INSTANCE));
    }

    private static FormDesigner getFormDesigner(FormClass formClass) {
        return new FormDesigner(new ResourceLocatorStub(), formClass);
    }

    private static List<EnumItem> generateEnumItems(int count) {
        Preconditions.checkState(count > 0);

        List<EnumItem> list = Lists.newArrayList();
        for (int i = 0; i < count; i++) {
            list.add(new EnumItem(ResourceId.valueOf("_enum_item_" + i), "Value " + i));
        }
        return list;
    }

    private static void show(Function<Void, String> createWidgetFunction) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        String textToShow = createWidgetFunction.apply(null);
        System.out.println("'new " + textToShow + "()' takes " + stopwatch.elapsedMillis() + "ms");
    }

}
