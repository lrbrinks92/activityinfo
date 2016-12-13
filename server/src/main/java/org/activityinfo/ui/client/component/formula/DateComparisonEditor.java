package org.activityinfo.ui.client.component.formula;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.datepicker.client.DatePicker;
import org.activityinfo.ui.client.widget.DateBox;

public class DateComparisonEditor implements FieldPredicateEditor {

    private DateBox dateBox;

    public DateComparisonEditor() {
        this.dateBox = new DateBox(new DatePicker(), null,
            new DateBox.DefaultFormat(DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_SHORT)));
    }

    @Override
    public Widget asWidget() {
        return dateBox;
    }
}
