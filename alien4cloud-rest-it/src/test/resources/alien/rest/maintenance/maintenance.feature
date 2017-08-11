Feature: Maintenance mode management

  Background:
    Given I am authenticated with "ADMIN" role
    And I disable maintenance mode
    And There are these users in the system
      | frodo |

  @reset
  Scenario: Enabling and Disabling maintenance mode as admin should succeed
    When I enable maintenance mode
    Then I should receive a RestResponse with no error
    When I disable maintenance mode
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Enabling maintenance mode if not admin should fail
    And I am authenticated with user named "frodo"
    When I enable maintenance mode
    Then I should receive a RestResponse with an error code 102

  @reset
  Scenario: Accessing any non-maintenance endpoint when maintenance mode is enabled should fail
    When I enable maintenance mode
    Then I should receive a RestResponse with no error
    When I search applications from 0 with result size of 20
    Then I should receive a RestResponse with an error code 0
    When I disable maintenance mode
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Disabling maintenance mode when not enabled should fail
    When I disable maintenance mode
    Then I should receive a RestResponse with an error code 504

  @reset
  Scenario: Login when maintenance mode is enabled should succeed
    When I enable maintenance mode
    Then I should receive a RestResponse with no error
    When I am authenticated with user named "frodo"
    And I get maintenance state
    Then I should receive a RestResponse with no error
    When I am authenticated with "ADMIN" role
    And I disable maintenance mode
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Getting maintenance status when maintenance is active should return an maintenance status
    When I enable maintenance mode
    Then I should receive a RestResponse with no error
    When I get maintenance state
    Then I should receive a RestResponse with no error
    When I disable maintenance mode
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Updating maintenance status should impact message an progress
    When I enable maintenance mode
    Then I should receive a RestResponse with no error
    When I update maintenance state, message: "Performing maintenance" percent: 25
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Updating maintenance status when no maintenance is enabled should fail
    When I update maintenance state, message: "Performing maintenance" percent: 25
    Then I should receive a RestResponse with an error code 504

  @reset
  Scenario: Updating maintenance status when not admin should fail
    When I enable maintenance mode
    Then I should receive a RestResponse with no error
    And I am authenticated with user named "frodo"
    When I update maintenance state, message: "Performing maintenance" percent: 25
    Then I should receive a RestResponse with an error code 102

  @reset
  Scenario: Getting maintenance status when no maintenance is active should return an empty rest response
    When I get maintenance state
    Then I should receive a RestResponse with no error
    Then I should receive a RestResponse with no data
