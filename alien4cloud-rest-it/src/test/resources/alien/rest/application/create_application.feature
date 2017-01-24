Feature: Application creation

  Background:
    Given I am authenticated with "APPLICATIONS_MANAGER" role

  @reset
  Scenario: Creating a new application
    When I create an application with name "watchmiddleearth", archive name "watchmiddleearth", description "Use my great eye to find frodo and the ring." and topology template id "null"
    Then I should receive a RestResponse with no error
    And I should receive a RestResponse with a string data "watchmiddleearth"
    # We should be able to query the application
    And I get the application with id "watchmiddleearth"
    And I should receive a RestResponse with no error
    And The application should have a user "applicationManager" having "APPLICATION_MANAGER" role
    # We should be able to query the application default version
    And I get the application version for application "watchmiddleearth" with id "watchmiddleearth:0.1.0-SNAPSHOT"
    And I should receive a RestResponse with no error
    # A default application topology version with no qualifier should have been created
    And The application version should have an application topology version with version "0.1.0-SNAPSHOT"
    # We should be able to query an empty topology for the default topology version
    # FIXME And I get the topology with id "watchmiddleearth:0.1.0-SNAPSHOT"
    # And I should receive a RestResponse with no error
    # And the topology should be empty
    # We should be able to query the application default environment
    And I get all application environments for application "watchmiddleearth"
    And I should receive a RestResponse with no error
    And I have 1 environments
    And Current environment name is "Environment" and version is "0.1.0-SNAPSHOT"

  @reset
  Scenario: Creating a new application with no name should fail
    When I create an application with name "null", archive name "watchmiddleearth", description "Use my great eye to find frodo and the ring." and topology template id "null"
    Then I should receive a RestResponse with an error code 501

  @reset
  Scenario: Creating a new application with empty name should fail
    When I create an application with name "", archive name "watchmiddleearth", description "Use my great eye to find frodo and the ring." and topology template id "null"
    Then I should receive a RestResponse with an error code 618

  @reset
  Scenario: Creating a new application with no archive name should fail
    When I create an application with name "watchmiddleearth", archive name "null", description "Use my great eye to find frodo and the ring." and topology template id "null"
    Then I should receive a RestResponse with an error code 501

  @reset
  Scenario: Creating a new application with empty archive name should fail
    When I create an application with name "watchmiddleearth", archive name "", description "Use my great eye to find frodo and the ring." and topology template id "null"
    Then I should receive a RestResponse with an error code 618

  @reset
  Scenario: Creating a new application with an invalid name should fail
    When I create an application with name "watchmiddl///eearth", archive name "watchmiddleearth", description "Use my great eye to find frodo and the ring." and topology template id "null"
    Then I should receive a RestResponse with an error code 618

  @reset
  Scenario: Creating a new application with an invalid archive name should fail
    When I create an application with name "watchmiddleearth", archive name "watchmiddl///eearth", description "Use my great eye to find frodo and the ring." and topology template id "null"
    Then I should receive a RestResponse with an error code 618

  @reset
  Scenario: Creating a new application with an existing name should fail
    Given I create an application with name "watchmiddleearth", archive name "watchmiddleearth", description "Use my great eye to find frodo and the ring." and topology template id "null"
    When I create an application with name "watchmiddleearth", archive name "sauroneye", description "Use my great eye to find frodo and the ring." and topology template id "null"
    Then I should receive a RestResponse with an error code 502

  @reset
  Scenario: Creating a new application with an existing archive name should fail
    Given I create an application with name "watchmiddleearth", archive name "watchmiddleearth", description "Use my great eye to find frodo and the ring." and topology template id "null"
    When I create an application with name "sauroneye", archive name "watchmiddleearth", description "Use my great eye to find frodo and the ring." and topology template id "null"
    Then I should receive a RestResponse with an error code 615
