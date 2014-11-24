Feature: CRUD operations on application environment

Background:
  Given I am authenticated with "ADMIN" role
  And I upload a plugin
  And I create a cloud with name "mock-paas-cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
  And I enable the cloud "mock-paas-cloud"
  And There are these users in the system
    | frodon |
  And I add a role "APPLICATIONS_MANAGER" to user "frodon"
  And I add a role "CLOUD_DEPLOYER" to user "frodon" on the resource type "CLOUD" named "mock-paas-cloud"
  And I am authenticated with user named "frodon"

Scenario: Create a new application environment for an application
  Given I create a new application with name "watchmiddleearth" and description "Use my great eye to find frodo and the ring."
  Then I should receive a RestResponse with no error
  And The application should have a user "frodon" having "APPLICATION_MANAGER" role
  And The RestResponse should contain an id string
  And the application can be found in ALIEN
  When I create an application environment of type "DEVELOPMENT" on cloud "mock-paas-cloud" with name "watchmiddleearth-env-mock" and description "Mock App Env" for the newly created application
  Then I should receive a RestResponse with no error

Scenario: Create a new application environment for an non existing/bad application must fail
  Given I create an application environment of type "DEVELOPMENT" on cloud "mock-paas-cloud" with name "watchmiddleearth-env-mock-2" and description "Mock App Env" for the newly created application
  Then I should receive a RestResponse with an error code 604

Scenario: Get an application environment from its id
  Given I create a new application with name "watchmiddleearth-1" and description "Use my great eye to find frodo and the ring."
  Then I should receive a RestResponse with no error
  And The application should have a user "frodon" having "APPLICATION_MANAGER" role
  And The RestResponse should contain an id string
  Given I create an application environment of type "DEVELOPMENT" on cloud "mock-paas-cloud" with name "watchmiddleearth-env-mock-1" and description "Mock App Env 1" for the newly created application
  Then I should receive a RestResponse with no error
  When I get the application environment from the registered application environment id
  Then I should receive a RestResponse with no error

Scenario: Update an application environment from its id
  Given I create a new application with name "watchmiddleearth-2" and description "Use my great eye to find frodo and the ring."
  Then I should receive a RestResponse with no error
  And The application should have a user "frodon" having "APPLICATION_MANAGER" role
  And The RestResponse should contain an id string
  Given I create an application environment of type "DEVELOPMENT" on cloud "mock-paas-cloud" with name "watchmiddleearth-env-mock-2" and description "Mock App Env 2" for the newly created application
  Then I should receive a RestResponse with no error
  When I update the created application environment with values
    | name         | watchmiddleearth-env-update name   |
    | description     | My description after update     |
    | environmentType   | INTEGRATION_TESTS           |
  Then I should receive a RestResponse with no error

Scenario: Update an non existing/bad application environment must fail
  When I update the created application environment with values
    | name       | watchmiddleearth-env-update name   |
    | description   | My description after update     |
  Then I should receive a RestResponse with an error code 604

Scenario: Delete an application environment from its id
  Given I create a new application with name "watchmiddleearth-3" and description "Use my great eye to find frodo and the ring."
  Then I should receive a RestResponse with no error
  And The application should have a user "frodon" having "APPLICATION_MANAGER" role
  And The RestResponse should contain an id string
  Given I create an application environment of type "DEVELOPMENT" on cloud "mock-paas-cloud" with name "watchmiddleearth-env-mock-3" and description "Mock App Env 3" for the newly created application
  Then I should receive a RestResponse with no error
  When I delete the registered application environment from its id
  Then I should receive a RestResponse with no error
