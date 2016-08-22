Feature: Manage user's authorizations on an application

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | sauron  |
      | gandalf |
    And There is a "lordOfRing" group in the system
    And I add the user "sauron" to the group "lordOfRing"
    And I add the user "gandalf" to the group "lordOfRing"

  @reset
  Scenario: Give to a group rights for an application
    Given I am authenticated with "APPLICATIONS_MANAGER" role
    And There is a "lord_of_ring" application
    And I add a role "APPLICATION_MANAGER" to group "lordOfRing" on the resource type "APPLICATION" named "lord_of_ring"
    Then I should receive a RestResponse with no error
    When I search for "lord_of_ring" application
    Then I should receive a RestResponse with no error
    And The application should have a group "lordOfRing" having "APPLICATION_MANAGER" role

  @reset
  Scenario: Removing rights of a group for an application
    Given I am authenticated with "APPLICATIONS_MANAGER" role
    And There is a "lord_of_ring" application
    And There is a group "lordOfRing" with the following roles on the application "lord_of_ring"
      | APPLICATION_MANAGER |
      | APPLICATION_DEVOPS  |
    When I remove a role "APPLICATION_MANAGER" to group "lordOfRing" on the resource type "APPLICATION" named "lord_of_ring"
    Then I should receive a RestResponse with no error
    When I search for "lord_of_ring" application
    Then I should receive a RestResponse with no error
    And The application should have the group "lordOfRing" not having "APPLICATION_MANAGER" role
    And The application should have the group "lordOfRing" having "APPLICATION_DEVOPS" role
