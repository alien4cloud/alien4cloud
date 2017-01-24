Feature: Manage Nodetemplates of a topology

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca base types 1.0"
    And I upload the archive "sample java types 1.0"
    And I create an application with name "watchmiddleearth", archive name "watchmiddleearth", description "Use my great eye to find frodo and the ring." and topology template id "null"
    And I add a node template "Compute1" related to the "tosca.nodes.Compute:1.0" node type
    And I add a node template "Compute2" related to the "tosca.nodes.Compute:1.0" node type

  @reset
  Scenario: Add/Remove group and group member
    When I add the node "Compute1" to the group "HA_group"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain a group named "HA_group" whose members are "Compute1" and policy is "tosca.policy.ha"
    When I add the node "Compute2" to the group "HA_group"
    Then The RestResponse should contain a group named "HA_group" whose members are "Compute1,Compute2" and policy is "tosca.policy.ha"
    When I update the name of the group "HA_group" to "Completely_new_HA_group"
    Then The RestResponse should contain a group named "Completely_new_HA_group" whose members are "Compute1,Compute2" and policy is "tosca.policy.ha"
    When I remove the node "Compute1" from the group "Completely_new_HA_group"
    Then The RestResponse should contain a group named "Completely_new_HA_group" whose members are "Compute2" and policy is "tosca.policy.ha"
    When I remove the group "Completely_new_HA_group"
    Then The RestResponse should not contain any group

  @reset
  Scenario: Remove node template
    Given I add the node "Compute1" to the group "HA_group"
    And I add the node "Compute2" to the group "HA_group"
    When I delete a node template "Compute1" from the topology
    Then I should receive a RestResponse with no error
    And The RestResponse should contain a group named "HA_group" whose members are "Compute2" and policy is "tosca.policy.ha"

  @reset
  Scenario: Rename node template
    Given I add the node "Compute1" to the group "HA_group"
    And I add the node "Compute2" to the group "HA_group"
    When I update the node template's name from "Compute1" to "Compute1bis"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain a group named "HA_group" whose members are "Compute1bis,Compute2" and policy is "tosca.policy.ha"
