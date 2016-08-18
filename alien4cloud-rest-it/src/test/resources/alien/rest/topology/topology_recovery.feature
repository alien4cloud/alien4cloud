Feature: Recover a topology after csar dependencies updates

  Background:
    Given I am authenticated with "ADMIN" role
    # Archives
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload the local archive "data/csars/topology_recovery_test/test-topo-recovery-types.yaml"
    And I upload the local archive "data/csars/topology_recovery_test/sample-topology-test-recovery.yml"
    And I get the topology related to the template with name "test-recovery-topology"
#    And I create a new application with name "watchmiddleearth" and description "Use my great eye to find frodo and the ring."

  @reset
  Scenario: Delete a node type from archive and recover the topology
  	Given I upload the local archive "data/csars/topology_recovery_test/test-recovery-nodetype-deleted-types.yaml"
  	When I ask for updated dependencies from the registered topology
  	Then The Response should contain the folowwing dependencies
  		|name                    |version|
  		|test-topo-recovery-types|0.1-SNAPSHOT|
    When I trigger the recovery of the topology
    Then I should receive a RestResponse with no error
    And the topology dto should contain 2 nodetemplates
    And The RestResponse should not contain a nodetemplate named "TestComponent"
    And I should have a relationship "hostedOnCompute" with type "tosca.relationships.HostedOn" from "TestComponentSource" to "Compute" in ALIEN
     
  @reset
  Scenario: Delete a relationship type from archive and recover the topology
  	Given I upload the local archive "data/csars/topology_recovery_test/test-recovery-reltype-deleted-types.yaml"
  	When I ask for updated dependencies from the registered topology
  	Then The Response should contain the folowwing dependencies
  		|name                    |version|
  		|test-topo-recovery-types|0.1-SNAPSHOT|
    When I trigger the recovery of the topology
    Then I should receive a RestResponse with no error
    And the topology dto should contains 3 nodetemplates
    And the node "TestComponentSource" in the topology dto should have 7 relationshipTemplates
    And I should not have the relationship "testComponentConnectsToTestComponent" in "TestComponentSource" node template
     
  @reset
  Scenario: Delete a capability and a requirement from a type archive and recover the topology
  	Given I upload the local archive "data/csars/topology_recovery_test/test-recovery-capa-requirement-deleted-types.yaml"
  	When I ask for updated dependencies from the registered topology
  	Then The Response should contain the folowwing dependencies
  		|name                    |version|
  		|test-topo-recovery-types|0.1-SNAPSHOT|
    When I trigger the recovery of the topology
    Then I should receive a RestResponse with no error
    And the topology dto should contain 3 nodetemplates
    And the node "TestComponentSource" in the topology dto should have 6 relationshipTemplates
    And the node "TestComponentSource" in the topology dto should not have the requirement "req_to_be_deleted"
    And the node "TestComponent" in the topology dto should not have the capability "capa_to_be_deleted"
    And there should not be the relationship "reqToBeDeletedTestComponent" in "TestComponentSource" node template in the topology dto
    And there should not be the relationship "capaToBeDeletedTestComponent" in "TestComponentSource" node template in the topology dto
    
  @reset
  Scenario: Change capabilities / requirements upper bound in an archive and recover the topology
  	Given I upload the local archive "data/csars/topology_recovery_test/test-recovery-bound-reached-types.yaml"
  	When I ask for updated dependencies from the registered topology
  	Then The Response should contain the folowwing dependencies
  		|name                    |version|
  		|test-topo-recovery-types|0.1-SNAPSHOT|
    When I trigger the recovery of the topology
    Then I should receive a RestResponse with no error
    And the topology dto should contain 3 nodetemplates
    And the node "TestComponentSource" in the topology dto should have 6 relationshipTemplates
    And there should not be the relationship "capaUpperBoundTestTestComponent" in "TestComponentSource" node template in the topology dto
    And there should not be the relationship "reqUpperBoundTestTestComponent" in "TestComponentSource" node template in the topology dto
    
    
  @reset
  Scenario: Update an archive, and reset the dependend topology 
  	Given I upload the local archive "data/csars/topology_recovery_test/test-recovery-nodetype-deleted-types.yaml"
  	When I ask for updated dependencies from the registered topology
  	Then The Response should contain the folowwing dependencies
  		|name                    |version|
  		|test-topo-recovery-types|0.1-SNAPSHOT|
    When I reset the topology
    Then I should receive a RestResponse with no error
    And the topology dto should contain an emty topology
#
#  @reset
#  Scenario: Add a nodetemplate based on a node type id with an invalid name should failed
#    When I add a node template "Template!!!" related to the "tosca.nodes.Compute:1.0" node type
#    Then I should receive a RestResponse with an error code 618
#
#  @reset
#  Scenario: Remove a nodetemplate from a topology
#    Given I add a node template "Template1" related to the "tosca.nodes.Compute:1.0" node type
#    When I delete a node template "Template1" from the topology
#    Then I should receive a RestResponse with no error
#    And The RestResponse should not contain a nodetemplate named "Template1"
#
#  @reset
#  Scenario: Remove a nodetemplate being a target of a relationship from a topology
#    Given I have added a node template "Template1" related to the "tosca.nodes.Compute:1.0" node type
#    And I have added a node template "Template2" related to the "fastconnect.nodes.Java:1.0" node type
#    And I have added a node template "Template3" related to the "fastconnect.nodes.Java:1.0" node type
#    And I add a relationship of type "tosca.relationships.HostedOn" defined in archive "tosca-base-types" version "1.0" with source "Template2" and target "Template1" for requirement "compute" of type "tosca.capabilities.Container" and target capability "compute"
#    And I add a relationship of type "tosca.relationships.DependsOn" defined in archive "tosca-base-types" version "1.0" with source "Template3" and target "Template1" for requirement "dependency" of type "tosca.capabilities.Feature" and target capability "feature"
#    When I delete a node template "Template1" from the topology
#    Then I should receive a RestResponse with no error
#    And The RestResponse should not contain a nodetemplate named "Template1"
#    And The RestResponse should not contain a relationship of type "tosca.relationships.HostedOn" with source "Template2" and target "Template1"
#    And The RestResponse should not contain a relationship of type "tosca.relationships.DependsOn" with source "Template3" and target "Template1"
#
#  @reset
#  Scenario: Update a nodetemplate's name from a topology
#    Given I have added a node template "Template1" related to the "tosca.nodes.Compute:1.0" node type
#    When I update the node template's name from "Template1" to "Template2"
#    Then I should receive a RestResponse with no error
#    When I try to retrieve the created topology
#    Then I should receive a RestResponse with no error
#    And The topology should contain a nodetemplate named "Template2"
#
#  @reset
#  Scenario: Update a nodetemplate's name from a topology with a new name with an accent should failed
#    Given I have added a node template "Template1" related to the "tosca.nodes.Compute:1.0" node type
#    When I update the node template's name from "Template1" to "Templateé"
#    Then I should receive a RestResponse with an error code 618
#    When I try to retrieve the created topology
#    Then I should receive a RestResponse with no error
#    And The topology should contain a nodetemplate named "Template1"
#
#  @reset
#  Scenario: Update a nodetemplate's name from a topology with a new name with a dash should failed
#    Given I have added a node template "Template1" related to the "tosca.nodes.Compute:1.0" node type
#    When I update the node template's name from "Template1" to "Template-"
#    Then I should receive a RestResponse with an error code 618
#    When I try to retrieve the created topology
#    Then I should receive a RestResponse with no error
#    And The topology should contain a nodetemplate named "Template1"
#
#  @reset
#  Scenario: Update a nodetemplate's property from a topology
#    Given I have added a node template "Template1" related to the "tosca.nodes.Compute:1.0" node type
#    When I update the node template "Template1"'s property "disk_size" to "1024 B"
#    Then I should receive a RestResponse with no error
#    When I try to retrieve the created topology
#    Then I should receive a RestResponse with no error
#    And The topology should contain a nodetemplate named "Template1" with property "disk_size" set to "1024 B"
#
#  @reset
#  Scenario: Update a nodetemplate's deployment artifact from a topology
#    Given I have added a node template "Template1" related to the "fastconnect.nodes.War:1.0" node type
#    When I update the node template "Template1"'s artifact "war" with "myWar.war"
#    Then I should receive a RestResponse with no error
#    When I try to retrieve the created topology
#    Then I should receive a RestResponse with no error
#    And The topology should contain a nodetemplate named "Template1" with an artifact "war" with the specified UID and name "myWar.war"
#
#  @reset
#  Scenario: Reset a nodetemplate's deployment artifact from a topology
#    Given I have added a node template "Template1" related to the "fastconnect.nodes.War:1.0" node type
#    When I update the node template "Template1"'s artifact "war" with "myWar.war"
#    Then I should receive a RestResponse with no error
#    When I try to retrieve the created topology
#    Then I should receive a RestResponse with no error
#    And The topology should contain a nodetemplate named "Template1" with an artifact "war" with the specified UID and name "myWar.war"
#    When I reset the node template "Template1"'s artifact "war" to default value
#    Then I should receive a RestResponse with no error
#    When I try to retrieve the created topology
#    Then I should receive a RestResponse with no error
#    And The topology should contain a nodetemplate named "Template1" with an artifact "war" with the specified UID and name ""
#
#  @reset
#  Scenario: Set a nodetemplate's artifact as application input artifact and update it
#    Given I have added a node template "Template1" related to the "fastconnect.nodes.War:1.0" node type
#    When I update the node template "Template1"'s artifact "war" with "myWar.war"
#      Then I should receive a RestResponse with no error
#    When I associate the artifact "war" of node template "Template1" as an input artifact "inputWar"
#      Then I should receive a RestResponse with no error
#    When I update the application's input artifact "inputWar" with "yourWar.war"
#      Then I should receive a RestResponse with no error
#
#  @reset
#  Scenario: Reset a nodetemplate's property with default value must put the default value
#    Given I have added a node template "Template1" related to the "tosca.nodes.Compute:1.0" node type
#    Then The topology should contain a nodetemplate named "Template1" with property "disk_size" set to "20 B"
#    When I update the node template "Template1"'s property "disk_size" to "1024 B"
#    Then I should receive a RestResponse with no error
#    When I try to retrieve the created topology
#    Then I should receive a RestResponse with no error
#    And The topology should contain a nodetemplate named "Template1" with property "disk_size" set to "1024 B"
#    When I reset the the node template "Template1"'s property "disk_size"
#    Then I should receive a RestResponse with no error
#    When I try to retrieve the created topology
#    Then I should receive a RestResponse with no error
#    And The topology should contain a nodetemplate named "Template1" with property "disk_size" set to "20 B"
#
#  @reset
#  Scenario: Reset a nodetemplate's property with no default value must put null value
#    Given I have added a node template "Template1" related to the "tosca.nodes.Compute:1.0" node type
#    Then The topology should contain a nodetemplate named "Template1" with property "num_cpus" set to null
#    When I update the node template "Template1"'s property "num_cpus" to "2"
#    Then I should receive a RestResponse with no error
#    When I try to retrieve the created topology
#    Then I should receive a RestResponse with no error
#    And The topology should contain a nodetemplate named "Template1" with property "num_cpus" set to "2"
#    When I reset the the node template "Template1"'s property "num_cpus"
#    Then I should receive a RestResponse with no error
#    When I try to retrieve the created topology
#    Then I should receive a RestResponse with no error
#    And The topology should contain a nodetemplate named "Template1" with property "num_cpus" set to null
