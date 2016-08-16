Feature: Topology editor: rename node

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Rename a node template in a topology
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Template1                                                             |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type     | org.alien4cloud.tosca.editor.operations.nodetemplate.RenameNodeOperation |
      | nodeName | Template1                                                                |
      | newName  | Template2                                                                |
    Then No exception should be thrown
    And The SPEL int expression "nodeTemplates.size()" should return 1
    And The SPEL expression "nodeTemplates['Template1']" should return "null"
    And The SPEL expression "nodeTemplates['Template2'].type" should return "tosca.nodes.Compute"

  Scenario: Rename a non existing node template in an empty topology should fail
    When I execute the operation
      | type     | org.alien4cloud.tosca.editor.operations.nodetemplate.RenameNodeOperation |
      | nodeName | missingNode                                                              |
      | newName  | Template2                                                                |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Rename a non existing nodetemplate in a topology should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Template1                                                             |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type     | org.alien4cloud.tosca.editor.operations.nodetemplate.RenameNodeOperation |
      | nodeName | missingNode                                                              |
      | newName  | Template2                                                                |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Rename a node template in a topology with an invalid name should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Template1                                                             |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type     | org.alien4cloud.tosca.editor.operations.nodetemplate.RenameNodeOperation |
      | nodeName | Template1                                                                |
      | newName  | Template1!!!                                                             |
    Then an exception of type "alien4cloud.exception.InvalidNodeNameException" should be thrown
    And The SPEL int expression "nodeTemplates.size()" should return 1
    And The SPEL expression "nodeTemplates['Template1'].type" should return "tosca.nodes.Compute"
