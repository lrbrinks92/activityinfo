Feature: Offline Mode

Scenario: Enabling Offline mode
  Given my browser supports offline mode
  Given that I am logged in as "qa@bedatadriven.com"
  Given offline mode is not enabled
  When I enable offline mode
  Then offline mode should be enabled
  
  

