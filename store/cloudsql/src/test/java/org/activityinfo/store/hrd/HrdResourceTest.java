package org.activityinfo.store.hrd;

import com.google.common.base.Stopwatch;
import org.activityinfo.model.auth.AuthenticatedUser;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.form.FormField;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.model.resource.FolderProjection;
import org.activityinfo.model.resource.Resource;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.resource.ResourceNode;
import org.activityinfo.model.resource.Resources;
import org.activityinfo.model.resource.UserResource;
import org.activityinfo.model.system.Folder;
import org.activityinfo.model.system.FolderClass;
import org.activityinfo.model.type.number.QuantityType;
import org.activityinfo.model.type.primitive.TextType;
import org.activityinfo.service.store.CommitStatus;
import org.activityinfo.service.store.FolderRequest;
import org.activityinfo.service.store.UpdateResult;
import org.activityinfo.store.EntityDeletedException;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class HrdResourceTest {

    @Rule
    public TestingEnvironment environment = new TestingEnvironment();

    @Test
    public void createWorkspace() {

        AuthenticatedUser me = environment.getUser();

        FormInstance workspace = createWorkspace("Workspace A");
        environment.getStore().create(me, workspace.asResource());

        List<ResourceNode> workspaces = environment.getStore().getOwnedOrSharedWorkspaces(me);
        assertThat(workspaces, hasSize(1));
    }

    @Test
    public void simple() throws IOException, SQLException {

        // Create a root folder

        FormInstance divA = createDivAFolder();

        UpdateResult divACreationResult = environment.getStore()
                .create(environment.getUser(), divA.asResource());

        assertThat(divACreationResult, hasProperty("status", equalTo(CommitStatus.COMMITTED)));

        Resource resource = environment.getStore().get(environment.getUser(), divA.getId()).getResource();
        assertThat(resource.getValue().getString(FolderClass.LABEL_FIELD_ID.asString()), equalTo("Division A"));
        assertThat(resource.getVersion(), equalTo(divACreationResult.getNewVersion()));

        // Ensure that this stuff gets cached
        Stopwatch stopwatch = Stopwatch.createStarted();
        int requestCount = 10;
        for (int i = 0; i != requestCount; ++i) {
            FolderProjection tree = environment.getStore().queryTree(
                    environment.getUser(),
                    new FolderRequest(divA.getId()));
            assertThat(tree.getRootNode().getId(), equalTo(divA.getId()));
            assertThat(tree.getRootNode().getLabel(), equalTo("Division A"));
            assertThat(tree.getRootNode().getVersion(), equalTo(divACreationResult.getNewVersion()));
            //assertThat(tree.getRootNode().getSubTreeVersion(), equalTo(divACreationResult.getNewVersion()));
        }
        System.out.println((requestCount / (double) stopwatch.elapsed(TimeUnit.SECONDS)) + " requests per second");

        // Create form class

        FormClass formClass = createWidgetsFormClass(divA.getId());
        UpdateResult formCreationResult = environment.getStore()
                .create(environment.getUser(), formClass.asResource());

        assertThat(formCreationResult.getStatus(), Matchers.equalTo(CommitStatus.COMMITTED));

        // Read the form class back and verify

        FormClass reformClass = FormClass.fromResource(
                environment.getStore().get(environment.getUser(), formClass.getId()).getResource());

        assertThat(reformClass.getId(), Matchers.equalTo(formClass.getId()));
        assertThat(reformClass.getOwnerId(), Matchers.equalTo(formClass.getOwnerId()));

        // Verify that the subtree version gets updated

        FolderProjection tree = environment.getStore().queryTree(
                environment.getUser(),
                new FolderRequest(divA.getId()));
        assertThat(tree.getRootNode().getVersion(), equalTo(divACreationResult.getNewVersion()));
        assertThat(tree.getRootNode().getId(), Matchers.equalTo(divA.getId()));
        assertThat(tree.getRootNode().getChildren().get(0).getId(), Matchers.equalTo(formClass.getId()));
        assertThat(tree.getRootNode().getChildren().get(0).getOwnerId(), Matchers.equalTo(divA.getId()));
    }


    @Test
    public void update() throws IOException, SQLException {

        // Create a root folder

        FormInstance workspace = createWorkspace("Workspace A");
        UpdateResult creationResult = environment.getStore()
                .create(environment.getUser(), workspace.asResource());

        assertThat(creationResult, hasProperty("status", equalTo(CommitStatus.COMMITTED)));

        workspace.set(FolderClass.LABEL_FIELD_ID, "Workspace B");
        UpdateResult updateResult = environment.getStore()
                .create(environment.getUser(), workspace.asResource());

        assertThat(updateResult, hasProperty("status", equalTo(CommitStatus.COMMITTED)));

        Resource newlyFetched = environment.getStore().get(environment.getUser(), workspace.getId()).getResource();
        assertThat(newlyFetched.getValue().getString(FolderClass.LABEL_FIELD_ID.asString()), Matchers.equalTo("Workspace B"));
    }

    @Test
    public void delete() {
        FormInstance workspace = createWorkspace("Workspace A");
        Resource folderLevel1 = createFolder("Level 1", workspace.getId());
        Resource folderLevel2 = createFolder("Level 2", folderLevel1.getId());
        Resource folderLevel3 = createFolder("Level 3", folderLevel2.getId());

        createInStore(workspace.asResource());
        createInStore(folderLevel1);
        createInStore(folderLevel2);
        createInStore(folderLevel3);

        FolderProjection folderProjection = environment.getStore().queryTree(environment.getUser(), new FolderRequest(folderLevel1.getId()));
        assertThat(folderProjection.getRootNode().getChildren().size(), Matchers.equalTo(1));

        try {
            environment.getStore().get(AuthenticatedUser.getAnonymous(), folderLevel1.getId());
        } catch (WebApplicationException e) {
            assertThat(e.getResponse().getStatus(), Matchers.equalTo(Response.Status.UNAUTHORIZED.getStatusCode()));
        }

        // delete leaf
        assertCommitted(environment.getStore().delete(environment.getUser(), folderLevel3.getId()));

        assertDeleted(folderLevel3.getId(), true);
        assertDeleted(folderLevel2.getId(), false);

        // delete node (level1) that has child (level2) - child (level2) must be marked as deleted automatically
        assertCommitted(environment.getStore().delete(environment.getUser(), folderLevel1.getId()));

        assertDeleted(folderLevel1.getId(), true);
        assertDeleted(folderLevel2.getId(), true);

        // GONE for authorized user
        try {
            environment.getStore().get(environment.getUser(), folderLevel1.getId());
        } catch (WebApplicationException e) {
            assertThat(e.getResponse().getStatus(), Matchers.equalTo(Response.Status.GONE.getStatusCode()));
        }

        // UNAUTHORIZED for anonymous user
        try {
            environment.getStore().get(AuthenticatedUser.getAnonymous(), folderLevel1.getId());
        } catch (WebApplicationException e) {
            assertThat(e.getResponse().getStatus(), Matchers.equalTo(Response.Status.UNAUTHORIZED.getStatusCode()));
        }

        // GONE for ACRs
        try {
            environment.getStore().getAccessControlRules(environment.getUser(), folderLevel2.getId());
        } catch (WebApplicationException e) {
            assertThat(e.getResponse().getStatus(), Matchers.equalTo(Response.Status.GONE.getStatusCode()));
        }

    }

    @Test
    public void getWorkspace() {

        FormInstance folder1 = createWorkspace("Folder 1");
        FormInstance folder2 = createWorkspace("Folder 2");

        assertCommitted(environment.getStore().create(environment.getUser(), folder1.asResource()));
        assertCommitted(environment.getStore().create(environment.getUser(), folder2.asResource()));

        List<ResourceNode> roots = environment.getStore().getOwnedOrSharedWorkspaces(environment.getUser());

        assertThat(roots, hasSize(2));
        assertThat(roots, containsInAnyOrder(
                hasProperty("label", equalTo("Folder 1")),
                hasProperty("label", equalTo("Folder 2"))));

    }

    private FormClass createWidgetsFormClass(ResourceId ownerId) {
        FormClass formClass = new FormClass(environment.generateId());
        formClass.setOwnerId(ownerId);
        formClass.setLabel("Widgets");

        FormField nameField = new FormField(Resources.generateId());
        nameField.setLabel("Name");
        nameField.setType(TextType.INSTANCE);
        formClass.addElement(nameField);

        FormField countField = new FormField(Resources.generateId());
        countField.setLabel("Count");
        countField.setType(new QuantityType().setUnits("widgets"));
        formClass.addElement(countField);
        return formClass;
    }

    private FormInstance createDivAFolder() {
        return createWorkspace("Division A");
    }

    private FormInstance createWorkspace(String label) {
        FormInstance divA = new FormInstance(environment.generateWorkspaceId(), FolderClass.CLASS_ID);
        divA.setOwnerId(Resources.ROOT_ID);
        divA.set(FolderClass.LABEL_FIELD_ID, label);
        return divA;
    }

    private Resource createFolder(String label, ResourceId parent) {
        Folder newFolder = new Folder();
        newFolder.setLabel(label);

        return Resources.newResource(parent, newFolder);
    }

    public void createInStore(Resource resource) {
        UpdateResult result = environment.getStore()
                .create(environment.getUser(), resource);
        assertCommitted(result);
    }

    private void assertCommitted(UpdateResult resource) {
        assertThat(resource.getStatus(), Matchers.equalTo(CommitStatus.COMMITTED));
    }

    private void assertDeleted(ResourceId resourceId, boolean deleted) {
        try {
            UserResource userResource = environment.getStore().get(environment.getUser(), resourceId);
            if (!deleted) {
                return;
            }
        } catch (EntityDeletedException | IllegalStateException e) {
            if (deleted) {
                return; // as expected we got "deleted" exception
            }
        } catch (WebApplicationException e) {
            if (deleted && e.getResponse().getStatus() == Response.Status.GONE.getStatusCode()) {
                return; // as expected we got "deleted" exception
            }
        }
        throw new AssertionError("Resource 'deleted' flag does not match expected value, resourceId: " +
                resourceId + ", expected: " + deleted);

    }
}