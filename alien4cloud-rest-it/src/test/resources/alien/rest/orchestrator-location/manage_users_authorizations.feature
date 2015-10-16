Feature: Manage user's authorizations on an location

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | frodon |
    And I upload the archive "tosca-normative-types-wd06"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "middle_earth" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"

  Scenario: Add / Remove rights to a user on a location with ADMIN role
    Given I add a role "DEPLOYER" to user "frodon" on the resource type "LOCATION" named "middle_earth"
    Then I should receive a RestResponse with no error
    When I remove a role "DEPLOYER" to user "frodon" on the resource type "LOCATION" named "middle_earth"
    Then I should receive a RestResponse with no error

  Scenario: Search / List locations when the user has sufficient right
    Given I create an orchestrator named "mordor" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"
    And I add a role "DEPLOYER" to user "frodon" on the resource type "LOCATION" named "middle_earth"
    And I am authenticated with "USER" role
    When I list locations of the orchestrator "Mount doom orchestrator"
    And Response should contains 1 location
    Given I am authenticated with "ADMIN" role
    When I remove a role "DEPLOYER" to user "frodon" on the resource type "LOCATION" named "middle_earth"
    Then I should receive a RestResponse with no error
    Given I am authenticated with "USER" role
    And I list locations of the orchestrator "Mount doom orchestrator"
    Then Response should contains 0 location
    Then I should receive a RestResponse with no error
    Given I am authenticated with "USER" role
    When I list locations of the orchestrator "Mount doom orchestrator"
    Then Response should contains 0 location
    Given I am authenticated with "ADMIN" role
    And I add a role "DEPLOYER" to user "frodon" on the resource type "LOCATION" named "mordor"
    And I add a role "DEPLOYER" to user "frodon" on the resource type "LOCATION" named "middle_earth"
    Then I should receive a RestResponse with no error
    Given I am authenticated with "USER" role
    And I list locations of the orchestrator "Mount doom orchestrator"
    Then Response should contains 2 location
    Then I should receive a RestResponse with no error

  Scenario: Remove user right on location when i ve no sufficent rights
    Given I add a role "DEPLOYER" to user "frodon" on the resource type "LOCATION" named "middle_earth"
    Then I should receive a RestResponse with no error
    When I am authenticated with "USER" role
    And I remove a role "DEPLOYER" to user "frodon" on the resource type "LOCATION" named "middle_earth"
    Then I should receive a RestResponse with an error code 102
    When I am authenticated with "ADMIN" role
    And I remove a role "DEPLOYER" to user "frodon" on the resource type "LOCATION" named "middle_earth"
    Then I should receive a RestResponse with no error
