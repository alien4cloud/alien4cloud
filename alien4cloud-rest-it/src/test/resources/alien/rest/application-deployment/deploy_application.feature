Feature: Deploy an application

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload a plugin
    And I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
    And I enable the cloud "Mount doom cloud"
    And There are these users in the system
      | sangoku |
    And I add a role "APPLICATIONS_MANAGER" to user "sangoku"
    And I add a role "CLOUD_DEPLOYER" to user "sangoku" on the resource type "CLOUD" named "Mount doom cloud"
    And I am authenticated with user named "sangoku"

  Scenario: Deploy an application with success
    Given I have an application with name "ALIEN"
    And I assign the cloud with name "Mount doom cloud" for the application
    When I deploy it
    Then I should receive a RestResponse with no error
    And The application's deployment must succeed

  Scenario: Deploy an application with failure
    Given I have an application with name "BAD-APPLICATION"
    And I assign the cloud with name "Mount doom cloud" for the application
    When I deploy it
    Then I should receive a RestResponse with no error
    And The application's deployment must fail

  Scenario: Deploy an application with warning
    Given I have an application with name "WARN-APPLICATION"
    And I assign the cloud with name "Mount doom cloud" for the application
    When I deploy it
    Then I should receive a RestResponse with no error
    And The application's deployment must finish with warning

  Scenario: Create 4 applications without deploying and check application's statuses
    Given I have an applications with names and descriptions
      | BAD-APPLICATION | This Application should be in FAILURE status...  |
      | ALIEN            | This application should be in DEPLOYED status... |
    When I can get applications statuses
    And I have expected applications statuses for "deployment" operation
      | BAD-APPLICATION | UNDEPLOYED |
      | ALIEN            | UNDEPLOYED |
    And I should receive a RestResponse with no error

  Scenario: Create 4 applications, deploy all and final check statuses
    Given I have an applications with names and descriptions
      | My-Software-Factory | This application should be in DEPLOYED status... |
      | WARN-APPLICATION    | This application should be in WARNING status...  |
      | BAD-APPLICATION     | This Application should be in FAILURE status...  |
      | ALIEN                | This application should be in DEPLOYED status... |
    When I can get applications statuses
    And I deploy all applications with cloud "Mount doom cloud"
    And I have expected applications statuses for "deployment" operation
      | BAD-APPLICATION     | FAILURE  |
      | ALIEN                | DEPLOYED |
      | WARN-APPLICATION    | WARNING  |
      | My-Software-Factory | DEPLOYED |
    And I should receive a RestResponse with no error

  Scenario: deleting an deployed application should fail
    Given I have an application with name "ALIEN"
    And I deploy the application "ALIEN" with cloud "Mount doom cloud" for the topology
    When I delete the application "ALIEN"
    Then I should receive a RestResponse with no error
    And I should receive a RestResponse with a boolean data "false"
    And the application can be found in ALIEN
