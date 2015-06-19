@web
Feature: New User Sign Up

  Scenario: New User Sign Up
    When I sign up for a new user account
    Then I should receive an email with a link to confirm my address
    When I follow the link
     And I choose the password "MySecretPassword123"
    Then my dashboard should open
    When I logout
     And I login with the password "MySecretPassword123"
    Then my dashboard should open

  Scenario: Sign up links cannot be re-used
    Given I have signed up for a new account
      And I have confirmed my account
     When I follow the invitation link again
     Then I should receive a message that the link has already been used
    
  Scenario: Passwords less than six characters are rejected
    When I sign up for a new user account
    Then I should receive an email with a link to confirm my address
    When I follow the link
     And I choose the password "a"
    Then I should receive a message that my password is too short
    When I choose the password "12345"
    Then I should receive a message that my password is too short
    When I choose the password ""
    Then I should receive a message that my password is too short
    When I choose the password "123456"
    Then my dashboard should open
