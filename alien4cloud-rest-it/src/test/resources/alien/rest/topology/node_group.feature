Feature: Manage Nodetemplates of a topology

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca base types 1.0"
    And I upload the archive "sample java types 1.0"
    And I create a new application with name "watchmiddleearth" and description "Use my great eye to find frodo and the ring."
    And I add a node template "Compute1" related to the "tosca.nodes.Compute:1.0" node type
    And I add a node template "Compute2" related to the "tosca.nodes.Compute:1.0" node type

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