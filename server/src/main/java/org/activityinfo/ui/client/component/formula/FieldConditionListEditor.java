package org.activityinfo.ui.client.component.formula;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.adapters.EditorSource;
import com.google.gwt.editor.client.adapters.ListEditor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import org.activityinfo.model.form.FormField;

import java.util.List;

/**
 * User interface for editing {@link FieldConditionList}.
 */
public class FieldConditionListEditor implements Editor<FieldConditionList>, IsWidget {

    interface FieldConditionListEditorUiBinder extends UiBinder<HTMLPanel, FieldConditionListEditor> {
    }

    private static FieldConditionListEditorUiBinder ourUiBinder = GWT.create(FieldConditionListEditorUiBinder.class);

    private final HTMLPanel panel;

    @UiField
    CriteriaEditor criteria;

    @UiField
    FlowPanel conditionPanel;

    ListEditor<FieldCondition, FieldConditionEditor> conditions;

    public FieldConditionListEditor(final List<FormField> fields) {
        this.panel = ourUiBinder.createAndBindUi(this);
        this.conditions = ListEditor.of(new EditorSource<FieldConditionEditor>() {

            @Override
            public FieldConditionEditor create(int index) {
                FieldConditionEditor editor = new FieldConditionEditor(fields);
                conditionPanel.insert(editor, index);
                return editor;
            }
        });
    }

    @Override
    public Widget asWidget() {
        return panel;
    }

    @UiHandler("addButton")
    public void addButtonClick(ClickEvent event) {

    }
}
