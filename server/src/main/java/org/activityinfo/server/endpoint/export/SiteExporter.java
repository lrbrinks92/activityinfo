package org.activityinfo.server.endpoint.export;

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

import com.bedatadriven.rebar.time.calendar.LocalDate;
import com.extjs.gxt.ui.client.Style.SortDir;
import com.extjs.gxt.ui.client.data.SortInfo;
import com.google.common.base.Strings;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.legacy.shared.command.*;
import org.activityinfo.legacy.shared.command.result.SiteResult;
import org.activityinfo.legacy.shared.model.*;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Exports sites in Excel format
 */
public class SiteExporter {

    private static final Logger LOGGER = Logger.getLogger(SiteExporter.class.getName());

    private static final int MAX_WORKSHEET_LENGTH = 31;

    private static final short FONT_SIZE = 8;
    private static final short TITLE_FONT_SIZE = 12;

    private static final short DIAGONAL = 45;

    private static final int COORD_COLUMN_WIDTH = 12;
    private static final int ATTRIBUTE_COLUMN_WIDTH = 5;
    private static final int INDICATOR_COLUMN_WIDTH = 16;
    private static final int LOCATION_COLUMN_WIDTH = 20;
    private static final int PARTNER_COLUMN_WIDTH = 16;
    private static final int HEADER_CELL_HEIGHT = 75;
    private static final int CHARACTERS_PER_WIDTH_UNIT = 255;
    private static final int SITE_BATCH_SIZE = 100;

    private final TaskContext context;

    private final HSSFWorkbook book;
    private final CreationHelper creationHelper;

    private Map<String, Integer> sheetNames;

    private CellStyle titleStyle;

    private CellStyle dateStyle;
    private CellStyle coordStyle;
    private CellStyle indicatorValueStyle;

    private CellStyle headerStyle;
    private CellStyle headerStyleCenter;
    private CellStyle headerStyleRight;

    private CellStyle attribHeaderStyle;
    private CellStyle indicatorHeaderStyle;

    private CellStyle attribValueStyle;

    private List<Integer> indicators;
    private List<Integer> attributes;
    private List<Integer> levels;
    private HSSFCellStyle dateTimeStyle;

    public SiteExporter(TaskContext context) {
        this.context = context;

        book = new HSSFWorkbook();
        creationHelper = book.getCreationHelper();

        sheetNames = new HashMap<>();

        declareStyles();
    }

    private void declareStyles() {
        dateStyle = book.createCellStyle();
        dateStyle.setDataFormat(creationHelper.createDataFormat().getFormat("m/d/yy"));

        dateTimeStyle = book.createCellStyle();
        dateTimeStyle.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy-mm-dd HH:MM:SS"));

        coordStyle = book.createCellStyle();
        coordStyle.setDataFormat(creationHelper.createDataFormat().getFormat("0.000000"));

        indicatorValueStyle = book.createCellStyle();
        indicatorValueStyle.setDataFormat(creationHelper.createDataFormat().getFormat("#,##0"));

        Font headerFont = book.createFont();
        headerFont.setBoldweight(Font.BOLDWEIGHT_BOLD);

        Font smallFont = book.createFont();
        smallFont.setFontHeightInPoints(FONT_SIZE);

        Font titleFont = book.createFont();
        titleFont.setFontHeightInPoints(TITLE_FONT_SIZE);
        titleFont.setBoldweight(Font.BOLDWEIGHT_BOLD);

        titleStyle = book.createCellStyle();
        titleStyle.setFont(titleFont);

        headerStyle = book.createCellStyle();
        headerStyle.setFont(headerFont);

        headerStyleCenter = book.createCellStyle();
        headerStyleCenter.setFont(headerFont);
        headerStyleCenter.setAlignment(CellStyle.ALIGN_CENTER);

        headerStyleRight = book.createCellStyle();
        headerStyleRight.setFont(headerFont);
        headerStyleRight.setAlignment(CellStyle.ALIGN_RIGHT);

        attribHeaderStyle = book.createCellStyle();
        attribHeaderStyle.setFont(smallFont);
        attribHeaderStyle.setRotation(DIAGONAL);
        attribHeaderStyle.setWrapText(true);

        indicatorHeaderStyle = book.createCellStyle();
        indicatorHeaderStyle.setFont(smallFont);
        indicatorHeaderStyle.setWrapText(true);
        indicatorHeaderStyle.setAlignment(CellStyle.ALIGN_RIGHT);

        attribValueStyle = book.createCellStyle();
        attribValueStyle.setFont(smallFont);

    }

    public void export(ActivityFormDTO activity, Filter filter) {

        HSSFSheet sheet = book.createSheet(composeUniqueSheetName(activity));
        sheet.createFreezePane(4, 2);

        // initConditionalFormatting(sheet);
        createHeaders(activity, sheet);
        createDataRows(activity, filter, sheet);

    }

    private String composeUniqueSheetName(ActivityFormDTO activity) {
        String sheetName = activity.getDatabaseName() + " - " + activity.getName();

        // to avoid conflict with our own disambiguation scheme, remove any trailing "(n)" 
        // from sheet names
        sheetName = sheetName.replaceFirst("\\((\\d+)\\)$", "$1");

        // shorten and translate the name to meet excel requirements
        String safeName = WorkbookUtil.createSafeSheetName(sheetName);

        // assure that the sheet name is unique
        if (!sheetNames.containsKey(safeName)) {
            sheetNames.put(safeName, 1);
            return safeName;
        } else {
            int index = sheetNames.get(safeName) + 1;
            sheetNames.put(safeName, index);

            String disambiguatedNamed = safeName + " (" + index + ")";
            if (disambiguatedNamed.length() > MAX_WORKSHEET_LENGTH) {
                int toTrim = disambiguatedNamed.length() - MAX_WORKSHEET_LENGTH;
                disambiguatedNamed = safeName.substring(0, safeName.length() - toTrim) + " (" + index + ")";
            }
            return disambiguatedNamed;
        }
    }

    private void createHeaders(ActivityFormDTO activity, HSSFSheet sheet) {

        // / The HEADER rows

        Row headerRow1 = sheet.createRow(0);
        Row headerRow2 = sheet.createRow(1);
        headerRow2.setHeightInPoints(HEADER_CELL_HEIGHT);

        // Create a title cell with the complete database + activity name
        Cell titleCell = headerRow1.createCell(0);
        titleCell.setCellValue(creationHelper.createRichTextString(
                activity.getDatabaseName() + " - " + activity.getName()));
        titleCell.setCellStyle(titleStyle);

        int column = 0;

        createHeaderCell(headerRow2, column++, "SiteId", CellStyle.ALIGN_LEFT);
        createHeaderCell(headerRow2, column++, "DateCreated", CellStyle.ALIGN_RIGHT);

        sheet.setColumnHidden(0, true);
        sheet.setColumnHidden(1, true);

        createHeaderCell(headerRow2, column++, "Date1", CellStyle.ALIGN_RIGHT);
        createHeaderCell(headerRow2, column++, "Date2", CellStyle.ALIGN_RIGHT);

        createHeaderCell(headerRow2, column, "Partner");
        sheet.setColumnWidth(column, characters(PARTNER_COLUMN_WIDTH));
        column++;

        createHeaderCell(headerRow2, column, activity.getLocationType().getName());
        sheet.setColumnWidth(column, characters(LOCATION_COLUMN_WIDTH));
        column++;

        createHeaderCell(headerRow2, column++, "Axe");

        indicators = new ArrayList<>(activity.getIndicators().size());
        for (IndicatorGroup group : activity.groupIndicators()) {
            if (group.getName() != null) {
                // create a merged cell on the top row spanning all members
                // of the group
                createHeaderCell(headerRow1, column, group.getName());
                sheet.addMergedRegion(new CellRangeAddress(0,
                        0,
                        column,
                        column + group.getIndicators().size() - 1));
            }
            for (IndicatorDTO indicator : group.getIndicators()) {
                indicators.add(indicator.getId());
                createHeaderCell(headerRow2, column, indicator.getName(), indicatorHeaderStyle);
                sheet.setColumnWidth(column, characters(INDICATOR_COLUMN_WIDTH));
                column++;
            }
        }

        attributes = new ArrayList<>();
        for (AttributeGroupDTO group : activity.getAttributeGroups()) {
            if (group.getAttributes().size() != 0) {
                createHeaderCell(headerRow1, column, group.getName(), CellStyle.ALIGN_CENTER);
                sheet.addMergedRegion(new CellRangeAddress(0, 0, column, column + group.getAttributes().size() - 1));

                for (AttributeDTO attrib : group.getAttributes()) {
                    attributes.add(attrib.getId());
                    createHeaderCell(headerRow2, column, attrib.getName(), attribHeaderStyle);
                    sheet.setColumnWidth(column, characters(ATTRIBUTE_COLUMN_WIDTH));
                    column++;
                }
            }
        }

        levels = new ArrayList<>();

        for (AdminLevelDTO level : activity.getAdminLevels()) {
            createHeaderCell(headerRow2, column++, "Code " + level.getName());
            createHeaderCell(headerRow2, column++, level.getName());
            levels.add(level.getId());
        }

        int latColumn = column++;
        int lngColumn = column++;

        createHeaderCell(headerRow2, latColumn, I18N.CONSTANTS.longitude(), CellStyle.ALIGN_RIGHT);
        createHeaderCell(headerRow2, lngColumn, I18N.CONSTANTS.latitude(), CellStyle.ALIGN_RIGHT);
        sheet.setColumnWidth(lngColumn, characters(COORD_COLUMN_WIDTH));
        sheet.setColumnWidth(latColumn, characters(COORD_COLUMN_WIDTH));

        createHeaderCell(headerRow2, column++, I18N.CONSTANTS.comments());

    }

    private SiteResult querySites(ActivityFormDTO activity, Filter filter, int offset) {

        Filter effectiveFilter = new Filter(filter);
        effectiveFilter.addRestriction(DimensionType.Activity, activity.getId());

        GetSites query = new GetSites();
        query.setFilter(effectiveFilter);
        query.setSortInfo(new SortInfo("date2", SortDir.DESC));
        query.setOffset(offset);
        query.setLimit(SITE_BATCH_SIZE);

        if(activity.getReportingFrequency() == ActivityFormDTO.REPORT_MONTHLY) {
            query.setFetchAllReportingPeriods(true);
            query.setFetchLinks(false);
        }

        SiteResult result = context.execute(query);
        
        if(result.getTotalLength() > 0) {
            context.updateProgress(Math.min(1.0, (double) offset) / ((double) result.getTotalLength()));
        }
        
        return result;
    }

    private void createDataRows(ActivityFormDTO activity, Filter filter, Sheet sheet) {

        int rowIndex = 2;

        // query in batches in order to avoid sinking MySQL
        int offset = 0;
        while (true) {
            LOGGER.log(Level.INFO, "Fetching batching at offset " + offset);

            SiteResult batch = querySites(activity, filter, offset);
            if (batch.getData().isEmpty()) {
                break; // break if no data
            }
            for (SiteDTO site : batch.getData()) {
                addDataRow(sheet, rowIndex++, site);
            }
            offset += SITE_BATCH_SIZE;
        }
    }

    private void addDataRow(Sheet sheet, int rowIndex, SiteDTO site) {
        Row row = sheet.createRow(rowIndex);
        int column = 0;

        createCell(row, column++, Integer.toString(site.getId()));
        createCell(row, column++, site.getDateCreated());

        createCell(row, column++, site.getDate1());
        createCell(row, column++, site.getDate2());
        createCell(row, column++, site.getPartnerName());

        createCell(row, column++, site.getLocationName());

        createCell(row, column++, site.getLocationAxe());

        for (Integer indicatorId : indicators) {
            createIndicatorValueCell(row, column++, site.getIndicatorValue(indicatorId));
        }

        for (Integer attribId : attributes) {

            boolean value = site.getAttributeValue(attribId);
            Cell valueCell = createCell(row, column, value);
            valueCell.setCellStyle(attribValueStyle);
            column++;
        }

        for (Integer levelId : levels) {
            AdminEntityDTO entity = site.getAdminEntity(levelId);
            if (entity != null) {
                createCell(row, column, "");
                createCell(row, column + 1, entity.getName());
            }
            column += 2;
        }

        if (site.hasLatLong()) {
            createCoordCell(row, column, site.getLongitude());
            createCoordCell(row, column + 1, site.getLatitude());
        }
        column += 2;

        if (!Strings.isNullOrEmpty(site.getComments())) {
            createCell(row, column, site.getComments());
        }
    }

    private Cell createHeaderCell(Row headerRow, int columnIndex, String text, CellStyle style) {
        Cell cell = headerRow.createCell(columnIndex);
        cell.setCellValue(creationHelper.createRichTextString(text));
        cell.setCellStyle(style);

        return cell;
    }

    private Cell createHeaderCell(Row headerRow, int columnIndex, String text) {
        return createHeaderCell(headerRow, columnIndex, text, CellStyle.ALIGN_LEFT);
    }

    private Cell createHeaderCell(Row headerRow, int columnIndex, String text, int align) {
        Cell cell = headerRow.createCell(columnIndex);
        cell.setCellValue(creationHelper.createRichTextString(text));

        switch (align) {
            case CellStyle.ALIGN_LEFT:
                cell.setCellStyle(headerStyle);
                break;
            case CellStyle.ALIGN_CENTER:
                cell.setCellStyle(headerStyleCenter);
                break;
            case CellStyle.ALIGN_RIGHT:
                cell.setCellStyle(headerStyleRight);
                break;
        }

        return cell;
    }

    private Cell createCell(Row row, int columnIndex, String text) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(creationHelper.createRichTextString(text));
        return cell;
    }

    private void createCell(Row row, int columnIndex, LocalDate date) {
        Cell cell = row.createCell(columnIndex);
        if (date != null) {
            cell.setCellValue(date.atMidnightInMyTimezone());
        }
        cell.setCellStyle(dateStyle);
    }


    private void createCell(Row row, int columnIndex, Date date) {
        Cell cell = row.createCell(columnIndex);
        if (date != null) {
            cell.setCellValue(date);
        }
        cell.setCellStyle(dateStyle);
    }


    private void createIndicatorValueCell(Row row, int columnIndex, Object value) {
        if (value != null) {
            Cell cell = row.createCell(columnIndex);
            cell.setCellStyle(indicatorValueStyle);
            if (value instanceof Double) {
                cell.setCellValue((Double) value);
            } else if (value instanceof String) {
                cell.setCellValue((String) value);
            } else if (value instanceof Date) {
                cell.setCellValue((Date) value);
            } else if (value instanceof LocalDate) {
                cell.setCellValue(((LocalDate) value).atMidnightInMyTimezone());
            } else if (value instanceof Boolean) {
                cell.setCellValue((Boolean) value);
            }
        }
    }

    private void createCoordCell(Row row, int columnIndex, double value) {

        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value);
        cell.setCellStyle(coordStyle);
    }

    private Cell createCell(Row row, int columnIndex, boolean value) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value);

        return cell;
    }

    public HSSFWorkbook getBook() {
        return book;
    }

    private int characters(int numberOfCharacters) {
        return numberOfCharacters * CHARACTERS_PER_WIDTH_UNIT;
    }

    public void done() {
        // an Excel workbook can't have zero sheets, so we need to
        // add something here for it to be valid
        if (book.getNumberOfSheets() == 0) {
            HSSFSheet sheet = book.createSheet("Sheet1");
            sheet.createRow(0).createCell(0).setCellValue("No matching sites.");
        }
    }

    public SiteExporter buildExcelWorkbook(Filter filter) {

        SchemaDTO schema = context.execute(new GetSchema());

        for (UserDatabaseDTO db : schema.getDatabases()) {
            if (!filter.isRestricted(DimensionType.Database) ||
                    filter.getRestrictions(DimensionType.Database).contains(db.getId())) {
                for (ActivityDTO activity : db.getActivities()) {
                    if (!filter.isRestricted(DimensionType.Activity) ||
                            filter.getRestrictions(DimensionType.Activity).contains(activity.getId())) {
                        export(context.execute(new GetActivityForm(activity.getId())), filter);
                    }
                }
            }
        }
        done();
        return this;
    }
}
