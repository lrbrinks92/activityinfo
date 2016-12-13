package org.activityinfo.ui.client.component.formula.editor;

import com.google.gwt.user.client.ui.IsWidget;
import org.activityinfo.model.type.FieldValue;

/**
 *
 */
public interface OperandEditor<T extends FieldValue> extends IsWidget {

    void setValue(T value);

    T getValue();
}
