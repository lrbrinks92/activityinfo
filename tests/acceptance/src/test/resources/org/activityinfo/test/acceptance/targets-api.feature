@api
Feature: Indicator Targets API
  As a systems analyst
  I want to be able to define target indicator values through the API
  So I can connect my existing systems with ActivityInfo

  Background:
    Given I have created a database "PAKA"
    And I have added partner "UNHCR PESHAWAR"
    And I have created the project "Direct - HCR Peshawar"
    And I have created a form named "Advocacy Conducted"
    And I have created a quantity field "Advocacy conducted USD" in "Advocacy Conducted"
    
  Scenario: Defining Targets by Partner
    When I execute the command:
      """
      type: AddTarget
      command:
        databaseId: $PAKA
        target:
          name: Pakistan
          partnerId: ${UNHCR PESHAWAR}
          projectId: ${Direct - HCR Peshawar}
          fromDate: 2014-01-01
          toDate: 2014-12-31
      """
    Then the response should be:
      """
      newId: $Pakistan
      """
    When I execute the command:
      """
      type: UpdateTargetValue
      command: 
        targetId: $Pakistan
        indicatorId: ${Advocacy conducted USD}
        changes:
          value: 11996
      """
    Then the response should be 204 No Content
    When I execute the command:
      """
      type: GetTargets
      command:
        databaseId: $PAKA
      """
    Then the response should be:
      """
      - id: $Pakistan
        name: Pakistan
        fromDate: 2014-01-01
        toDate: 2014-12-31
        partner:
          id: ${UNHCR PESHAWAR}
          name: UNHCR PESHAWAR
        project:
          id: ${Direct - HCR Peshawar}
          name: Direct - HCR Peshawar
        targetValues:
          - indicatorId: ${Advocacy conducted USD}
            value: 11996
      """
    
    Scenario: Deleting Targets
      Given I have created a target named "Goals" for database "PAKA"
      When I execute the command:
      """
        type: Delete
        command:
          entityName: Target
          id: $Goals
      """
      Then the response should be 204 No Content
      
      
    Scenario: Invalid command with missing dates
      When I execute the command:
      """
      type: AddTarget
      command:
        databaseId: $PAKA
        target:
          name: Pakistan
      """
      Then the response should fail with 400 Bad Request and mention "date"

      
    Scenario Outline: Creating a target requires design privileges 
       Given I have granted bob@bedatadriven.com permission to <role> on behalf of "UNHCR PESHAWAR"
       When bob@bedatadriven.com executes the command:
       """
      type: AddTarget
      command:
        databaseId: $PAKA
        target:
          name: Pakistan
          fromDate: 2014-01-01
          toDate: 2014-12-31
      """
      Then the response should be <code> <status>
  
      Examples:
        | role     | code | status     |
        | Design   | 201  | Created    |
        | View     | 403  | Forbidden  |
      
      
    Scenario Outline: Deleting a target requires design privileges
      Given I have created a target named "Goals" for database "PAKA"
        And I have granted bob@bedatadriven.com permission to <role> on behalf of "UNHCR PESHAWAR"
      When bob@bedatadriven.com executes the command:
      """
        type: Delete
        command:  
          entityName: Target
          id: $Goals
        """
      Then the response should be <code> <status>
      
      Examples:
      | role     | code | status     |
      | Design   | 204  | No Content |
      | View     | 403  | Forbidden  |
      | View All | 403  | Forbidden  |
      | Edit     | 403  | Forbidden  |
      | Edit All | 403  | Forbidden  |
      
   