package org.activityinfo.ui.client.component.formula;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import org.activityinfo.model.form.FormField;

public class NullPredicateEditor implements FieldPredicateEditor {

    private Label label = new Label();

    @Override
    public Widget asWidget() {
        return label;
    }

    @Override
    public FieldCondition createCondition(FormField selectedField, FieldPredicate selectedPredicate) {
        return null;
    }
}
