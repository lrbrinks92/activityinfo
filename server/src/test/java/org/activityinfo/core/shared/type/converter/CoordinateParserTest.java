package org.activityinfo.core.shared.type.converter;

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


import net.lightoze.gwt.i18n.server.LocaleProxy;
import net.lightoze.gwt.i18n.server.ThreadLocalLocaleProvider;
import org.activityinfo.core.server.type.converter.JreNumberFormats;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class CoordinateParserTest {

    @Before
    public void before() {
        LocaleProxy.initialize();
        ThreadLocalLocaleProvider.pushLocale(Locale.ENGLISH);
    }

    @After
    public void after() {
        ThreadLocalLocaleProvider.popLocale();
    }

    @Test
    public void DDdEnglish() throws CoordinateFormatException {

        CoordinateParser latitude = new CoordinateParser(CoordinateAxis.LATITUDE, new JreNumberFormats());
        CoordinateParser longitude = new CoordinateParser(CoordinateAxis.LONGITUDE, new JreNumberFormats());

        assertThat(latitude.parse("+6.325"), closeTo(6.325, 0d));
        assertThat(latitude.parse("+6.3"), closeTo(6.3, 0d));
        assertThat(latitude.parse("+6"), closeTo(6, 0d));

        // notation should be updated
        assertThat(latitude.getNotation(), equalTo(CoordinateParser.Notation.DDd));

        assertThat(latitude.parse("-6.325"), closeTo(-6.325, 0d));
        assertThat(latitude.parse("6.325 S"), closeTo(-6.325, 0d));

        assertThat(longitude.parse("6.325 E"), closeTo(+6.325, 0d));
        assertThat(longitude.parse("6.325 W"), closeTo(-6.325, 0d));

        assertThat(latitude.parse("S 2.45"), closeTo(-2.45, 0d));
        assertThat(latitude.parse("2N"), closeTo(2, 0d));
    }

    @Test(expected = CoordinateFormatException.class)
    public void DDdMissingHemisphere() {
        assertThat(parseLatitude("6.325"), closeTo(6.325, 0d));
    }

    @Test(expected = CoordinateFormatException.class)
    public void tooManyNumbers() {
        parseLatitude("25 10 54 23' N");
    }

    @Test(expected = CoordinateFormatException.class)
    public void noCoordinates() {
        parseLatitude(" ' N");
    }

    @Test
    public void DDdMissingHemisphereAllowed() {
        CoordinateParser parser = new CoordinateParser(CoordinateAxis.LATITUDE, new JreNumberFormats());
        parser.setRequireSign(false);

        assertThat(parser.parse("6.325"), closeTo(6.325, 0d));
        assertThat(parser.parse("6.3"), closeTo(6.3, 0d));
        assertThat(parser.parse("6"), closeTo(6, 0d));
    }

    @Test
    public void hemisphereInferredWhenNotAmbiguous() throws CoordinateFormatException {

        CoordinateParser parser = new CoordinateParser(CoordinateAxis.LATITUDE, new JreNumberFormats());
        parser.setMinValue(-20);
        parser.setMaxValue(-21);
        assertThat(parser.parse("20.5"), closeTo(-20.5, 0d));

        parser.setMinValue(10);
        parser.setMaxValue(35);
        assertThat(parser.parse("20.5"), closeTo(20.5, 0d));
    }

    @Test
    public void parseDMd() throws CoordinateFormatException {
        assertThat(parseLatitude("30 15.00\"  N"), equalTo(30.25));
        assertThat(parseLatitude("30 45.0000\" S"), equalTo(-30.75));
        assertThat(parseLatitude("S   25 15 "), equalTo(-25.25));
    }

    @Test(expected = CoordinateFormatException.class)
    public void parseInvalidMinutes() throws CoordinateFormatException {
        parseLatitude("30 79.00\"  N");
    }


    @Test
    public void parseDMdInFrenchLocale() throws CoordinateFormatException {
        try {
            ThreadLocalLocaleProvider.pushLocale(Locale.FRANCE);

            assertThat(parseLatitude("30 15,00\"  N"), equalTo(30.25));
            assertThat(parseLatitude("S   25 15 "), equalTo(-25.25));
        } finally {
            ThreadLocalLocaleProvider.popLocale();
        }
    }


    @Test
    public void parseDMS() throws CoordinateFormatException {
        assertThat(parseLatitude("25 10 54.23\"  N"), closeTo(25.18173056, 0.000001));
        assertThat(parseLatitude("176 50' 23\" S"), closeTo(-176.8397222, 0.000001));
    }

    @Test
    public void parseDMSInFrenchLocale() throws CoordinateFormatException {
        try {
            ThreadLocalLocaleProvider.pushLocale(Locale.FRANCE);

            assertThat(parseLatitude("25 10 54,23\"  N"), closeTo(25.18173056, 0.000001));
            assertThat(parseLatitude("176 50' 23\" S"), closeTo(-176.8397222, 0.000001));
        } finally {
            ThreadLocalLocaleProvider.popLocale();
        }
    }

    @Test
    public void formatDDd() {
        CoordinateParser parser = new CoordinateParser(CoordinateAxis.LATITUDE, new JreNumberFormats());
        parser.setNotation(CoordinateParser.Notation.DDd);

        assertThat(parser.format(2.405), equalTo("+2.405000"));
    }

    @Test
    public void formatDMd() {
        CoordinateParser parser = new CoordinateParser(CoordinateAxis.LATITUDE, new JreNumberFormats());
        parser.setNotation(CoordinateParser.Notation.DMd);

        assertThat(parser.format(2.405), equalTo("2° 0.40' N"));
    }

    @Test
    public void nullValues() {
        CoordinateParser parser = new CoordinateParser(CoordinateAxis.LATITUDE, new JreNumberFormats());

        assertThat(parser.parse(null), nullValue());
        assertThat(parser.parse(""), nullValue());

    }

    @Test
    public void formatDMdFrench() {
        try {
            ThreadLocalLocaleProvider.pushLocale(Locale.FRANCE);

            CoordinateParser parser = new CoordinateParser(CoordinateAxis.LONGITUDE, new JreNumberFormats());
            parser.setNotation(CoordinateParser.Notation.DMd);

            assertThat(parser.format(2.405), equalTo("2° 0,40' E"));
            assertThat(parser.format(-2.465), equalTo("2° 0,46' O"));
        } finally {
            ThreadLocalLocaleProvider.popLocale();
        }
    }


    @Test
    public void formatNearEquator() {
        CoordinateParser parser = new CoordinateParser(CoordinateAxis.LATITUDE, new JreNumberFormats());
        parser.setNotation(CoordinateParser.Notation.DMS);

        assertThat(parser.format(-0.9392889738082886), equalTo("0° 56' 21.44\" S"));
    }

    private double parseLatitude(String text) {
        CoordinateParser parser = new CoordinateParser(CoordinateAxis.LATITUDE, new JreNumberFormats());
        return parser.parse(text);
    }

    private double parseLongitude(String text) {
        CoordinateParser parser = new CoordinateParser(CoordinateAxis.LONGITUDE, new JreNumberFormats());
        return parser.parse(text);
    }


}
