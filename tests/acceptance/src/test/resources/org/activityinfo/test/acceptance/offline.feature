@web
Feature: Offline Mode

Scenario: Offline Data Entry
  Given I have created a database "RRMP"
    And I have added partner "NRC" to "RRMP"
    And I have granted jim@nrc.org permission to View on behalf of "NRC"
    And I have created a form named "NFI Distribution" in "RRMP"
    And I have created a quantity field "nb. kits" in "NFI Distribution"
    And I have created a quantity field "Satisfaction score" in "NFI Distribution"
    And I have submitted a "NFI Distribution" form with:
    | field              | value           |
    | partner            | NRC             |
    | nb. kits           | 1000            |
    And I have enabled offline mode
   When I open the application without an internet connection
   Then the application should be in offline mode
   Then the "NFI Distribution" form should have one submission
   When I submit a "NFI Distribution" form with:
    | field              | value           |
    | partner            | NRC             |
    | nb. kits           | 5000            |
   Then the "NFI Distribution" form should have 2 submissions
   When an internet connection becomes available
    And I synchronize with the server
    And I open a new session as jim@nrc.org
   Then the "NFI Distribution" form should have 2 submissions