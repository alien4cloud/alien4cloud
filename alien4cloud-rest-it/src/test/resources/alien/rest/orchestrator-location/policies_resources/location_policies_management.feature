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
  Scenario: Get a location and check policies types parsing
    When I get the location "Mount doom orchestrator"/"Thark location"
    Then I should receive a RestResponse with no error
    And I register the rest response data as SPEL context of type "alien4cloud.rest.orchestrator.model.LocationDTO"
    Then The SPEL expression "resources.policyTypes.size()" should return 2

  @reset
  Scenario: Create a policy template
    When I create a policy resource of type "org.alien4cloud.policies.mock.MinimalPolicyType" named "MinimalPolicyType" related to the location "Mount doom orchestrator"/"Thark location"
    Then I should receive a RestResponse with no error
    When I get the location "Mount doom orchestrator"/"Thark location"
    Then I should receive a RestResponse with no error
    And The location should contains a policy resource with name "MinimalPolicyType" and type "org.alien4cloud.policies.mock.MinimalPolicyType"

  @reset
  Scenario: Delete a location policy template should succeed
    Given I create a policy resource of type "org.alien4cloud.policies.mock.MinimalPolicyType" named "MinimalPolicyType" related to the location "Mount doom orchestrator"/"Thark location"
    When I delete the policy resource named "MinimalPolicyType" related to the location "Mount doom orchestrator"/"Thark location"
    Then I should receive a RestResponse with no error
    When I get the location "Mount doom orchestrator"/"Thark location"
    Then The location should not contain a policy resource with name "MinimalPolicyType" and type "org.alien4cloud.policies.mock.MinimalPolicyType"
