Feature: Locks on Monthly Reporting
  
  Background:     
    Given I have created a database "LCRP-R WASH"
    And I have created a monthly form named "Site"
    And I have added partner "ACF"
    And I have added partner "NRC"
    And I have created a quantity field "# with improved water supply"
    And I have submitted a "Site" form with partner ACF with monthly reports:
      | Month    | # with improved water supply |
      | 2014-01  |                         1000 |
      | 2014-02  |                          500 |
      | 2014-03  |                          250 |
      | 2015-01  |                         1000 |
      | 2015-02  |                          500 |
      | 2015-03  |                          250 |
    And I have submitted a "Site" form with partner NRC with monthly reports:
      | 2013-12  |                         1900 |
      | 2015-01  |                          500 |

  Scenario: Deleting sites
    When I add a lock on the database "LCRP-R WASH" from "2015-01-01" to "2015-01-31"
    Then the site belonging to NRC is locked
     But the site belonging to ACF is not locked
     And I can delete the site belonging to ACF
     But I cannot delete the site belonging to NRC
    