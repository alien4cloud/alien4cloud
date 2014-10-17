Feature: Checking roles on topologies regarding parent application

Background:
  Given I am authenticated with "APPLICATIONS_MANAGER" role
   And I create a new application with name "mordor" and description "Bad region for hobbits." without errors

Scenario: Get a topology with the default application creator
  Given I am authenticated with "APPLICATIONS_MANAGER" role
  When I try to retrieve it
  Then I should receive a RestResponse with no error
    And The RestResponse should contain a topology

Scenario: With ADMIN role i can read any topolygy
  Given I am authenticated with "ADMIN" role
  When I retrieve the newly created topology
   Then I should receive a RestResponse with no error

Scenario: Can get a topology when i've role APPLICATION_MANAGER on the parent application
  Given I add a role "APPLICATION_MANAGER" to user "admin" on the application "mordor"
   And I am authenticated with "ADMIN" role
  When I retrieve the newly created topology
   Then I should receive a RestResponse with no error
   And The RestResponse should contain a topology

Scenario: Can get a topology when i've role APPLICATION_DEVOPS on the parent application
  Given I add a role "APPLICATION_DEVOPS" to user "admin" on the application "mordor"
   And I am authenticated with "ADMIN" role
  When I retrieve the newly created topology
   Then I should receive a RestResponse with no error
   And The RestResponse should contain a topology

Scenario: Can get a topology when i've role DEPLOYMENT_MANAGER on the parent application
  Given I add a role "DEPLOYMENT_MANAGER" to user "admin" on the application "mordor"
   And I am authenticated with "ADMIN" role
  When I retrieve the newly created topology
   Then I should receive a RestResponse with no error
   And The RestResponse should contain a topology

Scenario: Can't get a topology when i've ARCHITECT role on the parent application
  Given I add a role "ARCHITECT" to user "appManager" on the application "mordor"
   And I am authenticated with "APP_MANAGER" role
  When I retrieve the newly created topology
   Then I should receive a RestResponse with an error code 102
