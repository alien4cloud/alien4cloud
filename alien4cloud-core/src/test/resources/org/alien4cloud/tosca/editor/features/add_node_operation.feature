Feature: Topology editor: nodes templates

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload CSAR from path "../../alien4cloud/target/it-artifacts/tosca-base-types-1.0.csar"
    And I upload CSAR from path "../../alien4cloud/target/it-artifacts/java-types-1.0.csar"
    And I create an empty topology template

#@Ignore
  Scenario: Add a node that exists in the repository shoud succeed.
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Template1                                                             |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    Then No exception should be thrown
    And The SPEL int expression "nodeTemplates.size()" should return 1
    And The SPEL expression "nodeTemplates['Template1'].type" should return "tosca.nodes.Compute"

  Scenario: Add a node that doesn't exists in the repository shoud succeed.
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Template1                                                             |
      | indexedNodeTypeId | the.node.that.does.not.Exists:1.0                                     |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

#@Ignore
  Scenario: Add a node with an invalid name should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Template1!!!!                                                         |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    Then an exception of type "alien4cloud.exception.InvalidNodeNameException" should be thrown

    #@Ignore
  Scenario: Add a node with an existing name should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Template1                                                             |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    Then No exception should be thrown
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Template1                                                             |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    Then an exception of type "alien4cloud.exception.AlreadyExistException" should be thrown




#@Ignore
  Scenario: Add a nodetemplate and a relationship
    Given I build the operation: add a node template "Template1" related to the "tosca.nodes.Compute:1.0" node type
    And I execute the current operation on the current topology
    And I build the operation: add a node template "Template2" related to the "fastconnect.nodes.Java:1.0" node type
    And I execute the current operation on the current topology
    When I build the operation: add a relationship of type "tosca.relationships.HostedOn" defined in archive "tosca-base-types" version "1.0" with source "Template2" and target "Template1" for requirement "host" of type "tosca.capabilities.Container" and target capability "compute"
    And I execute the current operation on the current topology
    Then No exception should be thrown
    And The SPEL int expression "nodeTemplates.size()" should return 2
    And The SPEL int expression "nodeTemplates['Template2'].relationships.size()" should return 1
    
