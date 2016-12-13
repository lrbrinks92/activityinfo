package org.activityinfo.ui.client.component.formula.editor;

import com.google.gwt.user.client.ui.Widget;
import org.activityinfo.model.type.number.Quantity;
import org.activityinfo.ui.client.widget.DoubleBox;

class QuantityOperandEditor implements OperandEditor<Quantity> {

    private DoubleBox doubleBox;

    public QuantityOperandEditor() {
        this.doubleBox = new DoubleBox();
    }

    @Override
    public Widget asWidget() {
        return doubleBox;
    }

    @Override
    public void setValue(Quantity value) {
        if(value == null) {
            doubleBox.setValue(null);
        } else {
            doubleBox.setValue(value.getValue());
        }
    }

    @Override
    public Quantity getValue() {
        if(doubleBox.getValue() == null) {
            return null;
        } else {
            return new Quantity(doubleBox.getValue());
        }
    }
}
