package org.activityinfo.ui.client.service;

import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.formTree.FormTree;
import org.activityinfo.model.query.ColumnSet;
import org.activityinfo.model.query.QueryModel;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.observable.Observable;

public interface FormService {

    Observable<FormClass> getFormClass(ResourceId formId);

    Observable<FormTree> getFormTree(ResourceId formId);

    Observable<ColumnSet> query(QueryModel queryModel);
}
