package org.activityinfo.i18n.shared;

import com.google.gwt.safehtml.shared.SafeHtml;
import java.util.Date;

/**
 * Messages for the application.
 */
public interface UiMessages extends com.google.gwt.i18n.client.Messages {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
     * ================================================================================================
     *
     * IMPORTANT
     *
     * (1) Do not use this class for text that takes no parameters: place those in UiConstants
     * (2) The message format requires apostrophes to be doubled: ''
     *
     * ================================================================================================
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */
    /**
     * Translated "Are you sure you want to delete the database <b>{0}</b>? <br><br>You will loose all activities and indicator results."
     *
     * @return translated "Are you sure you want to delete the database <b>{0}</b>? <br><br>You will loose all activities and indicator results."
     */
    @DefaultMessage("Are you sure you want to delete the database <b>{0}</b>? <br><br>You will loose all activities and indicator results.")
    String confirmDeleteDb(String arg0);

    /**
     * Translated "The coordinate falls outside of the bounds of {0}"
     *
     * @return translated "The coordinate falls outside of the bounds of {0}"
     */
    @DefaultMessage("The coordinate falls outside of the bounds of {0}")
    String coordOutsideBounds(String arg0);

    /**
     * Translated "Last Sync''d: {0}"
     *
     * @return translated "Last Sync''d: {0}"
     */
    @DefaultMessage("Last Sync''d: {0}")
    String lastSynced(String arg0);

    /**
     * Translated "There is already data entered for partner {0}. Delete this partner''s data first."
     *
     * @return translated "There is already data entered for partner {0}. Delete this partner''s data first."
     */
    @DefaultMessage("There is already data entered for partner {0}. Delete this partner''s data first.")
    String partnerHasDataWarning(String arg0);

    /**
     * Translated "There is already data entered for the project {0}. Before deleting this project, you must delete the project''s data."
     *
     * @return translated "There is already data entered for the project {0}. Before deleting this project, you must delete the project''s data."
     */
    @DefaultMessage("There is already data entered for the project {0}. Before deleting this project, you must delete the project''s data.")
    String projectHasDataWarning(String arg0);

    /**
     * Translated "Projects for database {0}"
     *
     * @return translated "Projects for database {0}"
     */
    @DefaultMessage("Projects for database {0}")
    String projectsForDatabase(String arg0);

    /**
     * Translated "{0} most recent added sites for search query"
     *
     * @return translated "{0} most recent added sites for search query"
     */
    @DefaultMessage("{0} most recent added sites for search query")
    String recentlyAddedSites(String arg0);

    /**
     * Translated "{0} most recent edited sites for search query"
     *
     * @return translated "{0} most recent edited sites for search query"
     */
    @DefaultMessage("{0} most recent edited sites for search query")
    String recentlyEditedSites(String arg0);

    /**
     * Translated "Filter by ''{0}''"
     *
     * @return translated "Filter by ''{0}''"
     */
    @DefaultMessage("Filter by ''{0}''")
    String filterBy(String arg0);

    /**
     * Translated "Nothing entered to search on: please enter something you want to search for"
     *
     * @return translated "Nothing entered to search on: please enter something you want to search for"
     */
    @DefaultMessage("Nothing entered to search on: please enter something you want to search for")
    String searchQueryEmpty();

    /**
     * Translated "Enter a search query with at least 3 characters"
     *
     * @return translated "Enter a search query with at least 3 characters"
     */
    @DefaultMessage("Enter a search query with at least 3 characters")
    String searchQueryTooShort();

    /**
     * Translated "For query "{0}", found {1} databases, {2} activities and {3} indicators"
     *
     * @return translated "For query "{0}", found {1} databases, {2} activities and {3} indicators"
     */
    @DefaultMessage("For query \"{0}\", found {1} databases, {2} activities and {3} indicators")
    String searchResultsFound(String arg0, String arg1, String arg2, String arg3);

    /**
     * Translated "Add new entry for form ''{0}''"
     *
     * @return translated "Add new entry for form ''{0}''"
     */
    @DefaultMessage("Add new entry for form ''{0}''")
    String addNewSiteForActivity(String activityName);

    /**
     * Translated "{0,number} matching sites"
     *
     * @return translated "{0,number} matching sites"
     */
    @DefaultMessage("{0,number} matching sites")
    String matchingLocations(int count);

    /**
     * Translated "Use site"
     *
     * @return translated "Use site"
     */
    @DefaultMessage("Use site ''{0}''")
    @Key("useNamedLocation")
    String useLocation(String name);

    /**
     * Translated "Targets for database {0}"
     *
     * @return translated "Targets for database {0}"
     */
    @DefaultMessage("Targets for database {0}")
    String targetsForDatabase(String arg0);

    /**
     * Translated "Report ''{0}'' added to dashboard."
     *
     * @return translated "Report ''{0}'' added to dashboard."
     */
    @DefaultMessage("Report ''{0}'' added to dashboard.")
    String addedToDashboard(String reportName);

    /**
     * Translated "Report ''{0}'' removed from dashboard."
     *
     * @return translated "Report ''{0}'' removed from dashboard."
     */
    @DefaultMessage("Report ''{0}'' removed from dashboard.")
    String removedFromDashboard(String reportName);

    /**
     * Translated "The report ''{0}'' has been saved."
     *
     * @return translated "The report ''{0}'' has been saved."
     */
    @DefaultMessage("The report ''{0}'' has been saved.")
    String reportSaved(String name);

    /**
     * Translated "Are you sure you want to delete the report "{0}""
     *
     * @return translated "Are you sure you want to delete the report "{0}""
     */
    @DefaultMessage("Are you sure you want to delete the report \"{0}\"")
    String confirmDeleteReport(String reportTitle);

    /**
     * Translated "You are not the owner of this report.<br/>Do you want to save a new copy?"
     *
     * @return translated "You are not the owner of this report.<br/>Do you want to save a new copy?"
     */
    @DefaultMessage("You are not the owner of this report.<br/>Do you want to save a new copy?")
    String confirmSaveCopy();

    /**
     * Translated "The form "{0}" has not been marked as public by the database owner and so cannot be embedded in a public web page. Please contact the database owner and request that the activity be published."
     *
     * @return translated "The form "{0}" has not been marked as public by the database owner and so cannot be embedded in a public web page. Please contact the database owner and request that the activity be published."
     */
    @DefaultMessage("The form \"{0}\" has not been marked as public by the database owner and so cannot be embedded in a public web page. Please contact the database owner and request that the activity be published.")
    String activityNotPublic(String name);

    /**
     * Translated "In order to embed this sheet in a public web page, the form "{0}" must be made public. Do you want to make this form public now?"
     *
     * @return translated "In order to embed this sheet in a public web page, the form "{0}" must be made public. Do you want to make this form public now?"
     */
    @DefaultMessage("In order to embed this sheet in a public web page, the form \"{0}\" must be made public. Do you want to make this form public now?")
    String promptPublishActivity(String name);

    /**
     * Translated "{0} minutes ago"
     *
     * @return translated "{0} minutes ago"
     */
    @DefaultMessage("{0} minutes ago")
    String minutesAgo(int minutes);

    /**
     * Translated "{0} hours ago"
     *
     * @return translated "{0} hours ago"
     */
    @DefaultMessage("{0} hours ago")
    String hoursAgo(int hours);

    /**
     * Translated "{0} days ago"
     *
     * @return translated "{0} days ago"
     */
    @DefaultMessage("{0} days ago")
    String daysAgo(int hours);

    /**
     * Translated "{0}: New {1} at {2} by {3}"
     *
     * @return translated "{0}: New {1} at {2} by {3}"
     */
    @DefaultMessage("{0}: New {1} at {2} by {3}")
    String newSiteSubject(String databaseName, String activityName, String locationName, String partnerName);

    /**
     * Translated "{0}: Updated {1} at {2}"
     *
     * @return translated "{0}: Updated {1} at {2}"
     */
    @DefaultMessage("{0}: Updated {1} at {2}")
    String updatedSiteSubject(String databaseName, String activityName, String locationName);

    /**
     * Translated "{0}: Deleted {1} at {2}"
     *
     * @return translated "{0}: Deleted {1} at {2}"
     */
    @DefaultMessage("{0}: Deleted {1} at {2}")
    String deletedSiteSubject(String databaseName, String activityName, String locationName);

    /**
     * Translated "Hi {0},"
     *
     * @return translated "Hi {0},"
     */
    @DefaultMessage("Hi {0},")
    String sitechangeGreeting(String userName);

    /**
     * Translated "{0} ({1}) created a new {2} at {3} in the {4} database on {5,date,dd-MM-yyyy 'at' HH:mm}. Here are the details:"
     *
     * @return translated "{0} ({1}) created a new {2} at {3} in the {4} database on {5,date,dd-MM-yyyy 'at' HH:mm}. Here are the details:"
     */
    @DefaultMessage("{0} ({1}) created a new {2} at {3} in the {4} database on {5,date,dd-MM-yyyy ''at'' HH:mm}. Here are the details:")
    String siteCreateIntro(String userName, String userEmail, String activityName, String locationName, String databaseName, Date date);

    /**
     * Translated "{0} ({1}) updated the {2} at {3} in the {4} database on {5,date,dd-MM-yyyy 'at' HH:mm}. Here are the details:"
     *
     * @return translated "{0} ({1}) updated the {2} at {3} in the {4} database on {5,date,dd-MM-yyyy 'at' HH:mm}. Here are the details:"
     */
    @DefaultMessage("{0} ({1}) updated the {2} at {3} in the {4} database on {5,date,dd-MM-yyyy ''at'' HH:mm}. Here are the details:")
    String siteChangeIntro(String userName, String userEmail, String activityName, String locationName, String database, Date date);

    /**
     * Translated "{0} ({1}) deleted the {2} at {3} in the {4} database on {5,date,dd-MM-yyyy 'at' HH:mm}."
     *
     * @return translated "{0} ({1}) deleted the {2} at {3} in the {4} database on {5,date,dd-MM-yyyy 'at' HH:mm}."
     */
    @DefaultMessage("{0} ({1}) deleted the {2} at {3} in the {4} database on {5,date,dd-MM-yyyy ''at'' HH:mm}.")
    String siteDeleteIntro(String userName, String userEmail, String activityName, String locationName, String database, Date date);

    /**
     * Translated "Best regards,<br>The ActivityInfo Team"
     *
     * @return translated "Best regards,<br>The ActivityInfo Team"
     */
    @DefaultMessage("Best regards,<br>The ActivityInfo Team")
    String sitechangeSignature();

    /**
     * Translated "{0,date,dd-MM-yyyy - HH:mm} {1} ({2}) added the entry."
     *
     * @return translated "{0,date,dd-MM-yyyy - HH:mm} {1} ({2}) added the entry."
     */
    @DefaultMessage("{0,date,dd-MM-yyyy - HH:mm} {1} ({2}) added the entry.")
    String siteHistoryCreated(Date date, String userName, String userEmail);

    /**
     * Translated "{0,date,dd-MM-yyyy - HH:mm} {1} ({2}) updated the entry:"
     *
     * @return translated "{0,date,dd-MM-yyyy - HH:mm} {1} ({2}) updated the entry:"
     */
    @DefaultMessage("{0,date,dd-MM-yyyy - HH:mm} {1} ({2}) updated the entry:")
    String siteHistoryUpdated(Date date, String userName, String userEmail);

    /**
     * Translated "No history is available for this form entry."
     *
     * @return translated "No history is available for this form entry."
     */
    @DefaultMessage("No history is available for this form entry.")
    String siteHistoryNotAvailable();

    /**
     * Translated "History on form entries is only available from {0,date,dd MMMM yyyy} onward."
     *
     * @return translated "History on form entries is only available from {0,date,dd MMMM yyyy} onward."
     */
    @DefaultMessage("History on form entries is only available from {0,date,dd MMMM yyyy} onward.")
    String siteHistoryAvailableFrom(Date date);

    /**
     * Translated "was: {0}"
     *
     * @return translated "was: {0}"
     */
    @DefaultMessage("was: {0}")
    String siteHistoryOldValue(Object oldValue);

    /**
     * Translated "was: blank"
     *
     * @return translated "was: blank"
     */
    @DefaultMessage("was: blank")
    String siteHistoryOldValueBlank();

    /**
     * Translated "{0}, {1,date,MMMM yyyy}"
     *
     * @return translated "{0}, {1,date,MMMM yyyy}"
     */
    @DefaultMessage("{0}, {1,date,MMMM yyyy}")
    String siteHistoryIndicatorName(String name, Date date);

    /**
     * Translated "Added attribute {0}"
     *
     * @return translated "Added attribute {0}"
     */
    @DefaultMessage("Added attribute {0}")
    String siteHistoryAttrAdd(String attrName);

    /**
     * Translated "Removed attribute {0}"
     *
     * @return translated "Removed attribute {0}"
     */
    @DefaultMessage("Removed attribute {0}")
    String siteHistoryAttrRemove(String attrName);

    /**
     * Translated "ActivityInfo digest for {0,date,dd-MM-yyyy}"
     *
     * @return translated "ActivityInfo digest for {0,date,dd-MM-yyyy}"
     */
    @DefaultMessage("ActivityInfo digest for {0,date,dd-MM-yyyy}")
    String digestSubject(Date now);

    /**
     * Translated "Hi {0},"
     *
     * @return translated "Hi {0},"
     */
    @DefaultMessage("Hi {0},")
    String digestGreeting(String userName);

    /**
     * Translated "If you don''t wish to receive this email, please click <a href="{0}" style="text-decoration: underline;">Unsubscribe</a>."
     *
     * @return translated "If you don''t wish to receive this email, please click <a href="{0}" style="text-decoration: underline;">Unsubscribe</a>."
     */
    @DefaultMessage("If you don''t wish to receive this email, please click <a href=\"{0}\" style=\"text-decoration: underline;\">Unsubscribe</a>.")
    String digestUnsubscribe(String unsubscribeLink);

    /**
     * Translated "Best regards,<br>The ActivityInfo Team"
     *
     * @return translated "Best regards,<br>The ActivityInfo Team"
     */
    @DefaultMessage("Best regards,<br>The ActivityInfo Team")
    String digestSignature();

    /**
     * Translated "Here are the updates to ActivityInfo in the last {0} hours, for your information."
     *
     * @return translated "Here are the updates to ActivityInfo in the last {0} hours, for your information."
     */
    @DefaultMessage("Here are the updates to ActivityInfo in the last {0} hours, for your information.")
    String geoDigestIntro(int hours);

    /**
     * Translated "<a href="mailto:{0}">{1}</a> edited the {2} at {3}"
     *
     * @return translated "<a href="mailto:{0}">{1}</a> edited the {2} at {3}"
     */
    @DefaultMessage("<a href=\"mailto:{0}\">{1}</a> edited the {2} at {3}")
    String geoDigestSiteMsg(String userEmail, String userName, String activityName, String locationName);

    /**
     * Translated "<span title="{0,date,dd-MM-yyyy}">today</span>."
     *
     * @return translated "<span title="{0,date,dd-MM-yyyy}">today</span>."
     */
    @DefaultMessage("<span title=\"{0,date,dd-MM-yyyy}\">today</span>.")
    String geoDigestSiteMsgDateToday(Date date);

    /**
     * Translated "<span title="{0,date,dd-MM-yyyy}">yesterday</span>."
     *
     * @return translated "<span title="{0,date,dd-MM-yyyy}">yesterday</span>."
     */
    @DefaultMessage("<span title=\"{0,date,dd-MM-yyyy}\">yesterday</span>.")
    String geoDigestSiteMsgDateYesterday(Date date);

    /**
     * Translated "on <span>{0,date,dd-MM-yyyy}</span>."
     *
     * @return translated "on <span>{0,date,dd-MM-yyyy}</span>."
     */
    @DefaultMessage("on <span>{0,date,dd-MM-yyyy}</span>.")
    String geoDigestSiteMsgDateOther(Date date);

    /**
     * Translated "Unmapped Sites"
     *
     * @return translated "Unmapped Sites"
     */
    @DefaultMessage("Unmapped Sites")
    String geoDigestUnmappedSites();

    /**
     * Translated "Here is the summary of the updates by user for the ActivityInfo databases you administer over the last {0} days."
     *
     * @return translated "Here is the summary of the updates by user for the ActivityInfo databases you administer over the last {0} days."
     */
    @DefaultMessage("Here is the summary of the updates by user for the ActivityInfo databases you administer over the last {0} days.")
    String activityDigestIntro(int days);

    /**
     * Translated "The following ActivityInfo databases have not been updated in the last {0} days:"
     *
     * @return translated "The following ActivityInfo databases have not been updated in the last {0} days:"
     */
    @DefaultMessage("The following ActivityInfo databases have not been updated in the last {0} days:")
    String activityDigestInactiveDatabases(int days);

    /**
     * Translated "{0} update(s) on {1,date,dd-MM-yyyy}"
     *
     * @return translated "{0} update(s) on {1,date,dd-MM-yyyy}"
     */
    @DefaultMessage("{0} update(s) on {1,date,dd-MM-yyyy}")
    String activityDigestGraphTooltip(int updates, Date date);

    /**
     * Translated "Set all rows to ''{0}''"
     *
     * @return translated "Set all rows to ''{0}''"
     */
    @DefaultMessage("Set all rows to ''{0}''")
    String updateAllRowsTo(String value);

    /**
     * Translated "Are you sure want to delete?"
     *
     * @return translated "Are you sure want to delete?"
     */
    @DefaultMessage("Are you sure want to delete?")
    String confirmDeleteSite();

    /**
     * Translated "Select a site above."
     *
     * @return translated "Select a site above."
     */
    @DefaultMessage("Select a site above.")
    String SelectSiteAbove();

    /**
     * Translated "Choose the destination field for the source column "<i>{0}</i>"."
     *
     * @return translated "Choose the destination field for the source column "<i>{0}</i>"."
     */
    @DefaultMessage("Choose the destination field for the source column \"<i>{0}</i>\".")
    SafeHtml columnMatchPrompt(String columnName);

    /**
     * Translated "Showing {0} of {1} columns."
     *
     * @return translated "Showing {0} of {1} columns."
     */
    @DefaultMessage("Showing {0} of {1} columns.")
    String showColumns(int numberOfColumnsShown, int numberOfColumnsTotal);

    /**
     * Translated "Are you sure you want to delete {0} row(s) from {1}?"
     *
     * @return translated "Are you sure you want to delete {0} row(s) from {1}?"
     */
    @DefaultMessage("Are you sure you want to delete {0} row(s) from {1}?")
    String removeTableRowsConfirmation(int numberOfRows, String formClassLabel);

    /**
     * Translated "{0} updated!"
     *
     * @return translated "{0} updated!"
     */
    @DefaultMessage("{0} updated!")
    String newVersion(String appTitle);

    /**
     * Translated "Do you want to retry deletion?"
     *
     * @return translated "Do you want to retry deletion?"
     */
    @DefaultMessage("Do you want to retry deleting {0} row(s) from {1}?")
    String retryDeletingRowRange(int size, String formClassLabel);

    /**
     * Translated "Deleting {0} row(s) from {1}..."
     *
     * @return translated "Deleting {0} row(s) from {1}..."
     */
    @DefaultMessage("Deleting {0} row(s) from {1}...")
    String deletingRows(int size, String formClassLabel);

    /**
     * Translated "Add new site"
     *
     * @return translated "Add new site"
     */
    @DefaultMessage("Add {0}")
    @Key("addNamedThing")
    String addLocation(String locationTypeName);

    /**
     * Translated "Edit {0}"
     *
     * @return translated "Edit {0}"
     */
    @DefaultMessage("Edit {0}")
    String editLocation(String locationTypeName);

    /**
     * Translated "<b>Showing {0} of {1} columns.</b> You can choose visible columns with ''{2}'' button"
     *
     * @return translated "<b>Showing {0} of {1} columns.</b> You can choose visible columns with ''{2}'' button"
     */
    @DefaultMessage("<b>Showing {0} of {1} columns.</b> You can choose visible columns with ''{2}'' button")
    SafeHtml notAllColumnsAreShown(int visibleColumns, int allColumns, String chooseColumnButtonName);

    /**
     * Translated "Field is mandatory but not mapped: {0}"
     *
     * @return translated "Field is mandatory but not mapped: {0}"
     */
    @DefaultMessage("Field is mandatory but not mapped: {0}")
    String fieldIsMandatory(String fieldLabel);

    /**
     * Translated "Please map all mandatory columns, missed mapping for {0}"
     *
     * @return translated "Please map all mandatory columns, missed mapping for {0}"
     */
    @DefaultMessage("Please map all mandatory columns, missed mapping for {0}")
    String pleaseMapAllMandatoryColumns(String columnLabels);

    /**
     * Translated "{0} rows are invalid and won''t be imported. Continue?"
     *
     * @return translated "{0} rows are invalid and won''t be imported. Continue?"
     */
    @DefaultMessage("{0} rows are invalid and won''t be imported. Continue?")
    String continueImportWithInvalidRows(int invalidRowsCount);

    /**
     * Translated "{0}% Complete"
     *
     * @return translated "{0}% Complete"
     */
    @DefaultMessage("{0}% Complete")
    String percentComplete(int percent);

    /**
     * Translated "Oh no! Your import is missing required column(s): {0}"
     *
     * @return translated "Oh no! Your import is missing required column(s): {0}"
     */
    @DefaultMessage("Oh no! Your import is missing required column(s): {0}")
    String missingColumns(String missingColumns);

    /**
     * Translated "{0} code does not exist."
     *
     * @return translated "{0} code does not exist."
     */
    @DefaultMessage("{0} code does not exist.")
    String doesNotExist(String placeholder);

    /**
     * Translated "Please provide valid comma separated text"
     *
     * @return translated "Please provide valid comma separated text"
     */
    @DefaultMessage("Please provide valid comma separated text. Column count does not match in row number {0}.")
    String columnCountMismatchAtRow(int rowNumber);

    /**
     * Translated "Exceeds maximum length of {0} characters."
     *
     * @return translated "Exceeds maximum length of {0} characters."
     */
    @DefaultMessage("Exceeds maximum length of {0} characters.")
    String exceedsMaximumLength(int maxLength);

    /**
     * Translated "Invalid value. Please enter date in following format: {0}"
     *
     * @return translated "Invalid value. Please enter date in following format: {0}"
     */
    @DefaultMessage("Invalid value. Please enter date in following format: {0}")
    String dateFieldInvalidValue(String format);

    /**
     * Translated "Please enter a number. For example: {0} or {1} or {2}"
     *
     * @return translated "Please enter a number. For example: {0} or {1} or {2}"
     */
    @DefaultMessage("Please enter a number. For example: {0} or {1} or {2}")
    String quantityFieldInvalidValue(int integer, double doubleWithoutPoint, double doubleWithPoint);
}
