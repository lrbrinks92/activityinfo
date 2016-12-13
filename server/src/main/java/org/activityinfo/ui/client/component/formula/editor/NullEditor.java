package org.activityinfo.ui.client.component.formula.editor;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import org.activityinfo.model.type.FieldValue;

public class NullEditor implements OperandEditor {

    private Label label = new Label();

    @Override
    public Widget asWidget() {
        return label;
    }

    @Override
    public void setValue(FieldValue value) {

    }

    @Override
    public FieldValue getValue() {
        return null;
    }
}
