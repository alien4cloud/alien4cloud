Feature: Update location

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"

  @reset
  Scenario: Update an location's name
    When I update location name from "Thark location" to "Lothar location" of the orchestrator "Mount doom orchestrator"
      Then I should receive a RestResponse with no error
    When I list locations of the orchestrator "Mount doom orchestrator"
      Then I should receive a RestResponse with no error
      And Response should contains a location with name "Lothar location"

  @reset
  Scenario: Update a location's name with same name should not fail (just ignored)
    When I update location name from "Thark location" to "Thark location" of the orchestrator "Mount doom orchestrator"
      Then I should receive a RestResponse with no error
    When I list locations of the orchestrator "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
      And Response should contains a location with name "Thark location"

  @reset
  Scenario: Update a location's name with existing name should fail
    When I create a location named "Zodanga location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I update location name from "Zodanga location" to "Thark location" of the orchestrator "Mount doom orchestrator"
      Then I should receive a RestResponse with an error code 502
    When I list locations of the orchestrator "Mount doom orchestrator"
      Then I should receive a RestResponse with no error
      And Response should contains 2 location
      And Response should contains a location with name "Thark location"
      And Response should contains a location with name "Zodanga location"

  @reset
  Scenario: Update a location's environment type
    When I update environment type to "DEVELOPMENT" of the location "Thark location" of the orchestrator "Mount doom orchestrator"
      Then I should receive a RestResponse with no error
