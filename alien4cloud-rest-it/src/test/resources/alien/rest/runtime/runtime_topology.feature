Feature: get runtime topology

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I create a resource of type "alien.nodes.mock.openstack.Flavor" named "Medium" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "2" for the resource named "Medium" related to the location "Mount doom orchestrator"/"Thark location"
    And I create a resource of type "alien.nodes.mock.openstack.Image" named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "img1" for the resource named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I autogenerate the on-demand resources for the location "Mount doom orchestrator"/"Thark location"
    And There are these users in the system
      | sangoku |
    And I add a role "APPLICATIONS_MANAGER" to user "sangoku"
    And I add a role "COMPONENTS_MANAGER" to user "sangoku"
    And I add a role "DEPLOYER" to user "sangoku" on the resource type "LOCATION" named "Thark location"
    And I am authenticated with user named "sangoku"
    And I upload the archive "tosca base types 1.0"
    And I should receive a RestResponse with no error
    And I upload the archive "sample apache lb types 0.1"
    And I should receive a RestResponse with no error

    And I pre register orchestrator properties
      | managementUrl | http://cloudifyurl:8099 |
      | numberBackup  | 1                       |
      | managerEmail  | admin@alien.fr          |

    Given I create a new application with name "ALIEN" and description "" and node templates
      | apacheLBGroovy | fastconnect.nodes.apacheLBGroovy:0.1 |
      | Compute        | tosca.nodes.Compute:1.0              |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | apacheLBGroovy                                                                        |
      | relationshipName       | hostedOnCompute                                                                       |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0                                                                                   |
      | requirementName        | host                                                                                  |
      | target                 | Compute                                                                               |
      | targetedCapabilityName | host                                                                                  |
    And I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodePropertyValueOperation |
      | nodeName      | Compute                                                                               |
      | propertyName  | os_type                                                                               |
      | propertyValue | linux                                                                                 |
    And I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodePropertyValueOperation |
      | nodeName      | Compute                                                                               |
      | propertyName  | os_arch                                                                               |
      | propertyValue | x86_64                                                                                |
    And I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateCapabilityPropertyValueOperation |
      | nodeName       | Compute                                                                                     |
      | capabilityName | compute                                                                                     |
      | propertyName   | containee_types                                                                             |
      | propertyValue  | dummy                                                                                       |
    And I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateCapabilityPropertyValueOperation |
      | nodeName       | Compute                                                                                     |
      | capabilityName | host                                                                                        |
      | propertyName   | containee_types                                                                             |
      | propertyValue  | dummy                                                                                       |
    And I save the topology
    And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes

  @reset
  Scenario: Runtime topology should not be impacted by updates on the version topology when snapshot
    When I deploy it
    When I execute the operation
      | type     | org.alien4cloud.tosca.editor.operations.nodetemplate.DeleteNodeOperation |
      | nodeName | apacheLBGroovy                                                           |
    And I save the topology
    When I ask the runtime topology of the application "ALIEN" on the location "Thark location" of "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain a nodetemplate named "apacheLBGroovy" and type "fastconnect.nodes.apacheLBGroovy"
    And The RestResponse should contain a nodetemplate named "Compute" and type "alien.nodes.mock.Compute"
    When I try to retrieve the created topology
    Then I should receive a RestResponse with no error
    And The RestResponse should contain a nodetemplate named "Compute" and type "tosca.nodes.Compute"
    And The RestResponse should not contain a nodetemplate named "apacheLBGroovy"

#  @reset
#  Scenario: get_input must be processed in a runtime topology
#    And I execute the operation
#      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
#      | inputName               | os_arch                                                          |
#      | propertyDefinition.type | string                                                           |
#    And I execute the operation
#      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodePropertyAsInputOperation |
#      | nodeName     | Compute                                                                                     |
#      | propertyName | os_arch                                                                                     |
#      | inputName    | os_arch                                                                                     |
#    And I save the topology
#    When I deploy it
#    When I ask the runtime topology of the application "ALIEN" on the location "Thark location" of "Mount doom orchestrator"
#    Then The topology should contain a nodetemplate named "Compute" with property "os_arch" set to "x86_64"
