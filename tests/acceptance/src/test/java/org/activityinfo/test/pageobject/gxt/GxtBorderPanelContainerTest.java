package org.activityinfo.test.pageobject.gxt;

import org.activityinfo.test.pageobject.api.Style;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class GxtBorderPanelContainerTest  {

    @Test
    public void parse() {
        Style style = new Style("left: 0px; top: 34px; width: 300px;");
        assertThat(style.getLeft(), equalTo(0));
        assertThat(style.getTop(), equalTo(34));
        assertThat(style.getWidth(), equalTo(300));
    }
}