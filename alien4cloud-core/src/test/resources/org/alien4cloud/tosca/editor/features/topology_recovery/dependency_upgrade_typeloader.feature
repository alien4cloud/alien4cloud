Feature: Topology editor: Upgrade version of a csar dependency implicitly when loading a type

  Background:
    Given I am authenticated with "ADMIN" role
  # initialize or reset the types as defined in the initial archive
    And I upload unzipped CSAR from path "src/test/resources/data/csars/dependency_version/dependency7.yml"
    And I upload unzipped CSAR from path "src/test/resources/data/csars/dependency_version/dependency8.yml"
    And I create an empty topology

  Scenario: Add two nodes of different versions should recover the topology
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | TestComponent                                                         |
      | indexedNodeTypeId | alien.test.nodes.TestComponent:0.7-SNAPSHOT                           |
    Then No exception should be thrown
    And The SPEL expression "dependencies.^[name == 'test-topo-dependencies-types'].version" should return "0.7-SNAPSHOT"
    And The SPEL expression "nodeTemplates.size()" should return 1
    And The SPEL expression "nodeTemplates['TestComponent'].properties['toBeDeleted'].value" should return "deleteMe"
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | TestComponent2                                                        |
      | indexedNodeTypeId | alien.test.nodes.TestComponent:0.8-SNAPSHOT                           |
    Then No exception should be thrown
    And The SPEL expression "dependencies.^[name == 'test-topo-dependencies-types'].version" should return "0.8-SNAPSHOT"
    And The SPEL expression "nodeTemplates.size()" should return 2
    And The SPEL expression "nodeTemplates['TestComponent'].properties['toBeDeleted']" should return "null"
    And The SPEL expression "nodeTemplates['TestComponent2'].properties['toBeDeleted']" should return "null"
