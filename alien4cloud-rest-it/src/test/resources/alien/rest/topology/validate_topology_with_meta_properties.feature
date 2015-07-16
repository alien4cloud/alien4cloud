Feature: Check if topology with properties defined as as input on cloud meta is deployable

Background:
  Given I am authenticated with "ADMIN" role
    And I upload a plugin
    And I create a cloud with name "mockCloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
    And I enable the cloud "mockCloud"
    And I upload the archive "tosca base types 1.0"
    And I should receive a RestResponse with no error
    And There is a "node type" with element name "tosca.nodes.Compute" and archive version "1.0"
    And I create a new application with name "App-with-meta-cloud" and description "An application with meta properties and inputs on cloud..."
    And I add a node template "Compute" related to the "tosca.nodes.Compute:1.0" node type
    And I assign the cloud with name "mockCloud" for the application
  When I load several configuration tags
    Then I should have 11 configuration tags loaded

Scenario: Use cloud metas to define a property on a node and check deployability
  Given I have the tag "NUMcpus" registered for "cloud"
  And I have the tag "osARCH" registered for "cloud"
  And I set the value "x86_64" for the cloud meta-property "osARCH" of the cloud "mockCloud"
  And I set the value "2" for the cloud meta-property "NUMcpus" of the cloud "mockCloud"

  When I define the property "os_arch" of the node "Compute" as input property
    Then I should receive a RestResponse with no error
    And The topology should have the property "os_arch" defined as input property
  When I define the property "os_type" of the node "Compute" as input property
    Then I should receive a RestResponse with no error
    And The topology should have the property "os_type" defined as input property
  When I check for the deployable status of the topology
    Then the topology should not be deployable

  When I associate the property "os_arch" of a node template "Compute" to the input "os_arch"
    Then I should receive a RestResponse with no error
  When I associate the property "os_type" of a node template "Compute" to the input "os_type"
    Then I should receive a RestResponse with no error
  When I check for the deployable status of the topology
    Then the topology should not be deployable

  When I rename the input "os_arch" to "cloud_meta_osARCH"
    And I rename the input "os_type" to "cloud_meta_CLOUD_META_1"
  When I check for the deployable status of the topology
    Then the topology should not be deployable
  When I set the property "containee_types" of capability "compute" the node "Compute" as input property name "cloud_meta_osARCH"
    Then I should receive a RestResponse with no error

  When I set the value "linux" for the cloud meta-property "CLOUD_META_1" of the cloud "mockCloud"
    And I check for the deployable status of the topology
    Then the topology should be deployable
