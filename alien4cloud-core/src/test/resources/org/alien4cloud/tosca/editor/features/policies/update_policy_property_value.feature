Feature: Topology editor: update policy property value

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Updating a scalar property value should succeed
    Given I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.policies.AddPolicyOperation |
      | policyName   | MyPolicy                                                            |
      | policyTypeId | org.alien4cloud.policies.Affinity:2.0.0-SNAPSHOT                    |
    When I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.policies.UpdatePolicyPropertyValueOperation |
      | policyName    | MyPolicy                                                                            |
      | propertyName  | level                                                                               |
      | propertyValue | zone                                                                                |
    Then No exception should be thrown
    And The SPEL expression "policies['MyPolicy'].properties['level'].value" should return "zone"

  Scenario: Updating a property value for a property that does not exists should fail
    Given I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.policies.AddPolicyOperation |
      | policyName   | MyPolicy                                                            |
      | policyTypeId | org.alien4cloud.policies.Affinity:2.0.0-SNAPSHOT                    |
    When I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.policies.UpdatePolicyPropertyValueOperation |
      | policyName    | MyPolicy                                                                            |
      | propertyName  | unknown_property                                                                    |
      | propertyValue | zone                                                                                |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Updating a scalar property value with an unmatched constraint should fail
    Given I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.policies.AddPolicyOperation |
      | policyName   | MyPolicy                                                            |
      | policyTypeId | org.alien4cloud.policies.Affinity:2.0.0-SNAPSHOT                    |
    When I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.policies.UpdatePolicyPropertyValueOperation |
      | policyName    | MyPolicy                                                                            |
      | propertyName  | level                                                                               |
      | propertyValue | pok                                                                                 |
    Then an exception of type "org.alien4cloud.tosca.editor.exception.PropertyValueException/org.alien4cloud.tosca.exceptions.ConstraintViolationException" should be thrown
