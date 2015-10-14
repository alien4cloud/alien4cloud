Feature: Plugin management

# Login and make sure that we upload a plugin first
Background:
  Given I am authenticated with "ADMIN" role
  And I upload a plugin

Scenario: Create an orchestrator
  When I create an orchestrator named "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"
  Then I should receive a RestResponse with no error
  When I list orchestrators
  Then I should receive a RestResponse with no error
  And Response should contains 1 orchestrator
  And Response should contains an orchestrator with name "Mount doom cloud"
  
Scenario: Create an orchestrator with existing name should fail
  When I create an orchestrator named "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"
  And I create an orchestrator named "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"
  Then I should receive a RestResponse with an error code 502
  When I list orchestrators
  Then I should receive a RestResponse with no error
  And Response should contains 1 orchestrator
  And Response should contains an orchestrator with name "Mount doom cloud"
  
Scenario: Delete an orchestrator
  When I create an orchestrator named "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"
  Then I should receive a RestResponse with no error
  When I delete an orchestrator with name "Mount doom cloud"
  Then I should receive a RestResponse with no error  
  When I list orchestrators
  Then I should receive a RestResponse with no error
  And Response should contains 0 orchestrator