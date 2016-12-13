package org.activityinfo.ui.icons;

/**
 * Application-wide icons
 */
public class Icons  {

    public static final Icons INSTANCE = new Icons();
    
    public void ensureInjected() {
        IconClientBundle.INSTANCE.iconStyle().ensureInjected();
    }

    //@Source(value = "icons/fontawesome.svg", glyph = 0xf055)
    public String add() {
        return "icon icon_add";
    }

    //@Source(value = "icons/fontawesome.svg", glyph = 0xf0d7)
    public String caretDown() {
        return "icon icon_caretDown";
    }

    //@Source(value = "icons/icomoon/plus.svg")
    public String plus() {
        return "icon icon_plus";
    }

    public String minus() {
        return "icon icon_minus";
    }

    //@Source(value = "icons/icomoon/remove.svg")
    public String remove() {
        return "icon icon_remove";
    }

    //@Source(value = "icons/icomoon/pencil.svg")
    public String edit() {
        return "icon icon_edit";
    }

    //@Source(value = "icons/icomoon/close.svg")
    public String close() {
        return "icon icon_close";
    }

    //@Source(value = "icons/icomoon/cloud-download.svg")
    public String cloudDownload() {
        return "icon icon_cloud_download";
    }

    //@Source(value = "icons/icomoon/filter.svg")
    public String filter() {
        return "icon icon_filter";
    }

    //@Source(value = "icons/icomoon/file.svg")
    public String file() {
        return "icon icon_file";
    }

    //@Source(value = "icons/icomoon/wrench.svg")
    public String wrench() {
        return "icon icon_wrench";
    }

    //@Source(value = "icons/ocha.svg", glyph = 0xe61a)
    public String form() {
        return "icon icon_file";
    }

    public String database() {
        return "icon icon_database";
    }

    //@Source(value = "icons/ocha.svg", glyph = 0xe6c4)
    public String location() {
        return "icon icon_file";
    }

    //@Source(value = "icons/fontawesome.svg", glyph = 0xf115)
    public String folder() {
        return "icon icon_folderOpen";
    }

    //@Source(value = "icons/fontawesome.svg", glyph = 0xf114)
    public String folderOpen() {
        return "icon icon-folderOpen";
    }

    //@Source(value = "icons/fontawesome.svg", glyph = 0xf057)
    public String delete() {
        return "icon icon-delete";
    }

    //@Source("icons/icomoon/mobile.svg")
    public String mobileDevice() {
        return "icon icon-mobile";
    }

    //@Source("icons/icomoon/file-excel.svg")
    public String excelFile() {
        return "icon icon_file-excel";
    }

    //@Source("icons/icomoon/file-pdf.svg")
    public String filePdf() {
        return "icon icon_file-pdf";
    }

    //@Source("icons/icomoon/file-word.svg")
    public String fileWord() {
        return "icon icon_file-word";
    }

    //@Source("icons/icomoon/cloud-upload.svg")
    public String importIcon() {
        return "icon icon_import";
    }

    //@Source("icons/icomoon/table.svg")
    public String table() {
        return "icon icon_table";
    }

    //@Source("icons/icomoon/earth.svg")
    public String map() {
        return "icon icon_earth";
    }

    //@Source("icons/icomoon/info.svg")
    public String overview() {
        return "icon icon_info";
    }

    //@Source("icons/icomoon/arrow-up.svg")
    public String arrowUp() {
        return "icon icon_arrow-up";
    }

    //@Source("icons/icomoon/arrow-down.svg")
    public String arrowDown() {
        return "icon icon_arrow-down";
    }

    //@Source("icons/icomoon/arrow-left.svg")
    public String arrowLeft() {
        return "icon icon_arrow-left";
    }

    //@Source("icons/icomoon/arrow-right.svg")
    public String arrowRight() {
        return "icon icon_arrow-right";
    }

    /**
     * Symbolizes a problem connecting to the server
     */
    //@Source("icons/disconnected.svg")
    public String connectionProblem() {
        return "icon icon_connectionProblem";
    }

    /**
     * Symbolizes an unexpected exception (that is, a bug!)
     */
    //@Source(value = "icons/fontawesome.svg", glyph = 0xf188)
    public String exception() {
        return "icon icon_exception";
    }

    public String bars() {
        return "icon icon_bars";
    }
    
    public String print() { return "icon icon_print"; }

    public String home() {
        return "icon icon_home";
    }
}
