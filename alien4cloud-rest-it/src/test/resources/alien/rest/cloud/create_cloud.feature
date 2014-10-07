Feature: Create cloud

Background:
  Given I am authenticated with "ADMIN" role
  And I upload a plugin

Scenario: Create a cloud
  When I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
  Then I should receive a RestResponse with no error
  When I list clouds
  Then I should receive a RestResponse with no error
  And Response should contains 1 cloud
  And Response should contains a cloud with name "Mount doom cloud"

Scenario: Create a cloud with existing name should fail
  When I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
  And I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
  Then I should receive a RestResponse with an error code 502
  When I list clouds
  Then I should receive a RestResponse with no error
  And Response should contains 1 cloud
  And Response should contains a cloud with name "Mount doom cloud"
 
