Feature: Searching for users

  Background:
    Given I am authenticated with "ADMIN" role
    And there is 20 users in the system

  #Scenario:  unauthorized person should not be able to search users
  #  Given I am authenticated with "COMPONENTS_MANAGER" role
  #  When I search in users for " " from 0 with result size of 20
  #  Then I should receive a RestResponse with an error code 102

  @reset
  Scenario:  Search for users should return the expected number of users
    Given I am authenticated with "ADMIN" role
      When I search in users for " " from 0 with result size of 10
    Then I should receive a RestResponse with no error
      And there should be 10 users in the response

  @reset
  Scenario: search for one user
    Given There is a "TEST" user in the system
      When I search in users for "TEST" from 0 with result size of 10
    Then I should receive a RestResponse with no error
      And The response should contain a user "TEST"
      And there should be 1 users in the response
