@web @api
Feature: Import from Excel

  Background:
    Given I have created a database "Import"
    And I have added partner "NRC" to "Import"
    And I have created a form named "NFI Distribution"
    And I have created a quantity field "nb. kits" in "NFI Distribution"
    And I have created a single-valued enumerated field "Donor" with items:
      | USAID |
      | ECHO  |
      | NRC   |

  Scenario: Import single-valued enum from Excel
    When I import into the form "NFI Distribution" spreadsheet:
      | Partner | Donor | Nb. kits | start date | end date   | Comments |
      | NRC     | USAID | 1,000    | 01/02/2014 | 01/03/2014 | row 1    |
      | NRC     | ECHO  | 500      | 01/03/2014 | 01/04/2014 | row 2    |
      | NRC     | ECHO  | 2,000    | 01/04/2014 | 01/05/2014 | row 3    |
    And open table for the "NFI Distribution" form in the database "Import"
    Then table has rows with hidden built-in columns:
      | Partner | Donor | Nb. kits |
      | NRC     | USAID | 1,000    |
      | NRC     | ECHO  | 500      |
      | NRC     | ECHO  | 2,000    |