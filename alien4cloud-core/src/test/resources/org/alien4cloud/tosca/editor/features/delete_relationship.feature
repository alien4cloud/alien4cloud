Feature: Topology editor: delete relationship

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Deleting a relationship should succeed
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
      | type             | org.alien4cloud.tosca.editor.operations.relationshiptemplate.DeleteRelationshipOperation |
      | nodeName         | Java                                                                                     |
      | relationshipName | MyRelationship                                                                           |
    Then No exception should be thrown
    And The SPEL int expression "nodeTemplates.size()" should return 2
    And The SPEL int expression "nodeTemplates['Java'].relationships.size()" should return 0

  Scenario: Deleting a relationship that does not exits should fail
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
      | type             | org.alien4cloud.tosca.editor.operations.relationshiptemplate.DeleteRelationshipOperation |
      | nodeName         | Java                                                                                     |
      | relationshipName | Missing Relationship                                                                     |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown