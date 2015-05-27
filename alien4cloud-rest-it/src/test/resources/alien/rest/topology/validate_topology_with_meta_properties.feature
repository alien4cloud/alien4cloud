Feature: Check if topology with properties defined as internal meta is deployable

Background:
  Given I am authenticated with "APPLICATIONS_MANAGER" role
    And I create a new application with name "watchmiddleearth" and description "Use my great eye to find frodo and the ring."
    And I already had a csar with name "myCsar" and version "1.0-SNAPSHOT"
    And I add to the csar "myCsar" "1.0-SNAPSHOT" the component "rootNodeType"
    And I create "capabilities" in an archive name "myCsar" version "1.0-SNAPSHOT"
      |tosca.capabilities.Container|
      |tosca.capabilities.Feature|
      |fastconnect.capabilities.Runner|
      |tosca.capabilities.Java|
    And i create a relationshiptype "test.HostedOn" in an archive name "myCsar" version "1.0-SNAPSHOT" with properties
      |validSource     | tosca.capabilities.Container|
      |validTarget     | tosca.capabilities.Container|
      |abstract        |false|
    And i create a relationshiptype "test.DependsOn" in an archive name "myCsar" version "1.0-SNAPSHOT" with properties
      |validSource     | tosca.capabilities.Feature|
      |validTarget     | tosca.capabilities.Feature|
      |abstract        |true|
  Given I am authenticated with "ADMIN" role
    And I load several configuration tags
    Then I should have 9 configuration tags loaded

#Scenario: Check if an empty topology is deployable
#  Given I am authenticated with "APPLICATIONS_MANAGER" role
#  When I check for the deployable status of the topology
#  Then I should receive a RestResponse with no error
#   And the topology should not be deployable

Scenario: Fill in meta properties for an application and check that is it deployable
  Given I am authenticated with "APPLICATIONS_MANAGER" role
    And I add to the csar "myCsar" "1.0-SNAPSHOT" the components
      |computeNodeType|
      |javaNodeType|
    And I have added a node template "Compute" related to the "fastconnect.nodes.Compute:1.0-SNAPSHOT" node type
  When I check for the deployable status of the topology
  Then I should receive a RestResponse with no error
    And the topology should be deployable
  When I define the property "os_arch" of the node "Compute" as input property
#    And The topology should have the property "os_arch" defined as input property
