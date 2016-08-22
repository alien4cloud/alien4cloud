Feature: Match location for a deployment configuration

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | frodon |
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I create a location named "Thark location 2" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I create a new application with name "ALIEN" and description "desc" and node templates
      | Compute | tosca.nodes.Compute:1.0.0-SNAPSHOT |

  @reset
  Scenario: Get locations matchings for this topology, using admin account
    When I ask for the locations matching for the current application
    Then I should receive a match result with 2 locations
      | Thark location   |
      | Thark location 2 |

  @reset
  Scenario: Get locations matchings for this topology, using a common account
    Given I am authenticated with user named "frodon"
    When I ask for the locations matching for the current application
    Then I should receive a match result with no locations

    When I authenticate with "ADMIN" role
    And I add a role "DEPLOYER" to user "frodon" on the resource type "LOCATION" named "Thark location"
    Then I should receive a RestResponse with no error

    When I am authenticated with user named "frodon"
    When I ask for the locations matching for the current application
    Then I should receive a match result with 1 locations
      | Thark location |
