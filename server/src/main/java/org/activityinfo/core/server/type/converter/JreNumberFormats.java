package org.activityinfo.core.server.type.converter;

import net.lightoze.gwt.i18n.server.LocaleProxy;
import org.activityinfo.core.shared.type.converter.CoordinateFormatException;
import org.activityinfo.core.shared.type.converter.CoordinateParser;

import java.text.NumberFormat;
import java.text.ParseException;

public class JreNumberFormats implements CoordinateParser.NumberFormatter {

    
    private final NumberFormat decimalFormat;
    private final NumberFormat shortDecimalFormat;
    private final NumberFormat intFormat;
    
    public JreNumberFormats() {
        decimalFormat = NumberFormat.getInstance(LocaleProxy.getLocale());
        decimalFormat.setMinimumFractionDigits(6);
        decimalFormat.setMaximumFractionDigits(6);
        decimalFormat.setMinimumIntegerDigits(0);
        
        shortDecimalFormat = NumberFormat.getInstance(LocaleProxy.getLocale());
        shortDecimalFormat.setMinimumFractionDigits(2);
        shortDecimalFormat.setMaximumFractionDigits(2);
        shortDecimalFormat.setMinimumIntegerDigits(1);
        
        intFormat = NumberFormat.getIntegerInstance(LocaleProxy.getLocale());
    }

    @Override
    public double parseDouble(String s) {
        try {
            return decimalFormat.parse(s).doubleValue();
        } catch (ParseException e) {
            throw new CoordinateFormatException(e.getMessage());
        }
    }

    @Override
    public String formatDDd(double value) {
        if (value > 0) {
            return "+" + decimalFormat.format(value);
        } else {
            return decimalFormat.format(value);
        }
    }

    @Override
    public String formatShortFraction(double value) {
        return shortDecimalFormat.format(value);
    }

    @Override
    public String formatInt(double value) {
        return intFormat.format(value);
    }
}
