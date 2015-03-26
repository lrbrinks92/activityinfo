package org.activityinfo.core.client;
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

import org.activityinfo.core.shared.Projection;
import org.activityinfo.core.shared.criteria.Criteria;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.model.resource.IsResource;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.promise.Promise;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author yuriyz on 03/26/2015.
 */
public class ResourceLocatorStub implements ResourceLocator {

    @Override
    public Promise<FormClass> getFormClass(ResourceId formId) {
        return Promise.rejected(new UnsupportedOperationException());
    }

    @Override
    public Promise<FormInstance> getFormInstance(ResourceId formId) {
        return Promise.rejected(new UnsupportedOperationException());
    }

    @Override
    public Promise<Void> persist(IsResource resource) {
        return Promise.rejected(new UnsupportedOperationException());
    }

    @Override
    public Promise<Void> persist(List<? extends IsResource> resources) {
        return Promise.rejected(new UnsupportedOperationException());
    }

    @Override
    public Promise<List<FormInstance>> queryInstances(Criteria criteria) {
        return Promise.rejected(new UnsupportedOperationException());
    }

    @Override
    public Promise<QueryResult<Projection>> queryProjection(InstanceQuery query) {
        return Promise.rejected(new UnsupportedOperationException());
    }

    @Override
    public Promise<List<Projection>> query(InstanceQuery query) {
        return Promise.rejected(new UnsupportedOperationException());
    }

    @Override
    public Promise<Void> remove(Collection<ResourceId> resources) {
        return Promise.rejected(new UnsupportedOperationException());
    }

    @Override
    public Promise<List<FormInstance>> queryInstances(Set<ResourceId> resourceIds) {
        return Promise.rejected(new UnsupportedOperationException());
    }
}
