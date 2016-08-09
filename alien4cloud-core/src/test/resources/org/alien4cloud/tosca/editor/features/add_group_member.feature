Feature: Topology editor: add group member operation

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Adding a node to an unexisting group should create the group and succeed
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.groups.AddGroupMemberOperation |
      | nodeName  | Compute                                                                |
      | groupName | simple_group                                                           |
    Then No exception should be thrown
    And The SPEL int expression "groups.size()" should return 1
    And The SPEL expression "groups['simple_group'].members[0]" should return "Compute"
    And The SPEL expression "nodeTemplates['Compute'].groups[0]" should return "simple_group"

  Scenario: Adding a node that doesn't exists to a group should fail
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.groups.AddGroupMemberOperation |
      | nodeName  | Compute                                                                |
      | groupName | simple_group                                                           |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Adding a node that is already in the group should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.groups.AddGroupMemberOperation |
      | nodeName  | Compute                                                                |
      | groupName | simple_group                                                           |
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.groups.AddGroupMemberOperation |
      | nodeName  | Compute                                                                |
      | groupName | simple_group                                                           |
    Then an exception of type "alien4cloud.exception.AlreadyExistException" should be thrown

  Scenario: Adding a node to a new group with an invalid name should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.groups.AddGroupMemberOperation |
      | nodeName  | Compute                                                                |
      | groupName | simple group                                                           |
    Then an exception of type "alien4cloud.exception.InvalidNameException" should be thrown