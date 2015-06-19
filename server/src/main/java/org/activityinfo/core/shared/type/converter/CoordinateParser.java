package org.activityinfo.core.shared.type.converter;

import org.activityinfo.i18n.shared.I18N;


public class CoordinateParser {


    /**
     * Notation used to format a geographic coordinate
     * 
     * @see <a href="http://en.wikipedia.org/wiki/Geographic_coordinate_conversion">Wikipedia</a>
     */
    public enum Notation {
        /**
         * Decimal degrees: 40.446° N or +40.446
         */
        DDd,

        /**
         * Degree minutes decimals. For example: 40° 26.767′ N
         */
        DMd,

        /**
         * Degree minutes seconds. For example: 40° 26′ 46″ N
         */
        DMS
    }

    /**
     * Provides number formatting & parsing. Extracted from the class to allow
     * for testing.
     */
    public static interface NumberFormatter {

        /*
         * Formats a coordinate as Degree-decimal, with exactly six decimal places and always with a
         * sign prefix. Equivalent the formatting pattern "+0.000000;-0.000000"
         */
        String formatDDd(double value);

        String formatShortFraction(double value);

        String formatInt(double value);

        double parseDouble(String string);
    }

    private static final double MINUTES_PER_DEGREE = 60;
    private static final double SECONDS_PER_DEGREE = 3600;
    private static final double MIN_MINUTES = 0;
    private static final double MAX_MINUTES = 60;
    private static final double MIN_SECONDS = 0;
    private static final double MAX_SECONDS = 60;

    private static final String DECIMAL_SEPARATORS = ".,";

    private final String posHemiChars;
    private final String negHemiChars;

    private double minValue;
    private double maxValue;

    private final String noNumberErrorMessage = I18N.CONSTANTS.noNumber();
    private final String tooManyNumbersErrorMessage = I18N.CONSTANTS.tooManyNumbers();
    private final String noHemisphereMessage = I18N.CONSTANTS.noHemisphere();
    private final String invalidSecondsMessage = I18N.CONSTANTS.invalidMinutes();
    private final String invalidMinutesMessage = I18N.CONSTANTS.invalidMinutes();


    private Notation notation = Notation.DMS;

    private boolean requireSign = true;


    private CoordinateAxis axis;
    private final NumberFormatter numberFormatter;


    public CoordinateParser(CoordinateAxis axis, NumberFormatter numberFormatter) {
        this.axis = axis;
        this.numberFormatter = numberFormatter;
        this.minValue = axis.getMinimumValue();
        this.maxValue = axis.getMaximumValue();
        this.posHemiChars = axis.getPositiveHemisphereCharacters();
        this.negHemiChars = axis.getNegativeHemisphereCharacters();
    }


    public void setRequireSign(boolean requireSign) {
        this.requireSign = requireSign;
    }

    /**
     * Parses the given string in either Degree Decimal, Degree Minutes Seconds, or
     * Degree Minutes Decimal notation.
     * 
     * <p>This parser's {@code notation} property is updated to the detected notation
     * after this method returns.</p>
     * 
     * @param value a coordinate value as text
     * @return the coordinate as a floating point number, or {@code null} if value is null or empty.
     * @throws CoordinateFormatException
     */
    public Double parse(String value) throws CoordinateFormatException {

        if (value == null || value.length() == 0) {
            return null;
        }

        StringBuffer[] numbers = new StringBuffer[]{
                new StringBuffer(),
                new StringBuffer(),
                new StringBuffer()};
        int numberIndex = 0;
        int i;

        /*
         * To assure correctness, we're going to insist that the user explicitly
         * enter the hemisphere (+/-/N/S).
         *
         * However, if the bounds dictate that the coordinate is in one
         * hemisphere, then we can assume the sign.
         */
        double sign = maybeInferSignFromBounds();

        for (i = 0; i != value.length(); ++i) {
            char c = value.charAt(i);

            if (isNegHemiChar(c)) {
                sign = -1;
            } else if (isPosHemiChar(c)) {
                sign = 1;
            } else if (isNumberPart(c)) {
                if (numberIndex > 2) {
                    throw new CoordinateFormatException(tooManyNumbersErrorMessage);
                }
                numbers[numberIndex].append(c);
            } else if (numberIndex != 2 && numbers[numberIndex].length() > 0) {
                // advance to the next token on anything else-- whitespace,
                // symbols like ' " ° -- we won't insist that they are used
                // in the right place
                numberIndex++;
            }
        }

        if (sign == 0) {
            if (requireSign) {
                throw new CoordinateFormatException(noHemisphereMessage);
            } else {
                sign = 1;
            }
        }

        return parseCoordinate(numbers) * sign;
    }

    /**
     * 
     * @return true if the hemisphere of this coordinate can be inferred from its bounds. 
     */
    private double maybeInferSignFromBounds() {
        double sign = 0;
        if (maxValue < 0) {
            sign = -1;
        } else if (minValue > 0) {
            sign = +1;
        }
        return sign;
    }

    private boolean isNumberPart(char c) {
        return Character.isDigit(c) || DECIMAL_SEPARATORS.indexOf(c) != -1;
    }

    private boolean isPosHemiChar(char c) {
        return c == '+' || posHemiChars.indexOf(c) != -1;
    }

    private boolean isNegHemiChar(char c) {
        return c == '-' || negHemiChars.indexOf(c) != -1;
    }

    private double parseCoordinate(StringBuffer[] tokens)
            throws CoordinateFormatException {
        if (tokens[0].length() == 0) {
            throw new CoordinateFormatException(noNumberErrorMessage);
        }

        double coordinate = Double.parseDouble(tokens[0].toString());
        notation = Notation.DDd;

        if (tokens[1].length() != 0) {
            double minutes = numberFormatter.parseDouble(tokens[1].toString());
            if (minutes < MIN_MINUTES || minutes > MAX_MINUTES) {
                throw new CoordinateFormatException(invalidMinutesMessage);
            }
            coordinate += minutes / MINUTES_PER_DEGREE;
            notation = Notation.DMd;

        }
        if (tokens[2].length() != 0) {
            double seconds = numberFormatter.parseDouble(tokens[2].toString());
            if (seconds < MIN_SECONDS || seconds > MAX_SECONDS) {
                throw new CoordinateFormatException(invalidSecondsMessage);
            }
            notation = Notation.DMS;
            coordinate += seconds / SECONDS_PER_DEGREE;
        }
        return coordinate;
    }



    /**
     * Formats the given coordinate {@code value} using Degree-Minute-decimal notation. 
     * For example: 40° 26.767′ N
     */
    public String formatAsDMd(double value) {

        double degrees = Math.floor(Math.abs(value));
        double minutes = (Math.abs(value) - degrees);

        StringBuilder sb = new StringBuilder();
        sb.append(numberFormatter.formatInt(Math.abs(degrees))).append("° ");
        sb.append(numberFormatter.formatShortFraction(minutes)).append("' ");
        sb.append(hemisphereChar(value));

        return sb.toString();
    }


    /**
     * Formats the given coordinate {@code value} using the decimal degree notation. 
     * For example: +40.767333
     */
    public String formatAsDDd(double coordinate) {
        return numberFormatter.formatDDd(coordinate);
    }

    /**
     * Formats the given coordinate {@code value} using Degree-Minute-Second notation.
     * For example: 40° 26′ 46″ N
     */
    public String formatAsDMS(double value) {
        double absv = Math.abs(value);

        double degrees = Math.floor(absv);
        double minutes = ((absv - degrees) * 60.0);
        double seconds = ((minutes - Math.floor(minutes)) * 60.0);
        minutes = Math.floor(minutes);

        StringBuilder sb = new StringBuilder();
        sb.append(numberFormatter.formatInt(Math.abs(degrees))).append("° ");
        sb.append(numberFormatter.formatInt(minutes)).append("' ");
        sb.append(numberFormatter.formatShortFraction(seconds)).append("\" ");
        sb.append(hemisphereChar(value));

        return sb.toString();
    }

    /**
     * Formats the given coordinate {@code value} using this parser's current notation. 
     */
    public String format(double coordinate) {
        return format(notation, coordinate);
    }

    /**
     * Formats the given coordinate {@code value} using the given {@code notation}.
     */
    public String format(Notation notation, double coordinate) {
        switch (notation) {
            case DDd:
                return formatAsDDd(coordinate);
            case DMd:
                return formatAsDMd(coordinate);
            default:
            case DMS:
                return formatAsDMS(coordinate);
        }
    }

    private char hemisphereChar(double value) {
        if (Math.signum(value) < 0) {
            return negHemiChars.charAt(0);
        } else {
            return posHemiChars.charAt(0);
        }
    }

    /**
     * 
     * @return the detected notation of the last coordinate parsed.
     */
    public Notation getNotation() {
        return notation;
    }

    /**
     * Sets the notation to be used for formatting. 
     * 
     * @param notation the notation to be used for formatting
     */
    public void setNotation(Notation notation) {
        this.notation = notation;
    }

    public double getMinValue() {
        return minValue;
    }

    /**
     * 
     * Sets the minimum coordinate value expected. This is used only for inferring a coordinate's sign while parsing.
     *
     */
    public void setMinValue(double minValue) {
        assert minValue >= axis.getMinimumValue();
        
        this.minValue = minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    /**
     *
     * Sets the maximum coordinate value expected. This is used only for inferring a coordinate's sign while parsing.
     *
     */
    public void setMaxValue(double maxValue) {
        assert maxValue <= axis.getMaximumValue();

        this.maxValue = maxValue;
    }
}
