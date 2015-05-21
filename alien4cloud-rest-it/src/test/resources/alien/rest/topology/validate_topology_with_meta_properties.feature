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

Scenario: checking if an empty topology is deployable
  Given I am authenticated with "APPLICATIONS_MANAGER" role
  When I check for the deployable status of the topology
  Then I should receive a RestResponse with no error
    And the topology should not be deployable