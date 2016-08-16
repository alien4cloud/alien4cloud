Feature: Topology editor: add relationship

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Adding a relationship to connect two compatible types should succeed
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Java                                                                  |
      | indexedNodeTypeId | fastconnect.nodes.Java:1.0                                            |
    When I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | Java                                                                                  |
      | relationshipName       | MyRelationship                                                                        |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0                                                                                   |
      | requirementName        | host                                                                                  |
      | target                 | Compute                                                                               |
      | targetedCapabilityName | host                                                                                  |
    Then No exception should be thrown
    And The SPEL int expression "nodeTemplates.size()" should return 2
    And The SPEL int expression "nodeTemplates['Java'].relationships.size()" should return 1

  Scenario: Adding a relationship that already exists should fail
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
      | relationshipType       | tosca.relationships.DependsOn                                                         |
      | relationshipVersion    | 1.0                                                                                   |
      | requirementName        | dependency                                                                            |
      | target                 | Compute                                                                               |
      | targetedCapabilityName | feature                                                                               |
    When I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | Java                                                                                  |
      | relationshipName       | MyRelationship                                                                        |
      | relationshipType       | tosca.relationships.DependsOn                                                         |
      | relationshipVersion    | 1.0                                                                                   |
      | requirementName        | dependency                                                                            |
      | target                 | Compute                                                                               |
      | targetedCapabilityName | feature                                                                               |
    Then an exception of type "alien4cloud.exception.AlreadyExistException" should be thrown

  Scenario: Adding a relationship without a name to connect two compatible types should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Java                                                                  |
      | indexedNodeTypeId | fastconnect.nodes.Java:1.0                                            |
    When I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | Java                                                                                  |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0                                                                                   |
      | requirementName        | host                                                                                  |
      | target                 | Compute                                                                               |
      | targetedCapabilityName | host                                                                                  |
    Then an exception of type "alien4cloud.exception.InvalidNameException" should be thrown

  Scenario: Adding a relationship with a missing type should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Java                                                                  |
      | indexedNodeTypeId | fastconnect.nodes.Java:1.0                                            |
    When I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | Java                                                                                  |
      | relationshipName       | MyRelationship                                                                        |
      | relationshipType       | tosca.relationships.MissingType                                                       |
      | relationshipVersion    | 1.0                                                                                   |
      | requirementName        | host                                                                                  |
      | target                 | Compute                                                                               |
      | targetedCapabilityName | host                                                                                  |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Adding a relationship on a missing source node should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Java                                                                  |
      | indexedNodeTypeId | fastconnect.nodes.Java:1.0                                            |
    When I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | missing node                                                                          |
      | relationshipName       | MyRelationship                                                                        |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0                                                                                   |
      | requirementName        | host                                                                                  |
      | target                 | Java                                                                                  |
      | targetedCapabilityName | host                                                                                  |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Adding a relationship on a missing source requirement node should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Java                                                                  |
      | indexedNodeTypeId | fastconnect.nodes.Java:1.0                                            |
    When I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | Compute                                                                               |
      | relationshipName       | MyRelationship                                                                        |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0                                                                                   |
      | requirementName        | missing requirement                                                                   |
      | target                 | Compute                                                                               |
      | targetedCapabilityName | host                                                                                  |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Adding a relationship on a missing target node should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Java                                                                  |
      | indexedNodeTypeId | fastconnect.nodes.Java:1.0                                            |
    When I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | Compute                                                                               |
      | relationshipName       | MyRelationship                                                                        |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0                                                                                   |
      | requirementName        | host                                                                                  |
      | target                 | missing target                                                                        |
      | targetedCapabilityName | host                                                                                  |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Adding a relationship on a missing target capability node should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Java                                                                  |
      | indexedNodeTypeId | fastconnect.nodes.Java:1.0                                            |
    When I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | Compute                                                                               |
      | relationshipName       | MyRelationship                                                                        |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0                                                                                   |
      | requirementName        | host                                                                                  |
      | target                 | Compute                                                                               |
      | targetedCapabilityName | missing capability                                                                    |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Adding a relationship on a missing target capability node should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Java                                                                  |
      | indexedNodeTypeId | fastconnect.nodes.Java:1.0                                            |
    When I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | Compute                                                                               |
      | relationshipName       | MyRelationship                                                                        |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0                                                                                   |
      | requirementName        | host                                                                                  |
      | target                 | Compute                                                                               |
      | targetedCapabilityName | missing capability                                                                    |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Adding a relationship causing the requirement bound to be reached should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Other_Compute                                                         |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Java                                                                  |
      | indexedNodeTypeId | fastconnect.nodes.Java:1.0                                            |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | Java                                                                               |
      | relationshipName       | MyRelationship                                                                        |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0                                                                                   |
      | requirementName        | host                                                                                  |
      | target                 | Compute                                                                               |
      | targetedCapabilityName | host                                                                                  |
    When I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | Java                                                                               |
      | relationshipName       | MyRelationship                                                                        |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0                                                                                   |
      | requirementName        | host                                                                                  |
      | target                 | Other_Compute                                                                         |
      | targetedCapabilityName | host                                                                                  |
    Then an exception of type "org.alien4cloud.tosca.editor.exception.RequirementBoundException" should be thrown