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

Scenario: Delete a cloud should be deleted reference of this cloud in all dependent application environment
  Given I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
  And I create a new application with name "LAMP" and description "LAMP Stack application..." without errors
  And I must have an environment named "Environment" for application "LAMP"
  And I update the environment named "Environment" to use cloud "Mount doom cloud" for application "LAMP"
  When I delete a cloud with name "Mount doom cloud"
  Then I should receive a RestResponse with no error
  When I check if an application environment of "LAMP" do not have cloudId
    Then I should receive a RestResponse with no error
