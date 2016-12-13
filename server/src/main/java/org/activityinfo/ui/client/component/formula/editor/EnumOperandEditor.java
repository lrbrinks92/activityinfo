package org.activityinfo.ui.client.component.formula.editor;

import com.google.gwt.user.client.ui.Widget;
import org.activityinfo.model.type.enumerated.EnumItem;
import org.activityinfo.model.type.enumerated.EnumType;
import org.activityinfo.model.type.enumerated.EnumValue;
import org.activityinfo.ui.client.widget.ListBox;

public class EnumOperandEditor implements OperandEditor<EnumValue> {

    private ListBox listBox;

    public EnumOperandEditor(EnumType enumType) {
        this.listBox = new ListBox();
        for (EnumItem enumItem : enumType.getValues()) {
            listBox.addItem(enumItem.getLabel(), enumItem.getId().asString());
        }
    }

    @Override
    public Widget asWidget() {
        return listBox;
    }

    @Override
    public void setValue(EnumValue value) {

    }

    @Override
    public EnumValue getValue() {
        return null;
    }
}
