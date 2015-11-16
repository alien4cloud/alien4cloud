Feature: Update orchestrator

Background:
  Given I am authenticated with "ADMIN" role
  And I upload a plugin
  And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"

Scenario: Update an orchestrator's name
  When I update orchestrator name from "Mount doom orchestrator" to "Mordor orchestrator"
    Then I should receive a RestResponse with no error
  When I list orchestrators
    Then I should receive a RestResponse with no error
    And Response should contains an orchestrator with name "Mordor orchestrator"

Scenario: Update an orchestrator's name with same name should not fail, just ignored
  When I update orchestrator name from "Mount doom orchestrator" to "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
  When I list orchestrators
  Then I should receive a RestResponse with no error
    And Response should contains an orchestrator with name "Mount doom orchestrator"

Scenario: Update an orchestrator's name with existing name should fail
  When I create an orchestrator named "Mordor orchestrator" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"
  And I update orchestrator name from "Mount doom orchestrator" to "Mordor orchestrator"
    Then I should receive a RestResponse with an error code 502
  When I list orchestrators
    Then I should receive a RestResponse with no error
    And Response should contains 2 orchestrator
    And Response should contains an orchestrator with name "Mordor orchestrator"
    And Response should contains an orchestrator with name "Mount doom orchestrator"