Feature: Topology editor: rename policy

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Rename a policy template in a topology
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.policies.AddPolicyOperation |
      | policyName   | MyPolicy                                                            |
      | policyTypeId | tosca.policies.Placement:1.0.0-SNAPSHOT                             |
    When I execute the operation
      | type       | org.alien4cloud.tosca.editor.operations.policies.RenamePolicyOperation |
      | policyName | MyPolicy                                                               |
      | newName    | MyPolicy2                                                              |
    Then No exception should be thrown
    And The SPEL expression "policies.size()" should return 1
    And The SPEL expression "policies['MyPolicy']" should return "null"
    And The SPEL expression "policies['MyPolicy2'].type" should return "tosca.policies.Placement"

  Scenario: Rename a non existing policy template in an empty topology should fail
    When I execute the operation
      | type       | org.alien4cloud.tosca.editor.operations.policies.RenamePolicyOperation |
      | policyName | MyPolicy                                                               |
      | newName    | MyPolicy2                                                              |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Rename a non existing policy template in a topology should fail
    Given I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.policies.AddPolicyOperation |
      | policyName   | MyPolicy                                                            |
      | policyTypeId | tosca.policies.Placement:1.0.0-SNAPSHOT                             |
    When I execute the operation
      | type       | org.alien4cloud.tosca.editor.operations.policies.RenamePolicyOperation |
      | policyName | missingPolicy                                                          |
      | newName    | MyPolicy2                                                              |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Rename a policy template in a topology with an invalid name should fail
    Given I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.policies.AddPolicyOperation |
      | policyName   | MyPolicy                                                            |
      | policyTypeId | tosca.policies.Placement:1.0.0-SNAPSHOT                             |
    When I execute the operation
      | type       | org.alien4cloud.tosca.editor.operations.policies.RenamePolicyOperation |
      | policyName | MyPolicy                                                               |
      | newName    | MyPolicy!!!!                                                           |
    Then an exception of type "alien4cloud.exception.InvalidNameException" should be thrown
    And The SPEL expression "policies.size()" should return 1
    And The SPEL expression "policies['MyPolicy'].type" should return "tosca.policies.Placement"
