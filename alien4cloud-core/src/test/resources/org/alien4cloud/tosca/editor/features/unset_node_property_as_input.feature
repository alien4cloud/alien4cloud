Feature: Topology editor: unset node property as input

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Unset a node property as input should succeed
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | software_component                                                    |
      | indexedNodeTypeId | tosca.nodes.SoftwareComponent:1.0.0-SNAPSHOT                          |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | component_version                                                |
      | propertyDefinition.type | version                                                          |
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodePropertyAsInputOperation |
      | nodeName     | software_component                                                                          |
      | propertyName | component_version                                                                           |
      | inputName    | component_version                                                                           |
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.UnsetNodePropertyAsInputOperation |
      | nodeName     | software_component                                                                            |
      | propertyName | component_version                                                                             |
    Then No exception should be thrown
    And The SPEL int expression "inputs.size()" should return 1
    And The SPEL expression "nodeTemplates['software_component'].properties['component_version']" should return "null"

  Scenario: Unset a node property as input should fail if the property is not set as input
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | software_component                                                    |
      | indexedNodeTypeId | tosca.nodes.SoftwareComponent:1.0.0-SNAPSHOT                          |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | component_version                                                |
      | propertyDefinition.type | version                                                          |
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.UnsetNodePropertyAsInputOperation |
      | nodeName     | software_component                                                                            |
      | propertyName | component_version                                                                             |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Unset a node property as input should fail if the property does not exists
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | software_component                                                    |
      | indexedNodeTypeId | tosca.nodes.SoftwareComponent:1.0.0-SNAPSHOT                          |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | component_version                                                |
      | propertyDefinition.type | version                                                          |
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodePropertyAsInputOperation |
      | nodeName     | software_component                                                                          |
      | propertyName | component_version                                                                           |
      | inputName    | component_version                                                                           |
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.UnsetNodePropertyAsInputOperation |
      | nodeName     | software_component                                                                            |
      | propertyName | does_not_exists                                                                               |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Unset a node property as input should fail if the node does not exists
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | software_component                                                    |
      | indexedNodeTypeId | tosca.nodes.SoftwareComponent:1.0.0-SNAPSHOT                          |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | component_version                                                |
      | propertyDefinition.type | version                                                          |
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodePropertyAsInputOperation |
      | nodeName     | software_component                                                                          |
      | propertyName | component_version                                                                           |
      | inputName    | component_version                                                                           |
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.UnsetNodePropertyAsInputOperation |
      | nodeName     | does_not_exists                                                                               |
      | propertyName | component_version                                                                             |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown
