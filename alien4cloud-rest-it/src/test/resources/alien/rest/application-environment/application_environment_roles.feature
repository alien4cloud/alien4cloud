Feature: Roles managements on application environment

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | frodon |
    And I create a new group with name "hobbits" in the system
    And I create a new application with name "LAMP" and description "LAMP Stack application..." without errors

  @reset
  Scenario: Add role to user
    When I add a role "DEPLOYMENT_MANAGER" to user "frodon" on the resource type "ENVIRONMENT" named "Environment"
    Then I should receive a RestResponse with no error
    When I get the application environment named "Environment"
    And I register the rest response data as SPEL context of type "alien4cloud.model.application.ApplicationEnvironment"
    And The SPEL expression "userRoles['frodon'][0]" should return "DEPLOYMENT_MANAGER"

  @reset
  Scenario: Add then remove role to user
    Given I add a role "DEPLOYMENT_MANAGER" to user "frodon" on the resource type "ENVIRONMENT" named "Environment"
    When I remove a role "DEPLOYMENT_MANAGER" to user "frodon" on the resource type "ENVIRONMENT" named "Environment"
    Then I should receive a RestResponse with no error
    When I get the application environment named "Environment"
    And I register the rest response data as SPEL context of type "alien4cloud.model.application.ApplicationEnvironment"
    And The SPEL boolean expression "userRoles['frodon'] == null" should return true

  @reset
  Scenario: Add role to group
    When I add a role "DEPLOYMENT_MANAGER" to group "hobbits" on the resource type "ENVIRONMENT" named "Environment"
    Then I should receive a RestResponse with no error
    When I get the application environment named "Environment"
    And I register the rest response data as SPEL context of type "alien4cloud.model.application.ApplicationEnvironment"
    And The SPEL expression "groupRoles.entrySet()[0].value[0]" should return "DEPLOYMENT_MANAGER"

  @reset
  Scenario: Add then remove role to group
    Given I add a role "DEPLOYMENT_MANAGER" to group "hobbits" on the resource type "ENVIRONMENT" named "Environment"
    When I remove a role "DEPLOYMENT_MANAGER" to group "hobbits" on the resource type "ENVIRONMENT" named "Environment"
    Then I should receive a RestResponse with no error
    When I get the application environment named "Environment"
    And I register the rest response data as SPEL context of type "alien4cloud.model.application.ApplicationEnvironment"
    And The SPEL boolean expression "groupRoles == null" should return true

  @reset
  Scenario: Only APPLICATION_MANAGER can edit roles, others can't
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | golum  |
      | sauron |
    And I add a role "APPLICATION_MANAGER" to user "frodon" on the resource type "APPLICATION" named "LAMP"
    And I add a role "APPLICATION_DEVOPS" to user "sauron" on the resource type "APPLICATION" named "LAMP"
    And I am authenticated with user named "frodon"
    When I add a role "DEPLOYMENT_MANAGER" to user "golum" on the resource type "ENVIRONMENT" named "Environment"
    Then I should receive a RestResponse with no error
    When I add a role "APPLICATION_USER" to group "hobbits" on the resource type "ENVIRONMENT" named "Environment"
    Then I should receive a RestResponse with no error
    When I am authenticated with user named "golum"
    And I add a role "APPLICATION_USER" to user "golum" on the resource type "ENVIRONMENT" named "Environment"
    Then I should receive a RestResponse with an error code 102
    And I add a role "APPLICATION_USER" to group "hobbits" on the resource type "ENVIRONMENT" named "Environment"
    Then I should receive a RestResponse with an error code 102
    And I remove a role "DEPLOYMENT_MANAGER" to user "golum" on the resource type "ENVIRONMENT" named "Environment"
    Then I should receive a RestResponse with an error code 102
    When I am authenticated with user named "sauron"
    And I add a role "APPLICATION_USER" to user "golum" on the resource type "ENVIRONMENT" named "Environment"
    Then I should receive a RestResponse with an error code 102
    And I remove a role "APPLICATION_USER" to group "hobbits" on the resource type "ENVIRONMENT" named "Environment"
    Then I should receive a RestResponse with an error code 102
    When I am authenticated with user named "frodon"
    And I remove a role "DEPLOYMENT_MANAGER" to user "golum" on the resource type "ENVIRONMENT" named "Environment"
    Then I should receive a RestResponse with no error
    When I remove a role "APPLICATION_USER" to group "hobbits" on the resource type "ENVIRONMENT" named "Environment"
    Then I should receive a RestResponse with no error



