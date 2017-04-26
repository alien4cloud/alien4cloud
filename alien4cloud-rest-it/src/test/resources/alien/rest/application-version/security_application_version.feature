Feature: Security on application version

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | luffy |
    And I add a role "APPLICATIONS_MANAGER" to user "luffy"
    And I am authenticated with user named "luffy"

  @reset
  Scenario: only APPLICATION_MANAGER should be able to create/ delete a version, others can't
    Given I create an application with name "ONE-PIECE", archive name "ONEPIECE", description "Test application" and topology template id "null"
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | zorro |
      | sanji |
    And I add a role "APPLICATIONS_MANAGER" to user "zorro"
    And I add a role "APPLICATION_DEVOPS" to user "sanji" on the resource type "APPLICATION" named "ONE-PIECE"
    And I create an application version for application "ONEPIECE" with version "0.3.0-SNAPSHOT", description "null", topology template id "null" and previous version id "null"
    Then I should receive a RestResponse with no error
    When I am authenticated with user named "zorro"
    And I create an application version for application "ONEPIECE" with version "0.4.0-SNAPSHOT", description "null", topology template id "null" and previous version id "null"
    Then I should receive a RestResponse with an error code 102
    When I update the application version for application "ONEPIECE" version id "ONEPIECE:0.1.0-SNAPSHOT" with new version "0.2.0-SNAPSHOT" and description "null"
    Then I should receive a RestResponse with an error code 102
    When I delete an application version for application "ONEPIECE" with version id "ONEPIECE:0.3.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 102
    When I am authenticated with user named "sanji"
    And I create an application version for application "ONEPIECE" with version "0.4.0-SNAPSHOT", description "null", topology template id "null" and previous version id "null"
    Then I should receive a RestResponse with an error code 102
    When I update the application version for application "ONEPIECE" version id "ONEPIECE:0.1.0-SNAPSHOT" with new version "0.2.0-SNAPSHOT" and description "null"
    Then I should receive a RestResponse with an error code 102
    When I delete an application version for application "ONEPIECE" with version id "ONEPIECE:0.3.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 102
