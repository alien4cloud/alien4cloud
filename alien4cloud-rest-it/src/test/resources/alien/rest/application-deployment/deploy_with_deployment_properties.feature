Feature: Deploy an application with deployment properties

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload a plugin
    And I create a cloud with name "mock cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
    And I enable the cloud "mock cloud"
    And There are these users in the system
      | sangoku |
    And I add a role "APPLICATIONS_MANAGER" to user "sangoku"
    And I add a role "CLOUD_DEPLOYER" to user "sangoku" on the resource type "CLOUD" named "mock cloud"
    And I am authenticated with user named "sangoku"

  Scenario: Deploy an application with deployment properties on mock-paas-provider
    Given I have an application with name "ALIEN"
    And I assign the cloud with name "mock cloud" for the application
    And I give deployment properties
      | managementUrl | http://cloudifyurl:8099 |
      | numberBackup  | 1                       |
      | managerEmail  | admin@alien.fr          |
    When I deploy it
    Then I should receive a RestResponse with no error
    And The application's deployment must succeed

  Scenario: Deploy an application with bad deployment properties on mock-paas-provider
    Given I have an application with name "ALIEN"
    And I assign the cloud with name "mock cloud" for the application
    And I give deployment properties
      | managementUrl | http://cloudifyurl:8099 |
      | numberBackup  | 0                       |
    Then I should receive a RestResponse with an error code 800
