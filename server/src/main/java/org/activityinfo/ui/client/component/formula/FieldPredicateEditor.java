package org.activityinfo.ui.client.component.formula;

import com.google.gwt.user.client.ui.IsWidget;
import org.activityinfo.model.form.FormField;

/**
 *
 */
public interface FieldPredicateEditor extends IsWidget {


    FieldCondition createCondition(FormField selectedField, FieldPredicate selectedPredicate);
}
