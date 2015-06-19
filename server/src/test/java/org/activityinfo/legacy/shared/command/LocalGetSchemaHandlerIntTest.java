package org.activityinfo.legacy.shared.command;

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

import org.activityinfo.fixtures.InjectionSupport;
import org.activityinfo.fixtures.Modules;
import org.activityinfo.fixtures.TestHibernateModule;
import org.activityinfo.legacy.shared.exception.CommandException;
import org.activityinfo.legacy.shared.model.ActivityFormDTO;
import org.activityinfo.legacy.shared.model.AttributeGroupDTO;
import org.activityinfo.legacy.shared.model.SchemaDTO;
import org.activityinfo.legacy.shared.model.UserDatabaseDTO;
import org.activityinfo.server.database.OnDataSet;
import org.activityinfo.server.endpoint.gwtrpc.GwtRpcModule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(InjectionSupport.class)
@Modules({
        TestHibernateModule.class,
        GwtRpcModule.class,
})
public class LocalGetSchemaHandlerIntTest extends LocalHandlerTestCase {

    @Test
    @OnDataSet("/dbunit/sites-simple1.db.xml")
    public void forDatabaseOwner() throws CommandException {

        synchronize();

        SchemaDTO schema = executeLocally(new GetSchema());
        assertThat(schema.getDatabases().size(), equalTo(3));
        assertThat(schema.getDatabaseById(1).isDesignAllowed(), equalTo(true));
        assertThat(schema.getDatabaseById(1).getAmOwner(), equalTo(true));
        assertThat(schema.getDatabaseById(2).getAmOwner(), equalTo(true));
        assertThat(schema.getDatabaseById(1).getOwnerName(), equalTo("Alex"));
    }

    @Test
    @OnDataSet("/dbunit/sites-simple1.db.xml")
    public void forUser() throws CommandException {

        setUser(4); // only has view access to databse 1
        synchronize();

        SchemaDTO schema = executeLocally(new GetSchema());

        assertThat(schema.getDatabases().size(), equalTo(2));

        UserDatabaseDTO pearDb = schema.getDatabaseById(1);
        assertThat(pearDb.getAmOwner(), equalTo(false));
        assertThat(pearDb.isViewAllAllowed(), equalTo(false));
        assertThat(pearDb.isEditAllowed(), equalTo(false));
        assertThat(pearDb.isEditAllAllowed(), equalTo(true));

        ActivityFormDTO activity = executeLocally(new GetActivityForm(1));
        assertThat(activity.getAttributeGroups().size(), equalTo(3));

        AttributeGroupDTO group = activity.getAttributeGroupById(1);
        assertThat(group.getName(), equalTo("cause"));
        assertThat(group.getAttributes().size(), equalTo(2));
    }
}
