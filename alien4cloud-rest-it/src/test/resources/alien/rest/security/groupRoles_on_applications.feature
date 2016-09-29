Feature: Testing group roles on application

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | sauron  |
      | bilbo   |
      | golum   |
      | gandalf |
    And There is a "lordOfRing" group in the system
    And There is a "hobbit" group in the system
    And I add the user "sauron" to the group "lordOfRing"
    And I add the user "gandalf" to the group "hobbit"
    And I am authenticated with "APPLICATIONS_MANAGER" role
    And I create a new application with name "mordor" and description "Bad region for hobbits." without errors

  @reset
  Scenario: I can't read an application if i have no application role on it
    Given I am authenticated with user named "bilbo"
    When I retrieve the newly created application
    Then I should receive a RestResponse with an error code 102

  @reset
  Scenario: I can read an application with at least one application role on it APPLICATION_DEVOPS
    Given I add a role "APPLICATION_DEVOPS" to group "lordOfRing" on the resource type "APPLICATION" named "mordor"
    And I am authenticated with user named "sauron"
    When I retrieve the newly created application
    Then I should receive a RestResponse with no error

  @reset
  Scenario: I can read an application with at least one application role on it APPLICATION_MANAGER
    Given I add a role "APPLICATION_DEVOPS" to group "lordOfRing" on the resource type "APPLICATION" named "mordor"
    And I am authenticated with user named "sauron"
    When I retrieve the newly created application
    Then I should receive a RestResponse with no error

  @reset
  Scenario: I can't read the application if i'm not in the granted group
    Given I add a role "APPLICATION_DEVOPS" to group "lordOfRing" on the resource type "APPLICATION" named "mordor"
    And I am authenticated with user named "gandalf"
    When I retrieve the newly created application
    Then I should receive a RestResponse with an error code 102

  @reset
  Scenario: Remove a group should remove also the right of users of this group on the application
    Given I add a role "APPLICATION_MANAGER" to group "lordOfRing" on the resource type "APPLICATION" named "mordor"
    And I am authenticated with user named "sauron"
    When I retrieve the newly created application
    Then I should receive a RestResponse with no error
    Given I am authenticated with "ADMIN" role
    And I delete the "lordOfRing" group
    And I am authenticated with user named "sauron"
    When I retrieve the newly created application
    Then I should receive a RestResponse with an error code 102
