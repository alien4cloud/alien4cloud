Feature: Topology editor: add policy template

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Add a policy that exists in the repository should succeed
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.policies.AddPolicyOperation |
      | policyName   | MyPolicy                                                            |
      | policyTypeId | tosca.policies.Placement:1.0.0-SNAPSHOT                             |
    Then No exception should be thrown
    And The SPEL expression "policies.size()" should return 1
    And The SPEL expression "policies['MyPolicy'].type" should return "tosca.policies.Placement"

  Scenario: Add a policy that does not exists in the repository should fail
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.policies.AddPolicyOperation |
      | policyName   | MyPolicy                                                            |
      | policyTypeId | the.policy.that.does.not.Exists:1.0                                 |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Add a policy with an invalid name should fail
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.policies.AddPolicyOperation |
      | policyName   | MyPolicy!!!!                                                        |
      | policyTypeId | tosca.policies.Placement:1.0.0-SNAPSHOT                             |
    Then an exception of type "alien4cloud.exception.InvalidNameException" should be thrown

  Scenario: Add a policy with an existing name should fail
    Given I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.policies.AddPolicyOperation |
      | policyName   | MyPolicy                                                            |
      | policyTypeId | tosca.policies.Placement:1.0.0-SNAPSHOT                             |
    Then No exception should be thrown
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.policies.AddPolicyOperation |
      | policyName   | MyPolicy                                                            |
      | policyTypeId | tosca.policies.Placement:1.0.0-SNAPSHOT                             |
    Then an exception of type "alien4cloud.exception.AlreadyExistException" should be thrown