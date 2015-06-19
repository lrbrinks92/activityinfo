package org.activityinfo.test.capacity.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.activityinfo.test.driver.ApiApplicationDriver;
import org.activityinfo.test.driver.Property;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.activityinfo.test.driver.Property.name;
import static org.activityinfo.test.driver.Property.property;

/**
 * Helper class for building a database
 */
public class DatabaseBuilder {

    private String currentDatabase = null;
    private String currentForm = null;
    private int formNumber = 1;
    private int fieldNumber = 1;

    private LinkedList<Property[]> databases = Lists.newLinkedList();
    private LinkedList<String[]> partners = Lists.newLinkedList();
    private LinkedList<Property[]> forms = Lists.newLinkedList();
    private LinkedList<Property[]> fields = Lists.newLinkedList();
    private LinkedList<Property[]> permissions = Lists.newLinkedList();
    private String currentPartner;

    public DatabaseBuilder() {

    }

    public void createDatabase(String name) throws Exception {

        Preconditions.checkNotNull(name, "databaseName");
        
        databases.add(new Property[] { name(name) });

        currentDatabase = name;
    }

    public void createForm() throws Exception {
        createForm(String.format("%s Form %d", currentDatabase, formNumber++));
    }

    public void createForm(String formName) throws Exception {
        Preconditions.checkState(currentDatabase != null, "Create a database first");

        forms.add(new Property[] {
                property("database", currentDatabase),
                property("name", formName) });

        currentForm = formName;
    }

    public void createEnumeratedField(int numItems) throws Exception {

        Preconditions.checkState(currentForm != null, "Create a form first");

        String fieldName = String.format("%s Donor %d", currentForm, fieldNumber++);

        List<String> items = Lists.newArrayList();
        for(int i=0;i<numItems;++i) {
            items.add("Choice item " + i);
        }

        fields.add(new Property[] {
                name(fieldName),
                property("form", currentForm),
                property("type", "enumerated"),
                property("items", items) });
    }

    public void createQuantityField(String fieldName) throws Exception {
        Preconditions.checkState(currentForm != null, "Create a form first");

        fields.add(new Property[]{
                property("form", currentForm),
                property("name", fieldName),
                property("type", "quantity")});
    }

    public void addPartner(String name) throws Exception {
        
        partners.add(new String[]{name, currentDatabase});
        currentPartner = name;
    }

    public void grantPermission(String user, String... allow) {
        permissions.add(new Property[] {
                property("user", user),
                property("database", currentDatabase),
                property("partner", currentPartner),
                property("permissions", Arrays.asList(allow))});
    }
    public void grantPermission(UserRole user, String... permissions) {
        grantPermission(user.getNickName(), permissions);
    }

    public void flush(ApiApplicationDriver driver) throws Exception {
        driver.startBatch();
        while(!databases.isEmpty()) {
            driver.createDatabase(databases.peek());
            databases.pop();
        }
        
        while(!forms.isEmpty()) {
            driver.createForm(forms.peek());
            forms.pop();
        }
        
        while(!fields.isEmpty()) {
            driver.createField(fields.peek());
            fields.pop();
        }

        while(!partners.isEmpty()) {
            String[] partner = partners.peek();
            driver.addPartner(partner[0], partner[1]);
            partners.pop();
        }
        
        while(!permissions.isEmpty()) {
            driver.grantPermission(permissions.peek());
            permissions.pop();
        }
        driver.submitBatch();
    }

    public void setDatabase(String databaseName) {
        currentDatabase = databaseName;
    }
}
