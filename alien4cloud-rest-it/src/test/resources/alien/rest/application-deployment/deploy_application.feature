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