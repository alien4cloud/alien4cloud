Feature: Topology editor: delete input

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Remove a simple input should succeed
    Given I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | simple_input                                                     |
      | propertyDefinition.type | string                                                           |
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.inputs.DeleteInputOperation |
      | inputName | simple_input                                                        |
    Then No exception should be thrown
    And The SPEL int expression "inputs.size()" should return 0

  Scenario: Remove an input that does not exists while there is no inputs should fail
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.inputs.DeleteInputOperation |
      | inputName | simple_input                                                        |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Remove an input that does not exists should fail
    Given I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | simple_input                                                     |
      | propertyDefinition.type | string                                                           |
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.inputs.DeleteInputOperation |
      | inputName | other_input                                                        |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Remove an input that is associated to a node property should remove the association
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | software_component                                                    |
      | indexedNodeTypeId | tosca.nodes.SoftwareComponent:1.0.0-SNAPSHOT                          |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | component_version                                                |
      | propertyDefinition.type | version                                                          |
    And I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodePropertyAsInputOperation |
      | nodeName     | software_component                                                                   |
      | propertyName | component_version                                                                    |
      | inputName    | component_version                                                                    |
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.inputs.DeleteInputOperation |
      | inputName | component_version                                                   |
    Then No exception should be thrown
    And The SPEL int expression "inputs.size()" should return 0
    And The SPEL expression "nodeTemplates['software_component'].properties['component_version']" should return "null"

  Scenario: Remove an input that is associated to a node capability property should remove the association
    Given I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | simple_input                                                     |
      | propertyDefinition.type | string                                                           |

  Scenario: Remove an input that is associated to a relationship property should remove the association
    Given I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | simple_input                                                     |
      | propertyDefinition.type | string                                                           |
