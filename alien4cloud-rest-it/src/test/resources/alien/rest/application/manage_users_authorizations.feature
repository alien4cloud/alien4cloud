Feature: Manage user's authorizations on an application

  Background:
    Given I am authenticated with "ADMIN" role
    And There is a "lord_of_ring" application
    And There are these users in the system
      | sauron  |
      | bilbo   |
      | golum   |
      | gandalf |

  @reset
  Scenario:  adding roles to a user on a an application
    Given I am authenticated with "ADMIN" role
    When I add a role "APPLICATION_MANAGER" to user "sauron" on the resource type "APPLICATION" named "lord_of_ring"
    Then I should receive a RestResponse with no error
    When I search for "lord_of_ring" application
    Then I should receive a RestResponse with no error
    And The application should have a user "sauron" having "APPLICATION_MANAGER" role

  @reset
  Scenario:  removing a role from a user on an application
    Given I am authenticated with "ADMIN" role
    And there is a user "sauron" with the following roles on the application "lord_of_ring"
      | APPLICATION_MANAGER |
      | APPLICATION_DEVOPS  |
    When I remove a role "APPLICATION_MANAGER" to user "sauron" on the resource type "APPLICATION" named "lord_of_ring"
    Then I should receive a RestResponse with no error
    When I search for "lord_of_ring" application
    Then I should receive a RestResponse with no error
    And The application should have a user "sauron" not having "APPLICATION_MANAGER" role
    And The application should have a user "sauron" having "APPLICATION_DEVOPS" role
