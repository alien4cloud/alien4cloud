Feature: Topology editor: unset node capability property as input

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Unset a node capability property to an input that matches type should succeed
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | compute_node                                                          |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | max_instances                                                    |
      | propertyDefinition.type | integer                                                          |
    When I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodeCapabilityPropertyAsInputOperation |
      | nodeName       | compute_node                                                                                          |
      | capabilityName | scalable                                                                                              |
      | propertyName   | max_instances                                                                                         |
      | inputName      | max_instances                                                                                         |
    When I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.UnsetNodeCapabilityPropertyAsInputOperation |
      | nodeName       | compute_node                                                                                            |
      | capabilityName | scalable                                                                                                |
      | propertyName   | max_instances                                                                                           |
    Then No exception should be thrown
    And The SPEL int expression "inputs.size()" should return 1
    And The SPEL expression "nodeTemplates['compute_node'].capabilities['scalable'].properties['max_instances'].value" should return "1"

  Scenario: Unset a node capability property as input should fail if the capability property is not set as input
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | compute_node                                                          |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | max_instances                                                    |
      | propertyDefinition.type | integer                                                          |
    When I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.UnsetNodeCapabilityPropertyAsInputOperation |
      | nodeName       | compute_node                                                                                            |
      | capabilityName | scalable                                                                                                |
      | propertyName   | max_instances                                                                                           |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown
    And The SPEL int expression "inputs.size()" should return 1

  Scenario: Unset a node capability property as input should fail if the property does not exists
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | compute_node                                                          |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | max_instances                                                    |
      | propertyDefinition.type | integer                                                          |
    When I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodeCapabilityPropertyAsInputOperation |
      | nodeName       | compute_node                                                                                          |
      | capabilityName | scalable                                                                                              |
      | propertyName   | max_instances                                                                                         |
      | inputName      | max_instances                                                                                         |
    When I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.UnsetNodeCapabilityPropertyAsInputOperation |
      | nodeName       | compute_node                                                                                            |
      | capabilityName | scalable                                                                                                |
      | propertyName   | does_not_exists                                                                                         |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown
    And The SPEL int expression "inputs.size()" should return 1
    And The SPEL expression "nodeTemplates['compute_node'].capabilities['scalable'].properties['max_instances'].function" should return "get_input"
    And The SPEL expression "nodeTemplates['compute_node'].capabilities['scalable'].properties['max_instances'].parameters[0]" should return "max_instances"

  Scenario: Unset a node capability property as input should fail if the capability does not exists
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | compute_node                                                          |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | max_instances                                                    |
      | propertyDefinition.type | integer                                                          |
    When I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodeCapabilityPropertyAsInputOperation |
      | nodeName       | compute_node                                                                                          |
      | capabilityName | scalable                                                                                              |
      | propertyName   | max_instances                                                                                         |
      | inputName      | max_instances                                                                                         |
    When I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.UnsetNodeCapabilityPropertyAsInputOperation |
      | nodeName       | compute_node                                                                                            |
      | capabilityName | does_not_exists                                                                                         |
      | propertyName   | max_instances                                                                                           |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown
    And The SPEL int expression "inputs.size()" should return 1
    And The SPEL expression "nodeTemplates['compute_node'].capabilities['scalable'].properties['max_instances'].function" should return "get_input"
    And The SPEL expression "nodeTemplates['compute_node'].capabilities['scalable'].properties['max_instances'].parameters[0]" should return "max_instances"

  Scenario: Unset a node capability property as input should fail if the node does not exists
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | compute_node                                                          |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | max_instances                                                    |
      | propertyDefinition.type | integer                                                          |
    When I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodeCapabilityPropertyAsInputOperation |
      | nodeName       | compute_node                                                                                          |
      | capabilityName | scalable                                                                                              |
      | propertyName   | max_instances                                                                                         |
      | inputName      | max_instances                                                                                         |
    When I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.UnsetNodeCapabilityPropertyAsInputOperation |
      | nodeName       | does_not_exists                                                                                         |
      | capabilityName | scalable                                                                                                |
      | propertyName   | max_instances                                                                                           |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown
    And The SPEL int expression "inputs.size()" should return 1
    And The SPEL expression "nodeTemplates['compute_node'].capabilities['scalable'].properties['max_instances'].function" should return "get_input"
    And The SPEL expression "nodeTemplates['compute_node'].capabilities['scalable'].properties['max_instances'].parameters[0]" should return "max_instances"
