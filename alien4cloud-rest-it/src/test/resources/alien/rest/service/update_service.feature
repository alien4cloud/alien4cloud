Feature: Update service resource

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    When I create a service with name "MyBdService", version "1.0.0", type "tosca.nodes.Database", archive version "1.0.0-SNAPSHOT"

  @reset
  Scenario: Updating the name of a service should succeed
    Given I am authenticated with "ADMIN" role


  @reset
  Scenario: Updating the name of a service with an existing name should fail
    Given I am authenticated with "ADMIN" role

  @reset
  Scenario: Updating the version of a service should succeed
    Given I am authenticated with "ADMIN" role


  @reset
  Scenario: Updating the version of a service with an existing version should fail
    Given I am authenticated with "ADMIN" role

  @reset
  Scenario: Updating the description of a service should succeed
    Given I am authenticated with "ADMIN" role
