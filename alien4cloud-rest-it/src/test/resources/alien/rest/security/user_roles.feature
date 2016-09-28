Feature: Test user roles management

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | trunk |

  @reset
  Scenario: Role attribution
    Given I am authenticated with "ADMIN" role
    And I add a role "COMPONENTS_MANAGER" to user "trunk"
    And I add a role "APPLICATIONS_MANAGER" to user "trunk"
    And I am authenticated with user named "trunk"
    When I upload the archive "tosca base types 1.0"
    Then I should receive a RestResponse with no error
    When I create a new application with name "app of trunk" and description "This is the best app for trunk"
    Then I should receive a RestResponse with no error

    # Remove component manager
    Given I am authenticated with "ADMIN" role
    And I remove a role "COMPONENTS_MANAGER" to user "trunk"
    And I am authenticated with user named "trunk"
    When I upload the archive "tosca base types 1.0"
    Then I should receive a RestResponse with an error code 102
    When I create a new application with name "app of trunk v2" and description "This is the best app for trunk v2"
    Then I should receive a RestResponse with no error

    # Remove application manager
    Given I am authenticated with "ADMIN" role
    And I remove a role "APPLICATIONS_MANAGER" to user "trunk"
    And I am authenticated with user named "trunk"
    When I upload the archive "tosca base types 1.0"
    Then I should receive a RestResponse with an error code 102
    When I create a new application with name "app of trunk v3" and description "This is the best app for trunk v3"
    Then I should receive a RestResponse with an error code 102
