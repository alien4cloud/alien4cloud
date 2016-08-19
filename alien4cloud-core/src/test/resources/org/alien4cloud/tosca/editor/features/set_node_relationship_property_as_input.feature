Feature: Topology editor: set node relationship property as input

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Set a node relationship property to an input that matches type should succeed
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Java                                                                  |
      | indexedNodeTypeId | fastconnect.nodes.Java:1.0                                            |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | Java                                                                                  |
      | relationshipName       | MyRelationship                                                                        |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0                                                                                   |
      | requirementName        | host                                                                                  |
      | target                 | Compute                                                                               |
      | targetedCapabilityName | host                                                                                  |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | password                                                         |
      | propertyDefinition.type | string                                                           |
    When I execute the operation
      | type             | org.alien4cloud.tosca.editor.operations.relationshiptemplate.inputs.SetRelationshipPropertyAsInputOperation |
      | nodeName         | Java                                                                                                        |
      | relationshipName | MyRelationship                                                                                              |
      | propertyName     | fake_password                                                                                               |
      | inputName        | password                                                                                                    |
    Then No exception should be thrown
    And The SPEL int expression "inputs.size()" should return 1
    And The SPEL expression "nodeTemplates['Java'].relationships['MyRelationship'].properties['fake_password'].function" should return "get_input"
    And The SPEL expression "nodeTemplates['Java'].relationships['MyRelationship'].properties['fake_password'].parameters[0]" should return "password"

  Scenario: Set a node relationship property to an input that does not exists should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Java                                                                  |
      | indexedNodeTypeId | fastconnect.nodes.Java:1.0                                            |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | Java                                                                                  |
      | relationshipName       | MyRelationship                                                                        |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0                                                                                   |
      | requirementName        | host                                                                                  |
      | target                 | Compute                                                                               |
      | targetedCapabilityName | host                                                                                  |
    When I execute the operation
      | type             | org.alien4cloud.tosca.editor.operations.relationshiptemplate.inputs.SetRelationshipPropertyAsInputOperation |
      | nodeName         | Java                                                                                                        |
      | relationshipName | MyRelationship                                                                                              |
      | propertyName     | fake_password                                                                                               |
      | inputName        | password                                                                                                    |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown
    And The SPEL expression "nodeTemplates['Java'].relationships['MyRelationship'].properties['fake_password']" should return "null"

  Scenario: Set a node relationship property to a relationship property that does not exists should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Java                                                                  |
      | indexedNodeTypeId | fastconnect.nodes.Java:1.0                                            |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | Java                                                                                  |
      | relationshipName       | MyRelationship                                                                        |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0                                                                                   |
      | requirementName        | host                                                                                  |
      | target                 | Compute                                                                               |
      | targetedCapabilityName | host                                                                                  |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | password                                                         |
      | propertyDefinition.type | string                                                           |
    When I execute the operation
      | type             | org.alien4cloud.tosca.editor.operations.relationshiptemplate.inputs.SetRelationshipPropertyAsInputOperation |
      | nodeName         | Java                                                                                                        |
      | relationshipName | MyRelationship                                                                                              |
      | propertyName     | does_not_exists                                                                                               |
      | inputName        | password                                                                                                    |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown
    And The SPEL expression "nodeTemplates['Java'].relationships['MyRelationship'].properties['fake_password']" should return "null"

  Scenario: Set a node relationship property to a relationship that does not exists should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Java                                                                  |
      | indexedNodeTypeId | fastconnect.nodes.Java:1.0                                            |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | Java                                                                                  |
      | relationshipName       | MyRelationship                                                                        |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0                                                                                   |
      | requirementName        | host                                                                                  |
      | target                 | Compute                                                                               |
      | targetedCapabilityName | host                                                                                  |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | password                                                         |
      | propertyDefinition.type | string                                                           |
    When I execute the operation
      | type             | org.alien4cloud.tosca.editor.operations.relationshiptemplate.inputs.SetRelationshipPropertyAsInputOperation |
      | nodeName         | Java                                                                                                        |
      | relationshipName | does_not_exists                                                                                              |
      | propertyName     | fake_password                                                                                               |
      | inputName        | password                                                                                                    |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown
    And The SPEL expression "nodeTemplates['Java'].relationships['MyRelationship'].properties['fake_password']" should return "null"

  Scenario: Set a node relationship property to a node that does not exists should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Java                                                                  |
      | indexedNodeTypeId | fastconnect.nodes.Java:1.0                                            |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | Java                                                                                  |
      | relationshipName       | MyRelationship                                                                        |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0                                                                                   |
      | requirementName        | host                                                                                  |
      | target                 | Compute                                                                               |
      | targetedCapabilityName | host                                                                                  |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | password                                                         |
      | propertyDefinition.type | string                                                           |
    When I execute the operation
      | type             | org.alien4cloud.tosca.editor.operations.relationshiptemplate.inputs.SetRelationshipPropertyAsInputOperation |
      | nodeName         | does_not_exists                                                                                                        |
      | relationshipName | MyRelationship                                                                                              |
      | propertyName     | fake_password                                                                                               |
      | inputName        | password                                                                                                    |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown
    And The SPEL expression "nodeTemplates['Java'].relationships['MyRelationship'].properties['fake_password']" should return "null"
