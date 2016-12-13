package org.activityinfo.ui.client.component.formula;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.Widget;

/**
 * Allows users to choose whether all criteria must be true, or any
 */
public class CriteriaEditor implements IsWidget, LeafValueEditor<FieldConditionList.Criteria> {

    interface CriteriaEditorUiBinder extends UiBinder<HTMLPanel, CriteriaEditor> {
    }

    private static CriteriaEditorUiBinder ourUiBinder = GWT.create(CriteriaEditorUiBinder.class);

    private final HTMLPanel panel;

    @UiField
    RadioButton allRadio;

    @UiField
    RadioButton anyRadio;

    public CriteriaEditor() {
        this.panel = ourUiBinder.createAndBindUi(this);
    }

    @Override
    public void setValue(FieldConditionList.Criteria value) {
        allRadio.setValue(value == FieldConditionList.Criteria.ALL);
        anyRadio.setValue(value == FieldConditionList.Criteria.ANY);
    }

    @Override
    public FieldConditionList.Criteria getValue() {
        if(allRadio.getValue()) {
            return FieldConditionList.Criteria.ALL;
        } else {
            return FieldConditionList.Criteria.ANY;
        }
    }

    @Override
    public Widget asWidget() {
        return panel;
    }

}