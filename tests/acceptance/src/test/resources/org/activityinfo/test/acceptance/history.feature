@web
Feature: Data Entry History
  As a database owner,
  I want to be able to see the history of all changes to form submissions
  So I can understand where values have come from and why they change
  
  Scenario: Changes are logged
    Given I have created a database "RRMP"
      And I have added partner "NRC" to "RRMP"
      And I have created a form named "NFI Distribution"
      And I have created a quantity field "nb. kits" in "NFI Distribution"
      And I have created a enumerated field "donor" with items:
        | USAID  |  
        | ECHO   |
        | NRC    |
     When I submit a "NFI Distribution" form with:
        | field              | value           |
        | partner            | NRC             |
        | nb. kits           | 1000            |
        | donor              | USAID           |
     Then the submission's history should show that I created it just now
     When I update the submission with:
        | field              | value           |
        | nb. kits           | 3400            | 
     Then the submission's history should show a change from 1000 to 3400
     When I update the submission with:
       | field              | value           |
       | donor              | NRC             |     
     Then the submission's history should show a change from USAID to NRC
    