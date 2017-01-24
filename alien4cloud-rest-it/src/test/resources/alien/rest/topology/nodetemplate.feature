Feature: Manage Nodetemplates of a topology

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca base types 1.0"
    And I should receive a RestResponse with no error
    And I upload the archive "sample java types 1.0"
    And I should receive a RestResponse with no error
    And There is a "node type" with element name "tosca.nodes.Compute" and archive version "1.0"
    And There is a "node type" with element name "fastconnect.nodes.JavaChef" and archive version "1.0"
    And There is a "node type" with element name "fastconnect.nodes.War" and archive version "1.0"
    And I create an application with name "watchmiddleearth", archive name "watchmiddleearth", description "Use my great eye to find frodo and the ring." and topology template id "null"

  @reset
  Scenario: Add a nodetemplate based on a node type id
    When I add a node template "Template1" related to the "tosca.nodes.Compute:1.0" node type
    Then I should receive a RestResponse with no error
    And The RestResponse should contain a nodetemplate named "Template1" and type "tosca.nodes.Compute"
    And The RestResponse should contain a node type with "tosca.nodes.Compute:1.0" id

  @reset
  Scenario: Add a nodetemplate based on a node type id with an invalid name should failed
    When I add a node template "Template!!!" related to the "tosca.nodes.Compute:1.0" node type
    Then I should receive a RestResponse with an error code 618

  @reset
  Scenario: Remove a nodetemplate from a topology
    Given I add a node template "Template1" related to the "tosca.nodes.Compute:1.0" node type
    When I delete a node template "Template1" from the topology
    Then I should receive a RestResponse with no error
    And The RestResponse should not contain a nodetemplate named "Template1"

  @reset
  Scenario: Remove a nodetemplate being a target of a relationship from a topology
    Given I have added a node template "Template1" related to the "tosca.nodes.Compute:1.0" node type
    And I have added a node template "Template2" related to the "fastconnect.nodes.Java:1.0" node type
    And I have added a node template "Template3" related to the "fastconnect.nodes.Java:1.0" node type
    And I add a relationship of type "tosca.relationships.HostedOn" defined in archive "tosca-base-types" version "1.0" with source "Template2" and target "Template1" for requirement "compute" of type "tosca.capabilities.Container" and target capability "compute"
    And I add a relationship of type "tosca.relationships.DependsOn" defined in archive "tosca-base-types" version "1.0" with source "Template3" and target "Template1" for requirement "dependency" of type "tosca.capabilities.Feature" and target capability "feature"
    When I delete a node template "Template1" from the topology
    Then I should receive a RestResponse with no error
    And The RestResponse should not contain a nodetemplate named "Template1"
    And The RestResponse should not contain a relationship of type "tosca.relationships.HostedOn" with source "Template2" and target "Template1"
    And The RestResponse should not contain a relationship of type "tosca.relationships.DependsOn" with source "Template3" and target "Template1"

  @reset
  Scenario: Update a nodetemplate's name from a topology
    Given I have added a node template "Template1" related to the "tosca.nodes.Compute:1.0" node type
    When I update the node template's name from "Template1" to "Template2"
    Then I should receive a RestResponse with no error
    When I try to retrieve the created topology
    Then I should receive a RestResponse with no error
    And The topology should contain a nodetemplate named "Template2"

  @reset
  Scenario: Update a nodetemplate's name from a topology with a new name with an accent should failed
    Given I have added a node template "Template1" related to the "tosca.nodes.Compute:1.0" node type
    When I update the node template's name from "Template1" to "Templateé"
    Then I should receive a RestResponse with an error code 618
    When I try to retrieve the created topology
    Then I should receive a RestResponse with no error
    And The topology should contain a nodetemplate named "Template1"

  @reset
  Scenario: Update a nodetemplate's name from a topology with a new name with a dash should failed
    Given I have added a node template "Template1" related to the "tosca.nodes.Compute:1.0" node type
    When I update the node template's name from "Template1" to "Template-"
    Then I should receive a RestResponse with an error code 618
    When I try to retrieve the created topology
    Then I should receive a RestResponse with no error
    And The topology should contain a nodetemplate named "Template1"

  @reset
  Scenario: Update a nodetemplate's property from a topology
    Given I have added a node template "Template1" related to the "tosca.nodes.Compute:1.0" node type
    When I update the node template "Template1"'s property "disk_size" to "1024 B"
    Then I should receive a RestResponse with no error
    When I try to retrieve the created topology
    Then I should receive a RestResponse with no error
    And The topology should contain a nodetemplate named "Template1" with property "disk_size" set to "1024 B"

  @reset
  Scenario: Update a nodetemplate's deployment artifact from a topology
    Given I have added a node template "Template1" related to the "fastconnect.nodes.War:1.0" node type
    When I update the node template "Template1"'s artifact "war" with "myWar.war"
    Then I should receive a RestResponse with no error
    When I try to retrieve the created topology
    Then I should receive a RestResponse with no error
    And The topology should contain a nodetemplate named "Template1" with an artifact "war" with the specified UID and name "myWar.war"

  @reset
  Scenario: Reset a nodetemplate's deployment artifact from a topology
    Given I have added a node template "Template1" related to the "fastconnect.nodes.War:1.0" node type
    When I update the node template "Template1"'s artifact "war" with "myWar.war"
    Then I should receive a RestResponse with no error
    When I try to retrieve the created topology
    Then I should receive a RestResponse with no error
    And The topology should contain a nodetemplate named "Template1" with an artifact "war" with the specified UID and name "myWar.war"
    When I reset the node template "Template1"'s artifact "war" to default value
    Then I should receive a RestResponse with no error
    When I try to retrieve the created topology
    Then I should receive a RestResponse with no error
    And The topology should contain a nodetemplate named "Template1" with an artifact "war" with the specified UID and name ""

  @reset
  Scenario: Set a nodetemplate's artifact as application input artifact and update it
    Given I have added a node template "Template1" related to the "fastconnect.nodes.War:1.0" node type
    When I update the node template "Template1"'s artifact "war" with "myWar.war"
      Then I should receive a RestResponse with no error
    When I associate the artifact "war" of node template "Template1" as an input artifact "inputWar"
      Then I should receive a RestResponse with no error
    When I update the application's input artifact "inputWar" with "yourWar.war"
      Then I should receive a RestResponse with no error

  @reset
  Scenario: Reset a nodetemplate's property with default value must put the default value
    Given I have added a node template "Template1" related to the "tosca.nodes.Compute:1.0" node type
    Then The topology should contain a nodetemplate named "Template1" with property "disk_size" set to "20 B"
    When I update the node template "Template1"'s property "disk_size" to "1024 B"
    Then I should receive a RestResponse with no error
    When I try to retrieve the created topology
    Then I should receive a RestResponse with no error
    And The topology should contain a nodetemplate named "Template1" with property "disk_size" set to "1024 B"
    When I reset the the node template "Template1"'s property "disk_size"
    Then I should receive a RestResponse with no error
    When I try to retrieve the created topology
    Then I should receive a RestResponse with no error
    And The topology should contain a nodetemplate named "Template1" with property "disk_size" set to "20 B"

  @reset
  Scenario: Reset a nodetemplate's property with no default value must put null value
    Given I have added a node template "Template1" related to the "tosca.nodes.Compute:1.0" node type
    Then The topology should contain a nodetemplate named "Template1" with property "num_cpus" set to null
    When I update the node template "Template1"'s property "num_cpus" to "2"
    Then I should receive a RestResponse with no error
    When I try to retrieve the created topology
    Then I should receive a RestResponse with no error
    And The topology should contain a nodetemplate named "Template1" with property "num_cpus" set to "2"
    When I reset the the node template "Template1"'s property "num_cpus"
    Then I should receive a RestResponse with no error
    When I try to retrieve the created topology
    Then I should receive a RestResponse with no error
    And The topology should contain a nodetemplate named "Template1" with property "num_cpus" set to null
