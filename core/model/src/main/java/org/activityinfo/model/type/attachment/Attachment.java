package org.activityinfo.model.type.attachment;
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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.activityinfo.model.resource.IsRecord;
import org.activityinfo.model.resource.Record;

/**
 * @author yuriyz on 8/12/14.
 */
public class Attachment implements IsRecord {

    private String mimeType;
    private String filename;
    private String blobId;

    private int height;
    private int width;

    public Attachment() {
    }

    public Attachment(String mimeType, String filename, String blobId) {
        this.mimeType = mimeType;
        this.filename = filename;
        this.blobId = blobId;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setBlobId(String blobId) {
        this.blobId = blobId;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFilename() {
        return filename;
    }

    public String getBlobId() {
        return blobId;
    }

    @Override
    public Record asRecord() {
        return new Record()
                .set("mimeType", mimeType)
                .set("width", width)
                .set("height", height)
                .set("filename", filename)
                .set("blobId", blobId);
    }

    public static Attachment fromRecord(Record record) {
        Attachment rowValue = new Attachment(
                record.getString("mimeType"), record.getString("filename"), record.getString("blobId"));
        rowValue.setHeight(record.getInt("height"));
        rowValue.setWidth(record.getInt("width"));
        return rowValue;
    }

    public JsonElement toJsonElement() {
        JsonObject object = new JsonObject();
        object.addProperty("mimeType", mimeType);
        object.addProperty("width", width);
        object.addProperty("height", height);
        object.addProperty("filename", filename);
        object.addProperty("blobId", blobId);
        return object;
    }
    

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Attachment that = (Attachment) o;

        return !(blobId != null ? !blobId.equals(that.blobId) : that.blobId != null);

    }

    @Override
    public int hashCode() {
        return blobId != null ? blobId.hashCode() : 0;
    }

}
