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
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.gwt.junit.client.GWTTestCase;
import org.activityinfo.core.client.GwtPromiseMatchers;
import org.activityinfo.core.client.ResourceLocatorStub;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.form.FormField;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.type.primitive.TextType;
import org.activityinfo.promise.Promise;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Main goal is to check time of creation of FormDesignerPanel.
 *
 * As reported by AI-1006, sometimes it takes more than 10 seconds which is not acceptable.
 * The goal is to keep FormDesignerPanel time less then 2 seconds.
 *
 * @author yuriyz on 03/26/2015.
 */
public class FormDesignerPerformanceTest extends GWTTestCase {

    private static final int MAX_ALLOWED_BUILD_TIME_FOR_FORM_DESIGNER_PANEL = 2; // in seconds

    protected void gwtSetUp() throws Exception {
    }

    @Override
    public String getModuleName() {
        return "org.activityinfo.ui.ActivityInfoSafariTest";
    }

    // we are forced to use JUnit3 because of GWTTestCase
    public void testTimeOfFormDesignerPanelBuild() {

        FormClass formClass = new FormClass(ResourceId.generateId());

        FormField textField = formClass.addField(ResourceId.generateId());
        textField.setType(TextType.INSTANCE);
        textField.setLabel("Text field");

        final Stopwatch stopwatch = Stopwatch.createStarted();
        FormDesigner formDesigner = new FormDesigner(new ResourceLocatorStub(), formClass);

        formDesigner.getFormDesignerPanel().buildWidgetContainers(formClass, 0);

        long buildTime = stopwatch.elapsedMillis();
        System.out.println("FormDesignerPanel creation takes: " + buildTime + "ms");

        if (buildTime > MAX_ALLOWED_BUILD_TIME_FOR_FORM_DESIGNER_PANEL * 1000) {
            throw new AssertionError("FormDesignerPanel build time takes more then " +
                    MAX_ALLOWED_BUILD_TIME_FOR_FORM_DESIGNER_PANEL + " second(s).");
        }
    }

}
