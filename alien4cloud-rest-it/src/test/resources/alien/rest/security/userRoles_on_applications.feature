Feature: Create an application an testing application roles on it

  Background:
    Given I am authenticated with "APPLICATIONS_MANAGER" role
    And I create a new application with name "mordor" and description "Bad region for hobbits." without errors

  @reset
  Scenario: I can't read an application if i have no application role on it
    Given I am authenticated with "APP_MANAGER" role
    And I retrieve the newly created application
    Then I should receive a RestResponse with an error code 102

  @reset
  Scenario: I can read an application with at least one application role on it - APPLICATION_MANAGER
    Given I add a role "APPLICATION_MANAGER" to user "appManager" on the resource type "APPLICATION" named "mordor"
    And I am authenticated with "APP_MANAGER" role
    And I retrieve the newly created application
    Then I should receive a RestResponse with no error

  @reset
  Scenario: I can read an application with at least one application role on it - APPLICATION_DEVOPS
    Given I add a role "APPLICATION_DEVOPS" to user "appManager" on the resource type "APPLICATION" named "mordor"
    And I am authenticated with "APP_MANAGER" role
    And I retrieve the newly created application
    Then I should receive a RestResponse with no error

  @reset
  Scenario: I can read an application as application creator - default role APPLICATION_MANAGER
    Given I retrieve the newly created application
    Then I should receive a RestResponse with no error

  @reset
  Scenario: As an ADMIN can see all applications
    Given There is 10 applications indexed in ALIEN
    And I am authenticated with "ADMIN" role
    When I search applications from 0 with result size of 20
    Then I should receive a RestResponse with no error
    And The RestResponse must contain 11 applications.

  @reset
  Scenario: Remove a user should remove also the right of user on the application
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | sauron |
    And I add a role "APPLICATION_MANAGER" to user "sauron" on the application "mordor"
    And I am authenticated with user named "sauron"
    When I retrieve the newly created application
    Then I should receive a RestResponse with no error
    Given I am authenticated with "ADMIN" role
    And I remove user "sauron"
    When I retrieve the newly created application
    Then I should receive an application without "sauron" as user
