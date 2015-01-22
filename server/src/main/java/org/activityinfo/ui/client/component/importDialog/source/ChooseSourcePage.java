package org.activityinfo.ui.client.component.importDialog.source;

import com.google.common.base.Strings;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.Widget;
import org.activityinfo.core.client.type.converter.JsConverterFactory;
import org.activityinfo.core.shared.importing.model.ImportModel;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.ui.client.component.importDialog.ImportPage;
import org.activityinfo.ui.client.component.importDialog.PageChangedEvent;
import org.activityinfo.ui.client.component.importDialog.data.PastedTable;
import org.activityinfo.ui.client.widget.TextArea;

/**
 * Start page for the table import process that prompts the user
 * for pasted table data
 */
public class ChooseSourcePage extends ResizeComposite implements ImportPage {


    private static ChoosePageUiBinder uiBinder = GWT
            .create(ChoosePageUiBinder.class);

    interface ChoosePageUiBinder extends UiBinder<Widget, ChooseSourcePage> {
    }

    private ImportModel model;
    private final EventBus eventBus;

    @UiField
    TextArea textArea;

    public ChooseSourcePage(ImportModel model, EventBus eventBus) {
        this.model = model;
        this.eventBus = eventBus;
        initWidget(uiBinder.createAndBindUi(this));
        textArea.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                fireStateChanged();
            }
        });
    }

    public void fireStateChanged() {

        // validate
        boolean valid = false;
        PastedTable pastedTable = null;
        try {
            pastedTable = new PastedTable(textArea.getValue());
            valid = !pastedTable.getRows().isEmpty();
        } catch (Exception e) {
            // ignore : text is not valid
        }

        if (valid) {
            pastedTable.guessColumnsType(JsConverterFactory.get());
            model.setSource(pastedTable);
            eventBus.fireEvent(new PageChangedEvent(true, ""));
        } else {
            eventBus.fireEvent(new PageChangedEvent(false, errorMessage(pastedTable)));
        }
    }

    private String errorMessage(PastedTable pastedTable) {
        if (pastedTable != null && pastedTable.getFirstInvalidRow() > 0) {
            return I18N.MESSAGES.columnCountMismatchAtRow(pastedTable.getFirstInvalidRow());
        }
        return I18N.CONSTANTS.pleaseProvideCommaSeparatedText();
    }

    @UiHandler("textArea")
    public void onTextChanged(ChangeEvent event) {
        fireStateChanged();
    }

    @Override
    public boolean isValid() {
        if (!Strings.isNullOrEmpty(textArea.getValue())) {
            try {
                final PastedTable pastedTable = new PastedTable(textArea.getValue());
                return !pastedTable.getRows().isEmpty();
            } catch (Exception e) {
                // ignore : text is not valid
            }
        }
        return false;
    }

    @Override
    public boolean hasNextStep() {
        return false;
    }

    @Override
    public boolean hasPreviousStep() {
        return false;
    }

    @Override
    public void start() {

    }

    @Override
    public void nextStep() {
    }

    @Override
    public void previousStep() {
    }
}
