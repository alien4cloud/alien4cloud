Feature: Topology editor: add input

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology template

  Scenario: Adding a simple input should succeed
    When I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | Simple input                                                     |
      | propertyDefinition.type | string                                                           |
    Then No exception should be thrown
    And The SPEL int expression "inputs.size()" should return 1
    And The SPEL expression "inputs['Simple input'].type" should return "string"

  Scenario: Adding an input that already exists should fail
    Given I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | Simple input                                                     |
      | propertyDefinition.type | string                                                           |
    When I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | Simple input                                                     |
      | propertyDefinition.type | int                                                              |
    Then an exception of type "alien4cloud.exception.AlreadyExistException" should be thrown
