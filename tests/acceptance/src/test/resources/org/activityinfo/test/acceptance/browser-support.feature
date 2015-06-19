@crossbrowser
Feature: Cross browser support

Scenario Outline: Supported Browsers
  Given I am using <browser> on <os>
  When I login as "auto.qa@bedatadriven.com" with my correct password
  Then my dashboard should open
  
  Examples:
    | browser    | os          |
    | IE 8       | Windows 7   |
    | IE 9       | Windows 7   |
    | IE 10      | Windows 7   |
    | IE 10      | Windows 8   |
    | IE 11      | Windows 8.1 |
    | Chrome     | Windows 7   |
    | Chrome     | Windows 8   |
    | Chrome     | Windows 8.1 |
    | Chrome     | Linux       |
    | Safari 5   | OS X 10.6   |
    | Safari 6   | OS X 10.8   |
    | Safari 7   | OS X 10.9   |
    | Safari 8   | OS X 10.10  |
    | Firefox 35 | Windows 7   |
    | Firefox 35 | Linux       |
    | Firefox 10 | Windows 7   |
    | Firefox 10 | Windows XP  |
    | Firefox 10 | Linux       |
    | Firefox 35 | OS X 10.10  |

Scenario Outline: Unsupported browsers
  Given I am using <browser> on <os>
  When I login as "qa@bedatadriven.com" with my correct password
  Then I should receive a message that my browser is not unsupported

  Examples:
    | browser    | os          |
    | IE 6       | Windows XP  |
    | IE 7       | Windows XP  |
    | Opera      | Windows 7   |
  
