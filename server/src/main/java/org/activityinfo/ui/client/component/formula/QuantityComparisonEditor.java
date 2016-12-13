package org.activityinfo.ui.client.component.formula;

import com.google.gwt.user.client.ui.Widget;
import org.activityinfo.ui.client.widget.DoubleBox;

public class QuantityComparisonEditor implements FieldPredicateEditor {

    private DoubleBox doubleBox;

    public QuantityComparisonEditor() {
        this.doubleBox = new DoubleBox();
    }

    @Override
    public Widget asWidget() {
        return doubleBox;
    }
}
