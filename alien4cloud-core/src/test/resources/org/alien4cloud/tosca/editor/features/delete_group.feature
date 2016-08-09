Feature: Topology editor: delete group operation

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Adding a node to an unexisting group should create the group and succeed
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.groups.AddGroupMemberOperation |
      | nodeName  | Compute                                                                |
      | groupName | simple_group                                                           |
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.groups.DeleteGroupOperation |
      | groupName | simple_group                                                        |
    Then No exception should be thrown
    And The SPEL int expression "groups.size()" should return 0
    And The SPEL int expression "nodeTemplates['Compute'].groups.size()" should return 0

  Scenario: Deleting a group that does not exist should
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.groups.DeleteGroupOperation |
      | groupName | simple_group                                                        |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown
