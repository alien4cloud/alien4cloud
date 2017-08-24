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
    And The SPEL expression "nodeTemplates.size()" should return 1
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
    Then an exception of type "alien4cloud.exception.InvalidNameException" should be thrown
    And The SPEL expression "nodeTemplates.size()" should return 1
    And The SPEL expression "nodeTemplates['Template1'].type" should return "tosca.nodes.Compute"

  Scenario: Rename a node template in a topology should rename outputs
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Template1                                                             |
      | indexedNodeTypeId | tosca.nodes.WebServer:1.0.0-SNAPSHOT                                  |
    And I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.SetNodePropertyAsOutputOperation |
      | nodeName     | Template1                                                                                     |
      | propertyName | component_version                                                                             |
    And I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.SetNodeAttributeAsOutputOperation |
      | nodeName      | Template1                                                                                      |
      | attributeName | tosca_name                                                                                     |
    And I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.SetNodeCapabilityPropertyAsOutputOperation |
      | nodeName       | Template1                                                                                               |
      | capabilityName | data_endpoint                                                                                           |
      | propertyName   | port                                                                                                    |

    When I execute the operation
      | type     | org.alien4cloud.tosca.editor.operations.nodetemplate.RenameNodeOperation |
      | nodeName | Template1                                                                |
      | newName  | Template2                                                                |
    Then No exception should be thrown
    And The SPEL expression "nodeTemplates.size()" should return 1
    And The SPEL expression "nodeTemplates['Template1']" should return "null"
    And The SPEL expression "nodeTemplates['Template2'].type" should return "tosca.nodes.WebServer"

    #output properties
    And The SPEL expression "outputProperties.size()" should return 1
    And The SPEL expression "outputProperties['Template1']" should return "null"
    And The SPEL expression "outputProperties['Template2'].size()" should return 1
    And The SPEL expression "outputProperties['Template2'].?[#this == 'component_version'].size()" should return 1

    #outputs attributes
    And The SPEL expression "outputAttributes.size()" should return 1
    And The SPEL expression "outputAttributes['Template1']" should return "null"
    And The SPEL expression "outputAttributes['Template2'].size()" should return 1
    And The SPEL expression "outputAttributes['Template2'].?[#this == 'tosca_name'].size()" should return 1

    #outputs capabilities properties
    And The SPEL expression "outputCapabilityProperties.size()" should return 1
    And The SPEL expression "outputCapabilityProperties['Template1']" should return "null"
    And The SPEL expression "outputCapabilityProperties['Template2'].size()" should return 1
    And The SPEL expression "outputCapabilityProperties['Template2']['data_endpoint'].size()" should return 1
    And The SPEL expression "outputCapabilityProperties['Template2']['data_endpoint'].?[#this == 'port'].size()" should return 1
