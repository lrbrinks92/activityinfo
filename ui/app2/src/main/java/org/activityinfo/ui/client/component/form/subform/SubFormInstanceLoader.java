package org.activityinfo.ui.client.component.form.subform;
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

import com.google.common.base.Function;
import com.google.common.collect.Sets;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.promise.Promise;
import org.activityinfo.ui.client.component.form.FormModel;

import java.util.List;
import java.util.Set;

/**
 * @author yuriyz on 01/22/2016.
 */
public class SubFormInstanceLoader {

    private final FormModel model;

    // keep copy of persisted instances, we need to track persisted instance to remove them on save if they were deleted by user.
    private final Set<FormInstance> persisted = Sets.newHashSet();

    public SubFormInstanceLoader(FormModel model) {
        this.model = model;
    }

    public Promise<List<FormInstance>> load(final FormClass subForm) {
        return model.getLocator().getSubFormInstances(subForm.getId(), model.getWorkingRootInstance().getId())
                .then(new Function<List<FormInstance>, List<FormInstance>>() {
                    @Override
                    public List<FormInstance> apply(List<FormInstance> instanceList) {
                        model.getSubFormInstances().put(new FormModel.SubformValueKey(subForm, model.getWorkingRootInstance()), Sets.newHashSet(instanceList));
                        persisted.addAll(instanceList);
                        return instanceList;
                    }
                });
    }

    public boolean isPersisted(FormInstance instance) {
        return persisted.contains(instance);
    }
}
