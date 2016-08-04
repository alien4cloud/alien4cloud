Feature: Topology editor: add node template

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Add a node that exists in the repository should succeed
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Template1                                                             |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    Then No exception should be thrown
    And The SPEL int expression "nodeTemplates.size()" should return 1
    And The SPEL expression "nodeTemplates['Template1'].type" should return "tosca.nodes.Compute"

  Scenario: Add a node that does not exists in the repository should succeed
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Template1                                                             |
      | indexedNodeTypeId | the.node.that.does.not.Exists:1.0                                     |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Add a node with an invalid name should fail
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Template1!!!!                                                         |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    Then an exception of type "alien4cloud.exception.InvalidNodeNameException" should be thrown

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
    
