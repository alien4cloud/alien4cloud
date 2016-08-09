Feature: Topology editor: unset node attribute as output

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Unset a node attribute to an input that matches type should succeed
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | network                                                               |
      | indexedNodeTypeId | tosca.nodes.Network:1.0.0-SNAPSHOT                                    |
    And I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.SetNodeAttributeAsOutputOperation |
      | nodeName      | network                                                                                        |
      | attributeName | tosca_name                                                                                     |
    And I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.SetNodeAttributeAsOutputOperation |
      | nodeName      | network                                                                                        |
      | attributeName | tosca_id                                                                                       |
    When I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.UnSetNodeAttributeAsOutputOperation |
      | nodeName      | network                                                                                          |
      | attributeName | tosca_name                                                                                       |
    Then No exception should be thrown
    And The SPEL int expression "outputAttributes.size()" should return 1
    And The SPEL int expression "outputAttributes['network'].?[#this == 'tosca_name'].size()" should return 0
    When I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.UnSetNodeAttributeAsOutputOperation |
      | nodeName      | network                                                                                          |
      | attributeName | tosca_id                                                                                         |
    Then No exception should be thrown
    And The SPEL int expression "outputAttributes.size()" should return 0

  Scenario: UnSet as output a node attribute that is not one should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | network                                                               |
      | indexedNodeTypeId | tosca.nodes.Network:1.0.0-SNAPSHOT                                    |
    When I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.SetNodeAttributeAsOutputOperation |
      | nodeName      | network                                                                                        |
      | attributeName | tosca_name                                                                                     |
    When I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.UnSetNodeAttributeAsOutputOperation |
      | nodeName      | network                                                                                          |
      | attributeName | tosca_id                                                                                         |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown
    When I get the edited topology
    Then The SPEL int expression "outputAttributes.size()" should return 1

  Scenario: UnSet as output a attribute that doesn't exists in a node should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | network                                                               |
      | indexedNodeTypeId | tosca.nodes.Network:1.0.0-SNAPSHOT                                    |
    When I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.UnSetNodeAttributeAsOutputOperation |
      | nodeName      | network                                                                                          |
      | attributeName | i_do_not_exist                                                                                   |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown
    When I get the edited topology
    Then The SPEL expression "outputAttributes" should return "null"
