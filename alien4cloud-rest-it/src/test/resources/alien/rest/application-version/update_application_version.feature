Feature: CRUD operations on application version

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | luffy |
    And I add a role "APPLICATIONS_MANAGER" to user "luffy"
    And I am authenticated with user named "luffy"
    And I create an application with name "watchmiddleearth", archive name "watchmiddleearth", description "Use my great eye to find frodo and the ring." and topology template id "null"
    And I should receive a RestResponse with no error

  @reset
  Scenario: Updating an application version with a new version should succeedd
    When I update the application version for application "watchmiddleearth" version id "watchmiddleearth:0.1.0-SNAPSHOT" with new version "0.2.0-SNAPSHOT" and description "null"
    Then I should receive a RestResponse with no error

#  @reset
#  Scenario: Updating an application version with a new description should succeedd
#
#  @reset
#  Scenario: Updating an application version with a new version and description should succeedd

  @reset
  Scenario: Updating an application version with an existing version should fail
    And I create an application version for application "watchmiddleearth" with version "0.2.0-SNAPSHOT", description "null", topology template id "null" and previous version id "null"
    And I should receive a RestResponse with no error
    When I update the application version for application "watchmiddleearth" version id "watchmiddleearth:0.1.0-SNAPSHOT" with new version "0.2.0-SNAPSHOT" and description "null"
    Then I should receive a RestResponse with an error code 502

  @reset
  Scenario: Updating the version of a released application version should fail
    And I create an application version for application "watchmiddleearth" with version "0.2.0", description "null", topology template id "null" and previous version id "null"
    And I should receive a RestResponse with no error
    When I update the application version for application "watchmiddleearth" version id "watchmiddleearth:0.2.0" with new version "0.2.1-SNAPSHOT" and description "null"
    Then I should receive a RestResponse with an error code 608

#  @reset
#  Scenario: Updating the version of a deployed application version should fail