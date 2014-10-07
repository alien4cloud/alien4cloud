Feature: Enable cloud

Background:
  Given I am authenticated with "ADMIN" role
  And I upload a plugin
  And I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"

Scenario: Enable a cloud should work
  Given I disable cloud "Mount doom cloud"
  When I enable "Mount doom cloud"
  Then I should receive a RestResponse with no error
  When I list clouds
  Then I should receive a RestResponse with no error
    And Response should contains a cloud with name "Mount doom cloud"
    And Response should contains a cloud with state enabled "true"

Scenario: Enable a cloud that is already enabled should not fail
  When I enable "Mount doom cloud"
  Then I should receive a RestResponse with no error
  When I list clouds
  Then I should receive a RestResponse with no error
    And Response should contains a cloud with name "Mount doom cloud"
    And Response should contains a cloud with state enabled "true"
