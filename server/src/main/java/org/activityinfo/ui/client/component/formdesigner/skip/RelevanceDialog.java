package org.activityinfo.ui.client.component.formdesigner.skip;
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.model.expr.ExprLexer;
import org.activityinfo.model.expr.ExprNode;
import org.activityinfo.model.expr.ExprParser;
import org.activityinfo.model.form.FormField;
import org.activityinfo.ui.client.component.formdesigner.container.FieldWidgetContainer;
import org.activityinfo.ui.client.component.formdesigner.properties.FieldEditor;
import org.activityinfo.ui.client.component.formula.FieldCondition;
import org.activityinfo.ui.client.component.formula.FieldConditionList;
import org.activityinfo.ui.client.component.formula.FieldConditionListEditor;
import org.activityinfo.ui.client.widget.ModalDialog;

/**
 * @author yuriyz on 7/23/14.
 */
public class RelevanceDialog {


    interface Driver extends SimpleBeanEditorDriver<FieldConditionList, FieldConditionListEditor> {}


    public static final int DIALOG_WIDTH = 900;


    private final FormField formField;
    private final ModalDialog dialog;
    private final FieldConditionListEditor editor;
    private final FieldConditionList model;
    private final Driver driver;


    public RelevanceDialog(final FieldWidgetContainer fieldWidgetContainer, final FieldEditor propertiesPresenter) {
        this.formField = fieldWidgetContainer.getFormField();
        this.model = parse(formField.getRelevanceConditionExpression());

        this.editor = new FieldConditionListEditor(fieldWidgetContainer.getFormDesigner().getModel().getAllFormsFields());
        this.driver = GWT.create(Driver.class);
        this.driver.initialize(editor);
        this.driver.edit(model);

        this.dialog = new ModalDialog(editor);
        this.dialog.setDialogTitle(I18N.CONSTANTS.defineRelevanceLogic());
        this.dialog.getDialogDiv().getStyle().setWidth(DIALOG_WIDTH, Style.Unit.PX);
        this.dialog.getPrimaryButton().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                FieldConditionList result = driver.flush();
                dialog.hide();
            }
        });
    }

    private FieldConditionList parse(String relevanceCondition) {
        FieldConditionList model = null;

        if(relevanceCondition != null) {
            try {
                ExprParser parser = new ExprParser(new ExprLexer(relevanceCondition));
                ExprNode rootNode = parser.parse();
                model = FieldConditionList.tryMatch(rootNode);
            } catch (Exception e) {
                // TODO: handle parsing error
            }
        }
        if(model == null) {
            model = new FieldConditionList();
        }
        if(model.getConditions().isEmpty()) {
            model.getConditions().add(new FieldCondition());
        }

        return model;
    }

    public void show() {
        dialog.show();
    }
}
