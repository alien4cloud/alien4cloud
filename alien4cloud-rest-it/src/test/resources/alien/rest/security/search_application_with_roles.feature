Feature: Search applications should be able to filter

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | khang |
      | dumb  |
    And There is a "a4c dev" group in the system
    And I add the user "khang" to the group "a4c dev"
    And There is a "Dumb dev" group in the system
    And I add the user "dumb" to the group "Dumb dev"
    And I am authenticated with "APPLICATIONS_MANAGER" role
    And I create an application with name "a4c", archive name "a4c", description "Great application." and topology template id "null"
    And I should receive a RestResponse with no error
    And I create an application with name "dumbest", archive name "dumbest", description "Dumbest application." and topology template id "null"
    And I should receive a RestResponse with no error
    And I create an application with name "smartest", archive name "smartest", description "Smartest application." and topology template id "null"
    And I should receive a RestResponse with no error
    And I create an application with name "tron", archive name "tron", description "Tron legacy application." and topology template id "null"
    And I should receive a RestResponse with no error
    And I create an application with name "lost", archive name "lost", description "Lost application." and topology template id "null"
    And I should receive a RestResponse with no error

  @reset
  Scenario: Searching applications with right given to groups
    Given I add a role "APPLICATION_MANAGER" to group "a4c dev" on the resource type "APPLICATION" named "a4c"
    And I add a role "APPLICATION_DEVOPS" to group "a4c dev" on the resource type "APPLICATION" named "smartest"
    And I am authenticated with user named "khang"
    When I search applications from 0 with result size of 20
    Then I should receive a RestResponse with no error
    And The RestResponse must contain these applications
      | a4c      |
      | smartest |

  @reset
  Scenario: Searching applications with right given to users
    Given I add a role "APPLICATION_DEVOPS" to user "dumb" on the resource type "APPLICATION" named "dumbest"
    And I add a role "APPLICATION_MANAGER" to user "dumb" on the resource type "APPLICATION" named "tron"
    And I am authenticated with user named "dumb"
    When I search applications from 0 with result size of 20
    Then I should receive a RestResponse with no error
    And The RestResponse must contain these applications
      | dumbest |
      | tron    |

  @reset
  Scenario: Searching applications with right given to groups and user
    Given I add a role "APPLICATION_MANAGER" to group "a4c dev" on the resource type "APPLICATION" named "a4c"
    And I add a role "APPLICATION_DEVOPS" to group "a4c dev" on the resource type "APPLICATION" named "smartest"
    And I add a role "APPLICATION_DEVOPS" to user "khang" on the resource type "APPLICATION" named "dumbest"
    And I add a role "APPLICATION_DEVOPS" to user "khang" on the resource type "APPLICATION" named "tron"
    And I am authenticated with user named "khang"
    When I search applications from 0 with result size of 20
    Then I should receive a RestResponse with no error
    And The RestResponse must contain these applications
      | a4c      |
      | smartest |
      | dumbest  |
      | tron     |
