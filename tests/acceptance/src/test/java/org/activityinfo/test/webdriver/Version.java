package org.activityinfo.test.webdriver;

public class Version implements Comparable<Version> {

    public static final Version UNKNOWN = new Version("");
    
    private String version;

    public Version(String version) {
        if(version == null) {
            version = "";
        }
        this.version = version;
    }

    public Version(int versionNumber) {
        this(Integer.toString(versionNumber));
    }

    private boolean isInteger(String s) {
        return s.matches("\\d+");
    }

    @Override public int compareTo(Version that) {
        if(that == null)
            return 1;
        String[] thisParts = normalize().split("[\\.\\s+]");
        String[] thatParts = that.normalize().split("[\\.\\s+]");
        int length = Math.max(thisParts.length, thatParts.length);
        for(int i = 0; i < length; i++) {
            String thisPart;
            String thatPart;
            if (i < thisParts.length) {
                thisPart = thisParts[i];
            } else {
                thisPart = "0";
            }
            if (i < thatParts.length) {
                thatPart = thatParts[i];
            } else {
                thatPart = "0";
            }
            int cmp;
            if(isInteger(thisPart) && isInteger(thatPart)) {
                cmp = Integer.compare(Integer.parseInt(thisPart), Integer.parseInt(thatPart));
            } else {
                cmp = thisPart.compareTo(thatPart);
            }
            if(cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    private String normalize() {
        if(version.equals("XP")) {
            return "6";
        } else {
            return version;
        }
    }

    @Override public boolean equals(Object that) {
        if(this == that)
            return true;
        if(that == null)
            return false;
        if(this.getClass() != that.getClass())
            return false;
        return this.compareTo((Version) that) == 0;
    }

    @Override
    public int hashCode() {
        return version.hashCode();
    }

    @Override
    public String toString() {
        return version;
    }

    public boolean isEmpty() {
        return version.isEmpty();
    }
}
