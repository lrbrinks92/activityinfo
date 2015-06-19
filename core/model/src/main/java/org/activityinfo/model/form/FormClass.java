package org.activityinfo.model.form;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.activityinfo.model.resource.*;

import javax.annotation.Nullable;
import javax.annotation.Nonnull;
import java.util.List;

/**
 * The FormClass defines structure and semantics for {@code Resource}s.
 *
 * {@code Resources} which fulfill the contract described by a {@code FormClass}
 * are called {@code FormInstances}.

 */
public class FormClass implements IsResource, FormElementContainer {


    /**
     * Because FormClasses are themselves FormInstances, they have a class id of their own
     */
    public static final ResourceId CLASS_ID = ResourceId.valueOf("_class");

    /**
     * Instances of FormClass have one FormField: a label, which has its own
     * FormField id. It is defined at the application level to be a subproperty of
                                     * {@code _label}
                                     */
    public static final String LABEL_FIELD_ID = "_class_label";


    @Nonnull
    private ResourceId id;
    private ResourceId ownerId;

    private String label;
    private String description;
    private final List<FormElement> elements = Lists.newArrayList();

    public FormClass(ResourceId id) {
        Preconditions.checkNotNull(id);
        this.id = id;
    }

    public ResourceId getOwnerId() {
        return ownerId;
    }

    public FormClass setOwnerId(ResourceId ownerId) {
        this.ownerId = ownerId;
        return this;
    }

    public ResourceId getParentId() { return ownerId; }

    public void setParentId(ResourceId resourceId) {
        setOwnerId(resourceId);
    }

    public FormElementContainer getParent(FormElement childElement) {
        return getContainerElementsImpl(this, childElement);
    }

    private static FormElementContainer getContainerElementsImpl(FormElementContainer container, final FormElement searchElement) {
        if (container.getElements().contains(searchElement)) {
            return container;
        }
        for (FormElement elem : container.getElements()) {
            if (elem instanceof FormElementContainer) {
                final FormElementContainer result = getContainerElementsImpl((FormElementContainer) elem, searchElement);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    public void traverse(FormElementContainer element, TraverseFunction traverseFunction) {
        for (FormElement elem : Lists.newArrayList(element.getElements())) {
            traverseFunction.apply(elem, element);
            if (elem instanceof FormElementContainer) {
                traverse((FormElementContainer) elem, traverseFunction);
            }
        }
    }

    public void remove(final FormElement formElement) {
        traverse(this, new TraverseFunction() {
            @Override
            public void apply(FormElement element, FormElementContainer container) {
                if (element.equals(formElement)) {
                    container.getElements().remove(formElement);
                }
            }
        });
    }

    @Override
    public ResourceId getId() {
        return id;
    }

    public void setId(ResourceId id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public FormClass setLabel(String label) {
        this.label = label;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public List<FormElement> getElements() {
        return elements;
    }

    public List<FormField> getFields() {
        final List<FormField> fields = Lists.newArrayList();
        collectFields(fields, getElements(), new Predicate<FormElement>() {
            @Override
            public boolean apply(@Nullable FormElement input) {
                return input instanceof FormField;
            }
        });
        return fields;
    }

    public List<FormSection> getSections() {
        final List<FormSection> sections = Lists.newArrayList();
        collectFields(sections, getElements(), new Predicate<FormElement>() {
            @Override
            public boolean apply(@Nullable FormElement input) {
                return input instanceof FormSection;
            }
        });
        return sections;

    }

    private static void collectFields(List result, List<FormElement> elements, Predicate<FormElement> predicate) {
        for (FormElement element : elements) {

            if (predicate.apply(element)) {
                result.add(element);
            }

            if (element instanceof FormField) {
                // do nothing
            } else if (element instanceof FormSection) {
                final FormSection formSection = (FormSection) element;
                collectFields(result, formSection.getElements(), predicate);
            }
        }
    }

    public FormField getField(ResourceId fieldId) {
        for(FormField field : getFields()) {
            if(field.getId().equals(fieldId)) {
                return field;
            }
        }
        throw new IllegalArgumentException("No such field: " + fieldId);
    }

    @Override
    public FormClass addElement(FormElement element) {
        elements.add(element);
        return this;
    }


    public FormField addField(ResourceId fieldId) {
        FormField field = new FormField(fieldId);
        elements.add(field);
        return field;
    }

    public FormClass insertElement(int index, FormElement element) {
        elements.add(index, element);
        return this;
    }

    @Override
    public String toString() {
        return "<FormClass: " + getLabel() + ">";
    }

    public static FormClass fromResource(Resource resource) {
        FormClass formClass = new FormClass(resource.getId());
        formClass.setOwnerId(resource.getOwnerId());
        formClass.setLabel(Strings.nullToEmpty(resource.isString(LABEL_FIELD_ID)));
        formClass.elements.addAll(fromRecords(resource.getRecordList("elements")));
        return formClass;
    }

    private static List<FormElement> fromRecords(List<Record> elementArray) {
        List<FormElement> elements = Lists.newArrayList();
        for(Record elementRecord : elementArray) {
            if("section".equals(elementRecord.isString("type"))) {
                FormSection section = new FormSection(ResourceId.valueOf(elementRecord.getString("id")));
                section.setLabel(elementRecord.getString("label"));
                section.getElements().addAll(fromRecords(elementRecord.getRecordList("elements")));
                elements.add(section);
            } else {
                elements.add(FormField.fromRecord(elementRecord));
            }
        }
        return elements;
    }

    @Override
    public Resource asResource() {
        Resource resource = Resources.createResource();
        resource.setId(id);
        resource.setOwnerId(ownerId);
        resource.set("classId", CLASS_ID);
        resource.set(LABEL_FIELD_ID, label);
        resource.set("elements", FormElement.asRecordList(elements));
        return resource;
    }

}
