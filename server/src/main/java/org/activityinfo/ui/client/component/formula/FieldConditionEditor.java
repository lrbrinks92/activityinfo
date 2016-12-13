package org.activityinfo.ui.client.component.formula;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import org.activityinfo.model.expr.SymbolExpr;
import org.activityinfo.model.form.FormField;
import org.activityinfo.model.type.FieldType;

import java.util.ArrayList;
import java.util.List;

/**
 * User interface to edit a simple condition involving a single field.
 */
public class FieldConditionEditor implements IsWidget, LeafValueEditor<FieldCondition> {



    interface FieldConditionEditorUiBinder extends UiBinder<HTMLPanel, FieldConditionEditor> {
    }

    private static FieldConditionEditorUiBinder ourUiBinder = GWT.create(FieldConditionEditorUiBinder.class);

    private final HTMLPanel panel;

    @UiField
    ListBox fieldListBox;

    @UiField
    ListBox predicateListBox;

    @UiField
    HTMLPanel predicateEditorContainer;

    private List<FormField> fields;



    /**
     * The currently selected field.
     */
    private FormField selectedField;

    /**
     * The predicates which apply to the selected field type
     */
    private List<FieldPredicate> predicates;

    private FieldPredicate selectedPredicate;

    /**
     * The editor for the selected predicate
     */
    private FieldPredicateEditor predicateEditor;


    public FieldConditionEditor(List<FormField> fields) {
        this.fields = new ArrayList<>();
        for (FormField field : fields) {
            if(isSelectable(field)) {
                this.fields.add(field);
            }
        }
        this.panel = ourUiBinder.createAndBindUi(this);

        // Populate fieldListBox selection list
        for (FormField field : this.fields) {
            fieldListBox.addItem(field.getLabel());
        }
    }


    private boolean isSelectable(FormField field) {
        FieldType type = field.getType();
        for (FieldPredicate fieldPredicate : FieldPredicate.values()) {
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


    /**
     * Called by the EditorDriver at the start of the edtiting session.
     */
    @Override
    public void setValue(FieldCondition value) {
        // Match the symbol to a selected field
        initFieldSelection(value.getField());
    }

    private void initFieldSelection(SymbolExpr field) {
        if(field == null) {
            fieldListBox.setSelectedIndex(-1);
        } else {
            for (int i = 0; i < fields.size(); i++) {
                if (fields.get(i).getId().asString().endsWith(field.getName())) {
                    fieldListBox.setSelectedIndex(i);
                    selectedField = fields.get(i);
                    return;
                }
            }
        }
    }

    /**
     * Called when the user selects a new field.
     */
    @UiHandler("fieldListBox")
    void onFieldChange(ChangeEvent event) {
        int fieldIndex = fieldListBox.getSelectedIndex();
        selectedField = fields.get(fieldIndex);

        initPredicateSelection();
    }

    @UiHandler("predicateListBox")
    void onPredicateChange(ChangeEvent event) {
        selectedPredicate = predicates.get(predicateListBox.getSelectedIndex());
    }

    /**
     * Initializes the list of acceptable predicates for the selected field.
     */
    private void initPredicateSelection() {

        predicateListBox.clear();
        predicates = new ArrayList<>();

        for (FieldPredicate fieldPredicate : FieldPredicate.values()) {
            if(fieldPredicate.accept(selectedField.getType())) {
                predicateListBox.addItem(fieldPredicate.getLabel());
                predicates.add(fieldPredicate);
            }
        }

        // Change the selected predicate if the current selection is no longer an option
        if(!predicates.contains(selectedPredicate)) {
            selectedPredicate = predicates.get(0);
        }

        predicateListBox.setSelectedIndex(predicates.indexOf(selectedPredicate));

        initPredicateEditor();
    }


    /**
     * Updates the predicate editor to match the field type and predicate.
     */
    private void initPredicateEditor() {

        predicateEditorContainer.clear();

        predicateEditor = PredicateEditors.create(selectedField.getType(), selectedPredicate);

        predicateEditorContainer.add(predicateEditor);
    }


    @Override
    public FieldCondition getValue() {
        return predicateEditor.createCondition(selectedField, selectedPredicate);
    }
}