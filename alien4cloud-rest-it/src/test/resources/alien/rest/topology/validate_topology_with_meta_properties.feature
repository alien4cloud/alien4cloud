Feature: Check if topology with properties defined as as input on cloud meta is deployable

Background:
  Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca base types 1.0"
    And I should receive a RestResponse with no error
    And There is a "node type" with element name "tosca.nodes.Compute" and archive version "1.0"
    And I create a new application with name "ioMan" and description "Yeo man!"
    And I add a node template "Compute" related to the "tosca.nodes.Compute:1.0" node type
    And I add a node template "BlockStorage" related to the "tosca.nodes.BlockStorage:1.0" node type
    And I add a node template "BlockStorage-2" related to the "tosca.nodes.BlockStorage:1.0" node type
    And I add a relationship of type "tosca.relationships.HostedOn" defined in archive " tosca-base-types" version "1.0" with source "BlockStorage" and target "Compute" for requirement "attach" of type "tosca.capabilities.Container" and target capability "compute"
    And I add a relationship of type "tosca.relationships.HostedOn" defined in archive " tosca-base-types" version "1.0" with source "BlockStorage-2" and target "Compute" for requirement "attach" of type "tosca.capabilities.Container" and target capability "compute"
  When I load several configuration tags
    Then I should have 10 configuration tags loaded

Scenario: Define a property as input
  When I define the property "os_arch" of the node "Compute" as input property
    Then I should receive a RestResponse with no error
    And The topology should have the property "os_arch" defined as input property
  When I check for the deployable status of the topology
    Then the topology should not be deployable
  When I rename the property "os_arch" to "cloud_meta_osARCH"
    Then I should receive a RestResponse with no error
#  When I check for the deployable status of the topology
#   Then the topology should be deployable
