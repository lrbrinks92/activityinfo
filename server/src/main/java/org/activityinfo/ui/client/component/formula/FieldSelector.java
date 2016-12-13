package org.activityinfo.ui.client.component.formula;

import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import org.activityinfo.model.expr.SymbolExpr;
import org.activityinfo.model.form.FormField;

import java.util.List;

public class FieldSelector implements LeafValueEditor<SymbolExpr>, IsWidget, HasValueChangeHandlers<FormField> {

    private final ListBox listBox;
    private final List<FormField> fields;
    private final EventBus eventBus = new SimpleEventBus();

    /**
     * @param fields the available fields that can be chosen.
     */
    public FieldSelector(List<FormField> fields) {
        this.fields = fields;

        this.listBox = new ListBox(false);
        this.listBox.addStyleName("form-control");

        for (FormField field : fields) {
            listBox.addItem(field.getLabel(), field.getId().asString());
        }

        this.listBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
            }
        });
    }

    @Override
    public Widget asWidget() {
        return listBox;
    }

    @Override
    public void setValue(SymbolExpr value) {
        if(value != null) {
            // Find the fieldListBox with the given id
            for (int i = 0; i < fields.size(); i++) {
                FormField field = fields.get(i);
                if (field.getId().asString().equals(value.getName())) {
                    listBox.setSelectedIndex(i);
                    return;
                }
            }
        }
        listBox.setSelectedIndex(-1);
    }

    @Override
    public SymbolExpr getValue() {
        int index = listBox.getSelectedIndex();
        if(index == -1) {
            return null;
        } else {
            return new SymbolExpr(fields.get(index).getId());
        }
    }

    @Override
    public HandlerRegistration addValueChangeHandler(final ValueChangeHandler<FormField> handler) {
        return eventBus.addHandler(ValueChangeEvent.getType(), handler);
    }

    @Override
    public void fireEvent(GwtEvent<?> event) {
        eventBus.fireEvent(event);
    }
}
