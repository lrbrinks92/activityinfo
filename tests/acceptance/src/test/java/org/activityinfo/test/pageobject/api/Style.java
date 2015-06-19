package org.activityinfo.test.pageobject.api;

public class Style {

    private final String style;

    public Style(String text) {
        this.style = text;
    }

    public int getLeft() {
        return parseValue("left");
    }

    public int getTop() {
        return parseValue("top");
    }

    public int getWidth() {
        return parseValue("width");
    }

    public int getHeight() {
        return parseValue("height");
    }

    public boolean hasValue(String attribute) {
        int i = style.indexOf(attribute + ":");
        return i >= 0;
    }
    
    private int parseValue(String attribute) {
        int i = style.indexOf(attribute + ":");
        i += attribute.length() + 1;

        // skip whitespace
        while(style.charAt(i) == ' ') {
            i++;
        }

        int start = i;
        while(Character.isDigit(style.charAt(i))) {
            i++;
        }

        return Integer.parseInt(style.substring(start, i));
    }

    public boolean hasHeight() {
        return hasValue("height");
    }
    
    public boolean hasWidth() {
        return hasValue("width");
    }

    
}
