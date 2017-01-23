Feature: Test user roles management

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | sauron |

  @reset
  Scenario: Role attribution
    Given I am authenticated with "ADMIN" role
    And I add a role "COMPONENTS_MANAGER" to user "sauron"
    And I add a role "APPLICATIONS_MANAGER" to user "sauron"
    And I am authenticated with user named "sauron"
    When I upload the archive "tosca base types 1.0"
    Then I should receive a RestResponse with no error
    When I create an application with name "ring", archive name "ring", description "Ring to rule them all." and topology template id "null"
    Then I should receive a RestResponse with no error

    # Remove component manager
    Given I am authenticated with "ADMIN" role
    And I remove a role "COMPONENTS_MANAGER" to user "sauron"
    And I am authenticated with user named "sauron"
    When I upload the archive "tosca base types 1.0"
    Then I should receive a RestResponse with an error code 102
    When I create an application with name "isengard", archive name "isengard", description "Build an orc army." and topology template id "null"
    Then I should receive a RestResponse with no error

    # Remove application manager
    Given I am authenticated with "ADMIN" role
    And I remove a role "APPLICATIONS_MANAGER" to user "sauron"
    And I am authenticated with user named "sauron"
    When I upload the archive "tosca base types 1.0"
    Then I should receive a RestResponse with an error code 102
    When I create an application with name "watchmiddleearth", archive name "watchmiddleearth", description "Use my great eye to find frodo and the ring." and topology template id "null"
    Then I should receive a RestResponse with an error code 102
