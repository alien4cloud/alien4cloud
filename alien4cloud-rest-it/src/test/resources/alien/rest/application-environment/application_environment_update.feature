Feature: Updates operation on application environment

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | frodon |
    And I add a role "APPLICATIONS_MANAGER" to user "frodon"
    And I create an application with name "LAMP", archive name "LAMP", description "LAMP Stack application..." and topology template id "null"
    And I should receive a RestResponse with no error
    And I must have an environment named "Environment" for application "LAMP"
    And I add a role "APPLICATION_MANAGER" to user "frodon" on the resource type "APPLICATION" named "LAMP"

  @reset
  Scenario: Update an application environment from its id
    Given I am authenticated with user named "frodon"
    When I update the application environment named "Environment" with values
      | name         | watchmiddleearth-env-update name   |
      | description     | My description after update     |
      | environmentType   | INTEGRATION_TESTS           |
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Update an non existing/bad application environment must fail
    When I update the application environment named "bad-environment-name" with values
      | name       | watchmiddleearth-env-update name   |
      | description   | My description after update     |
    Then I should receive a RestResponse with an error code 504

  @reset
  Scenario: Only APPLICATION_MANAGER can update an environment, others can't
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | golum  |
      | sauron |
    And I add a role "DEPLOYMENT_MANAGER" to user "golum" on the resource type "ENVIRONMENT" named "Environment"
    And I add a role "APPLICATION_DEVOPS" to user "sauron" on the resource type "APPLICATION" named "LAMP"
    When I am authenticated with user named "golum"
    And I update the application environment named "Environment" with values
      | name | watchmiddleearth-env-update name |
    Then I should receive a RestResponse with an error code 102
    When I am authenticated with user named "sauron"
    When I update the application environment named "Environment" with values
      | name | watchmiddleearth-env-update name |
    Then I should receive a RestResponse with an error code 102
