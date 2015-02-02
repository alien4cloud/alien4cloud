Feature: Updates operation on application environment

Background:
  Given I am authenticated with "ADMIN" role
  And I upload a plugin
  And I create a cloud with name "mock-paas-cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
  And I enable the cloud "mock-paas-cloud"
  And I create a cloud with name "mock-paas-cloud-second" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
  And I enable the cloud "mock-paas-cloud-second"
  And There are these users in the system
    | frodon |
  And I add a role "APPLICATIONS_MANAGER" to user "frodon"
  And I add a role "CLOUD_DEPLOYER" to user "frodon" on the resource type "CLOUD" named "mock-paas-cloud"
  And I create a new application with name "LAMP" and description "LAMP Stack application..." without errors
  And I must have an environment named "Environment" for application "LAMP"
  And I update the environment named "Environment" to use cloud "mock-paas-cloud" for application "LAMP"
  And I add a role "DEPLOYMENT_MANAGER" to user "frodon" on the resource type "ENVIRONMENT" named "Environment"
  
Scenario: Update an application environment from its id
  Given I create an application environment of type "DEVELOPMENT" on cloud "mock-paas-cloud" with name "LAMP-ENV" and description "LAMP Env 2" for the newly created application
  Then I should receive a RestResponse with no error
  When I update the application environment named "LAMP-ENV" with values
    | name         | watchmiddleearth-env-update name   |
    | description     | My description after update     |
    | environmentType   | INTEGRATION_TESTS           |
  Then I should receive a RestResponse with no error

Scenario: Update an non existing/bad application environment must fail
  When I update the application environment named "bad-environment-name" with values
    | name       | watchmiddleearth-env-update name   |
    | description   | My description after update     |
  Then I should receive a RestResponse with an error code 504

Scenario: Update cloud id for an environment when i have CLOUD_DEPLOYER role on the cloud must succeed
  Given I am authenticated with user named "frodon"
  And I update the application environment named "Environment" with values
    | cloudName   | mock-paas-cloud |
  Then I should receive a RestResponse with no error

Scenario: Update cloud id for an environment with no rights on the underlying cloud must fail
  Given I remove a role "CLOUD_DEPLOYER" to user "frodon" on the resource type "CLOUD" named "mock-paas-cloud"
  And I am authenticated with user named "frodon"
  When I update the application environment named "Environment" with values
    | cloudName   | mock-paas-cloud |
  Then I should receive a RestResponse with an error code 102

Scenario: Update cloud id for a deployed environment must fail
  Given I am authenticated with user named "frodon"
  When I deploy an application environment "Environment" for application "LAMP"
  And I have the environment "Environment" with status "DEPLOYED" for the application "LAMP"
  Then I should receive a RestResponse with no error
  When I update the environment named "Environment" to use cloud "mock-paas-cloud-second" for application "LAMP"
  Then I should receive a RestResponse with an error code 604