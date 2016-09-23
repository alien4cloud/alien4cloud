Feature: trigger custom commands

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I create a resource of type "alien.nodes.mock.openstack.Flavor" named "Small" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "1" for the resource named "Small" related to the location "Mount doom orchestrator"/"Thark location"
    And I create a resource of type "alien.nodes.mock.openstack.Image" named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "img1" for the resource named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I autogenerate the on-demand resources for the location "Mount doom orchestrator"/"Thark location"
    And I upload the archive "custom-interface-mock-types"
    And I should receive a RestResponse with no error

    And There are these users in the system
      | sangoku |
    And I add a role "APPLICATIONS_MANAGER" to user "sangoku"
    And I add a role "DEPLOYER" to user "sangoku" on the resource type "LOCATION" named "Thark location"
    And I am authenticated with user named "sangoku"

    And I pre register orchestrator properties
      | managementUrl | http://cloudifyurl:8099 |
      | numberBackup  | 1                       |
      | managerEmail  | admin@alien.fr          |

    And I create a new application with name "ALIEN" and description "" and node templates
      | Compute         | tosca.nodes.Compute:1.0.0-SNAPSHOT                     |
      | customInterface | alien4cloud.tests.nodes.CustomInterface:1.1.0-SNAPSHOT |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | customInterface                                                                       |
      | relationshipName       | hostedOnCompute                                                                       |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0.0-SNAPSHOT                                                                        |
      | requirementName        | host                                                                                  |
      | target                 | Compute                                                                               |
      | targetedCapabilityName | host                                                                                  |
    And I save the topology
    And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes
    And I deploy it

  @reset
  Scenario: Trigger a custom command other than [updateWar,updateWarFile, addNode] on apache LB node and throw an operation failed error
    When I trigger on the node template "customInterface" the custom command "fail" of the interface "mock" for application "ALIEN"
    Then I should receive a RestResponse with an error code 371

  @reset
  Scenario: Trigger a custom command updateWarFile on an deployed application with success
    When I trigger on the node template "customInterface" the custom command "success" of the interface "mock" for application "ALIEN"
    Then The operation response should contain the result "OK" for instance "1"

  @reset
  Scenario: Trigger a custom command updateWar on an deployed application with missing parameters error
    When I trigger on the node template "customInterface" the custom command "success_param" of the interface "mock" for application "ALIEN"
    Then I should receive a RestResponse with an error code 805

  @reset
  Scenario: Trigger a custom command failure: interface not existing
    When I trigger on the node template "customInterface" the custom command "success" of the interface "IDoNotSeeYou" for application "ALIEN"
    Then I should receive a RestResponse with an error code 504 and a message containing "Interface [IDoNotSeeYou] not found in the node template [customInterface] related to [alien4cloud.tests.nodes.CustomInterface:1.1.0-SNAPSHOT]"

  @reset
  Scenario: Trigger a custom command failure: operation not defined in interface
    When I trigger on the node template "customInterface" the custom command "virtualCommand" of the interface "mock" for application "ALIEN"
    Then I should receive a RestResponse with an error code 504 and a message containing "Operation [virtualCommand] is not defined in the interface [mock] of the node [customInterface]"
