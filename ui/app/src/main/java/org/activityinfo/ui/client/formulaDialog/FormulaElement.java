package org.activityinfo.ui.client.formulaDialog;

import com.google.gwt.resources.client.ImageResource;
import com.sencha.gxt.core.client.IdentityValueProvider;
import com.sencha.gxt.core.client.ValueProvider;
import com.sencha.gxt.data.shared.ModelKeyProvider;
import org.activityinfo.model.form.FormField;
import org.activityinfo.model.formTree.FormTree;
import org.activityinfo.ui.client.icons.IconBundle;

/**
 * An element that can be added to the
 */
public class FormulaElement {

    public static final ModelKeyProvider<FormulaElement> KEY_PROVIDER = (element) -> element.getKey();

    public static final ValueProvider<FormulaElement, FormulaElement> VALUE_PROVIDER = new IdentityValueProvider<>();

    private String key;

    private String code;
    private String label;
    private ImageResource icon;

    public FormulaElement(FormField field) {
        this.key = "field:" + field.getId().asString();
        this.label = field.getLabel();
        this.code = field.getCode();
    }

    private FormulaElement(String key, String label) {
        this.key = key;
        this.label = label;
    }

    private FormulaElement(String key, String label, ImageResource icon) {
        this.key = key;
        this.label = label;
        this.icon = icon;
    }

    public String getKey() {
        return key;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public boolean hasCode() {
        return code != null;
    }

    public static FormulaElement folder(String id, String label) {
        return new FormulaElement(id, label);
    }

    public static FormulaElement fieldNode(FormTree.Node node) {
        return new FormulaElement("field:" + node.getPath().toString(),
                node.getField().getLabel(),
                IconBundle.iconForField(node.getType()));
    }

    public ImageResource getIcon() {
        return icon;
    }

    public boolean matches(String filter) {
        String filterLowered = filter.toLowerCase();
        return (code != null && code.toLowerCase().contains(filterLowered)) ||
                label.toLowerCase().contains(filter);

    }
}
