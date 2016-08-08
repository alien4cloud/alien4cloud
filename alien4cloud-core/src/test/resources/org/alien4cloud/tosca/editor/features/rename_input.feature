Feature: Topology editor: rename input

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Rename an input should succeed
    Given I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | simple_input                                                     |
      | propertyDefinition.type | string                                                           |
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.inputs.RenameInputOperation |
      | inputName    | simple_input                                                        |
      | newInputName | new_simple_input                                                    |
    Then No exception should be thrown
    And The SPEL int expression "inputs.size()" should return 1
    And The SPEL expression "inputs['new_simple_input'].type" should return "string"

  Scenario: Rename an input when new name already exists should fail
    Given I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | simple_input                                                     |
      | propertyDefinition.type | string                                                           |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | other_simple_input                                               |
      | propertyDefinition.type | string                                                           |
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.inputs.RenameInputOperation |
      | inputName    | simple_input                                                        |
      | newInputName | other_simple_input                                                  |
    Then an exception of type "alien4cloud.exception.AlreadyExistException" should be thrown

  Scenario: Rename an input with an invalid name should fail
    Given I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | simple_input                                                     |
      | propertyDefinition.type | string                                                           |
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.inputs.RenameInputOperation |
      | inputName    | simple_input                                                        |
      | newInputName | Simple input                                                        |
    Then an exception of type "alien4cloud.exception.InvalidNameException" should be thrown

  Scenario: Rename an input that does not exists should fail
    Given I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | Simple input                                                     |
      | propertyDefinition.type | string                                                           |
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.inputs.RenameInputOperation |
      | inputName    | Other Simple input                                                  |
      | newInputName | New Simple input                                                    |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Rename an input that is assigned to a node property should succeed
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
      | type         | org.alien4cloud.tosca.editor.operations.inputs.RenameInputOperation |
      | inputName    | component_version                                                   |
      | newInputName | the_component_version                                               |
    Then No exception should be thrown
    And The SPEL int expression "inputs.size()" should return 1
    And The SPEL expression "nodeTemplates['software_component'].properties['component_version'].function" should return "get_input"
    And The SPEL expression "nodeTemplates['software_component'].properties['component_version'].parameters[0]" should return "the_component_version"