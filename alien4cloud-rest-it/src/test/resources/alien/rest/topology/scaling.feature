Feature: Add / Remove / Edit scaling policy

  Background:
    Given I am authenticated with "APPLICATIONS_MANAGER" role
    And I create an application with name "scaled", archive name "scaled", description "Pump it up." and topology template id "null"
    And I should receive a RestResponse with no error
    And I upload the archive "tosca-normative-types"
    And I should receive a RestResponse with no error
    And I have added a node template "Compute" related to the "tosca.nodes.Compute:1.0.0.wd03-SNAPSHOT" node type
    And I update the node template "Compute"'s property "os_arch" to "x86_64"
    And I update the node template "Compute"'s property "os_type" to "linux"

  @reset
  Scenario: Edit a scaling policy
    When I update the node template "Compute"'s capability "scalable" of type "tosca.capabilities.Scalable"'s property "max_instances" to "4"
    Then I should receive a RestResponse with no error
    When I update the node template "Compute"'s capability "scalable" of type "tosca.capabilities.Scalable"'s property "default_instances" to "3"
    Then I should receive a RestResponse with no error
    When I update the node template "Compute"'s capability "scalable" of type "tosca.capabilities.Scalable"'s property "min_instances" to "2"
    Then I should receive a RestResponse with no error
    And the scaling policy of the node "Compute" should match max instances equals to 4, initial instances equals to 3 and min instances equals to 2

    When I update the node template "Compute"'s capability "scalable" of type "tosca.capabilities.Scalable"'s property "max_instances" to "-1"
    And I check for the valid status of the topology
    Then the topology should not be valid
    And The topology should have scalability policy error concerning "max_instances"
    When I update the node template "Compute"'s capability "scalable" of type "tosca.capabilities.Scalable"'s property "max_instances" to "3"
    And I check for the valid status of the topology
    Then the topology should be valid
    When I update the node template "Compute"'s capability "scalable" of type "tosca.capabilities.Scalable"'s property "min_instances" to "4"
    And I check for the valid status of the topology
    Then the topology should not be valid
    And The topology should have scalability policy error concerning "max_instances"
    And The topology should have scalability policy error concerning "min_instances"
    And The topology should have scalability policy error concerning "default_instances"
    When I update the node template "Compute"'s capability "scalable" of type "tosca.capabilities.Scalable"'s property "min_instances" to "1"
    And I check for the valid status of the topology
    Then the topology should be valid
    When I update the node template "Compute"'s capability "scalable" of type "tosca.capabilities.Scalable"'s property "default_instances" to "2"
    And I check for the valid status of the topology
    Then the topology should be valid
    When I update the node template "Compute"'s capability "scalable" of type "tosca.capabilities.Scalable"'s property "default_instances" to "6"
    And I check for the valid status of the topology
    Then the topology should not be valid
    And The topology should have scalability policy error concerning "max_instances"
    And The topology should have scalability policy error concerning "default_instances"
    When I update the node template "Compute"'s capability "scalable" of type "tosca.capabilities.Scalable"'s property "default_instances" to "2"
    And I check for the valid status of the topology
    Then the topology should be valid

    When I update the node template "Compute"'s capability "scalable" of type "tosca.capabilities.Scalable"'s property "max_instances" to "1"
    Then I should receive a RestResponse with no error
    When I update the node template "Compute"'s capability "scalable" of type "tosca.capabilities.Scalable"'s property "default_instances" to "1"
    Then I should receive a RestResponse with no error
    When I update the node template "Compute"'s capability "scalable" of type "tosca.capabilities.Scalable"'s property "min_instances" to "1"
    Then I should receive a RestResponse with no error
    And There's no defined scaling policy for the node "Compute"
