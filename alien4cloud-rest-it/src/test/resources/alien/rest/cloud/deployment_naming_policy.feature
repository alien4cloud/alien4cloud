Feature: Update cloud

Background:
  Given I am authenticated with "ADMIN" role
  And I upload a plugin
  Given I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"

Scenario: Update the naming policy of deployment
  When I list clouds
  Then I should receive a RestResponse with no error
    And Response should contains a cloud with deploymentNamePattern "environment.name + application.name"
  When I update deployment name pattern of "Mount doom cloud" to "environment.name"
    Then I should receive a RestResponse with no error
  When I list clouds
  Then I should receive a RestResponse with no error
    And Response should contains a cloud with deploymentNamePattern "environment.name"
    
Scenario: Deploy an application when the cloud as a wrong naming policy should failed
  When I update deployment name pattern of "Mount doom cloud" to "environment.name +"
    Then I should receive a RestResponse with no error
  When I list clouds
    Then I should receive a RestResponse with no error
    And Response should contains a cloud with deploymentNamePattern "environment.name +"
  And I enable the cloud "Mount doom cloud"
  Given I have an application with name "ALIEN"
  And I assign the cloud with name "Mount doom cloud" for the application
  When I deploy it
    Then I should receive a RestResponse with an error code 372
