package org.activityinfo.ui.client.component.formula;

import com.google.gwt.user.client.ui.Widget;
import org.activityinfo.model.form.FormField;
import org.activityinfo.ui.client.widget.TextBox;

public class TextComparisonEditor implements FieldPredicateEditor {

    private FormField field;
    private FieldPredicate predicate;

    private TextBox textBox;

    public TextComparisonEditor() {
        this.textBox = new TextBox();
    }

    @Override
    public Widget asWidget() {
        return textBox;
    }

    @Override
    public FieldCondition createCondition(FormField selectedField, FieldPredicate selectedPredicate) {
        return new FieldCondition();
    }
}
