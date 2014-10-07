Feature: Create an application an testing application group roles on it

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | trunk   |
      | freezer |
      | sangoku |
      | krilin  |
    And There is a "evil" group in the system
    And There is a "kind" group in the system

  Scenario: Role attribution with user added to the group
    # Before the user is added to the group, create a new application should not work
    Given I add the role "APPLICATIONS_MANAGER" to the group "evil"
    And I am authenticated with user named "trunk"
    When I create a new application with name "Trunk is the best" and description "This is the best app for trunk"
    Then I should receive a RestResponse with an error code 102
    # After that the user is added to the group, then creating a new application should work
    Given I am authenticated with "ADMIN" role
    And I add the user "trunk" to the group "evil"
    And I am authenticated with user named "trunk"
    When I create a new application with name "Trunk is the best" and description "This is the best app for trunk"
    Then I should receive a RestResponse with no error
    # Remove the user from the group should remove also the right to create application
    Given I am authenticated with "ADMIN" role
    And I remove the user "trunk" from the group "evil"
    And I am authenticated with user named "trunk"
    When I create a new application with name "Trunk is the best v2" and description "This is the best app for trunk"
    Then I should receive a RestResponse with an error code 102

  Scenario: Role attribution with role added to group
    # Before the role is added to the group, create a csar will not work
    Given I add the user "sangoku" to the group "kind"
    And I am authenticated with user named "sangoku"
    When I create a CSAR with name "Sangoku on the cloud" and version "1.0"
    Then I should receive a RestResponse with an error code 102
    # After the role is added to the group, create a csar will work
    Given I am authenticated with "ADMIN" role
    And I add the role "COMPONENTS_MANAGER" to the group "kind"
    And I am authenticated with user named "sangoku"
    When I create a CSAR with name "sangoku-on-the-cloud" and version "1.0"
    Then I should receive a RestResponse with no error
    # Remove the role from the group, create csar will stop working
    Given I am authenticated with "ADMIN" role
    And I remove the role "COMPONENTS_MANAGER" from the group "kind"
    And I am authenticated with user named "sangoku"
    When I create a CSAR with name "sangoku-on-the-cloud" and version "2.0"
    Then I should receive a RestResponse with an error code 102

  Scenario: Remove a group should remove also the right of user of this group
    Given I am authenticated with "ADMIN" role
    And I add the role "APPLICATIONS_MANAGER" to the group "evil"
    And I add the user "freezer" to the group "evil"
    And I am authenticated with user named "freezer"
    When I create a new application with name "freezer is the best" and description "This is the best app for freezer"
    Then I should receive a RestResponse with no error
    Given I am authenticated with "ADMIN" role
    And I delete the "evil" group
    And I am authenticated with user named "freezer"
    When I create a new application with name "freezer is not the best anymore" and description "This is not the best app anymore for freezer"
    Then I should receive a RestResponse with an error code 102