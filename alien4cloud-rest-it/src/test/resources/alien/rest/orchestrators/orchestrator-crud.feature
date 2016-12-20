Feature: Orchestrator management

  # Login and make sure that we upload a plugin first
  Background:
    Given I am authenticated with "ADMIN" role
    And I upload a plugin

  @reset
  Scenario: Create an orchestrator
    When I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    Then I should receive a RestResponse with no error
    When I list orchestrators
    Then I should receive a RestResponse with no error
    And Response should contains 1 orchestrator
    And Response should contains an orchestrator with name "Mount doom orchestrator"

  @reset
  Scenario: Create an orchestrator with existing name should fail
    When I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    Then I should receive a RestResponse with an error code 502
    When I list orchestrators
    Then I should receive a RestResponse with no error
    And Response should contains 1 orchestrator
    And Response should contains an orchestrator with name "Mount doom orchestrator"

  @reset
  Scenario: Create an orchestrator on disable plugin should fail
    When I disable the plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    Then I should receive a RestResponse with an error code 500

  @reset
  Scenario: Delete an orchestrator
    When I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    Then I should receive a RestResponse with no error
    When I delete an orchestrator with name "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
    When I list orchestrators
    Then I should receive a RestResponse with no error
    And Response should contains 0 orchestrator

  @reset
  Scenario: Delete an enabled orchestrator should fail
    When I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    When I enable the orchestrator "Mount doom orchestrator"
      Then I should receive a RestResponse with no error
    When I delete an orchestrator with name "Mount doom orchestrator"
      Then I should receive a RestResponse with an error code 505

  @reset
  Scenario: Delete an orchestrator when there is another one disabled should not fail
    When I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    When I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    Then I should receive a RestResponse with no error
    When I create an orchestrator named "Mount doom orchestrator 2" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    Then I should receive a RestResponse with no error
    When I enable the orchestrator "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
    When I enable the orchestrator "Mount doom orchestrator 2"
    Then I should receive a RestResponse with no error
    When I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
    When I create a location named "Thark location 2" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator 2"
    Then I should receive a RestResponse with no error
    When I disable "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
    When I disable "Mount doom orchestrator 2"
    Then I should receive a RestResponse with no error
    When I delete an orchestrator with name "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
    When I list orchestrators
    Then I should receive a RestResponse with no error
    And Response should contains 1 orchestrator
    And Response should contains an orchestrator with name "Mount doom orchestrator 2"
