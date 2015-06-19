package org.activityinfo.test.pageobject.web.components;


public abstract class ModalDialog {

    /**
     * 
     * @return the form component embedded in this modal dialog
     */
    public abstract Form form();

    public abstract void accept();
    
}
