Feature: Topology editor: delete policy template

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Remove a policy template from a topology
    Given I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.policies.AddPolicyOperation |
      | policyName   | MyPolicy                                                            |
      | policyTypeId | tosca.policies.Placement:1.0.0-SNAPSHOT                             |
    When I execute the operation
      | type       | org.alien4cloud.tosca.editor.operations.policies.DeletePolicyOperation |
      | policyName | MyPolicy                                                               |
    Then No exception should be thrown
    And The SPEL expression "policies.size()" should return 0

  Scenario: Remove a non existing policy template from an empty topology should fail
    When I execute the operation
      | type       | org.alien4cloud.tosca.editor.operations.policies.DeletePolicyOperation |
      | policyName | missingPolicy                                                          |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Remove a non existing policy template from a topology should fail
    Given I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.policies.AddPolicyOperation |
      | policyName   | MyPolicy                                                            |
      | policyTypeId | tosca.policies.Placement:1.0.0-SNAPSHOT                             |
    When I execute the operation
      | type       | org.alien4cloud.tosca.editor.operations.policies.DeletePolicyOperation |
      | policyName | missingPolicy                                                          |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown
