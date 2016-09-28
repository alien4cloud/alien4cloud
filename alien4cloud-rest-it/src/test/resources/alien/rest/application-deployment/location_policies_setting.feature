Feature: Set location policies

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
  Scenario: Set location policy for all groups in the topology
    Given I add a role "DEPLOYER" to user "frodon" on the resource type "LOCATION" named "Thark location"
#		When I Set the following location policies with orchestrator "Mount doom orchestrator" for groups
#			| _A4C_ALL | Thark location |
    When I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes
    Then I should receive a RestResponse with no error
    And the deployment topology shoud have the following location policies
      | _A4C_ALL | Mount doom orchestrator | Thark location |

  @reset
  Scenario: Setting a location policy for a group other than _A4C_ALL should fail
    Given I add a role "DEPLOYER" to user "frodon" on the resource type "LOCATION" named "Thark location"
    When I Set the following location policies with orchestrator "Mount doom orchestrator" for groups
      | TEST_GROUP | Thark location |
    Then I should receive a RestResponse with an error code 500

  @reset
  Scenario: Setting location policy for more than one group should fail
    Given I add a role "DEPLOYER" to user "frodon" on the resource type "LOCATION" named "Thark location"
    When I Set the following location policies with orchestrator "Mount doom orchestrator" for groups
      | HAHAHA  | Thark location |
      | HOHOHOH | Thark location |
    Then I should receive a RestResponse with an error code 509
