Feature: Updates operation on application environment

Background:
  Given I am authenticated with "ADMIN" role
  And There are these users in the system
    | frodon |
  And I add a role "APPLICATIONS_MANAGER" to user "frodon"
  And I create a new application with name "LAMP" and description "LAMP Stack application..." without errors
  And I must have an environment named "Environment" for application "LAMP"
  And I add a role "DEPLOYMENT_MANAGER" to user "frodon" on the resource type "ENVIRONMENT" named "Environment"
  
Scenario: Update an application environment from its id
  Given I create an application environment of type "DEVELOPMENT" with name "LAMP-ENV" and description "LAMP Env 2" for the newly created application
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

