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
  When I load several configuration tags
    Then I should have 11 configuration tags loaded

Scenario: Use cloud metas to define a property on a node
  Given I have the tag "cloud_meta_NUMcpus" registered for "cloud"
  And I have the tag "cloud_meta_osARCH" registered for "cloud"
  And I set the value "x86_64" for the cloud meta-property "cloud_meta_osARCH" of the cloud "mockCloud"
  And I set the value "2" for the cloud meta-property "cloud_meta_NUMcpus" of the cloud "mockCloud"
  And I set the property "containee_types" of capability "compute" the node "Compute" as input property name "os_arch"
  When I define the property "os_arch" of the node "Compute" as input property
    Then I should receive a RestResponse with no error
    And The topology should have the property "os_arch" defined as input property
  When I check for the deployable status of the topology
    Then the topology should not be deployable
  When I rename the property "os_arch" to "cloud_meta_osARCH"
  Then I associate the property "os_arch" of a node template "Compute" to the input "cloud_meta_osARCH"
    Then I should receive a RestResponse with no error
  When I check for the deployable status of the topology
    Then the topology should not be deployable
#  When I define the property "num_cpus" of the node "Compute" as input property
#    Then I should receive a RestResponse with no error
#    And The topology should have the property "num_cpus" defined as input int property
#  When I associate the property "num_cpus" of a node template "Compute" to the input "cloud_meta_NUMcpus"
#    Then I should receive a RestResponse with no error
