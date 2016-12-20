Feature: Manage user's authorizations on an orchestrator

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | frodon |
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "middle_earth" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"

  @reset
  Scenario: Add / Remove rights to a user on an orchestrator with ADMIN role
    Given I add a role "DEPLOYER" to user "frodon" on the resource type "ORCHESTRATOR" named "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
    When I get the orchestrator named "Mount doom orchestrator"
    Then The SPEL boolean expression "authorizedUsers.size() == 1 and authorizedUsers[0] == 'frodon'" should return true
    #check the role was added on the location also
    When I get the location "Mount doom orchestrator"/"middle_earth"
    Then The SPEL boolean expression "location.userRoles['frodon'].size() == 1 and location.userRoles['frodon'][0] == 'DEPLOYER'" should return true
    # removal
    When I remove a role "DEPLOYER" to user "frodon" on the resource type "ORCHESTRATOR" named "middle_earth"
    Then I should receive a RestResponse with no error
    When I get the orchestrator named "Mount doom orchestrator"
    Then The SPEL int expression "authorizedUsers.size()" should return 0
    #check removal on the location also
    When I get the location "Mount doom orchestrator"/"middle_earth"
    Then The SPEL expression "location.userRoles" should return "null"

  @reset
  Scenario: Remove user right on location when i have no sufficent rights
    Given I add a role "DEPLOYER" to user "frodon" on the resource type "ORCHESTRATOR" named "middle_earth"
    Then I should receive a RestResponse with no error
    When I am authenticated with "USER" role
    And I remove a role "DEPLOYER" to user "frodon" on the resource type "ORCHESTRATOR" named "middle_earth"
    Then I should receive a RestResponse with an error code 102
    When I am authenticated with "ADMIN" role
    And I remove a role "DEPLOYER" to user "frodon" on the resource type "ORCHESTRATOR" named "middle_earth"
    Then I should receive a RestResponse with no error

  ##ALIEN-1894
  @reset
  Scenario: Deleting a user from alien should also remove him from the orchestrator's authorized users
    Given I add a role "DEPLOYER" to user "frodon" on the resource type "ORCHESTRATOR" named "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
    When I get the orchestrator named "Mount doom orchestrator"
    Then The SPEL boolean expression "authorizedUsers.size() == 1 and authorizedUsers[0] == 'frodon'" should return true
    #check the role was added on the location also
    When I get the location "Mount doom orchestrator"/"middle_earth"
    Then The SPEL boolean expression "location.userRoles['frodon'].size() == 1 and location.userRoles['frodon'][0] == 'DEPLOYER'" should return true
    # deleting the user
    When I delete the user "frodon"
    Then I should receive a RestResponse with no error
    When I get the orchestrator named "Mount doom orchestrator"
    Then The SPEL int expression "authorizedUsers.size()" should return 0
    #check removal on the location also
    When I get the location "Mount doom orchestrator"/"middle_earth"
    Then The SPEL expression "location.userRoles" should return "null"
