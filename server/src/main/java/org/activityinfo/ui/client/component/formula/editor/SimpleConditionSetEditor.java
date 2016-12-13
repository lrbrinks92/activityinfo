package org.activityinfo.ui.client.component.formula.editor;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;
import org.activityinfo.model.form.FormField;
import org.activityinfo.ui.client.component.formula.model.SimpleCondition;
import org.activityinfo.ui.client.component.formula.model.SimpleConditionSet;
import org.activityinfo.ui.client.widget.Button;

import java.util.ArrayList;
import java.util.List;

/**
 * User interface for editing {@link SimpleConditionSet}.
 */
public class SimpleConditionSetEditor implements IsWidget {


    private static final int MAX_CONDITIONS = 5;

    interface FieldConditionListEditorUiBinder extends UiBinder<HTMLPanel, SimpleConditionSetEditor> {
    }

    private static FieldConditionListEditorUiBinder ourUiBinder = GWT.create(FieldConditionListEditorUiBinder.class);

    private final HTMLPanel panel;

    private List<FormField> fields;

    @UiField
    ListBox criteria;

    @UiField
    FlowPanel conditionPanel;
    
    @UiField
    Button addButton;

    private List<SimpleConditionEditor> conditionEditors = new ArrayList<>();

    public SimpleConditionSetEditor(final List<FormField> fields) {
        this.fields = fields;
        this.panel = ourUiBinder.createAndBindUi(this);
    }

    @Override
    public Widget asWidget() {
        return panel;
    }

    public void setValue(SimpleConditionSet value) {
        conditionEditors.clear();
        conditionPanel.clear();

        // Set the ANY/ALL criteria
        criteria.setSelectedIndex(SimpleConditionSet.Criteria.ALL.ordinal());

        // Add the conditions to the UI
        for (SimpleCondition simpleCondition : value.getConditions()) { 
            SimpleConditionEditor editor = new SimpleConditionEditor(fields);
            addCondition(editor);
        }
        onConditionCountChanged();
    }

    @UiHandler("addButton")
    public void addButtonClick(ClickEvent event) {
        SimpleConditionEditor editor = new SimpleConditionEditor(fields);
        addCondition(editor);
        onConditionCountChanged();
    }

    private void addCondition(final SimpleConditionEditor editor) {
        conditionEditors.add(editor);
        conditionPanel.add(editor);
        editor.getRemoveButton().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                int index = conditionEditors.indexOf(editor);
                conditionEditors.remove(index);
                conditionPanel.remove(index);
                onConditionCountChanged();
            }
        });
    }

    private void onConditionCountChanged() {
        // Only show the remove button if there are multiple conditions
        for (SimpleConditionEditor conditionEditor : conditionEditors) {
            conditionEditor.setRemoveButtonVisible(conditionEditors.size() > 1);
        }
        
        // Place a limit on the number of conditions
        addButton.setVisible(conditionEditors.size() < MAX_CONDITIONS);
    }
}
