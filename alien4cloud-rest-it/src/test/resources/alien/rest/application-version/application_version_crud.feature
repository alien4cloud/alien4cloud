Feature: CRUD operations on application version

  Background: 
    Given I am authenticated with "ADMIN" role
    And I upload a plugin
    And I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
    And I enable the cloud "Mount doom cloud"
    And There are these users in the system
      | lufy |
    And I add a role "APPLICATIONS_MANAGER" to user "lufy"
    And I add a role "CLOUD_DEPLOYER" to user "lufy" on the resource type "CLOUD" named "Mount doom cloud"
    And I am authenticated with user named "lufy"

  Scenario: Create an application version with failure
    Given I have an application with name "ALIEN"
    And I create an application version with version "0.3..0-SNAPSHOT-SHOULD-FAILED"
    Then I should receive a RestResponse with an error code 605

  Scenario: Create an application version with the same name version raise a conflict
    Given I have an application with name "ALIEN"
    And I create an application version with version "0.1.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 502

  Scenario: Create an application version with success
    Given I have an application with name "ALIEN"
    And I create an application version with version "0.3.0-SNAPSHOT"
    Then I should receive a RestResponse with no error

  Scenario: Delete an application version when it' the last should failled
    Given I have an application with name "ALIEN"
    And I delete an application version with name "0.1.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 610

  Scenario: Delete an application version with failure
    Given I have an application with name "ALIEN"
    And I delete an application version with name "0.2.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 504
    
  Scenario: Delete an application version with failure when application is deployed
    Given I have an application with name "ALIEN"
    And I deploy the application "ALIEN" with cloud "Mount doom cloud" for the topology
    And I delete an application version with name "0.1.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 507

  Scenario: Delete an application version with success
    Given I have an application with name "ALIEN"
    And I create an application version with version "0.2.0-SNAPSHOT"
    Then I should receive a RestResponse with no error
    And I delete an application version with name "0.2.0-SNAPSHOT"
    Then I should receive a RestResponse with no error

  Scenario: Search for application versions
    Given I have an application with name "ALIEN"
    And I create an application version with version "0.3.0-SNAPSHOT"
    Then I should receive a RestResponse with no error
    When I search for application versions
    Then I should receive 2 application versions in the search result

  Scenario: Update an application version with success
    Given I have an application with name "ALIEN"
    And I create an application version with version "0.2.0-SNAPSHOT"
    Then I should receive a RestResponse with no error
    And I update an application version with version "0.2.0-SNAPSHOT" to "0.4.0-SNAPSHOT"
    Then I should receive a RestResponse with no error

  Scenario: Update an application version with an existing name should be failed
    Given I have an application with name "ALIEN"
    And I create an application version with version "0.2.0-SNAPSHOT"
    Then I should receive a RestResponse with no error
    And I update an application version with version "0.1.0-SNAPSHOT" to "0.2.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 502

  Scenario: Update a released application version should be failed
    Given I have an application with name "ALIEN"
    And I create an application version with version "0.2.0"
    Then I should receive a RestResponse with no error
    And I update an application version with version "0.2.0" to "0.2.1"
    Then I should receive a RestResponse with an error code 608