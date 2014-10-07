Feature: Manage group's authorizations on cloud

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | user |
    And There is a "lordOfRing" group in the system
    And There is a "hobbits" group in the system
    And I add the user "user" to the group "lordOfRing"
    And I add the user "user" to the group "hobbits"
    And I upload a plugin
    And I create a cloud with name "middle_earth" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
    And I enable "middle_earth"

  Scenario: Add / Remove rights to a group on a cloud with ADMIN role
    Given I add a role "CLOUD_DEPLOYER" to group "lordOfRing" on the resource type "CLOUD" named "middle_earth"
    Then I should receive a RestResponse with no error
    When I remove a role "CLOUD_DEPLOYER" to group "lordOfRing" on the resource type "CLOUD" named "middle_earth"
    Then I should receive a RestResponse with no error

  Scenario: Search / List clouds when the user belongs to a group has sufficient right
    Given I add a role "CLOUD_DEPLOYER" to group "lordOfRing" on the resource type "CLOUD" named "middle_earth"
    When I am authenticated with "USER" role
    And I list clouds
    Then Response should contains 1 cloud
    Given I am authenticated with "ADMIN" role
    When I remove a role "CLOUD_DEPLOYER" to group "lordOfRing" on the resource type "CLOUD" named "middle_earth"
    Then I should receive a RestResponse with no error
    Given I am authenticated with "USER" role
    And I list clouds
    Then Response should contains 0 cloud
    Then I should receive a RestResponse with no error
    Given I am authenticated with "ADMIN" role
    And I add a role "CLOUD_DEPLOYER" to group "hobbits" on the resource type "CLOUD" named "middle_earth"
    And I create a cloud with name "middle_earth_second" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
    And I add a role "CLOUD_DEPLOYER" to group "lordOfRing" on the resource type "CLOUD" named "middle_earth_second"
    Then I should receive a RestResponse with no error
    Given I am authenticated with "USER" role
    When I list clouds
    Then Response should contains 2 cloud
    Then I should receive a RestResponse with no error

  Scenario: Remove group right on cloud when i ve no sufficent rights
    Given I add a role "CLOUD_DEPLOYER" to group "lordOfRing" on the resource type "CLOUD" named "middle_earth"
    Then I should receive a RestResponse with no error
    When I am authenticated with "USER" role
    And I remove a role "CLOUD_DEPLOYER" to group "lordOfRing" on the resource type "CLOUD" named "middle_earth"
    Then I should receive a RestResponse with an error code 102
    When I am authenticated with "ADMIN" role
    And I remove a role "CLOUD_DEPLOYER" to group "lordOfRing" on the resource type "CLOUD" named "middle_earth"
    Then I should receive a RestResponse with no error
