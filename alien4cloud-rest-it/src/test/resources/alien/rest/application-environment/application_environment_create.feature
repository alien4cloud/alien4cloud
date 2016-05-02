Feature: Create / Delete operations on application environment

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | frodon |
    And I add a role "APPLICATIONS_MANAGER" to user "frodon"
    And I am authenticated with user named "frodon"

  @reset
  Scenario: Create a new application environment for an application
    Given I create a new application with name "watchmiddleearth" and description "Use my great eye to find frodo and the ring."
    Then I should receive a RestResponse with no error
    And The application should have a user "frodon" having "APPLICATION_MANAGER" role
    And The RestResponse should contain an id string
    And the application can be found in ALIEN
    When I create an application environment of type "DEVELOPMENT" with name "watchmiddleearth-env-mock" and description "Mock App Env" for the newly created application
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Create a new application environment with a name already used must fail
    Given I create a new application with name "watchmiddleearth" and description "Use my great eye to find frodo and the ring."
    Then I should receive a RestResponse with no error
    And The application should have a user "frodon" having "APPLICATION_MANAGER" role
    And The RestResponse should contain an id string
    And the application can be found in ALIEN
    When I create an application environment of type "DEVELOPMENT" with name "watchmiddleearth-env-mock" and description "Mock App Env" for the newly created application
    Then I should receive a RestResponse with no error
    When I create an application environment of type "INTEGRATION_TESTS" with name "watchmiddleearth-env-mock" and description "Mock App Env, Existing application env name" for the newly created application
    Then I should receive a RestResponse with an error code 502
    When I create an application environment of type "DEVELOPMENT" with name "watchmiddleearth-env-mock-clone" and description "Mock App Env Clone" for the newly created application
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Create a new application environment for an non existing/bad application must fail
    Given I create an application environment of type "DEVELOPMENT" with name "watchmiddleearth-env-mock-2" and description "Bad application must fail" for the newly created application
    Then I should receive a RestResponse with an error code 504

  @reset
  Scenario: Get an application environment from its id
    Given I create a new application with name "watchmiddleearth-1" and description "Use my great eye to find frodo and the ring."
    Then I should receive a RestResponse with no error
    And The application should have a user "frodon" having "APPLICATION_MANAGER" role
    And The RestResponse should contain an id string
    Given I create an application environment of type "DEVELOPMENT" with name "watchmiddleearth-env-mock-get" and description "Mock App Env 1" for the newly created application
    Then I should receive a RestResponse with no error
    And I add a role "DEPLOYMENT_MANAGER" to user "frodon" on the resource type "ENVIRONMENT" named "watchmiddleearth-env-mock-get"
    When I get the application environment named "watchmiddleearth-env-mock-get"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Delete an application environment from its id
    Given I create a new application with name "watchmiddleearth-3" and description "Use my great eye to find frodo and the ring."
    Then I should receive a RestResponse with no error
    And The application should have a user "frodon" having "APPLICATION_MANAGER" role
    And The RestResponse should contain an id string
    Given I create an application environment of type "DEVELOPMENT" with name "watchmiddleearth-env-mock-3" and description "Mock App Env 3" for the newly created application
    Then I should receive a RestResponse with no error
    When I delete the registered application environment named "watchmiddleearth-env-mock-3" from its id
    Then I should receive a RestResponse with no error
