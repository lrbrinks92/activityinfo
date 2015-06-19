package org.activityinfo.geoadmin;

public class Codes {

    public static boolean hasCode(Object[] attributeValues, String code) {
        if (code.matches("\\d+")) {
            return hasIntCode(attributeValues, Long.parseLong(code));
        } else {
            // TODO
            return false;
        }
    }

    private static boolean hasIntCode(Object[] attributeValues, long code) {
        for (Object value : attributeValues) {
            try {
                if (value instanceof Number) {
                    if (((Number) value).longValue() == code) {
                        return true;
                    }
                } else if (value instanceof String) {
                    if (Long.parseLong((String) value) == code) {
                        return true;
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return false;
    }
}
