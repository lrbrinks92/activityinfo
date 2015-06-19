package org.activityinfo.server.report.util;

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

import org.activityinfo.legacy.shared.command.Month;
import org.activityinfo.legacy.shared.reports.model.DateRange;
import org.activityinfo.legacy.shared.reports.model.DateUnit;
import org.activityinfo.legacy.shared.reports.util.DateUtil;

import java.util.Calendar;
import java.util.Date;

public class DateUtilCalendarImpl extends DateUtil {

    @Override
    public Month getCurrentMonth() {
        Calendar calendar = Calendar.getInstance();
        return new Month(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1);
    }

    @Override
    public DateRange yearRange(int year) {

        DateRange range = new DateRange();

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, 0);
        calendar.set(Calendar.DATE, 1);
        range.setMinDate(calendar.getTime());

        calendar.set(Calendar.MONTH, Calendar.DECEMBER);
        calendar.set(Calendar.DATE, 31);
        range.setMaxDate(calendar.getTime());

        return range;
    }

    @Override
    public DateRange monthRange(int year, int month) {

        DateRange range = new DateRange();

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DATE, 1);
        range.setMinDate(calendar.getTime());

        calendar.set(Calendar.DATE, calendar.getActualMaximum(Calendar.DATE));
        range.setMaxDate(calendar.getTime());

        return range;
    }

    @Override
    public int getDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        return calendar.get(Calendar.DATE);
    }

    @Override
    public int getYear(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        return calendar.get(Calendar.YEAR);
    }

    @Override
    public int getMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        return calendar.get(Calendar.MONTH) + 1;
    }

    @Override
    public Date floorMonth(Date date) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.DATE, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
        return calendar.getTime();
    }

    @Override
    public Date ceilMonth(Date date) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.DATE, calendar.getActualMaximum(Calendar.DATE));
        return calendar.getTime();
    }

    @Override
    public Date add(Date date, DateUnit dateUnit, int count) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        if (dateUnit == DateUnit.YEAR) {

            calendar.add(Calendar.YEAR, count);

        } else if (dateUnit == DateUnit.QUARTER) {

            calendar.add(Calendar.MONTH, count * 3);

        } else if (dateUnit == DateUnit.MONTH) {

            calendar.add(Calendar.MONTH, count);

        } else if (dateUnit == DateUnit.WEEK_MON) {

            calendar.add(Calendar.DATE, count * 7);

        }

        return calendar.getTime();
    }

    @Override
    public boolean isLastDayOfMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        return calendar.get(Calendar.DATE) == calendar.getActualMaximum(Calendar.DATE);
    }
}
