package org.activityinfo.server.command.handler;

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
import org.activityinfo.legacy.shared.command.GetCountries;
import org.activityinfo.legacy.shared.command.result.CommandResult;
import org.activityinfo.legacy.shared.command.result.CountryResult;
import org.activityinfo.legacy.shared.exception.CommandException;
import org.activityinfo.legacy.shared.model.CountryDTO;
import org.activityinfo.server.database.hibernate.entity.Country;
import org.activityinfo.server.database.hibernate.entity.User;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class GetCountriesHandler implements CommandHandler<GetCountries> {

    private final static Logger LOG = Logger.getLogger(GetCountriesHandler.class.getName());

    private final EntityManager entityManager;

    @Inject
    public GetCountriesHandler(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @SuppressWarnings("unchecked") @Override
    public CommandResult execute(GetCountries cmd, User user) throws CommandException {
        List<Country> countries = entityManager
            .createQuery("SELECT c FROM Country c ORDER by c.name", Country.class)
            .getResultList();
        
        return new CountryResult(mapToDtos(countries));
    }

    private ArrayList<CountryDTO> mapToDtos(List<Country> countries) {
        ArrayList<CountryDTO> dtos = new ArrayList<CountryDTO>();
        for (Country country : countries) {
            CountryDTO dto = new CountryDTO();
            dto.setId(country.getId());
            dto.setName(country.getName());
            dto.setCodeISO(country.getCodeISO());
            dtos.add(dto);
        }
        return dtos;
    }
}
