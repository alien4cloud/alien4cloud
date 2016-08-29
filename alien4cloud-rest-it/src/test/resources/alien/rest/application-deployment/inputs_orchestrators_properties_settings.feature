Feature: inputs and orchestrator proerties settings in deployment topology

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | frodon |
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"

    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I create a resource of type "alien.nodes.mock.openstack.Flavor" named "Small" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "1" for the resource named "Small" related to the location "Mount doom orchestrator"/"Thark location"
    And I create a resource of type "alien.nodes.mock.openstack.Image" named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "img1" for the resource named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I create a resource of type "alien.nodes.mock.openstack.Image" named "Debian" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "img2" for the resource named "Debian" related to the location "Mount doom orchestrator"/"Thark location"
    And I autogenerate the on-demand resources for the location "Mount doom orchestrator"/"Thark location"
    And I create a resource of type "alien.nodes.mock.Compute" named "Manual_Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "imageId" to "img1" for the resource named "Manual_Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "flavorId" to "1" for the resource named "Manual_Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I add a role "DEPLOYER" to user "frodon" on the resource type "LOCATION" named "Thark location"

    And I create a new application with name "ALIEN" and description "ALIEN_1" and node templates
      | WebServer | tosca.nodes.WebServer:1.0.0-SNAPSHOT |

    And I add a role "APPLICATION_MANAGER" to user "frodon" on the resource type "APPLICATION" named "ALIEN"

    And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes


  @reset
  Scenario: Setting values to input properties
    Given I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | component_version                                                |
      | propertyDefinition.type | version                                                          |
    And I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodePropertyAsInputOperation |
      | nodeName     | WebServer                                                                                   |
      | propertyName | component_version                                                                           |
      | inputName    | component_version                                                                           |
    And I save the topology
    And I am authenticated with user named "frodon"
    When I set the following inputs properties
      | component_version | 3.0 |
    Then I should receive a RestResponse with no error
    And the deployment topology should have the following inputs properties
      | component_version | 3.0 |
    And the following nodes properties values sould be "3.0"
      | WebServer | component_version |

  @reset
  Scenario: Setting wrong values to inputs properties should fail
    Given I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | component_version                                                |
      | propertyDefinition.type | version                                                          |
    And I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodePropertyAsInputOperation |
      | nodeName     | WebServer                                                                                   |
      | propertyName | component_version                                                                           |
      | inputName    | component_version                                                                           |
    And I save the topology
    And I am authenticated with user named "frodon"
    When I set the following inputs properties
      | component_version | hahahaha |
    Then I should receive a RestResponse with an error code 804

###### ORCHESTRATOR PROPERTIES ######
#####################################
  @reset
  Scenario: Setting values to orchestrator properties
    Given I am authenticated with user named "frodon"
    When I set the following orchestrator properties
      | managementUrl | http://cloudifyurl:8099 |
      | numberBackup  | 1                       |
    Then I should receive a RestResponse with no error
    And the deployment topology should have the following orchestrator properties
      | managementUrl | http://cloudifyurl:8099 |
      | numberBackup  | 1                       |

  @reset
  Scenario: Setting wrong values to orchestrator properties should fail
    Given I am authenticated with user named "frodon"
    When I set the following orchestrator properties
      | numberBackup | not an integer |
    Then I should receive a RestResponse with an error code 804
    When I set the following orchestrator properties
      | numberBackup | 0 |
    Then I should receive a RestResponse with an error code 800
