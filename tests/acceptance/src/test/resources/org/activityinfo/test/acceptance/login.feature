@web @cross-browser
Feature: Login

Scenario: Successful login
  Given that the user "auto.qa@bedatadriven.com" is signed up
  When I login as "auto.qa@bedatadriven.com" with my correct password
  Then my dashboard should open

Scenario: Incorrect email address
  Given that the user "nonexistuser@example.com" is not signed up
  When I login as "nonexistantuser@example.com" with password "foobar"
  Then I should see an error alert
