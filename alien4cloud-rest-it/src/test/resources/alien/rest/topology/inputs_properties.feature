Feature: Topology inputs controller

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca base types 1.0"
    And I should receive a RestResponse with no error
    And There is a "node type" with element name "tosca.nodes.Compute" and archive version "1.0"
    And I create a new application with name "ioMan" and description "Yeo man!"
    And I add a node template "Compute" related to the "tosca.nodes.Compute:1.0" node type
    And I add a node template "BlockStorage" related to the "tosca.nodes.BlockStorage:1.0" node type
    And I add a node template "BlockStorage_2" related to the "tosca.nodes.BlockStorage:1.0" node type
    And I add a relationship of type "tosca.relationships.HostedOn" defined in archive " tosca-base-types" version "1.0" with source "BlockStorage" and target "Compute" for requirement "attach" of type "tosca.capabilities.Container" and target capability "compute"
    And I add a relationship of type "tosca.relationships.HostedOn" defined in archive " tosca-base-types" version "1.0" with source "BlockStorage_2" and target "Compute" for requirement "attach" of type "tosca.capabilities.Container" and target capability "compute"

  Scenario: Define a property as input
    When I define the property "os_arch" of the node "Compute" as input property
    Then I should receive a RestResponse with no error
    And The topology should have the property "os_arch" defined as input property

  Scenario: Define a property as input when the input already exist should failed
    When I define the property "os_arch" of the node "Compute" as input property
    Then I should receive a RestResponse with no error
    And The topology should have the property "os_arch" defined as input property
    When I define the property "os_arch" of the node "Compute" as input property
    Then I should receive a RestResponse with an error code 502

  Scenario: Remove an input property
    Given I define the property "os_arch" of the node "Compute" as input property
    When I remove the input property "os_arch"
    Then I should receive a RestResponse with no error
    And The topology should not have the property "os_arch" defined as input property

  Scenario: Remove an non existing input property should failed
    Given I define the property "os_arch" of the node "Compute" as input property
    When I remove the input property "os_arch_should-failed"
    Then I should receive a RestResponse with an error code 504

  Scenario: Define a property as input with an alreading existing name should failed
    Given I define the property "os_arch" of the node "Compute" as input property
    Then I should receive a RestResponse with no error
    Given I define the property "os_arch" of the node "Compute" as input property
    Then I should receive a RestResponse with an error code 502

  Scenario: Rename a property input
    When I define the property "os_arch" of the node "Compute" as input property
    Then I should receive a RestResponse with no error
    And The topology should have the property "os_arch" defined as input property
    When I rename the property "os_arch" to "os_arch_new_name"
    Then I should receive a RestResponse with no error

  Scenario: Rename property input to an already existing name hould failed
    When I define the property "os_arch" of the node "Compute" as input property
    Then I should receive a RestResponse with no error
    And The topology should have the property "os_arch" defined as input property
    When I rename the property "os_arch" to "os_arch"
    Then I should receive a RestResponse with an error code 502

  Scenario: Rename a non existing property input should failed
    When I define the property "os_arch" of the node "Compute" as input property
    Then I should receive a RestResponse with no error
    And The topology should have the property "os_arch" defined as input property
    When I rename the property "os_arch_should_failed" to "os_arch_new_name"
    Then I should receive a RestResponse with an error code 504

  Scenario: Associate the property of a node template to an input of the topology
    When I define the property "os_distribution" of the node "Compute" as input property
    Then I should receive a RestResponse with no error
      And The topology should have the property "os_distribution" defined as input property
    Then I associate the property "os_version" of a node template "Compute" to the input "os_distribution"
      And I should receive a RestResponse with no error

  Scenario: Unset the property of a node template to an input of the topology
    When I define the property "os_distribution" of the node "Compute" as input property
    Then I should receive a RestResponse with no error
      And The topology should have the property "os_distribution" defined as input property
    When I unset the property "os_distribution" of the node "Compute" as input property
      And I should receive a RestResponse with no error

  Scenario: Unset the non existing property of a node template to an input of the topology should failed
    When I unset the property "os_distribution" of the node "Compute" as input property
      And I should receive a RestResponse with an error code 504

###
# Tests on the relationships properties inputs
###
 Scenario: Set the property of a relationship template to an input of the topology
    Given I define the property "fake_password" of the node "BlockStorage" as input property
    Then I set the property "fake_password" of a relationship "HostedOn_Compute" for the node template "BlockStorage_2" to the input "fake_password"
    Then I should receive a RestResponse with no error

 Scenario: Set the property of a relationship template to an input of the topology with different constraints must fail
    Given I define the property "os_distribution" of the node "Compute" as input property
    Then I should receive a RestResponse with no error
      And The topology should have the property "os_distribution" defined as input property
    Then I set the property "password" of a relationship "HostedOn_Compute" for the node template "BlockStorage" to the input "os_distribution"
      And I should receive a RestResponse with an error code 500

 Scenario: Set the property of a relationship template to an non existing input of the topology should failed
    Given I define the property "os_arch" of the node "Compute" as input property
    Then I set the property "password" of a relationship "HostedOn_Compute" for the node template "BlockStorage" to the input "os_distribution_should_failed"
    Then I should receive a RestResponse with an error code 504

 Scenario: Unset the property of a relationship template to an input of the topology
    Given I define the property "fake_password" of the node "BlockStorage" as input property
    Then I set the property "fake_password" of a relationship "HostedOn_Compute" for the node template "BlockStorage_2" to the input "fake_password"
      Then I should receive a RestResponse with no error
	Then I unset the property "fake_password" of a relationship "HostedOn_Compute" for the node template "BlockStorage_2"
	  Then I should receive a RestResponse with no error

 Scenario: Unset the non existing property of a relationship template to an input of the topology should failed
    Given I unset the property "fake_password_should_failed" of a relationship "HostedOn_Compute" for the node template "BlockStorage_2"
      Then I should receive a RestResponse with an error code 504

###
# Tests on the capabilities properties inputs
###
 Scenario: Set the property of a capability template to an input of the topology
    Given I define the property "os_arch" of the node "Compute" as input property
    When I set the property "containee_types" of capability "compute" the node "Compute" as input property name "os_arch"
      Then I should receive a RestResponse with no error

 Scenario: Set the property of a capability template to an input of the topology with different constraints must fail
    Given I define the property "os_arch" of the node "Compute" as input int property
    When I set the property "containee_types" of capability "compute" the node "Compute" as input property name "os_arch"
      Then I should receive a RestResponse with an error code 500

 Scenario: Set the property of a capability template to an non existing input of the topology should failed
    Given I define the property "os_arch" of the node "Compute" as input property
    When I set the property "containee_types" of capability "compute" the node "Compute" as input property name "os_arch2"
      Then I should receive a RestResponse with an error code 504

 Scenario: Unset the property of a capability template to an input of the topology
    Given I define the property "os_arch" of the node "Compute" as input property
    When I set the property "containee_types" of capability "compute" the node "Compute" as input property name "os_arch"
    When I unset the property "containee_types" of capability "compute" the node "Compute" as input property
    Then I should receive a RestResponse with no error

 Scenario: Unset the non existing property of a capability template to an input of the topology should failed
    Given I unset the property "containee_types_should_failed" of capability "compute" the node "Compute" as input property
      Then I should receive a RestResponse with an error code 504

###
# Tests the input candidates method
###
  Scenario: The input candidates are well managed
    Given I add a node template "Compute2" related to the "tosca.nodes.Compute:1.0" node type
    When I ask for the input candidate for the node template "Compute2" and property "os_distribution"
      Then The SPEL boolean expression "#root.size() == 0" should return true
    Given I define the property "os_distribution" of the node "Compute" as input property
    When I ask for the input candidate for the node template "Compute2" and property "os_distribution"
      Then The SPEL boolean expression "#root.size() == 1" should return true
      And The SPEL expression "#root[0]" should return "os_distribution"
    Given I define the property "mem_size" of the node "Compute" as input int property
    When I ask for the input candidate for the node template "Compute2" and property "os_distribution"
      Then The SPEL boolean expression "#root.size() == 1" should return true
      And The SPEL expression "#root[0]" should return "os_distribution"

  Scenario: The relationship input candidates are well managed
    Given I ask for the input candidate for the node template "BlockStorage" and property "fake_password" of relationship "HostedOn_Compute"
      Then The SPEL boolean expression "#root.size() == 0" should return true
    And I define the property "fake_password" of the node "BlockStorage_2" as input property
    When I ask for the input candidate for the node template "BlockStorage" and property "fake_password" of relationship "HostedOn_Compute"
      Then The SPEL boolean expression "#root.size() == 1" should return true

  Scenario: The capability input candidates are well managed
    Given I add a node template "Compute2" related to the "tosca.nodes.Compute:1.0" node type
    When I ask for the input candidate for the node template "Compute" and property "containee_types" of capability "compute"
      Then The SPEL boolean expression "#root.size() == 0" should return true
    And I define the property "containee_types" of the node "Compute" as input property
    When I ask for the input candidate for the node template "Compute" and property "containee_types" of capability "compute"
      Then The SPEL boolean expression "#root.size() == 1" should return true