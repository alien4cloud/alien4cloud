Feature: Create application environment

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | frodon |
      | golum  |
      | sauron |
    And I add a role "APPLICATIONS_MANAGER" to user "frodon"
    And I am authenticated with user named "frodon"

  @reset
  Scenario: Create a new application environment for an application
    Given I create an application with name "watchmiddleearth", archive name "watchmiddleearth", description "Use my great eye to find frodo and the ring." and topology template id "null"
    Then I should receive a RestResponse with no error
    And The application should have a user "frodon" having "APPLICATION_MANAGER" role
    And I should receive a RestResponse with a string data "watchmiddleearth"
    And the application can be found in ALIEN
    When I create an application environment of type "DEVELOPMENT" with name "watchmiddleearth-env-mock" and description "Mock App Env" for the newly created application
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Create a new application environment with a name already used must fail
    Given I create an application with name "watchmiddleearth", archive name "watchmiddleearth", description "Use my great eye to find frodo and the ring." and topology template id "null"
    Then I should receive a RestResponse with no error
    And The application should have a user "frodon" having "APPLICATION_MANAGER" role
    And I should receive a RestResponse with a string data "watchmiddleearth"
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
    Given I create an application with name "watchmiddleearth-1", archive name "watchmiddleearth1", description "Use my great eye to find frodo and the ring." and topology template id "null"
    Then I should receive a RestResponse with no error
    And The application should have a user "frodon" having "APPLICATION_MANAGER" role
    And I should receive a RestResponse with a string data "watchmiddleearth1"
    Given I create an application environment of type "DEVELOPMENT" with name "watchmiddleearth-env-mock-get" and description "Mock App Env 1" for the newly created application
    Then I should receive a RestResponse with no error
    And I add a role "DEPLOYMENT_MANAGER" to user "frodon" on the resource type "ENVIRONMENT" named "watchmiddleearth-env-mock-get"
    When I get the application environment named "watchmiddleearth-env-mock-get"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: APPLICATION_MANAGER should be able to create an environment, others can't
    Given I create an application with name "watchmiddleearth", archive name "watchmiddleearth", description "..." and topology template id "null"
    And I should receive a RestResponse with no error
    And I add a role "APPLICATION_MANAGER" to user "golum" on the resource type "APPLICATION" named "watchmiddleearth"
    And I add a role "APPLICATION_DEVOPS" to user "sauron" on the resource type "APPLICATION" named "watchmiddleearth"
    When I am authenticated with user named "sauron"
    And I create an application environment of type "OTHER" with name "SHOULD_FAIL" and description "" for the newly created application
    Then I should receive a RestResponse with an error code 102
    And I am authenticated with user named "golum"
    When I create an application environment of type "OTHER" with name "other" and description "" for the newly created application
    Then I should receive a RestResponse with no error
