Feature: Add / Remove / Edit scaling policy

  Background:
    Given I am authenticated with "APPLICATIONS_MANAGER" role
    And I create a new application with name "scaled" and description "Pump it up." without errors
    And I upload the archive "tosca-normative-types"
    And I should receive a RestResponse with no error
    And I have added a node template "Compute" related to the "tosca.nodes.Compute:1.0.0.wd03-SNAPSHOT" node type

  Scenario: Edit a scaling policy
    When I update the node template "Compute"'s capability "scalable" of type "tosca.capabilities.Scalable"'s property "max_instances" to "4"
    Then I should receive a RestResponse with no error
    When I update the node template "Compute"'s capability "scalable" of type "tosca.capabilities.Scalable"'s property "default_instances" to "3"
    Then I should receive a RestResponse with no error
    When I update the node template "Compute"'s capability "scalable" of type "tosca.capabilities.Scalable"'s property "min_instances" to "2"
    Then I should receive a RestResponse with no error
    And the scaling policy of the node "Compute" should match max instances equals to 4, initial instances equals to 3 and min instances equals to 2

    When I update the node template "Compute"'s capability "scalable" of type "tosca.capabilities.Scalable"'s property "max_instances" to "-1"
    And I check for the deployable status of the topology
    Then the topology should not be deployable

    When I update the node template "Compute"'s capability "scalable" of type "tosca.capabilities.Scalable"'s property "max_instances" to "1"
    Then I should receive a RestResponse with no error
    When I update the node template "Compute"'s capability "scalable" of type "tosca.capabilities.Scalable"'s property "default_instances" to "1"
    Then I should receive a RestResponse with no error
    When I update the node template "Compute"'s capability "scalable" of type "tosca.capabilities.Scalable"'s property "min_instances" to "1"
    Then I should receive a RestResponse with no error
    And There's no defined scaling policy for the node "Compute"
