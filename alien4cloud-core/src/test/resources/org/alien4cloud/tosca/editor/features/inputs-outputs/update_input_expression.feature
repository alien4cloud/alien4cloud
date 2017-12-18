Feature: Topology editor: add input

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Adding a simple input should succeed
    When I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | simple_input                                                     |
      | propertyDefinition.type | string                                                           |
    Then No exception should be thrown
    And The SPEL expression "inputs.size()" should return 1
    And The SPEL expression "inputs['simple_input'].type" should return "string"

  Scenario: Update expression for an input should succeed
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | simple_input                                                     |
      | propertyDefinition.type | string                                                           |
    When I execute the operation
      | type       | org.alien4cloud.tosca.editor.operations.inputs.UpdateInputExpressionOperation |
      | name       | simple_input                                                                  |
      | expression | simple str value                                                              |
    Then No exception should be thrown

  Scenario: Update expression for an input that does not exist should fail
    When I execute the operation
      | type       | org.alien4cloud.tosca.editor.operations.inputs.UpdateInputExpressionOperation |
      | name       | simple_input                                                                  |
      | expression | simple str value                                                              |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown
