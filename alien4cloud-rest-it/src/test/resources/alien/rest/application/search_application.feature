Feature: Application search

  Background:
    Given I am authenticated with "APPLICATIONS_MANAGER" role

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
