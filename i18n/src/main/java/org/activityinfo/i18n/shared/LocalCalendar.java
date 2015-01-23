package org.activityinfo.i18n.shared;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.i18n.shared.Localizable;
import com.google.gwt.i18n.client.constants.DateTimeConstants;
import com.google.gwt.i18n.client.constants.DateTimeConstantsImpl;
import net.lightoze.gwt.i18n.client.LocaleFactory;

/**
 * Provides localized time period names
 */
public class LocalCalendar implements Localizable {

  private final DateTimeConstantsImpl dateTimeConstants;

  public LocalCalendar() {
    if(GWT.isClient()) {
      dateTimeConstants = GWT.create(DateTimeConstantsImpl.class);
    } else {
      dateTimeConstants = LocaleFactory.get(DateTimeConstantsImpl.class);
    }
  }

  /**
   * 
   * @param month the gregorian month (1-12)
   * @return the localized name of the month
   */
  public final String monthName(int month) {
    assert month > 0 && month <= 12 : "month should be in the range [1-12]";
    return dateTimeConstants.months()[month-1];
  }

  /**
   * 
   * @param quarter the one-based quarter index (1-4)
   * @return the short quarter name (english: Q1, Q2, Q3, Q4)
   */
  public final String shortQuarterName(int quarter) {
    assert quarter > 0 && quarter <= 4 : "quarter should be in the range [1-4]";
    return dateTimeConstants.shortQuarters()[quarter];
  }

  /**
   *
   * @param year the four-digit common year
   * @param quarter one-based quarter index (1-4)
   * @return qualified quarter name (for example: "2014Q2")
   */
  public final String yearQuarterName(int year, int quarter) {
    assert quarter > 0 && quarter <= 4 : "quarter should be in the range [1-4]";
    return localizedYearQuarterName(year, quarter);
  }

  protected String localizedYearQuarterName(int year, int quarter) {
    return year + "Q" + quarter;
  }

  public String weekName(int year, int week) {
    return year + "W" + week;
  }
}
