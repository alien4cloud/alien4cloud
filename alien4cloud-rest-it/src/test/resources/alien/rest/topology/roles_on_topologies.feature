Feature: Checking roles on topologies regarding parent application

  Background:
    Given I am authenticated with "APPLICATIONS_MANAGER" role
    And I create an application with name "mordor", archive name "mordor", description "Bad region for hobbits." and topology template id "null"
    And I should receive a RestResponse with no error

  @reset
  Scenario: Get a topology with the default application creator
    Given I am authenticated with "APPLICATIONS_MANAGER" role
    When I try to retrieve it
    Then I should receive a RestResponse with no error
      And The RestResponse should contain a topology

  @reset
  Scenario: With ADMIN role i can read any topolygy
    Given I am authenticated with "ADMIN" role
    When I retrieve the newly created topology
     Then I should receive a RestResponse with no error

  @reset
  Scenario: Can get a topology when i've role APPLICATION_MANAGER on the parent application
    Given I add a role "APPLICATION_MANAGER" to user "admin" on the application "mordor"
     And I am authenticated with "ADMIN" role
    When I retrieve the newly created topology
     Then I should receive a RestResponse with no error
     And The RestResponse should contain a topology

  @reset
  Scenario: Can get a topology when i've role APPLICATION_DEVOPS on the parent application
    Given I add a role "APPLICATION_DEVOPS" to user "admin" on the application "mordor"
     And I am authenticated with "ADMIN" role
    When I retrieve the newly created topology
     Then I should receive a RestResponse with no error
     And The RestResponse should contain a topology

  @reset
  Scenario: Can't get a topology when i've ARCHITECT role on the parent application
    Given I add a role "ARCHITECT" to user "appManager" on the application "mordor"
     And I am authenticated with "APP_MANAGER" role
    When I retrieve the newly created topology
   Then I should receive a RestResponse with an error code 102
