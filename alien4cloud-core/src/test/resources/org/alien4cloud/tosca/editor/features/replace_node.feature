Feature: Topology editor: replace nodes templates

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Replace a nodetemplate in a topology
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.nodetemplate.ReplaceNodeOperation |
      | nodeName  | compute                                                                   |
      | newTypeId | tosca.nodes.WebApplication:1.0.0-SNAPSHOT                                 |
    Then No exception should be thrown
    And The SPEL int expression "nodeTemplates.size()" should return 1
    And The SPEL expression "nodeTemplates['compute'].type" should return "tosca.nodes.WebApplication"