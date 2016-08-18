Feature: Enable/disable an orchestrator

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"

  @reset
  Scenario: Enable an orchestrator should work
    When I get the orchestrator named "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
    And Response should contains the orchestrator with name "Mount doom orchestrator" and state enabled "false"
    When I enable the orchestrator "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
    When I get the orchestrator named "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
    And Response should contains the orchestrator with name "Mount doom orchestrator" and state enabled "true"

  @reset
  Scenario: Enable an orchestrator that is already enabled should fail
    When I enable the orchestrator "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
    When I enable the orchestrator "Mount doom orchestrator"
    Then I should receive a RestResponse with an error code 502

  @reset
  Scenario: Disable an enabled orchestrator not used for a deployment should not fail
    When I enable the orchestrator "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
    When I get the orchestrator named "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
    And Response should contains the orchestrator with name "Mount doom orchestrator" and state enabled "true"
    When I disable "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
    When I get the orchestrator named "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
    And Response should contains the orchestrator with name "Mount doom orchestrator" and state enabled "false"

  @reset
  Scenario: Disable an enabled orchestrator used for a deployment should fail
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I create a resource of type "alien.nodes.mock.openstack.Flavor" named "Small" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "1" for the resource named "Small" related to the location "Mount doom orchestrator"/"Thark location"
    And I create a resource of type "alien.nodes.mock.openstack.Image" named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "img1" for the resource named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I autogenerate the on-demand resources for the location "Mount doom orchestrator"/"Thark location"
    And I pre register orchestrator properties
      | managementUrl | http://cloudifyurl:8099 |
      | numberBackup  | 1                       |
      | managerEmail  | admin@alien.fr          |
    And I create a new application with name "ALIEN" and description "" and node templates
      | Compute | tosca.nodes.Compute:1.0.0-SNAPSHOT |
    And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes
    And I deploy it
    Then I should receive a RestResponse with no error
    And The application's deployment must succeed

    When I disable "Mount doom orchestrator"
    Then I should receive a RestResponse with an error code 508
    And I should receive a RestResponse with a non-empty list of usages

    When I get the orchestrator named "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
    And Response should contains the orchestrator with name "Mount doom orchestrator" and state enabled "true"
