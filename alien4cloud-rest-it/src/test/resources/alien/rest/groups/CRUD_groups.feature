Feature: Creating a new group

  Background:
    Given I am authenticated with "ADMIN" role

  @reset
  Scenario: Creating a new group
    When I create a new group with name "lordOfRing" in the system
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Creating a new group with an empty or null groupname should fail
    When I create a new group with name "" in the system
    Then I should receive a RestResponse with an error code 501 and a field error with field "name" and code "NotBlank"

  @reset
  Scenario: Creating a new group  should fail if group already exist
    Given There is a "lordOfRing" group in the system
    When I create a new group with name "lordOfRing" in the system
    Then I should receive a RestResponse with an error code 502

  @reset
  Scenario: Creating a new group with roles and users
    Given There is a "sauron" user in the system
    When I create a new group in the system with name "lordOfRing" , a role "ADMIN" and a user "sauron"
    Then I should receive a RestResponse with no error
    And the group "lordOfRing" should have the following roles
      | ADMIN |
    And the group "lordOfRing" should have the following users
      | sauron |

  @reset
  Scenario: Creating a new group with wrong roles or users should fail
    Given There is a "sauron" user in the system
    When I create a new group in the system with name "lordOfRing1" , a role "FAKE_ADMIN" and a user "sauron"
    Then I should receive a RestResponse with an error code 504
    When I create a new group in the system with name "lordOfRing1" , a role "ADMIN" and a user "gandalf"
    Then I should receive a RestResponse with an error code 504

  @reset
  Scenario: Getting a group from its name should succeed
    Given There is a "lordOfRing" group in the system
    When I get the "lordOfRing" group
    Then I should receive a RestResponse with no error
    And The RestResponse should contain a group with name "lordOfRing"

  @reset
  Scenario: Getting a group that doesnt exists should return null
    When I get the "lordOfRing" group
    Then I should receive a RestResponse with no error
    And I should receive a RestResponse with no data

  @reset
  Scenario: Mass getting of groups
    Given There is a "lordOfRing1" group in the system
    And There is a "lordOfRing2" group in the system
    And There is a "lordOfRing1_lordOfRing2" group in the system
    When I get the groups
      | lordOfRing1 |
      | lordOfRing2 |
    Then I should receive a RestResponse with no error
    And The RestResponse should contain the groups named
      | lordOfRing1 |
      | lordOfRing2 |

  @reset
  Scenario: Deleting a group
    Given There is a "lordOfRing" group in the system
    When I delete the "lordOfRing" group
    Then I should receive a RestResponse with no error
    And There should not be a group "lordOfRing" in the system

  @reset
  Scenario: Deleting a group that doesnt exists
    When I delete the "lordOfRing" group
    Then I should receive a RestResponse with an error code 504
    And There should not be a group "lordOfRing" in the system

  @reset
  Scenario: Search for groups should return the expected number of groups
    Given There are groups in the system
      | lordOfRing1 |
      | lordOfRing2 |
      | lordOfRing3 |
      | lordOfRing4 |
      | otaku       |
    When I search in groups for "lordOf" from 0 with result size of 10
    Then I should receive a RestResponse with no error
    And there should be 4 groups in the response

  @reset
  Scenario: I can delete a group even if there are users in this group
    Deleting a group having members should be able to update the members groups.
    Given There is a "lordOfRing" group in the system
    And There is a "sauron" user in the system
    Given I have added to the group "lordOfRing" users
      | sauron |
    When I delete the "lordOfRing" group
    Then I should receive a RestResponse with no error
    And There should not be a group "lordOfRing" in the system
    And the user "sauron" should not have any group

  @reset
  Scenario: Updating a group
    Given There is a "lordOfRing" group in the system
    When I update the "lordOfRing" group's name to "lordOfRingRenamed"
    Then I should receive a RestResponse with no error
    And There should be a group "lordOfRingRenamed" in the system
