package org.activityinfo.ui.client.component.formula.editor;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import org.activityinfo.model.expr.SymbolExpr;
import org.activityinfo.model.form.FormField;
import org.activityinfo.model.type.FieldType;
import org.activityinfo.ui.client.component.formula.model.SimpleCondition;
import org.activityinfo.ui.client.component.formula.model.SimpleOperator;
import org.activityinfo.ui.client.widget.Button;

import java.util.ArrayList;
import java.util.List;

/**
 * User interface to edit a simple condition involving a single field.
 */
public class SimpleConditionEditor implements IsWidget {

    interface MyUiBinder extends UiBinder<Widget, SimpleConditionEditor> {
    }

    private static MyUiBinder ourUiBinder = GWT.create(MyUiBinder.class);

    private final Widget panel;

    @UiField
    DivElement removeGroup;

    @UiField
    Button removeButton;

    @UiField
    ListBox fieldSelect;

    @UiField
    ListBox operatorSelect;

    @UiField
    FlowPanel operandGroup;


    private List<FormField> fields;


    /**
     * The currently selected field.
     */
    private FormField selectedField;

    /**
     * The operators which apply to the selected field type
     */
    private List<SimpleOperator> operators;

    /**
     * The currently selected operand
     */
    private SimpleOperator selectedOperator;

    /**
     * The editor for the selected predicate
     */
    private OperandEditor operandEditor;


    public SimpleConditionEditor(List<FormField> fields) {
        this.fields = new ArrayList<>();
        for (FormField field : fields) {
            if(isSelectable(field)) {
                this.fields.add(field);
            }
        }
        this.panel = ourUiBinder.createAndBindUi(this);

        // Populate fieldSelect selection list
        for (FormField field : this.fields) {
            fieldSelect.addItem(field.getLabel());
        }
    }

    public void setRemoveButtonVisible(boolean visible) {
        if(visible) {
            removeGroup.getStyle().clearDisplay();
        } else {
            removeGroup.getStyle().setDisplay(Style.Display.NONE);
        }
    }

    private boolean isSelectable(FormField field) {
        FieldType type = field.getType();
        for (SimpleOperator fieldPredicate : SimpleOperator.values()) {
            if(fieldPredicate.accept(type)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Widget asWidget() {
        return panel;
    }

    public HasClickHandlers getRemoveButton() {
        return removeButton;
    }

    /**
     * Called by the EditorDriver at the start of the edtiting session.
     */
    public void setValue(SimpleCondition value) {
        // Match the symbol to a selected field
        initFieldSelection(value.getField());
    }

    private void initFieldSelection(SymbolExpr field) {
        if(field == null) {
            fieldSelect.setSelectedIndex(-1);
        } else {
            for (int i = 0; i < fields.size(); i++) {
                if (fields.get(i).getId().asString().endsWith(field.getName())) {
                    fieldSelect.setSelectedIndex(i);
                    selectedField = fields.get(i);
                    return;
                }
            }
        }
    }

    /**
     * Called when the user selects a new field.
     */
    @UiHandler("fieldSelect")
    void onFieldChange(ChangeEvent event) {
        int fieldIndex = fieldSelect.getSelectedIndex();
        selectedField = fields.get(fieldIndex);

        initOperatorSelection();
    }

    @UiHandler("operatorSelect")
    void onPredicateChange(ChangeEvent event) {
        selectedOperator = operators.get(operatorSelect.getSelectedIndex());
    }

    /**
     * Initializes the list of acceptable operators for the selected field.
     */
    private void initOperatorSelection() {

        operatorSelect.clear();
        operators = new ArrayList<>();

        for (SimpleOperator fieldPredicate : SimpleOperator.values()) {
            if(fieldPredicate.accept(selectedField.getType())) {
                operatorSelect.addItem(fieldPredicate.getLabel());
                operators.add(fieldPredicate);
            }
        }

        // Change the selected predicate if the current selection is no longer an option
        if(!operators.contains(selectedOperator)) {
            selectedOperator = operators.get(0);
        }

        operatorSelect.setSelectedIndex(operators.indexOf(selectedOperator));

        initOperandEditor();
    }


    /**
     * Updates the predicate editor to match the field type and predicate.
     */
    private void initOperandEditor() {

        if(operandEditor != null) {
            operandEditor.asWidget().removeFromParent();
        }

        operandEditor = OperandEditors.create(selectedField.getType(), selectedOperator);
        operandGroup.add(operandEditor);
    }
}