package org.activityinfo.ui.client.component.importDialog;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import org.activityinfo.core.client.ResourceLocator;
import org.activityinfo.core.client.form.tree.AsyncFormTreeBuilder;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.formTree.FormTree;
import org.activityinfo.core.shared.importing.model.ImportModel;
import org.activityinfo.core.shared.importing.model.MapExistingAction;
import org.activityinfo.core.shared.importing.strategy.FieldImportStrategies;
import org.activityinfo.core.shared.importing.strategy.ImportTarget;
import org.activityinfo.promise.Promise;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.ui.client.component.importDialog.mapping.ColumnMappingPage;
import org.activityinfo.ui.client.component.importDialog.source.ChooseSourcePage;
import org.activityinfo.ui.client.component.importDialog.validation.ValidationPage;
import org.activityinfo.ui.client.widget.FullScreenOverlay;
import org.activityinfo.ui.client.widget.ModalDialog;

import java.util.List;

public class ImportPresenter {

    private final EventBus eventBus = GWT.create(SimpleEventBus.class);

    private final ImportModel importModel;
    private final Importer importer;

    private ImportDialog dialogBox = new ImportDialog();
    private FullScreenOverlay overlay = new FullScreenOverlay();

    private List<ImportPage> pages;
    private ImportPage currentPage;

    public ImportPresenter(ResourceLocator resourceLocator, FormTree formTree) {
        this.importModel = new ImportModel(formTree);
        this.importer = new Importer(resourceLocator, formTree, FieldImportStrategies.get());

        final ChooseSourcePage chooseSourcePage = new ChooseSourcePage(importModel, eventBus);
        final ColumnMappingPage matchingPage = new ColumnMappingPage(importModel, createMatchingColumnActions(), eventBus);
        final ValidationPage validationPage = new ValidationPage(importModel, importer);

        pages = Lists.<ImportPage>newArrayList(chooseSourcePage, matchingPage, validationPage);

        dialogBox.getPreviousButton().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                previousPage();
            }
        });

        dialogBox.getNextButton().addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                nextPage();
            }
        });

        dialogBox.getCancelButton().addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                overlay.hide();
            }
        });

        dialogBox.getFinishButton().addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                int invalidRowsCount = validationPage.getInvalidRowsCount();
                if (invalidRowsCount > 0) {
                    final ModalDialog confirmDialog = new ModalDialog();
                    confirmDialog.getModalBody().add(new HTML(I18N.MESSAGES.continueImportWithInvalidRows(invalidRowsCount)));
                    confirmDialog.getPrimaryButton().addClickHandler(new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            confirmDialog.hide();
                            persistData();
                        }
                    });
                    confirmDialog.show();
                } else { // all rows are valid -> persist directly
                    persistData();
                }
            }
        });

        dialogBox.getTitleWidget().setText(I18N.CONSTANTS.importDialogTitle());

        eventBus.addHandler(PageChangedEvent.TYPE, new PageChangedEventHandler() {
            @Override
            public void onPageChanged(PageChangedEvent event) {
                if (event.isValid()) {
                    dialogBox.getStatusText().removeClassName("alert");
                } else {
                    dialogBox.getStatusText().addClassName("alert");
                }
                dialogBox.setStatusText(event.getStatusMessage());
            }
        });
        setButtonsState();
    }

    private List<MapExistingAction> createMatchingColumnActions() {
        final List<MapExistingAction> columnActions = Lists.newArrayList();
        final List<ImportTarget> importTargets = importer.getImportTargets();
        for (ImportTarget target : importTargets) {
            columnActions.add(new MapExistingAction(target));
        }
        return columnActions;
    }

    protected void persistData() {

        dialogBox.getFinishButton().setEnabled(false);
        dialogBox.setStatusText(I18N.CONSTANTS.importing());

        importer.persist(importModel).then(new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                showDelayedFailure(caught);
            }

            @Override
            public void onSuccess(Void result) {
                overlay.hide();
                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        eventBus.fireEvent(new ImportResultEvent(true));
                    }
                });
            }
        });
    }

    private void showDelayedFailure(final Throwable caught) {
        // Show failure message only after a short fixed delay to ensure that
        // the progress stage is displayed. Otherwise if we have a synchronous error, clicking
        // the retry button will look like it's not working.
        Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                dialogBox.setStatusText(I18N.CONSTANTS.importFailed());
                dialogBox.getFinishButton().setText(I18N.CONSTANTS.retry());
                dialogBox.getFinishButton().setEnabled(true);
                eventBus.fireEvent(new ImportResultEvent(false));
                return false;
            }
        }, 500);
    }

    public void show() {
        gotoPage(pages.get(0));
        overlay.show(dialogBox);
    }

    private void gotoPage(ImportPage page) {
        currentPage = page;
        currentPage.start();
        dialogBox.setPage(currentPage);
        setButtonsState();
    }

    private void nextPage() {
        int nextIndex = pages.indexOf(currentPage) + 1;
        if (nextIndex < pages.size()) {
            if (currentPage.isValid()) {
                gotoPage(pages.get(nextIndex));
            } else {
                currentPage.fireStateChanged();
            }
        }
    }

    private void previousPage() {
        int prevIndex = pages.indexOf(currentPage) - 1;
        if (prevIndex >= 0) {
            gotoPage(pages.get(prevIndex));
        }
        dialogBox.setStatusText(""); // clear status text
    }

    private void setButtonsState() {
        int index = pages.indexOf(currentPage);
        dialogBox.getPreviousButton().setEnabled((index-1) >= 0);
        dialogBox.getNextButton().setEnabled((index+1) < pages.size());
        dialogBox.getFinishButton().setVisible((index+1) == pages.size() && currentPage.isValid());
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public static Promise<ImportPresenter> showPresenter(ResourceId activityId, final ResourceLocator resourceLocator) {
        AsyncFormTreeBuilder treeBuilder = new AsyncFormTreeBuilder(resourceLocator);
        return treeBuilder.apply(activityId).then(new Function<FormTree, ImportPresenter>() {
            @Override
            public ImportPresenter apply(FormTree input) {
                return new ImportPresenter(resourceLocator, input);
            }
        });
    }
}
