Feature: Topology editor: rename group operation

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Renaming a group should succeed
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.groups.AddGroupMemberOperation |
      | nodeName  | Compute                                                                |
      | groupName | simple_group                                                           |
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.groups.RenameGroupOperation |
      | groupName    | simple_group                                                        |
      | newGroupName | new_simple_group                                                    |
    Then No exception should be thrown
    And The SPEL int expression "groups.size()" should return 1
    And The SPEL expression "groups['new_simple_group'].members[0]" should return "Compute"
    And The SPEL expression "nodeTemplates['Compute'].groups[0]" should return "new_simple_group"

  Scenario: Renaming an empty group should succeed
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.groups.AddGroupMemberOperation |
      | nodeName  | Compute                                                                |
      | groupName | simple_group                                                           |
    And I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.groups.RemoveGroupMemberOperation |
      | nodeName  | Compute                                                                   |
      | groupName | simple_group                                                              |
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.groups.RenameGroupOperation |
      | groupName    | simple_group                                                        |
      | newGroupName | new_simple_group                                                    |
    Then No exception should be thrown
    And The SPEL int expression "groups.size()" should return 1
    And The SPEL expression "groups['new_simple_group'].members.size()" should return "0"

  Scenario: Renaming a group when no groups exists should fail
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.groups.RenameGroupOperation |
      | groupName    | simple_group                                                        |
      | newGroupName | new_simple_group                                                    |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Renaming a group that does not exists should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.groups.AddGroupMemberOperation |
      | nodeName  | Compute                                                                |
      | groupName | simple_group                                                           |
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.groups.RenameGroupOperation |
      | groupName    | another_simple_group                                                |
      | newGroupName | new_another_simple_group                                            |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Renaming a group with a name that already exists should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.groups.AddGroupMemberOperation |
      | nodeName  | Compute                                                                |
      | groupName | simple_group                                                           |
    And I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.groups.AddGroupMemberOperation |
      | nodeName  | Compute                                                                |
      | groupName | other_simple_group                                                     |
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.groups.RenameGroupOperation |
      | groupName    | simple_group                                                        |
      | newGroupName | other_simple_group                                                  |
    Then an exception of type "alien4cloud.exception.AlreadyExistException" should be thrown

  Scenario: Renaming a group with the same name should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.groups.AddGroupMemberOperation |
      | nodeName  | Compute                                                                |
      | groupName | simple_group                                                           |
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.groups.RenameGroupOperation |
      | groupName    | simple_group                                                        |
      | newGroupName | simple_group                                                        |
    Then an exception of type "alien4cloud.exception.AlreadyExistException" should be thrown

  Scenario: Renaming a group with an invalid name should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.groups.AddGroupMemberOperation |
      | nodeName  | Compute                                                                |
      | groupName | simple_group                                                           |
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.groups.RenameGroupOperation |
      | groupName    | simple_group                                                        |
      | newGroupName | simple group                                                        |
    Then an exception of type "alien4cloud.exception.InvalidNameException" should be thrown