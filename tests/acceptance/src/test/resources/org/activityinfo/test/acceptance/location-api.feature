@api
Feature: Locations API
  As an information management specialist
  I want to be able to manage locations through the API
  So I can integrate ActivityInfo with external geographic information systems
  
  @AI-962
  Scenario: Querying locations and their codes
    Given I have created a database "My Geodatabase"
    And I have created a location type "Settlements"
    When I execute the command:
    """
      type: CreateLocation
      command:
        properties:
          id: $X
          name: Village X
          axe: V10001
          locationTypeId: $Settlements
    """
    Then the response should be 204 No Content
    When I request /resources/locations?type=$Settlements
    Then the response should be:
    """
      - id: $X
        name: Village X
        code: V10001
    """
    
  @AI-985
  Scenario: Querying sites and their location codes
    Given I have created a database "My Geodatabase"
    And I have added partner "UNICEF"
    And I have created a location type "Settlements"
    And I have created a location "Village X" in "Settlements" with code "VX4445"
    And I have created a form named "Distributions" with location type "Settlements"
    And I have submitted a "Distributions" form with:
      | field       | value        |
      | location    | Village X    |
      | partner     | UNICEF       |
      | fromDate    | 2014-01-01   |
      | toDate      | 2014-04-10   |
    When I request /resources/sites?activity=$Distributions
    Then the response should be:
    """
    - id: $SiteId
      activity: $Distributions
      timestamp: ${Village X Last Edit}
      startDate: 2014-01-01
      endDate:   2014-04-10
      location:
        id: ${Village X}
        name: Village X
        code: VX4445
      partner:
        id: ${UNICEF}
        name: UNICEF
    """
    