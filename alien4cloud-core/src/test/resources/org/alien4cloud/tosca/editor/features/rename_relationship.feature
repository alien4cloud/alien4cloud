Feature: Topology editor: rename relationship

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Rename a relationship template in a topology
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
      | type                | org.alien4cloud.tosca.editor.operations.relationshiptemplate.RenameRelationshipOperation |
      | nodeName            | Java                                                                                     |
      | relationshipName    | MyRelationship                                                                           |
      | newRelationshipName | MyRenamedRelationship                                                                    |
    Then No exception should be thrown
    And The SPEL int expression "nodeTemplates.size()" should return 2
    And The SPEL int expression "nodeTemplates['Java'].relationships.size()" should return 1
    And The SPEL expression "nodeTemplates['Java'].relationships['MyRelationship']" should return "null"
    And The SPEL expression "nodeTemplates['Java'].relationships['MyRenamedRelationship'].type" should return "tosca.relationships.HostedOn"

  Scenario: Rename a non existing relationship template in an empty topology should fail
    When I execute the operation
      | type                | org.alien4cloud.tosca.editor.operations.relationshiptemplate.RenameRelationshipOperation |
      | nodeName            | Java                                                                                     |
      | relationshipName    | MyRelationship                                                                           |
      | newRelationshipName | MyRenamedRelationship                                                                    |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Rename a non existing relationship on an existing node in a topology should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Template1                                                             |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type                | org.alien4cloud.tosca.editor.operations.relationshiptemplate.RenameRelationshipOperation |
      | nodeName            | Template1                                                                                |
      | relationshipName    | MyRelationship                                                                           |
      | newRelationshipName | MyRenamedRelationship                                                                    |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Rename a relationship template in a topology with a name that already exists should fail
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
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | Java                                                                                  |
      | relationshipName       | MyDependsRelationship                                                                 |
      | relationshipType       | tosca.relationships.DependsOn                                                         |
      | relationshipVersion    | 1.0                                                                                   |
      | requirementName        | dependency                                                                            |
      | target                 | Compute                                                                               |
      | targetedCapabilityName | feature                                                                               |
    When I execute the operation
      | type                | org.alien4cloud.tosca.editor.operations.relationshiptemplate.RenameRelationshipOperation |
      | nodeName            | Java                                                                                     |
      | relationshipName    | MyRelationship                                                                           |
      | newRelationshipName | MyDependsRelationship                                                                    |
    Then an exception of type "alien4cloud.exception.AlreadyExistException" should be thrown

  Scenario: Rename a relationship template in a topology with an empy name should fail
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
      | type                | org.alien4cloud.tosca.editor.operations.relationshiptemplate.RenameRelationshipOperation |
      | nodeName            | Java                                                                                     |
      | relationshipName    | MyRelationship                                                                           |
      | newRelationshipName |                                                                                          |
    Then an exception of type "alien4cloud.exception.InvalidNameException" should be thrown

  Scenario: Rename a relationship template in a topology with a null name should fail
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
      | type                | org.alien4cloud.tosca.editor.operations.relationshiptemplate.RenameRelationshipOperation |
      | nodeName            | Java                                                                                     |
      | relationshipName    | MyRelationship                                                                           |
    Then an exception of type "alien4cloud.exception.InvalidNameException" should be thrown