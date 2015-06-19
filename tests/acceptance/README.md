
# Acceptance Test Suite

## Introduction

Acceptance tests are executable specifications of the behavior of the application, intended to be "business-facing"
and so written in terms of the value they provide to users.

Acceptance tests are run against each build of ActivityInfo, and only those that pass the full acceptance test suite
are eligible for deployment.


## Running

The acceptance test suite can run locally on a developer's workstation against the AppEngine Development Server
before committing or for debugging failing tests.

Download and install [https://sites.google.com/a/chromium.org/chromedriver/downloads](ChromeDriver) on your 
development machine.

From the root project, execute:

    ./gradlew acceptanceTest
    
## Running interactively

To run individual tests interactively from IntelliJ, first start the development server, either 
from the 'Gradle tasks' tool window or from the command line by running:

    ./gradlew appengineStartAT
    
If you install the IntelliJ [https://plugins.jetbrains.com/plugin/7212?pr=idea](Cucumber for Java]
plugin, you can now right-click on individual .feature files and choose "Run" or "Debug"


## Design

The test suite is broadly composed of three components: functional tests, cross-functional tests, and 
api tests.

### Functional Testing

Functional tests describe and verify that ActivityInfo serves specific functions to meet the requirements of our
users. End users should be able to read these tests and should be able to critique and eventually validate them.

These tests should not assume any specific user interface and should certainly not make reference to buttons or 
specific user interface elements: ideally we can use the same test to verify that the application meets end user
requirements when accessed through a desktop browser, some future mobile interface, the API, or even a third
party application like ODK Collect.

### Cross-functional Testing

The acceptance test suite also includes tests of what are sometimes called "non-functional requirements" or
"cross-functional requirements."

Such requirements include for example: 
* All features must usable across a set of "recent" browsers 
* All features must be usable on connections with limited bandwidth and/or high latency
* Certain features must be available in offline mode without an internet connection (tagged with @offline)
* Most features should be usable via the API (tagged with @api)

### Scale Testing



### Capacity Testing



### API Testing

While most of the acceptance tests are written in a way that focuses on the pure functionality with minimal
reference to the specifics of the user interface, the public-facing API has the additional requirement that 
it must remain consistent at a very detailed level to ensure that external integrations are not broken.