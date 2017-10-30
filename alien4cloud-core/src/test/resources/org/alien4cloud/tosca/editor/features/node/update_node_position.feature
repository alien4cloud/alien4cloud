Feature: Topology editor: move node template

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Move a node template to another position should succeed
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Template1                                                             |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
      | coords.x          | 10                                                                    |
      | coords.y          | 20                                                                    |
    Then No exception should be thrown
    And The SPEL expression "nodeTemplates.size()" should return 1
    And The SPEL expression "nodeTemplates['Template1'].type" should return "tosca.nodes.Compute"
    And The SPEL expression "nodeTemplates['Template1'].tags[0].value" should return "10"
    And The SPEL expression "nodeTemplates['Template1'].tags[1].value" should return "20"
    When I execute the operation
      | type     | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodePositionOperation |
      | nodeName | Template1                                                                        |
      | coords.x | 26                                                                               |
      | coords.y | 15                                                                               |
    Then No exception should be thrown
    And The SPEL expression "nodeTemplates['Template1'].tags[0].value" should return "26"
    And The SPEL expression "nodeTemplates['Template1'].tags[1].value" should return "15"
