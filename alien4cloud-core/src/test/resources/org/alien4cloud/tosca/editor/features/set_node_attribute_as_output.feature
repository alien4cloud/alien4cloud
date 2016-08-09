Feature: Topology editor: set node attribute as output

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Set a node attribute as output should succeed
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | network                                                               |
      | indexedNodeTypeId | tosca.nodes.Network:1.0.0-SNAPSHOT                                    |
    When I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.SetNodeAttributeAsOutputOperation |
      | nodeName      | network                                                                                        |
      | attributeName | tosca_name                                                                                     |
    Then No exception should be thrown
    And The SPEL int expression "outputAttributes.size()" should return 1
    And The SPEL int expression "outputAttributes['network'].size()" should return 1
    And The SPEL int expression "outputAttributes['network'].?[#this == 'tosca_name'].size()" should return 1
    When I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.SetNodeAttributeAsOutputOperation |
      | nodeName      | network                                                                                        |
      | attributeName | tosca_id                                                                                       |
    Then No exception should be thrown
    And The SPEL int expression "outputAttributes.size()" should return 1
    And The SPEL int expression "outputAttributes['network'].size()" should return 2
    And The SPEL int expression "outputAttributes['network'].?[#this == 'tosca_id'].size()" should return 1

  Scenario: Set as output a attribute that doesn't exists in a node should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | network                                                               |
      | indexedNodeTypeId | tosca.nodes.Network:1.0.0-SNAPSHOT                                    |
    When I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.SetNodeAttributeAsOutputOperation |
      | nodeName      | network                                                                                        |
      | attributeName | i_do_not_exist                                                                                 |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown
    When I get the edited topology
    Then The SPEL expression "outputAttributes" should return "null"
