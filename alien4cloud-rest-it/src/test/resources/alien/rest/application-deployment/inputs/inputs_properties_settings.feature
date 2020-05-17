Feature: inputs properties settings in deployment setup

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | frodon |
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"

    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I create a resource of type "org.alien4cloud.nodes.mock.Compute" named "Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "imageId" to "img1" for the resource named "Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "flavorId" to "1" for the resource named "Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I grant access to the resource type "LOCATION" named "Thark location" to the user "frodon"
    And I successfully grant access to the resource type "LOCATION_RESOURCE" named "Mount doom orchestrator/Thark location/Small_Ubuntu" to the user "frodon"

    And I create a new application with name "ALIEN" and description "ALIEN_1" and node templates
      | Compute | tosca.nodes.Compute:1.0.0-SNAPSHOT |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | version                                                          |
      | propertyDefinition.type | version                                                          |
    And I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodeCapabilityPropertyAsInputOperation |
      | nodeName       | Compute                                                                                               |
      | capabilityName | os                                                                                                    |
      | propertyName   | version                                                                                               |
      | inputName      | version                                                                                               |
    And I save the topology
    And I add a role "APPLICATION_MANAGER" to user "frodon" on the resource type "APPLICATION" named "ALIEN"
    And I get the deployment topology for the current application
    And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes
    And I am authenticated with user named "frodon"

  @reset
  Scenario: Setting values to input properties
    When I set the following inputs properties
      | version | 3.0 |
    Then I should receive a RestResponse with no error
    And the deployment topology should have the following inputs properties
      | version | 3.0 |

  @reset
  Scenario: Setting wrong values to inputs properties should fail
    When I set the following inputs properties
      | version | hahahaha |
    Then I should receive a RestResponse with an error code 804


  @reset
  Scenario: Deleted input from topology should not appear in deployment setup
    Given I set the following inputs properties
      | version | 3.0 |
    And the deployment topology should have the following inputs properties
      | version | 3.0 |
    And I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.inputs.DeleteInputOperation |
      | inputName | version                                                             |
    And I save the topology
    When I get the deployment topology for the current application
    Then the deployment topology should not have the following input properties
      | version |
