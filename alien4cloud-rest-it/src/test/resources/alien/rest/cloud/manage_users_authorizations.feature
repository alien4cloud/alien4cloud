Feature: Manage user's authorizations on an cloud

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload a plugin
    And I create a cloud with name "middle_earth" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
    And I enable "middle_earth"

  Scenario: Add / Remove rights to a user on a cloud with ADMIN role
    Given I add a role "CLOUD_DEPLOYER" to user "user" on the resource type "CLOUD" named "middle_earth"
    Then I should receive a RestResponse with no error
    When I remove a role "CLOUD_DEPLOYER" to user "user" on the resource type "CLOUD" named "middle_earth"
    Then I should receive a RestResponse with no error

  Scenario: Search / List clouds when the user has sufficient right
    Given I create a cloud with name "mordor" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
    And I add a role "CLOUD_DEPLOYER" to user "user" on the resource type "CLOUD" named "middle_earth"
    And I am authenticated with "USER" role
    When I list clouds
    And Response should contains 1 cloud
    Given I am authenticated with "ADMIN" role
    When I remove a role "CLOUD_DEPLOYER" to user "user" on the resource type "CLOUD" named "middle_earth"
    Then I should receive a RestResponse with no error
    Given I am authenticated with "USER" role
    And I list clouds
    Then Response should contains 0 cloud
    Then I should receive a RestResponse with no error
    Given I am authenticated with "USER" role
    When I list clouds
    Then Response should contains 0 cloud
    Given I am authenticated with "ADMIN" role
    And I add a role "CLOUD_DEPLOYER" to user "user" on the resource type "CLOUD" named "mordor"
    And I add a role "CLOUD_DEPLOYER" to user "user" on the resource type "CLOUD" named "middle_earth"
    Then I should receive a RestResponse with no error
    Given I am authenticated with "USER" role
    And I list clouds
    Then Response should contains 2 cloud
    Then I should receive a RestResponse with no error

  Scenario: Remove user right on cloud when i ve no sufficent rights
    Given I add a role "CLOUD_DEPLOYER" to user "user" on the resource type "CLOUD" named "middle_earth"
    Then I should receive a RestResponse with no error
    When I am authenticated with "USER" role
    And I remove a role "CLOUD_DEPLOYER" to user "user" on the resource type "CLOUD" named "middle_earth"
    Then I should receive a RestResponse with an error code 102
    When I am authenticated with "ADMIN" role
    And I remove a role "CLOUD_DEPLOYER" to user "user" on the resource type "CLOUD" named "middle_earth"
    Then I should receive a RestResponse with no error
