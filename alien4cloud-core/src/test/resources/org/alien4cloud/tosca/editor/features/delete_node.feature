Feature: Topology editor: delete node template

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Remove a node template from a topology
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Template1                                                             |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type     | org.alien4cloud.tosca.editor.operations.nodetemplate.DeleteNodeOperation |
      | nodeName | Template1                                                                |
    Then No exception should be thrown
    And The SPEL int expression "nodeTemplates.size()" should return 0

  Scenario: Remove a non existing node template from an empty topology should fail
    When I execute the operation
      | type     | org.alien4cloud.tosca.editor.operations.nodetemplate.DeleteNodeOperation |
      | nodeName | missingNode                                                              |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Remove a non existing node template from a topology should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Template1                                                             |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type     | org.alien4cloud.tosca.editor.operations.nodetemplate.DeleteNodeOperation |
      | nodeName | missingNode                                                              |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Remove a node template being a source of a relationship from a topology should succeed
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Template1                                                             |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Template2                                                             |
      | indexedNodeTypeId | fastconnect.nodes.Java:1.0                                            |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | Template2                                                                             |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0                                                                                   |
      | requirementName        | host                                                                                  |
      | target                 | Template1                                                                             |
      | targetedCapabilityName | compute                                                                               |
    Given I execute the operation
      | type     | org.alien4cloud.tosca.editor.operations.nodetemplate.DeleteNodeOperation |
      | nodeName | missingNode                                                              |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Remove a node template being a target of a relationship from a topology should succeed
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Template1                                                             |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |

  Scenario: Remove a node template used in a group should succeed and remove the node from the group
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    And I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.groups.AddGroupMemberOperation |
      | nodeName  | Compute                                                                |
      | groupName | simple_group                                                           |
    Given I execute the operation
      | type     | org.alien4cloud.tosca.editor.operations.nodetemplate.DeleteNodeOperation |
      | nodeName | Compute                                                                  |
    Then No exception should be thrown
    And The SPEL int expression "groups.size()" should return 1
    And The SPEL int expression "nodeTemplates.size()" should return 0