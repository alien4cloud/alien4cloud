Feature: Set/Remove Out properties

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca base types 1.0"
    And I should receive a RestResponse with no error
    And I upload the archive "sample java types 1.0"
    And I should receive a RestResponse with no error
    And There is a "node type" with element name "tosca.nodes.Compute" and archive version "1.0"
    And There is a "node type" with element name "fastconnect.nodes.JavaChef" and archive version "1.0"
    And I create an application with name "ioMan", archive name "ioMan", description "Yeo man!" and topology template id "null"
    And I add a node template "Compute" related to the "tosca.nodes.Compute:1.0" node type
    And I add a node template "Java" related to the "fastconnect.nodes.JavaChef:1.0" node type

  @reset
  Scenario: Define a property as output
    When I define the property "os_arch" of the node "Compute" as output property
    Then I should receive a RestResponse with no error
    And The topology should have the property "os_arch" of the node "Compute" defined as output property

  @reset
  Scenario: Remove an output property
    Given I define the property "os_arch" of the node "Compute" as output property
    When I define the property "os_arch" of the node "Compute" as non output property
    Then I should receive a RestResponse with no error
    And The topology should not have the property "os_arch" of the node "Compute" defined as output property

  @reset
  Scenario: Define an attribute as output
    When I define the attribute "ip_address" of the node "Compute" as output attribute
    Then I should receive a RestResponse with no error
    And The topology should have the attribute "ip_address" of the node "Compute" defined as output attribute

  @reset
  Scenario: Define an non existing attribute as output
    When I define the attribute "public_ip_address" of the node "Compute" as output attribute
    Then I should receive a RestResponse with an error code 802
    When I define the attribute "private_ip_address" of the node "Compute" as output attribute
    Then I should receive a RestResponse with an error code 802

  @reset
  Scenario: Remove an attribute as output
    Given I define the attribute "ip_address" of the node "Compute" as output attribute
    When I remove the attribute "ip_address" of the node "Compute" from the output attributes
    Then I should receive a RestResponse with no error
    And The topology should not have the attribute "ip_address" of the node "Compute" defined as output attribute

  @reset
  Scenario: Define an capability property as output
    When I define the property "containee_types" of the capability "compute" of the node "Compute" as output property
    Then I should receive a RestResponse with no error
    And The topology should have the capability property "containee_types" of the capability "compute" for the node "Compute" defined as output property

  @reset
  Scenario: Remove an capability output property
    When I define the property "containee_types" of the capability "compute" of the node "Compute" as output property
    When I define the property "containee_types" of the capability "compute" of the node "Compute" as non output property
    Then I should receive a RestResponse with no error
    And The topology should not have the capability property "containee_types" of the capability "compute" for the node "Compute" defined as output property

  @reset
  Scenario: Define an non existing capability property as output should failed
    When I define the property "containee_types_should-failed" of the capability "compute" of the node "Compute" as output property
    Then I should receive a RestResponse with an error code 504

  @reset
  Scenario: Define an existing property of non existing capability as output should failed
    When I define the property "containee_types" of the capability "compute_should_failed" of the node "Compute" as output property
    Then I should receive a RestResponse with an error code 504
