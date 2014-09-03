package org.activityinfo.ui.app.client.page.folder;

import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.resource.ResourceNode;
import org.activityinfo.model.system.FolderClass;
import org.activityinfo.ui.app.client.page.form.FormPlace;
import org.activityinfo.ui.app.client.page.form.FormViewType;
import org.activityinfo.ui.app.client.store.Router;
import org.activityinfo.ui.style.BaseStyles;
import org.activityinfo.ui.style.Grid;
import org.activityinfo.ui.style.Media;
import org.activityinfo.ui.style.Panel;
import org.activityinfo.ui.style.icons.FontAwesome;
import org.activityinfo.ui.vdom.shared.html.Icon;
import org.activityinfo.ui.vdom.shared.tree.*;

import static org.activityinfo.ui.vdom.shared.html.H.*;

public class FolderView extends VThunk<FolderView> {

    private FolderPage page;

    public FolderView(FolderPage page) {
        this.page = page;
    }

    @Override
    public VTree render() {

        return div(BaseStyles.CONTENTPANEL,
                div(BaseStyles.ROW,
                        listColumn(page),
                        timelineColumn(),
                        helpColumn()));
    }

    private static VTree listColumn(FolderPage page) {

        return Grid.column(5,  childTable(page));

    }

    private static VTree childTable(FolderPage page) {
        return new Panel(
        div(className(BaseStyles.WIDGET_BLOGLIST), map(page.getChildNodes(), new Render<ResourceNode>() {
                    @Override
                    public VTree render(ResourceNode item) {
                        return media(item);
                    }
                })));
    }

    private static VTree media(ResourceNode child) {
        return Media.media(childIcon(child),
                link(child),
                t(child.getLabel()), description(child));
    }

    private static SafeUri link(ResourceNode node) {
        if(node.getClassId().equals(FormClass.CLASS_ID)) {
            return Router.uri(new FormPlace(node.getId(), FormViewType.TABLE));
        } else if(node.getClassId().equals(FolderClass.CLASS_ID)) {
            return Router.uri(new FolderPlace(node.getClassId()));
        } else {
            return UriUtils.fromTrustedString("#");
        }
    }

    private static VTree description(ResourceNode child) {
        return t("Something descriptive here");
    }

    private static VTree childIcon(ResourceNode child) {
        Icon icon;
        if(FormClass.CLASS_ID.equals(child.getClassId())) {
            icon = FontAwesome.EDIT;
        } else {
            icon = FontAwesome.FOLDER_OPEN_O;
        }

        Style iconStyle = new Style().fontSize(50).lineHeight(50);
        PropMap iconProps = new PropMap();
        iconProps.setStyle(iconStyle);
        iconProps.setClass(icon.getCssClass());

        return div(className(BaseStyles.MEDIA_OBJECT), span(iconProps));
    }

    private static VNode childIcon() {
        return FontAwesome.FOLDER_OPEN_O.render();
    }

    private static VTree timelineColumn() {
        return Grid.column(4, new Panel("Recent Activity", p("Todo...")));
    }

    private static VTree helpColumn() {
        return Grid.column(3,
                new Panel("Common Tasks", p("Todo...")),
                new Panel("Administration", p("Todo..."))
                );
    }

}
