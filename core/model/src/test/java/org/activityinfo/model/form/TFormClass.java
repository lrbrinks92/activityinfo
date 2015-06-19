package org.activityinfo.model.form;
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

import com.google.common.base.Preconditions;
import org.hamcrest.Matcher;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;

/**
 * Tester class for FormClass
 *
 * @author yuriyz on 03/24/2015.
 * @see org.activityinfo.model.form.FormClass
 */
public class TFormClass {

    private FormClass formClass;

    public TFormClass(FormClass formClass) {
        Preconditions.checkNotNull(formClass);
        this.formClass = formClass;
    }

    public FormField getFieldByLabel(String label) {
        return find(formClass.getFields(), hasProperty("label", equalTo(label)));
    }

    public int indexOfField(String fieldLabel) {
        return indexOf(formClass.getFields(), hasProperty("label", equalTo(fieldLabel)));
    }

    private <T> T find(List<T> list, Matcher<? super T> matcher) {

        assertThat(list, hasItem(matcher));

        for (T t : list) {
            if (matcher.matches(t)) {
                return t;
            }
        }
        throw new AssertionError();
    }

    private <T> int indexOf(List<T> list, Matcher<? super T> matcher) {

        assertThat(list, hasItem(matcher));

        for (int i = 0; i != list.size(); ++i) {
            if (matcher.matches(list.get(i))) {
                return i;
            }
        }
        throw new AssertionError();
    }


    public FormClass getFormClass() {
        return formClass;
    }
}
