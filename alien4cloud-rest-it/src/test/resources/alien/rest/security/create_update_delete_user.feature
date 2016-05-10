Feature: Creating, updating and deleting a user account

  @reset
  Scenario: Creating a new user account
    Given I am authenticated with "ADMIN" role
    And There is no "sauron" user in the system
    When I create a new user with username "sauron" and password "thering" in the system
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Creating a new user account with an empty or null username should fail
    Given I am authenticated with "ADMIN" role
    And There is no "witchkingofangmar" user in the system
    When I create a new user with username "" and password "witchkingofangmar" in the system
    Then I should receive a RestResponse with an error code 501 and a field error with field "username" and code "NotBlank"

  @reset
  Scenario: Creating a new user account with an empty or null password should fail
    Given I am authenticated with "ADMIN" role
    And There is no "witchkingofangmar" user in the system
    When I create a new user with username "witchkingofangmar" and password "" in the system
    Then I should receive a RestResponse with an error code 501 and a field error with field "password" and code "NotBlank"

  @reset
  Scenario: Creating a new user account should fail if user already exist
    Given I am authenticated with "ADMIN" role
    And There is a "sauron" user in the system
    When I create a new user with username "sauron" and password "thering" in the system
    Then I should receive a RestResponse with an error code 502

  @reset
  Scenario: Removing a user account
    Given I am authenticated with "ADMIN" role
    And There is a "sauron" user in the system
    When I remove user "sauron"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Removing a user account that does not exist
    Given I am authenticated with "ADMIN" role
    And There is no "sauron" user in the system
    When I remove user "sauron"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Updating a group.
    Given I am authenticated with "ADMIN" role
    And There is a "bilbo" user in the system
    When I update "bilbo" user's fields:
      |name|value|
      |firstName|Baggins|
    Then I should receive a RestResponse with no error
    And There should be a user "bilbo" with firstname "Baggins" in the system
