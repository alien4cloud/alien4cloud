Feature: Un-Deploy an application

Background:
  Given I am authenticated with "ADMIN" role
  And I upload a plugin
  And I create a cloud with name "mock cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
  And I enable the cloud "mock cloud"
  And There are these users in the system
    | sangoku |
  And I add a role "APPLICATIONS_MANAGER" to user "sangoku"
  And I add a role "CLOUD_DEPLOYER" to user "sangoku" on the resource type "CLOUD" named "mock cloud"
  And I am authenticated with user named "sangoku"

Scenario: Create 1 application, deploy it, check statuses, undeploy it and check statuses
  Given I have applications with names and descriptions
    | The great eye | This application should be in DEPLOYED status...  |
  When I can get applications statuses
   And I deploy all applications with cloud "mock cloud"
   And I have expected applications statuses for "deployment" operation
    | The great eye | DEPLOYED |
   And I undeploy all environments for applications
   And I should receive a RestResponse with no error
   And I have expected applications statuses for "undeployment" operation
    | The great eye | UNDEPLOYED |
   When I ask for detailed deployments for cloud "mock cloud"
   Then I should receive a RestResponse with no error
   And the response should contains 1 deployments DTO and applications with an end date set
   | The great eye |
#   And I should not get a deployment if I ask one for application "The great eye" on cloud "mock cloud"
#   And I should get a deployment with an end date not null if I ask one for application "The great eye" on cloud "mock cloud"
