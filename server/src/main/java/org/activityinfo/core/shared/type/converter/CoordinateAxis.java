package org.activityinfo.core.shared.type.converter;

import org.activityinfo.i18n.shared.I18N;

public enum CoordinateAxis {
    LATITUDE {
        @Override
        public double getMaximumValue() {
            return 90;
        }

        @Override
        public String getPositiveHemisphereCharacters() {
            return I18N.CONSTANTS.northHemiChars();
        }

        @Override
        public String getNegativeHemisphereCharacters() {
            return I18N.CONSTANTS.southHemiChars();
        }

    },
    LONGITUDE {
        @Override
        public double getMaximumValue() {
            return 180;
        }

        @Override
        public String getPositiveHemisphereCharacters() {
            return I18N.CONSTANTS.eastHemiChars();
        }

        @Override
        public String getNegativeHemisphereCharacters() {
            return I18N.CONSTANTS.westHemiChars();
        }

    };
    
    public abstract double getMaximumValue();
    
    public double getMinimumValue() {
        return -getMaximumValue();
    }

    /**
     * 
     * @return the range of characters used denote the positive hemisphere for this
     * axis in the current Locale. For example, 'N' is used to indicate the positive hemisphere for latitude.
     */
    public abstract String getPositiveHemisphereCharacters();

    /**
     * 
     * @return the range of characters used to denote the negative hemisphere for this
     * axis in the current Locale. For example, 'S', is used to indicate the negative hemisphere for latitude.
     */
    public abstract String getNegativeHemisphereCharacters();
}
