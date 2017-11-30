Feature: Manage the secrets in location resources

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I successfully create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I successfully enable the orchestrator "Mount doom orchestrator"
    And I create a location named "Thark location" and infrastructure type "Amazon" to the orchestrator "Mount doom orchestrator"

  @reset
  Scenario: Create an on-demand resource and set a secret for a resource property
    When I create a resource of type "org.alien4cloud.nodes.mock.aws.Compute" named "Medium" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "imageId" to get_secret function with a secret path "kv/imageId" for the resource named "Medium" related to the location "Mount doom orchestrator"/"Thark location"
    Then I should receive a RestResponse with no error