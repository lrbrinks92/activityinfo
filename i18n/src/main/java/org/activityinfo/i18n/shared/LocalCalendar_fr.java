package org.activityinfo.i18n.shared;

/**
 * French overrides
 */
public class LocalCalendar_fr extends LocalCalendar {

  @Override
  protected String localizedYearQuarterName(int year, int quarter) {
    return year + "T" + quarter;
  }
}
