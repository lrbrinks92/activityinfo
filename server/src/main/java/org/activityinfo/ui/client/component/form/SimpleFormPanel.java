package org.activityinfo.ui.client.component.form;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import org.activityinfo.core.client.ResourceLocator;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.model.form.*;
import org.activityinfo.model.resource.Resource;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.type.FieldValue;
import org.activityinfo.promise.Promise;
import org.activityinfo.ui.client.component.form.event.FieldMessageEvent;
import org.activityinfo.ui.client.component.form.field.FormFieldWidget;
import org.activityinfo.ui.client.component.form.field.FormFieldWidgetFactory;
import org.activityinfo.ui.client.widget.DisplayWidget;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Displays a simple view of the form, where users can edit instances
 */
public class SimpleFormPanel implements DisplayWidget<FormInstance> {

    private final FieldContainerFactory containerFactory;
    private final FormFieldWidgetFactory widgetFactory;

    private final FlowPanel panel;
    private final ScrollPanel scrollPanel;
    private final boolean withScroll;
    private final EventBus eventBus = new SimpleEventBus();

    private final Map<ResourceId, FieldContainer> containers = Maps.newHashMap();

    /**
     * The original, unmodified instance
     */
    private Resource instance;

    /**
     * A new version of the instance, being updated by the user
     */
    private FormInstance workingInstance;

    private FormClass formClass;
    private ResourceLocator locator;
    private RelevanceHandler relevanceHandler;

    // validation form class is used to refer to "top-level" form class.
    // For example "Properties panel" renders current type-formClass but in order to validate expression we need
    // reference to formClass that is currently editing on FormDesigner.
    // it can be null.
    private FormClass validationFormClass = null;

    public SimpleFormPanel(ResourceLocator locator, FieldContainerFactory containerFactory,
                           FormFieldWidgetFactory widgetFactory) {
        this(locator, containerFactory, widgetFactory, true);
    }

    public SimpleFormPanel(ResourceLocator locator, FieldContainerFactory containerFactory,
                           FormFieldWidgetFactory widgetFactory, boolean withScroll) {
        FormPanelStyles.INSTANCE.ensureInjected();

        this.locator = locator;
        this.containerFactory = containerFactory;
        this.widgetFactory = widgetFactory;
        this.withScroll = withScroll;
        this.relevanceHandler = new RelevanceHandler(this);

        panel = new FlowPanel();
        panel.setStyleName(FormPanelStyles.INSTANCE.formPanel());
        scrollPanel = new ScrollPanel(panel);

        bindEvents();
    }

    private void bindEvents() {
        eventBus.addHandler(FieldMessageEvent.TYPE, new FieldMessageEvent.Handler() {
            @Override
            public void handle(FieldMessageEvent event) {
                showFieldMessage(event);
            }
        });
    }

    private void showFieldMessage(FieldMessageEvent event) {
        FieldContainer container = containers.get(event.getFieldId());
        if (event.isClearMessage()) {
            container.setValid();
        } else {
            container.setInvalid(event.getMessage());
        }
    }

    public FormInstance getInstance() {
        return workingInstance;
    }

    @Override
    public Promise<Void> show(final FormInstance instance) {
        return show(instance.asResource());
    }

    public Promise<Void> show(final Resource instance) {
        this.instance = instance;
        return locator.getFormClass(instance.getResourceId("classId")).join(new Function<FormClass, Promise<Void>>() {
            @Nullable
            @Override
            public Promise<Void> apply(@Nullable FormClass formClass) {
                return buildForm(formClass);
            }
        }).join(new Function<Void, Promise<Void>>() {
            @Nullable
            @Override
            public Promise<Void> apply(@Nullable Void input) {
                return setValue(instance);
            }
        });
    }

    private Promise<Void> buildForm(final FormClass formClass) {
        this.formClass = formClass;
        this.relevanceHandler.formClassChanged();

        try {
            return createWidgets().then(new Function<Void, Void>() {
                @Nullable
                @Override
                public Void apply(@Nullable Void input) {
                    addFormElements(formClass, 0);
                    return null;
                }
            });

        } catch (Throwable caught) {
            return Promise.rejected(caught);
        }
    }

    private Promise<Void> createWidgets() {
        final String resourceId = instance.getId().asString();
        return Promise.forEach(formClass.getFields(), new Function<FormField, Promise<Void>>() {
            @Override
            public Promise<Void> apply(final FormField field) {
                if (!field.isVisible()) {
                    return Promise.resolved(null); // we have join inside forEach, must return promise
                } else {
                    return widgetFactory.createWidget(resourceId, field, new ValueUpdater<FieldValue>() {
                        @Override
                        public void update(FieldValue value) {
                            onFieldUpdated(field, value);
                        }
                    }, validationFormClass, eventBus).then(new Function<FormFieldWidget, Void>() {
                        @Override
                        public Void apply(@Nullable FormFieldWidget widget) {
                            containers.put(field.getId(), containerFactory.createContainer(field, widget, 4));
                            return null;
                        }
                    });
                }
            }
        });
    }

    public Promise<Void> setValue(Resource instance) {
        this.instance = instance;
        this.workingInstance = FormInstance.fromResource(instance);

        List<Promise<Void>> tasks = Lists.newArrayList();

        for (FieldContainer container : containers.values()) {
            FormField field = container.getField();
            FieldValue value = workingInstance.get(field.getId(), field.getType());

            if(value != null && value.getTypeClass() == field.getType().getTypeClass()) {
                tasks.add(container.getFieldWidget().setValue(value));
            } else {
                container.getFieldWidget().clearValue();
            }
            container.setValid();
        }

        return Promise.waitAll(tasks).then(new Function<Void, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable Void input) {
                relevanceHandler.onValueChange(); // invoke relevance handler once values are set
                return null;
            }
        });
    }

    private void addFormElements(FormElementContainer container, int depth) {
        for (FormElement element : container.getElements()) {
            if (element instanceof FormSection) {
                panel.add(createHeader(depth, ((FormSection) element)));
                addFormElements((FormElementContainer) element, depth + 1);
            } else if (element instanceof FormField) {
                FormField formField = (FormField) element;
                if (formField.isVisible()) {
                    panel.add(containers.get(formField.getId()));
                }
            }
        }
    }

    public void onFieldUpdated(FormField field, FieldValue newValue) {
        if (!Objects.equals(workingInstance.get(field.getId()), newValue)) {
            workingInstance.set(field.getId(), newValue);
            relevanceHandler.onValueChange(); // skip handler must be applied after workingInstance is updated
        }
        validateField(containers.get(field.getId()));
    }

    private boolean validateField(FieldContainer container) {
        FormField field = container.getField();
        FieldValue value = getCurrentValue(field);
        if (value != null && value.getTypeClass() != field.getType().getTypeClass()) {
            value = null;
        }
        if (field.isRequired() && value == null && field.isVisible()) { // if field is not visible user doesn't have chance to fix it
            container.setInvalid(I18N.CONSTANTS.requiredFieldMessage());
            return false;
        } else {
            container.setValid();
            return true;
        }
    }

    public boolean validate() {
        boolean valid = true;
        for (FieldContainer container : this.containers.values()) {
            if (!validateField(container)) {
                valid = false;
            }
        }
        return valid;
    }

    private FieldValue getCurrentValue(FormField field) {
        return workingInstance.get(field.getId());
    }

    private Widget createHeader(int depth, FormSection section) {
        StringBuilder html = new StringBuilder();
        String hn = "h" + (3 + depth);
        html.append("<").append(hn).append(">")
                .append(SafeHtmlUtils.htmlEscape(section.getLabel()))
                .append("</").append(hn).append(">");
        return new HTML(html.toString());
    }

    @Override
    public Widget asWidget() {
        return withScroll ? scrollPanel : panel;
    }

    public FormClass getFormClass() {
        return formClass;
    }

    public FieldContainer getFieldContainer(ResourceId fieldId) {
        return containers.get(fieldId);
    }

    public ResourceLocator getLocator() {
        return locator;
    }

    public Map<ResourceId, FieldContainer> getContainers() {
        return containers;
    }

    public void setValidationFormClass(FormClass validationFormClass) {
        this.validationFormClass = validationFormClass;
    }

    public FormClass getValidationFormClass() {
        return validationFormClass;
    }

}
