package org.activityinfo.test.driver.model;
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

/**
 * @author yuriyz on 06/03/2015.
 */
public class IndicatorLink {

    private String sourceDb;
    private String sourceIndicator;
    private String destDb;
    private String destIndicator;

    public IndicatorLink() {
    }

    public IndicatorLink(String sourceDb, String sourceIndicator, String destDb, String destIndicator) {
        this.sourceDb = sourceDb;
        this.sourceIndicator = sourceIndicator;
        this.destDb = destDb;
        this.destIndicator = destIndicator;
    }

    public String getDestIndicator() {
        return destIndicator;
    }

    public void setDestIndicator(String destIndicator) {
        this.destIndicator = destIndicator;
    }

    public String getSourceDb() {
        return sourceDb;
    }

    public void setSourceDb(String sourceDb) {
        this.sourceDb = sourceDb;
    }

    public String getSourceIndicator() {
        return sourceIndicator;
    }

    public void setSourceIndicator(String sourceIndicator) {
        this.sourceIndicator = sourceIndicator;
    }

    public String getDestDb() {
        return destDb;
    }

    public void setDestDb(String destDb) {
        this.destDb = destDb;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IndicatorLink that = (IndicatorLink) o;

        if (destDb != null ? !destDb.equals(that.destDb) : that.destDb != null) return false;
        if (destIndicator != null ? !destIndicator.equals(that.destIndicator) : that.destIndicator != null)
            return false;
        if (sourceDb != null ? !sourceDb.equals(that.sourceDb) : that.sourceDb != null) return false;
        return !(sourceIndicator != null ? !sourceIndicator.equals(that.sourceIndicator) : that.sourceIndicator != null);

    }

    @Override
    public int hashCode() {
        int result = sourceDb != null ? sourceDb.hashCode() : 0;
        result = 31 * result + (sourceIndicator != null ? sourceIndicator.hashCode() : 0);
        result = 31 * result + (destDb != null ? destDb.hashCode() : 0);
        result = 31 * result + (destIndicator != null ? destIndicator.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "IndicatorLink{" +
                "sourceDb='" + sourceDb + '\'' +
                ", sourceIndicator='" + sourceIndicator + '\'' +
                ", destDb='" + destDb + '\'' +
                ", destIndicator='" + destIndicator + '\'' +
                '}';
    }
}
