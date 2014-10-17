Feature: Delete cloud

Background:
  Given I am authenticated with "ADMIN" role
  And I upload a plugin

Scenario: Delete a cloud
  Given I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
  When I delete a cloud with name "Mount doom cloud"
  Then I should receive a RestResponse with no error
  When I list clouds
  Then I should receive a RestResponse with no error
    And Response should contains 0 cloud

Scenario: Delete a cloud that does not exist should fail
  When I delete a cloud with name "Mount doom cloud"
  Then I should receive a RestResponse with an error code 504
  When I list clouds
  Then I should receive a RestResponse with no error
    And Response should contains 0 cloud
