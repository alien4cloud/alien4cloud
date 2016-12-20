Feature: Location management

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"

  @reset
  Scenario: Create a location
    When I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
      Then I should receive a RestResponse with no error
    When I list locations of the orchestrator "Mount doom orchestrator"
      Then I should receive a RestResponse with no error
      And Response should contains 1 location
      And Response should contains a location with name "Thark location"

  @reset
  Scenario: Create a location with existing name should fail
    When I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
      Then I should receive a RestResponse with an error code 502

  @reset
  Scenario: Create a location on disable orchestor should fail
    When I disable "Mount doom orchestrator"
    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
      Then I should receive a RestResponse with an error code 500

  @reset
  Scenario: Delete a location
    When I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
      Then I should receive a RestResponse with no error
    When I delete a location with name "Thark location" to the orchestrator "Mount doom orchestrator"
      Then I should receive a RestResponse with no error
    When I list locations of the orchestrator "Mount doom orchestrator"
      Then I should receive a RestResponse with no error
      And Response should contains 0 location

  # Scenario: Delete a location used by an application should fail
  #  When I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
  #  When I enable the orchestrator "Mount doom orchestrator"
  #    Then I should receive a RestResponse with no error
  #  When I delete an orchestrator with name "Mount doom orchestrator"
  #    Then I should receive a RestResponse with an error code 370
