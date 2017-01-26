Feature: Manage user's authorizations on an location

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | frodon |
      | sam    |
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "middle_earth" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"

  @reset
  Scenario: Add / Remove rights to a user on a location
    Given I grant access to the resource type "LOCATION" named "middle_earth" to the user "frodon"
    Then I should have following list of users:
      | frodon |
    When I get the authorised users for the resource type "LOCATION" named "middle_earth"
    Then I should have following list of users:
      | frodon |
    Given I grant access to the resource type "LOCATION" named "middle_earth" to the user "sam"
    Then I should have following list of users:
      | frodon |
      | sam    |
    When I get the authorised users for the resource type "LOCATION" named "middle_earth"
    Then I should have following list of users:
      | frodon |
      | sam    |
    Given I revoke access to the resource type "LOCATION" named "middle_earth" from the user "sam"
    When I get the authorised users for the resource type "LOCATION" named "middle_earth"
    Then I should have following list of users:
      | frodon |
    Given I revoke access to the resource type "LOCATION" named "middle_earth" from the user "frodon"
    Then I should not have any authorized users