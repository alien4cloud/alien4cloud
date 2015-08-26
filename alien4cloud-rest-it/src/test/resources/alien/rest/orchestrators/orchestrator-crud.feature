Feature: Plugin management

# Login and make sure that we upload a plugin first
Background:
  Given I am authenticated with "ADMIN" role
  And I upload a plugin

#
Scenario: Create an orchestrator
  Given I create an orchestrator named "orchestrator" using plugin
  Then I should receive a RestResponse with no error

Scenario: Create a cloud
  When I create an orchestrator named "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
  Then I should receive a RestResponse with no error
  When I list clouds
  Then I should receive a RestResponse with no error
  And Response should contains 1 cloud
  And Response should contains a cloud with name "Mount doom cloud"