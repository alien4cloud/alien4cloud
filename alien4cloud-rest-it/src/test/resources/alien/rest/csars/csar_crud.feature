Feature: CSAR CRUD operations

  Background:
    Given I am authenticated with "COMPONENTS_MANAGER" role

  @reset
  Scenario: Create a snapshot CSAR
    Given I have CSAR name "mycsar1" and version "v1"
    When I create a CSAR
    Then I should receive a RestResponse with no error
     And I have CSAR created with id "mycsar1:v1-SNAPSHOT"

  @reset
  Scenario: Create a snapshot CSAR with an existing ID
    Given I have CSAR name "mycsar1" and version "v1"
    And I create a CSAR
    When I create a CSAR
    Then I should receive a RestResponse with an error code 502

  @reset
  Scenario: Delete a CSAR
    Given I have CSAR name "mycsar" and version "2.1"
    When I create a CSAR
     And I delete a CSAR with id "mycsar:2.1-SNAPSHOT"
    Then I should receive a RestResponse with no error
     And I have no CSAR created with id "mycsar:2.1-SNAPSHOT"

  @reset
  Scenario: Add dependency to a CSAR
    Given I create a CSAR with name "mycsar" and version "2.1"
      And I create a CSAR with name "anothercsar" and version "1.0"
    When I add a dependency with name "anothercsar" version "1.0" to the CSAR with name "mycsar" version "2.1"
    Then I should receive a RestResponse with no error
    	And I have the CSAR "mycsar" version "2.1" to contain a dependency to "anothercsar" version "1.0"
