Feature: Manage group's authorizations on location

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | frodon |
    And There is a "lordOfRing" group in the system
    And There is a "hobbits" group in the system
    And I add the user "frodon" to the group "lordOfRing"
    And I add the user "frodon" to the group "hobbits"
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "middle_earth" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"

  @reset
  Scenario: Add / Remove rights to a group on a location with ADMIN role
    Given I add a role "DEPLOYER" to group "lordOfRing" on the resource type "LOCATION" named "middle_earth"
    Then I should receive a RestResponse with no error
    When I get the location "Mount doom orchestrator"/"middle_earth"
    Then The SPEL int expression "location.groupRoles.size()" should return 1
    When I remove a role "DEPLOYER" to group "lordOfRing" on the resource type "LOCATION" named "middle_earth"
    Then I should receive a RestResponse with no error
    When I get the location "Mount doom orchestrator"/"middle_earth"
    Then The SPEL expression "location.groupRoles" should return "null"


  @reset
  Scenario: Remove group right on location when i ve no sufficent rights
    Given I add a role "DEPLOYER" to group "lordOfRing" on the resource type "LOCATION" named "middle_earth"
    Then I should receive a RestResponse with no error
    When I am authenticated with "USER" role
    And I remove a role "DEPLOYER" to group "lordOfRing" on the resource type "LOCATION" named "middle_earth"
    Then I should receive a RestResponse with an error code 102
    When I am authenticated with "ADMIN" role
    And I remove a role "DEPLOYER" to group "lordOfRing" on the resource type "LOCATION" named "middle_earth"
    Then I should receive a RestResponse with no error
