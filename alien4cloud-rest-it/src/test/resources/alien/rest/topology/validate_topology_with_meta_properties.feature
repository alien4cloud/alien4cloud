Feature: Check if topology with properties defined as as input on location meta is valid

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I upload the archive "tosca base types 1.0"
    And I should receive a RestResponse with no error
    And There is a "node type" with element name "tosca.nodes.Compute" and archive version "1.0"
    And I create a new application with name "App-with-meta-location" and description "An application with meta properties and inputs on location..."
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    And I save the topology
    When I load several configuration tags
    Then I should have 11 configuration tags loaded

  @reset
  Scenario: Use location metas to define a property on a node and check deployability
    Given I have the tag "NUMcpus" registered for "location"
    And I have the tag "osARCH" registered for "location"
    And I set the value "x86_64" to the location meta-property "osARCH" of the location "Thark location" of the orchestrator "Mount doom orchestrator"
    And I set the value "2" to the location meta-property "NUMcpus" of the location "Thark location" of the orchestrator "Mount doom orchestrator"
    When I define the property "os_arch" of the node "Compute" as input property
    Then I should receive a RestResponse with no error
    And The topology should have the property "os_arch" defined as input property
    When I define the property "os_type" of the node "Compute" as input property
    Then I should receive a RestResponse with no error
    And The topology should have the property "os_type" defined as input property
    When I check for the valid status of the topology
    Then the topology should not be valid

    When I associate the property "os_arch" of a node template "Compute" to the input "os_arch"
    Then I should receive a RestResponse with no error
    When I associate the property "os_type" of a node template "Compute" to the input "os_type"
    Then I should receive a RestResponse with no error
    When I check for the valid status of the topology
    Then the topology should not be valid

    When I rename the input "os_arch" to "location_meta_osARCH"
    And I rename the input "os_type" to "location_meta_LOCATION_META_1"
    When I check for the valid status of the topology
    Then the topology should not be valid
    When I set the property "containee_types" of capability "compute" the node "Compute" as input property name "location_meta_osARCH"
    And I set the property "containee_types" of capability "host" the node "Compute" as input property name "location_meta_osARCH"
    Then I should receive a RestResponse with no error

    When I set the value "linux" to the location meta-property "LOCATION_META_1" of the location "Thark location" of the orchestrator "Mount doom orchestrator"
    And I check for the valid status of the topology
    Then the topology should be valid