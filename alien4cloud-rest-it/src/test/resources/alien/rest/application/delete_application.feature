Feature: Delete application

  Background:
    Given I am authenticated with "APPLICATIONS_MANAGER" role

  @reset
  Scenario: deleting an application
    And I have applications with names and descriptions
      | watchmiddleearth | Use my great eye to find frodo and the ring. |
    When I delete the application "watchmiddleearth"
    Then I should receive a RestResponse with no error
    And I should receive a RestResponse with a boolean data "true"
    And the application should not be found
