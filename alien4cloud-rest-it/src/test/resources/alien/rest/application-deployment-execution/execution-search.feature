Feature: search executions

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I create a resource of type "org.alien4cloud.nodes.mock.openstack.Flavor" named "Small" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "1" for the resource named "Small" related to the location "Mount doom orchestrator"/"Thark location"
    And I create a resource of type "org.alien4cloud.nodes.mock.openstack.Image" named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "img1" for the resource named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I autogenerate the on-demand resources for the location "Mount doom orchestrator"/"Thark location"
    And I pre register orchestrator properties
      | managementUrl | http://somewhere.fr:8099 |
      | numberBackup  | 1                       |
      | managerEmail  | admin@alien.fr         |

    And I create a new application with name "The great eye" and description "" and node templates
      | Compute | tosca.nodes.Compute:1.0.0-SNAPSHOT |
    And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes
    And I deploy it
    Then I should receive a RestResponse with no error

  @reset
  Scenario: search for all executions
    When I search for executions
    Then I should receive a RestResponse with no error
    And I should get some executions
  
  @reset
  Scenario: search for executions for current deployment
    When I search for executions for current deployment
    Then I should receive a RestResponse with no error
    And I should get current deployment executions

  @reset
  Scenario: search for executions for non existing deployment
    When I search for executions for non existing deployment
    Then I should receive a RestResponse with no error
    And I should get no execution
  
  @reset
  Scenario: search for current execution
    When I search for executions for current deployment
    Then I should receive a RestResponse with no error
    And I should get some executions
    When I search for current execution
    Then I should receive a RestResponse with no error
    And I should find current execution

  @reset
  Scenario: search for non existing execution
    When I search for non existing execution
    Then I should receive a RestResponse with no error
    And I should get no execution
  
  @reset
  Scenario: get current execution
    When I search for executions for current deployment
    Then I should receive a RestResponse with no error
    And I should get some executions
    When I get current execution
    Then I should receive a RestResponse with no error
    And I should get current execution
  
  @reset
  Scenario: get non existing execution
    When I get non existing execution
    Then I should receive a RestResponse with an error code 504
