package org.activityinfo.ui.client.widget;
/*
 * #%L
 * ActivityInfo Server
 * %%
 * Copyright (C) 2009 - 2013 UNICEF
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.google.common.collect.Lists;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Composite;
import org.activityinfo.ui.client.util.GwtUtil;

import java.util.Arrays;
import java.util.List;

/**
 * @author yuriyz on 2/25/14.
 */
public class CompositeWithMirror extends Composite {

    private final List<Element> mirrorElements = Lists.newArrayList();

    @Override
    public void setVisible(boolean visible) {
        GwtUtil.setVisible(visible, getElement());
        for (Element mirrorElement : mirrorElements) {

            GwtUtil.setVisible(!visible, mirrorElement);
        }
    }

    public List<Element> getMirrorElements() {
        return mirrorElements;
    }

    public void setMirrorElements(Element... mirrorElements) {
        getMirrorElements().clear();
        if (mirrorElements != null) {
            getMirrorElements().addAll(Arrays.asList(mirrorElements));
        }
    }
}
