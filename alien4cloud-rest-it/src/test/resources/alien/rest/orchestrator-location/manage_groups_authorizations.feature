Feature: Manage group's authorizations on location

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | frodon |
      | sam    |
    And There is a "lordOfRing" group in the system
    And There is a "hobbits" group in the system
    And I add the user "frodon" to the group "lordOfRing"
    And I add the user "sam" to the group "hobbits"
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "middle_earth" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"

  @reset
  Scenario: Add / Remove rights to a group on a location
    Given I grant access to the resource type "LOCATION" named "middle_earth" to the group "lordOfRing"
    Then I should have following list of groups:
      | lordOfRing |
    When I get the authorised groups for the resource type "LOCATION" named "middle_earth"
    Then I should have following list of groups:
      | lordOfRing |
    Given I grant access to the resource type "LOCATION" named "middle_earth" to the group "hobbits"
    When I get the authorised groups for the resource type "LOCATION" named "middle_earth"
    Then I should have following list of groups:
      | lordOfRing |
      | hobbits    |
    Given I revoke access to the resource type "LOCATION" named "middle_earth" from the group "lordOfRing"
    Then I should have following list of groups:
      | hobbits |
    When I get the authorised groups for the resource type "LOCATION" named "middle_earth"
    Then I should have following list of groups:
      | hobbits |
    Given I revoke access to the resource type "LOCATION" named "middle_earth" from the user "frodon"
    Then I should not have any authorized groups