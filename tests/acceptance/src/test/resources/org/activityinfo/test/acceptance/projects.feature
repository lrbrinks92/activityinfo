@web @cross-browser
Feature: Projects
  As a program manager
  I want to be able to associate results with specific projects such as contracts or funding agreements
  So I can report results at the project level

  Background:
    Given I have created a database "RRMP"
    And I have added partner "NRC" to "RRMP"
    And I have created the project "Contract FY2014"
    And I have created the project "Contract FY2015"
    And I have created a form named "NFI Distribution" in "RRMP"
    And I have created a quantity field "nb. kits" in "NFI Distribution"
    And I have submitted a "NFI Distribution" form with:
      | field              | value           |
      | partner            | NRC             |
      | project            | Contract FY2014 |
      | nb. kits           | 1000            |
    And I have submitted a "NFI Distribution" form with:
      | field              | value           |
      | partner            | NRC             |
      | project            | Contract FY2015 |
      | nb. kits           | 500             |

  Scenario: Defining per-project targets
    When I create a target for project "Contract FY2014" with values:
      | field              | value           |
      | nb. kits           | 1500            |
    When I create a target for project "Contract FY2015" with values:
      | field              | value           |
      | nb. kits           | 600             |
    Then aggregating the indicator "nb. kits" by Project and Realized / Targeted should yield:
      |                  | Value |
      | Contract FY2014  |       |
      |   Realized       | 1,000 |
      |   Targeted       | 1,500 |
      | Contract FY2015  |       |
      |   Realized       |   500 |
      |   Targeted       |   600 | 
    
  