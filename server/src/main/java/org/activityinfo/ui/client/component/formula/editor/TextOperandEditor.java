package org.activityinfo.ui.client.component.formula.editor;

import com.google.gwt.user.client.ui.Widget;
import org.activityinfo.model.form.FormField;
import org.activityinfo.model.type.primitive.TextValue;
import org.activityinfo.ui.client.component.formula.model.SimpleOperator;
import org.activityinfo.ui.client.widget.TextBox;

public class TextOperandEditor implements OperandEditor<TextValue> {

    private FormField field;
    private SimpleOperator predicate;

    private TextBox textBox;

    public TextOperandEditor() {
        this.textBox = new TextBox();
    }

    @Override
    public Widget asWidget() {
        return textBox;
    }


    @Override
    public void setValue(TextValue value) {

    }

    @Override
    public TextValue getValue() {
        return null;
    }
}
