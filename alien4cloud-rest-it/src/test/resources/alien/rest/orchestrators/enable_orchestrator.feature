Feature: Enable orchestrator

Background:
  Given I am authenticated with "ADMIN" role
  And I upload a plugin
  And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"

Scenario: Enable an orchestrator should work
  When I get the orchestrator named "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
    And Response should contains the orchestrator with name "Mount doom orchestrator" and state enabled "false"
  When I enable the orchestrator "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
  When I get the orchestrator named "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
    And Response should contains the orchestrator with name "Mount doom orchestrator" and state enabled "true"

Scenario: Enable an orchestrator that is already enabled should fail
  When I enable the orchestrator "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
  When I enable the orchestrator "Mount doom orchestrator"
    Then I should receive a RestResponse with an error code 502
    
Scenario: Disable an enabled orchestrator should not fail
  When I enable the orchestrator "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
  When I get the orchestrator named "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
    And Response should contains the orchestrator with name "Mount doom orchestrator" and state enabled "true"
  When I disable "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
  When I get the orchestrator named "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
    And Response should contains the orchestrator with name "Mount doom orchestrator" and state enabled "false"
