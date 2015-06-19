@web @api
Feature: Export to Excel

  Scenario: Export single form to Excel
    Given I have created a database "RRMP"
    And I have added partner "NRC" to "RRMP"
    And I have created a form named "NFI Distribution" with the submissions:
      | Partner | Donor | Nb. kits  |
      | enum    | enum  | quantity  |
      | NRC     | USAID |  1000     |
      | NRC     | ECHO  |   500     |
      | NRC     | ECHO  |  2000     |
    When I export the form "NFI Distribution"
    Then the exported spreadsheet contains:
      | Partner | USAID | ECHO  | Nb. kits  |
      | NRC     | true  | false | 1,000     |
      | NRC     | false | true  | 500       |
      | NRC     | false | true  | 2,000     |

  Scenario: Export single form without submissions to Excel
    Given I have created a database "RRMP"
    And I have created a form named "NFI Distribution"
    And I have created a quantity field "Nb. of kits"
    When I export the form "NFI Distribution"
    Then the exported spreadsheet contains:
      | Partner | Nb. of kits  |
    
    
  Scenario: Export form with monthly reporting to Excel
    Given I have created a database "LCRP-R WASH"
    And I have created a monthly form named "Site"
    And I have added partner "ACF"
    And I have created a quantity field "# with improved water supply"
    And I have submitted a "Site" form with partner ACF with monthly reports:
      | Month    | # with improved water supply |
      | 2015-01  |                         1000 |
      | 2015-02  |                          500 |
      | 2015-03  |                          250 |
    When I export the form "Site"
    Then the exported spreadsheet contains:
      | Date1  | Date2   | Partner | # with improved water supply |
      | 1/1/15 | 1/31/15 | ACF     |                        1,000 |
      | 2/1/15 | 2/28/15 | ACF     |                          500 |
      | 3/1/15 | 3/31/15 | ACF     |                          250 |   

