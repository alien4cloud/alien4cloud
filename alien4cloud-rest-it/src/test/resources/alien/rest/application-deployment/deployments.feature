Feature: get deployments

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload a plugin
    And I create a cloud with name "mock cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
    And I create a cloud with name "mock cloud 2" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
    And I enable the cloud "mock cloud"
    And I enable the cloud "mock cloud 2"
    And There are these users in the system
      | sangoku |
    And I add a role "APPLICATIONS_MANAGER" to user "sangoku"
    And I add a role "CLOUD_DEPLOYER" to user "sangoku" on the resource type "CLOUD" named "mock cloud"
    And I add a role "CLOUD_DEPLOYER" to user "sangoku" on the resource type "CLOUD" named "mock cloud 2"
    And I am authenticated with user named "sangoku"
    And I have applications with names and descriptions
      | The great eye | This application should be in DEPLOYED status... |
    And I deploy all applications with cloud "mock cloud"

  Scenario: Ask for detailed deployment object of one cloud
    Given I have applications with names and descriptions
      | ALIEN_1 | ALIEN 1 |
      | ALIEN_2 | ALIEN 2 |
    And I deploy all applications with cloud "mock cloud 2"
    When I ask for detailed deployments for cloud "mock cloud"
    Then I should receive a RestResponse with no error
    And the response should contains 1 deployments DTO and applications
      | The great eye |
    When I ask for detailed deployments for cloud "mock cloud 2"
    Then I should receive a RestResponse with no error
    And the response should contains 2 deployments DTO and applications
      | ALIEN_1 |
      | ALIEN_2 |

  Scenario: Ask for detailed deployment object of all cloud
    Given I have applications with names and descriptions
      | ALIEN_1 | ALIEN 1 |
      | ALIEN_2 | ALIEN 2 |
    And I deploy all applications with cloud "mock cloud 2"
    When I ask for detailed deployments for all cloud
    Then I should receive a RestResponse with no error
    And the response should contains 3 deployments DTO and applications
      | The great eye |
      | ALIEN_1       |
      | ALIEN_2       |
