Feature: Manage Nodetemplates of a topology

Background:
  Given I am authenticated with "ADMIN" role
  And I have a CSAR folder that is "containing base types"
  And I upload it
  And I should receive a RestResponse with no error
  And I have a CSAR folder that is "containing java types"
  And I upload it
  And I should receive a RestResponse with no error
  And There is a "node type" with element name "tosca.nodes.Compute" and archive version "1.0"
  And There is a "node type" with element name "fastconnect.nodes.JavaChef" and archive version "1.0"
  And There is a "node type" with element name "fastconnect.nodes.War" and archive version "1.0"
  And I create a new application with name "watchmiddleearth" and description "Use my great eye to find frodo and the ring."

Scenario: Add a nodetemplate based on a node type id
  When I add a node template "Template1" related to the "tosca.nodes.Compute:1.0" node type
  Then I should receive a RestResponse with no error
    And The RestResponse should contain a nodetemplate named "Template1" and type "tosca.nodes.Compute"
    And The RestResponse should contain a node type with "tosca.nodes.Compute:1.0" id

Scenario: Remove a nodetemplate from a topology
  Given I add a node template "Template1" related to the "tosca.nodes.Compute:1.0" node type
  When I delete a node template "Template1" from the topology
  Then I should receive a RestResponse with no error
    And The RestResponse should not contain a nodetemplate named "Template1"

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

Scenario: Update a nodetemplate's name from a topology
  Given I have added a node template "Template1" related to the "tosca.nodes.Compute:1.0" node type
  When I update the node template's name from "Template1" to "Template2"
  Then I should receive a RestResponse with no error
  When I try to retrieve the created topology
  Then I should receive a RestResponse with no error
    And The topology should contain a nodetemplate named "Template2"

Scenario: Update a nodetemplate's property from a topology
  Given I have added a node template "Template1" related to the "tosca.nodes.Compute:1.0" node type
  When I update the node template "Template1"'s property "disk_size" to "1024"
  Then I should receive a RestResponse with no error
  When I try to retrieve the created topology
  Then I should receive a RestResponse with no error
    And The topology should contain a nodetemplate named "Template1" with property "disk_size" set to "1024"

Scenario: Update a nodetemplate's deployment artifact from a topology
  Given I have added a node template "Template1" related to the "fastconnect.nodes.War:1.0" node type
  When I update the node template "Template1"'s artifact "war" with "myWar.war"
  Then I should receive a RestResponse with no error
  	And the response should contain the artifact reference
  When I try to retrieve the created topology
  Then I should receive a RestResponse with no error
    And The topology should contain a nodetemplate named "Template1" with an artifact "war" with the specified UID and name "myWar.war"
