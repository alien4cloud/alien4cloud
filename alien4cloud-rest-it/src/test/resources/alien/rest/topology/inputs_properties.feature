Feature: Topology inputs controller

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca base types 1.0"
    And I should receive a RestResponse with no error
    And There is a "node type" with element name "tosca.nodes.Compute" and archive version "1.0"
    And I create a new application with name "ioMan" and description "Yeo man!"
    And I add a node template "Compute" related to the "tosca.nodes.Compute:1.0" node type
  
  Scenario: Define a property as input
    When I define the property "os_arch" of the node "Compute" as input property
    Then I should receive a RestResponse with no error
    And The topology should have the property "os_arch" defined as input property
    
  Scenario: Remove an input property
    Given I define the property "os_arch" of the node "Compute" as input property
    When I remove the input property "os_arch"
    Then I should receive a RestResponse with no error
    And The topology should not have the property "os_arch" defined as input property
    
  Scenario: Define a property as input with an alreading existing name
    Given I define the property "os_arch" of the node "Compute" as input property
    Then I should receive a RestResponse with no error
    Given I define the property "os_arch" of the node "Compute" as input property
    Then I should receive a RestResponse with an error code 502
    
  Scenario: Associate the property of a node template to an input of the topology
    When I define the property "os_distribution" of the node "Compute" as input property
    Then I should receive a RestResponse with no error
      And The topology should have the property "os_distribution" defined as input property
    Then I associate the property "os_version" of a node template to the input "os_distribution"
      And I should receive a RestResponse with no error
    
  Scenario: Associate the property of a relationship template to an input of the topology
    When I define the property "os_distribution" of the node "Compute" as input property
    Then I should receive a RestResponse with no error
      And The topology should have the property "os_distribution" defined as input property
    Then I associate the property "os_version" of a relationship to the input "os_distribution"
      And I should receive a RestResponse with no error
 