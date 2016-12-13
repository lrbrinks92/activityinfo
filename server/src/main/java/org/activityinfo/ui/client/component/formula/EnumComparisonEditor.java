package org.activityinfo.ui.client.component.formula;

import com.google.gwt.user.client.ui.Widget;
import org.activityinfo.model.type.enumerated.EnumItem;
import org.activityinfo.model.type.enumerated.EnumType;
import org.activityinfo.ui.client.widget.ListBox;

public class EnumComparisonEditor implements FieldPredicateEditor {

    private ListBox listBox;

    public EnumComparisonEditor(EnumType enumType) {
        this.listBox = new ListBox();
        for (EnumItem enumItem : enumType.getValues()) {
            listBox.addItem(enumItem.getLabel(), enumItem.getId().asString());
        }
    }

    @Override
    public Widget asWidget() {
        return listBox;
    }
}
