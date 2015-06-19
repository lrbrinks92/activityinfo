@web
Feature: Clone database

  Background:
    Given I have created a database "RRMP"
    And I have added partner "NRC" to "RRMP"
    And I have added partner "PP" to "RRMP"
    And I have created a form named "NFI Distribution"

  @AI-1057
  Scenario: Clone database
    Given I have cloned a database "RRMP" with name "RRMP_Clone"
    Given I have added partner "PP1" to "RRMP"
    Then "RRMP_Clone" database has "NRC" partner
    Then "RRMP_Clone" database has "PP" partner
    Then "RRMP_Clone" database has "NFI Distribution" form
    Then "NFI Distribution" form has "Partner" form field with values in database "RRMP_Clone":
      | NRC  |
      | PP   |
