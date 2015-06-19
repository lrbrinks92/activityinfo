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
import org.activityinfo.legacy.shared.command.GetMonthlyReports;
import org.activityinfo.legacy.shared.command.Month;
import org.activityinfo.legacy.shared.command.result.CommandResult;
import org.activityinfo.legacy.shared.command.result.MonthlyReportResult;
import org.activityinfo.legacy.shared.exception.CommandException;
import org.activityinfo.legacy.shared.exception.IllegalAccessCommandException;
import org.activityinfo.legacy.shared.model.IndicatorRowDTO;
import org.activityinfo.server.database.hibernate.entity.*;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * See GetMonthlyReports
 *
 * @author Alex Bertram
 */
public class GetMonthlyReportsHandler implements CommandHandler<GetMonthlyReports> {

    private static final Logger LOGGER = Logger.getLogger(GetMonthlyReportsHandler.class.getName());

    private final EntityManager em;
    private final PermissionOracle permissionOracle;

    @Inject
    public GetMonthlyReportsHandler(EntityManager em, PermissionOracle permissionOracle) {
        this.em = em;
        this.permissionOracle = permissionOracle;
    }

    @Override
    public CommandResult execute(GetMonthlyReports cmd, User user) throws CommandException {

        Site site = em.find(Site.class, cmd.getSiteId());
        if(!permissionOracle.isViewAllowed(site, user)) {
            LOGGER.severe("User " + user.getEmail() + " has no view privs on site " + site.getId() + "," +
                          "partner = " + site.getPartner().getName() + " " + site.getPartner().getId());
            throw new IllegalAccessCommandException();
        }

        List<ReportingPeriod> periods = em.createQuery(
            "SELECT p from ReportingPeriod p WHERE p.site.id = :siteId", ReportingPeriod.class)
            .setParameter("siteId", cmd.getSiteId())
            .getResultList();

        List<Indicator> indicators = em.createQuery(
            "SELECT i from Indicator i " +
             "WHERE i.activity.id IN (SELECT s.activity.id FROM Site s WHERE s.id = :siteId) " + 
               "AND i.dateDeleted IS NULL " +
          "ORDER BY i.sortOrder", Indicator.class) 
           .setParameter("siteId", cmd.getSiteId())
           .getResultList();

        List<IndicatorRowDTO> list = new ArrayList<IndicatorRowDTO>();

        for (Indicator indicator : indicators) {

            IndicatorRowDTO dto = new IndicatorRowDTO();
            dto.setIndicatorId(indicator.getId());
            dto.setSiteId(cmd.getSiteId());
            dto.setIndicatorName(indicator.getName());
            dto.setCategory(indicator.getCategory());
            dto.setActivityName(indicator.getActivity().getName());

            for (ReportingPeriod period : periods) {

                Month month = HandlerUtil.monthFromRange(period.getDate1(), period.getDate2());
                if (month != null &&
                    month.compareTo(cmd.getStartMonth()) >= 0 &&
                    month.compareTo(cmd.getEndMonth()) <= 0) {

                    for (IndicatorValue value : period.getIndicatorValues()) {
                        if (value.getIndicator().getId() == indicator.getId()) {
                            dto.setValue(month, value.getValue());
                        }
                    }
                }
            }

            list.add(dto);
        }

        return new MonthlyReportResult(list);
    }
}
