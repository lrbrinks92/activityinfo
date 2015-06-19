package org.activityinfo.server.database.hibernate;

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

import com.bedatadriven.appengine.cloudsql.CloudSqlFilter;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;
import org.activityinfo.server.database.hibernate.dao.HibernateDAOModule;
import org.activityinfo.server.database.hibernate.dao.TransactionModule;
import org.activityinfo.service.DeploymentConfiguration;
import org.hibernate.Session;
import org.hibernate.ejb.HibernateEntityManager;
import org.hibernate.validator.HibernateValidator;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

public class HibernateModule extends ServletModule {

    @Override
    protected void configureServlets() {

        /**
         * Define a scope for the EntityManager that linked to the 
         * start and end of a request
         */
        HibernateSessionScope sessionScope = new HibernateSessionScope();
        bindScope(HibernateSessionScoped.class, sessionScope);
        bind(HibernateSessionScope.class).toInstance(sessionScope);
        
        bind(EntityManager.class).toProvider(EntityManagerProvider.class).in(HibernateSessionScoped.class);

        /*
         * Important: the CloudSqlFilter must be listed before
         * the HibernateSessionFilter as otherwise the CloudSql filter
         * will cleanup the connection before Hibernate has a chance to clean
         * up the associated EntityManager
         */
        
        bind(CloudSqlFilter.class).in(Singleton.class);
        filter("/*").through(CloudSqlFilter.class);
        
        filter("/*").through(HibernateSessionFilter.class);

        install(new HibernateDAOModule());
        install(new TransactionModule());
    }

    @Provides
    @Singleton
    public EntityManagerFactory provideFactory(DeploymentConfiguration config) {
        return Persistence.createEntityManagerFactory("ActivityInfo", config.asProperties());
    }

    @Provides
    public Session provideSession(EntityManager em) {
        HibernateEntityManager hem = (HibernateEntityManager) em;
        return hem.getSession();
    }

    @Provides 
    @Singleton
    public Validator provideValidator() {
        ValidatorFactory validatorFactory = Validation.byProvider(HibernateValidator.class)
                                                      .configure()
                                                      .buildValidatorFactory();
        return validatorFactory.getValidator();
    }
    
    @Provides 
    @Singleton
    protected HibernateEntityManager provideHibernateEntityManager(EntityManager entityManager) {
        return (HibernateEntityManager) entityManager;
    }
}
