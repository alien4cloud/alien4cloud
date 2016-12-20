Feature: Update orchestrator

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"

  @reset
  Scenario: Update an orchestrator's name
    When I update orchestrator name from "Mount doom orchestrator" to "Mordor orchestrator"
      Then I should receive a RestResponse with no error
    When I list orchestrators
      Then I should receive a RestResponse with no error
      And Response should contains an orchestrator with name "Mordor orchestrator"

  @reset
  Scenario: Update an orchestrator's name with same name should not fail, just ignored
    When I update orchestrator name from "Mount doom orchestrator" to "Mount doom orchestrator"
      Then I should receive a RestResponse with no error
    When I list orchestrators
    Then I should receive a RestResponse with no error
      And Response should contains an orchestrator with name "Mount doom orchestrator"

  @reset
  Scenario: Update an orchestrator's name with existing name should fail
    When I create an orchestrator named "Mordor orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I update orchestrator name from "Mount doom orchestrator" to "Mordor orchestrator"
      Then I should receive a RestResponse with an error code 502
    When I list orchestrators
      Then I should receive a RestResponse with no error
      And Response should contains 2 orchestrator
      And Response should contains an orchestrator with name "Mordor orchestrator"
      And Response should contains an orchestrator with name "Mount doom orchestrator"

  @reset
  Scenario: Update orchestrator properties or reset a value should work
    When I create an orchestrator named "Mordor orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I update orchestrator "Mordor orchestrator"'s configuration property "firstArgument" to "test1"
    And I update orchestrator "Mordor orchestrator"'s configuration property "secondArgument" to "test2"
    Then I should receive a RestResponse with no error
    And The orchestrator configuration should contains the property "firstArgument" with value "test1"
    And The orchestrator configuration should contains the property "secondArgument" with value "test2"
    When I update orchestrator "Mordor orchestrator"'s configuration property "firstArgument" to "null"
    Then I should receive a RestResponse with no error
    And The orchestrator configuration should contains the property "firstArgument" with value "null"
    And The orchestrator configuration should contains the property "secondArgument" with value "test2"