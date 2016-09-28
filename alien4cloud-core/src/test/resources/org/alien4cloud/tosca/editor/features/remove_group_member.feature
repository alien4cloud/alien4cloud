Feature: Topology editor: remove group member operation

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Removing a node from a group when the node was the only member should succeed
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.groups.AddGroupMemberOperation |
      | nodeName  | Compute                                                                |
      | groupName | simple_group                                                           |
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.groups.RemoveGroupMemberOperation |
      | nodeName  | Compute                                                                   |
      | groupName | simple_group                                                              |
    Then No exception should be thrown
    And The SPEL expression "groups.size()" should return 1
    And The SPEL expression "nodeTemplates['Compute'].groups.size()" should return 0

  Scenario: Removing a node from a group that has other member should succeed
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute_2                                                             |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.groups.AddGroupMemberOperation |
      | nodeName  | Compute                                                                |
      | groupName | simple_group                                                           |
    And I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.groups.AddGroupMemberOperation |
      | nodeName  | Compute_2                                                              |
      | groupName | simple_group                                                           |
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.groups.RemoveGroupMemberOperation |
      | nodeName  | Compute                                                                   |
      | groupName | simple_group                                                              |
    Then No exception should be thrown
    And The SPEL expression "groups.size()" should return 1
    And The SPEL expression "groups['simple_group'].members[0]" should return "Compute_2"
    And The SPEL expression "nodeTemplates['Compute'].groups.size()" should return 0
    And The SPEL expression "nodeTemplates['Compute_2'].groups[0]" should return "simple_group"

  Scenario: Removing a node from a group when the node is not a member should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute_2                                                             |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.groups.AddGroupMemberOperation |
      | nodeName  | Compute                                                                |
      | groupName | simple_group                                                           |
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.groups.RemoveGroupMemberOperation |
      | nodeName  | Compute_2                                                                 |
      | groupName | simple_group                                                              |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Removing a node from a group that does not exists should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.groups.RemoveGroupMemberOperation |
      | nodeName  | Compute_2                                                                 |
      | groupName | simple_group                                                              |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown
