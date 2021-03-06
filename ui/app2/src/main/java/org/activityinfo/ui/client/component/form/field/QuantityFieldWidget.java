package org.activityinfo.ui.client.component.form.field;

import com.google.common.base.Strings;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Widget;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.type.FieldType;
import org.activityinfo.model.type.number.Quantity;
import org.activityinfo.model.type.number.QuantityType;
import org.activityinfo.promise.Promise;
import org.activityinfo.ui.client.component.form.event.FieldMessageEvent;
import org.activityinfo.ui.client.widget.DoubleBox;

import javax.annotation.Nullable;
import java.text.ParseException;

public class QuantityFieldWidget implements FormFieldWidget<Quantity> {

    @Nullable
    private final EventBus eventBus;
    private final ResourceId fieldId;
    private final FlowPanel panel;
    private final DoubleBox box;
    private final InlineLabel unitsLabel;
    private final ValueUpdater<Quantity> valueUpdater;
    private final QuantityType type;

    public QuantityFieldWidget(final QuantityType type, final ValueUpdater<Quantity> valueUpdater,
                               @Nullable EventBus eventBus, ResourceId fieldId) {
        this.type = type;
        this.eventBus = eventBus;
        this.fieldId = fieldId;
        this.valueUpdater = valueUpdater;

        box = new DoubleBox();
        box.addValueChangeHandler(new ValueChangeHandler<Double>() {
            @Override
            public void onValueChange(ValueChangeEvent<Double> event) {
                fireValueChanged();
            }
        });
        box.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                validate(true);
            }
        });

        unitsLabel = new InlineLabel(type.getUnits());
        unitsLabel.setStyleName("input-group-addon");

        panel = new FlowPanel();
        panel.setStyleName("input-group");
        panel.add(box);
        panel.add(unitsLabel);
    }

    @Override
    public void fireValueChanged() {
        valueUpdater.update(box.getValue() != null ? new Quantity(box.getValue(), type.getUnits()) : null);
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        box.setReadOnly(readOnly);
    }

    @Override
    public boolean isReadOnly() {
        return box.isReadOnly();
    }

    @Override
    public Promise<Void> setValue(Quantity value) {
        box.setValue(value != null ? value.getValue() : null);
        return Promise.done();
    }

    @Override
    public void clearValue() {
        box.setValue(null);
    }

    @Override
    public void setType(FieldType type) {
        unitsLabel.setText(((QuantityType) type).getUnits());
    }

    @Override
    public Widget asWidget() {
        return panel;
    }

    public boolean isValid() {
        return Strings.isNullOrEmpty(box.getText()) ||
                (!Strings.isNullOrEmpty(box.getText()) && box.getValue() != null);
    }

    private void validate(boolean onKeyUp) {
        if (eventBus == null) {
            return;
        }

        if (isValid()) {
            eventBus.fireEvent(new FieldMessageEvent(fieldId, "").setClearMessage(true));
        }

        try {
            // The value sanitization algorithm is as follows: If the value of the element is not a valid floating-point number,
            // then set it to the empty string instead.
            // http://stackoverflow.com/questions/18852244/how-to-get-the-raw-value-an-input-type-number-field
            // SOLUTION : if we know that user typed something and value is null then browser sanitized it for us -> input invalid
            if (onKeyUp && box.getValue() == null) {
                eventBus.fireEvent(new FieldMessageEvent(fieldId, invalidErrorMessage()));
            }
            box.getValueOrThrow();
        } catch (ParseException e) {
            eventBus.fireEvent(new FieldMessageEvent(fieldId, invalidErrorMessage()));
        }
    }

    private String invalidErrorMessage() {
        NumberFormat decimalFormat = NumberFormat.getDecimalFormat();
        return I18N.MESSAGES.quantityFieldInvalidValue(15, decimalFormat.format(2000), decimalFormat.format(1.5));
    }
}
