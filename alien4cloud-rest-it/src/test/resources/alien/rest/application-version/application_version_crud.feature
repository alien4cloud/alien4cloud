Feature: CRUD operations on application version

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload a plugin
    And There are these users in the system
      | lufy |
    And I add a role "APPLICATIONS_MANAGER" to user "lufy"
    And I am authenticated with user named "lufy"

  Scenario: Create an application version with failure
    Given I have an application with name "ALIEN"
	And I create an application version with version "0.3..0-SNAPSHOT-SHOULD-FAILED"
	Then I should receive a RestResponse with an error code 605

  Scenario: Create an application version with success
    Given I have an application with name "ALIEN"
	And I create an application version with version "0.3.0-SNAPSHOT"
	Then I should receive a RestResponse with no error

  Scenario: Delete an application version with failure
    Given I have an application with name "ALIEN"
	And I delete an application version with name "0.2.0-SNAPSHOT"
	Then I should receive a RestResponse with an error code 500

  Scenario: Delete an application version with success
    Given I have an application with name "ALIEN"
    And I create an application version with version "0.2.0-SNAPSHOT"
	Then I should receive a RestResponse with no error
	And I delete an application version with name "0.2.0-SNAPSHOT"
	Then I should receive a RestResponse with no error

  Scenario: Search for application versions
	Given I have an application with name "ALIEN"
	And I create an application version with version "0.3.0-SNAPSHOT"
	Then I should receive a RestResponse with no error
	When I search for application versions
    Then I should receive 2 application versions in the search result

  Scenario: Update an application version with success
    Given I have an application with name "ALIEN"
    And I create an application version with version "0.2.0-SNAPSHOT"
	Then I should receive a RestResponse with no error
	And I update an application version with version "0.2.0-SNAPSHOT" to "0.4.0-SNAPSHOT"
	Then I should receive a RestResponse with no error