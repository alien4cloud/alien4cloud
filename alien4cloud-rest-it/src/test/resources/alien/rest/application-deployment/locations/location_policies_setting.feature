Feature: Set location policies

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | frodon |
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I create an orchestrator named "Isengard orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I enable the orchestrator "Isengard orchestrator"
    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I create a location named "Thark location 2" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I create a location named "Saruman location" and infrastructure type "OpenStack" to the orchestrator "Isengard orchestrator"
    And I create a new application with name "ALIEN" and description "desc" and node templates
      | Compute | tosca.nodes.Compute:1.0.0-SNAPSHOT |
      | Compute_2 | tosca.nodes.Compute:1.0.0-SNAPSHOT |

  @reset
  Scenario: Set location policy for all groups in the topology
    Given I grant access to the resource type "LOCATION" named "Thark location" to the user "frodon"
    And I get the deployment topology for the current application
    When I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes
    Then I should receive a RestResponse with no error
    And the deployment topology shoud have the following location policies
      | Compute_Group | Mount doom orchestrator | Thark location |
      | Compute_2_Group | Mount doom orchestrator | Thark location |

  @reset
  Scenario: Set several location policies on the same orchestrator
    Given I grant access to the resource type "LOCATION" named "Thark location" to the user "frodon"
    And I grant access to the resource type "LOCATION" named "Thark location 2" to the user "frodon"
    And I get the deployment topology for the current application
    When I Set the following location policies with orchestrator "Mount doom orchestrator" for groups
      | Compute_Group | Mount doom orchestrator | Thark location |
      | Compute_2_Group | Mount doom orchestrator | Thark location 2 |
    Then I should receive a RestResponse with no error
    And the deployment topology shoud have the following location policies
      | Compute_Group | Mount doom orchestrator | Thark location |
      | Compute_2_Group | Mount doom orchestrator | Thark location 2 |

  @reset
  Scenario: Setting location policies to different orchestrators should fail
    Given I grant access to the resource type "LOCATION" named "Thark location" to the user "frodon"
    And I grant access to the resource type "LOCATION" named "Saruman location" to the user "frodon"
    And I get the deployment topology for the current application
    When I Set the following location policies with orchestrator "Mount doom orchestrator" for groups
      | Compute_Group | Mount doom orchestrator | Thark location |
      | Compute_2_Group | Isengard orchestrator | Saruman location |
    Then I should receive a RestResponse with an error code 500 and a message containing "Location Saruman location doesn't belong to the given orchestrator"
