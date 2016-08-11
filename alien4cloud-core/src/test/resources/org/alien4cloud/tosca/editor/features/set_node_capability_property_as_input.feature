Feature: Topology editor: set node capability property as input

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Set a node capability property to an input that matches type should succeed
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | software_component                                                    |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | max_instances                                                    |
      | propertyDefinition.type | int                                                              |
    When I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodeCapabilityPropertyAsInputOperation |
      | nodeName       | software_component                                                                                    |
      | capabilityName | scalable                                                                                              |
      | propertyName   | max_instances                                                                                         |
      | inputName      | max_instances                                                                                         |
    Then No exception should be thrown
    And The SPEL int expression "inputs.size()" should return 1
    And The SPEL expression "nodeTemplates['software_component'].properties['component_version'].function" should return "get_input"
    And The SPEL expression "nodeTemplates['software_component'].properties['component_version'].parameters[0]" should return "component_version"

  Scenario: Set a node capability property to an input that matches type should succeed
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | software_component                                                    |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | max_instances                                                    |
      | propertyDefinition.type | int                                                              |
    When I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodeCapabilityPropertyAsInputOperation |
      | nodeName       | software_component                                                                                    |
      | capabilityName | scalable                                                                                              |
      | propertyName   | max_instances                                                                                         |
      | inputName      | max_instances                                                                                         |
    Then No exception should be thrown
    And The SPEL int expression "inputs.size()" should return 1
    And The SPEL expression "nodeTemplates['software_component'].properties['component_version'].function" should return "get_input"
    And The SPEL expression "nodeTemplates['software_component'].properties['component_version'].parameters[0]" should return "component_version"

  Scenario: Set a node capability property to an input that does not exists should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | software_component                                                    |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    When I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodeCapabilityPropertyAsInputOperation |
      | nodeName       | software_component                                                                                    |
      | capabilityName | scalable                                                                                              |
      | propertyName   | component_version                                                                                     |
      | inputName      | component_version                                                                                     |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown
    And The SPEL expression "nodeTemplates['software_component'].properties['component_version']" should return "null"

  Scenario: Set a node capability property to a capability property that does not exists should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | software_component                                                    |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    When I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodeCapabilityPropertyAsInputOperation |
      | nodeName       | software_component                                                                                    |
      | capabilityName | scalable                                                                                              |
      | propertyName   | do_not_exist                                                                                          |
      | inputName      | component_version                                                                                     |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown
    And The SPEL expression "nodeTemplates['software_component'].properties['component_version']" should return "null"

  Scenario: Set a node capability property to a capability that does not exists should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | software_component                                                    |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    When I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodeCapabilityPropertyAsInputOperation |
      | nodeName       | software_component                                                                                    |
      | capabilityName | do_not_exist                                                                                          |
      | propertyName   | component_version                                                                                     |
      | inputName      | component_version                                                                                     |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown
    And The SPEL expression "nodeTemplates['software_component'].properties['component_version']" should return "null"