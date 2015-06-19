package org.activityinfo.server.branding;

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

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.activityinfo.server.database.hibernate.entity.Domain;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;

import static com.google.common.base.Strings.emptyToNull;

/**
 * Provides information on the domain branding to use based
 * on this thread's current request.
 */
public class DomainProvider implements Provider<Domain> {

    private final Provider<HttpServletRequest> request;
    private final Provider<EntityManager> entityManager;

    protected DomainProvider() {
        request = null;
        entityManager = null;
    }

    @Inject
    public DomainProvider(Provider<HttpServletRequest> request, Provider<EntityManager> entityManager) {
        super();
        this.request = request;
        this.entityManager = entityManager;
    }

    @Override
    public Domain get() {

        String host = getBrandHostName();
        Domain result = entityManager.get().find(Domain.class, host);
        if (result == null) {
            result = new Domain();
            result.setTitle("ActivityInfo");
            result.setHost(host);
            result.setPort(request.get().getServerPort());
            result.setSignUpAllowed(true);
        } else {
            entityManager.get().detach(result);
        }
        result.setHost(getExternalHostName());
        return result;
    }

    private String getExternalHostName() {
        String host = getHeader("X-Forwarded-Host");
        if(host != null) {
            return host;
        }
        return request.get().getServerName();
    }

    /**
     *
     * Return the hostname to use for looking up the branded domain.
     *
     * If the request is forwarded from a proxy server, this host name might
     * be different from both the requested host name ('proxy.default.activityinfoeu.appspot.com')
     * and the host name requested by the end user ('proxy.activityinfo.org') if we are
     * are setting up an alias to an existing host.
     *
     * @return the host name to use for looking up the branded version of AI to serve.
     *
     */
    private String getBrandHostName() {

        String host = getHeader("X-AI-Domain");
        if(host != null) {
            return host;
        }
        host = getHeader("X-Forwarded-Host");
        if(host != null) {
            return host;
        }
        return request.get().getServerName();
    }

    private String getHeader(String headerName) {
        return emptyToNull(request.get().getHeader(headerName));
    }
}
