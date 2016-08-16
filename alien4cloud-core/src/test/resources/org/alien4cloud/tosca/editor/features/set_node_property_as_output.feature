Feature: Topology editor: set node property as output

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Set a node property as output should succeed
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | network                                                    |
      | indexedNodeTypeId | tosca.nodes.Network:1.0.0-SNAPSHOT                          |
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.SetNodePropertyAsOutputOperation |
      | nodeName     | network                                                                   |
      | propertyName | network_name                                                                    |
    Then No exception should be thrown
    And The SPEL int expression "outputProperties.size()" should return 1
    And The SPEL int expression "outputProperties['network'].size()" should return 1
    And The SPEL int expression "outputProperties['network'].?[#this == 'network_name'].size()" should return 1
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.SetNodePropertyAsOutputOperation |
      | nodeName     | network                                                                   |
      | propertyName | network_id                                                                                     |
    Then No exception should be thrown
    And The SPEL int expression "outputProperties.size()" should return 1
    And The SPEL int expression "outputProperties['network'].size()" should return 2
    And The SPEL int expression "outputProperties['network'].?[#this == 'network_id'].size()" should return 1

  Scenario: Set as output a property that doesn't exists in a node should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | network                                                    |
      | indexedNodeTypeId | tosca.nodes.Network:1.0.0-SNAPSHOT                          |
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.SetNodePropertyAsOutputOperation |
      | nodeName     | network                                                                   |
      | propertyName | i_do_not_exist                                                                 |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown
    When I get the edited topology
    Then The SPEL expression "outputProperties" should return "null"
