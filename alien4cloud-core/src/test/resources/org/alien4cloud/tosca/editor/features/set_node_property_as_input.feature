Feature: Topology editor: set node property as input

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Set a node property to an input that matches type should succeed
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
    Then No exception should be thrown
    And The SPEL int expression "inputs.size()" should return 1
    And The SPEL expression "nodeTemplates['software_component'].properties['component_version'].function" should return "get_input"
    And The SPEL expression "nodeTemplates['software_component'].properties['component_version'].parameters[0]" should return "component_version"

  Scenario: Set a node property to an input that does not match type should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | software_component                                                    |
      | indexedNodeTypeId | tosca.nodes.SoftwareComponent:1.0.0-SNAPSHOT                          |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | component_version                                                |
      | propertyDefinition.type | int                                                              |
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodePropertyAsInputOperation |
      | nodeName     | software_component                                                                          |
      | propertyName | component_version                                                                           |
      | inputName    | component_version                                                                           |
    Then an exception of type "alien4cloud.model.components.IncompatiblePropertyDefinitionException" should be thrown
    And The SPEL expression "nodeTemplates['software_component'].properties['component_version']" should return "null"

  Scenario: Set a node property to an input that does not exists should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | software_component                                                    |
      | indexedNodeTypeId | tosca.nodes.SoftwareComponent:1.0.0-SNAPSHOT                          |
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodePropertyAsInputOperation |
      | nodeName     | software_component                                                                          |
      | propertyName | component_version                                                                           |
      | inputName    | component_version                                                                           |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown
    And The SPEL expression "nodeTemplates['software_component'].properties['component_version']" should return "null"

  Scenario: Set a node capability property to a property that does not exists should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | software_component                                                    |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodePropertyAsInputOperation |
      | nodeName     | software_component                                                                          |
      | propertyName | do_not_exists                                                                               |
      | inputName    | component_version                                                                           |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown
    And The SPEL expression "nodeTemplates['software_component'].properties['component_version']" should return "null"
