package org.activityinfo.ui.client.component.formula.editor;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.datepicker.client.DatePicker;
import org.activityinfo.model.type.time.LocalDate;
import org.activityinfo.ui.client.widget.DateBox;

public class DateEditor implements OperandEditor<LocalDate> {

    private DateBox dateBox;

    public DateEditor() {
        this.dateBox = new DateBox(new DatePicker(), null,
            new DateBox.DefaultFormat(DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_SHORT)));
    }

    @Override
    public Widget asWidget() {
        return dateBox;
    }

    @Override
    public void setValue(LocalDate value) {
        if(value == null) {
            dateBox.setValue(null);
        } else {
            dateBox.setValue(value.atMidnightInMyTimezone());
        }
    }

    @Override
    public LocalDate getValue() {
        if(dateBox.getValue() == null) {
            return null;
        } else {
            return new LocalDate(dateBox.getValue());
        }
    }
}
