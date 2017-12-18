Feature: Manage location policies resources

  Background:
    Given I am authenticated with "ADMIN" role
    And I have uploaded the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I successfully create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I successfully enable the orchestrator "Mount doom orchestrator"
    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Update a location policy resource property
    Given I create a policy resource of type "org.alien4cloud.mock.policies.AntiAffinity" named "AntiAffinity" related to the location "Mount doom orchestrator"/"Thark location"
    When I set the property "availability_zones" to a secret with a secret path "kv/availability_zones" for the policy resource named "AntiAffinity" related to the location "Mount doom orchestrator"/"Thark location"
    Then I should receive a RestResponse with no error
    Given I get the location "Mount doom orchestrator"/"Thark location"
