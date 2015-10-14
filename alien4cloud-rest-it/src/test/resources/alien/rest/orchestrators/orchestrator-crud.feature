Feature: Orchestrator management

# Login and make sure that we upload a plugin first
Background:
  Given I am authenticated with "ADMIN" role
  And I upload a plugin

Scenario: Create an orchestrator
  When I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"
  Then I should receive a RestResponse with no error
  When I list orchestrators
  Then I should receive a RestResponse with no error
  And Response should contains 1 orchestrator
  And Response should contains an orchestrator with name "Mount doom orchestrator"
  
Scenario: Create an orchestrator with existing name should fail
  When I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"
  And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"
  Then I should receive a RestResponse with an error code 502
  When I list orchestrators
  Then I should receive a RestResponse with no error
  And Response should contains 1 orchestrator
  And Response should contains an orchestrator with name "Mount doom orchestrator"
  
Scenario: Delete an orchestrator
  When I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"
  Then I should receive a RestResponse with no error
  When I delete an orchestrator with name "Mount doom orchestrator"
  Then I should receive a RestResponse with no error  
  When I list orchestrators
  Then I should receive a RestResponse with no error
  And Response should contains 0 orchestrator

Scenario: Delete an enabled orchestrator should fail
  When I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"
  When I enable the orchestrator "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
  When I delete an orchestrator with name "Mount doom orchestrator"
    Then I should receive a RestResponse with an error code 370
