Feature: Search application version

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | luffy |
    And I add a role "APPLICATIONS_MANAGER" to user "luffy"
    And I am authenticated with user named "luffy"

  @reset
  Scenario: Search for application versions
    Given I create an application with name "ONE-PIECE", archive name "ONEPIECE", description "Test application" and topology template id "null"
    And I should receive a RestResponse with no error
    When I search for application versions
    Then I should receive 1 application versions in the search result
    And I create an application version for application "ONEPIECE" with version "0.3.0-SNAPSHOT", description "null", topology template id "null" and previous version id "null"
    And I should receive a RestResponse with no error
    When I search for application versions
    Then I should receive 2 application versions in the search result
