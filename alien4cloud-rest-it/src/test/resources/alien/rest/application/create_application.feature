Feature: Creating a new application

  Background:
    Given I am authenticated with "APPLICATIONS_MANAGER" role

  @reset
  Scenario: Creating a new application
    When I create a new application with name "watchmiddleearth" and description "Use my great eye to find frodo and the ring."
    Then I should receive a RestResponse with no error
    And The application should have a user "applicationManager" having "APPLICATION_MANAGER" role
    And The RestResponse should contain an id string
    And the application can be found in ALIEN

  @reset
  Scenario: Creating a new application with an invalid name should fail
    When I create a new application with name "watchmiddl///eearth" and description "Use my great eye to find frodo and the ring."
    Then I should receive a RestResponse with an error code 619

  @reset
  Scenario: Creating a new application when already exist should fail
    Given There is a "watchmiddleearth" application
    When I create a new application with name "watchmiddleearth" and description "Use my great eye to find frodo and the ring."
    Then I should receive a RestResponse with an error code 502

  @reset
  Scenario: Searching applications
    Given There is 20 applications indexed in ALIEN
    When I search applications from 0 with result size of 20
    Then I should receive a RestResponse with no error
    And The RestResponse must contain 20 applications.

  @reset
  Scenario: Searching applications using pagination
    Given There is 20 applications indexed in ALIEN
    And I search applications from 0 with result size of 10
    When I search applications from 10 with result size of 20
    Then I should receive a RestResponse with no error
    And I should be able to view the 10 other applications.

  @reset
  Scenario: deleting an application
    And I have applications with names and descriptions
      | watchmiddleearth | Use my great eye to find frodo and the ring. |
    When I delete the application "watchmiddleearth"
    Then I should receive a RestResponse with no error
    And I should receive a RestResponse with a boolean data "true"
    And the application should not be found
