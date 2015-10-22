Feature: trigger custom commands

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-wd06"
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
    And I add a role "DEPLOYER" to user "sangoku" on the resource type "LOCATION" named "Mount doom cloud"
    And I am authenticated with user named "sangoku"
    And I upload the archive "tosca base types 1.0"
    And I should receive a RestResponse with no error
    And I upload the archive "sample apache lb types 0.1"
    And I should receive a RestResponse with no error
    Given I have an application "ALIEN" with a topology containing a nodeTemplate "apacheLBGroovy" related to "fastconnect.nodes.apacheLBGroovy:0.1"
    And I have added a node template "Compute" related to the "tosca.nodes.Compute:1.0" node type
    And I add a relationship of type "tosca.relationships.HostedOn" defined in archive "tosca-base-types" version "1.0" with source "apacheLBGroovy" and target "Compute" for requirement "host" of type "tosca.capabilities.Container" and target capability "compute"
    And I deploy the application "ALIEN" with cloud "Mount doom cloud" for the topology

  Scenario: Trigger a custom command other than [updateWar,updateWarFile, addNode] on apache LB node and throw an operation failed error
    When I trigger on the node template "apacheLBGroovy" the custom command "removeNode" of the interface "custom" for application "ALIEN"
    Then I should receive a RestResponse with an error code 371

  Scenario: Trigger a custom command updateWarFile on an deployed application with success
    When I trigger on the node template "apacheLBGroovy" the custom command "updateWarFile" of the interface "custom" for application "ALIEN"
    Then The operation response should contain the result "OK" for instance "1"

  Scenario: Trigger a custom command updateWar on an deployed application with missing parameters error
    When I trigger on the node template "apacheLBGroovy" the custom command "updateWar" of the interface "custom" for application "ALIEN"
    Then I should receive a RestResponse with an error code 805

  Scenario: Trigger a custom command failure: interface not existing
    When I trigger on the node template "apacheLBGroovy" the custom command "addNode" of the interface "IDoNotSeeYou" for application "ALIEN"
    Then I should receive a RestResponse with an error code 504 and a message containing "Interface [IDoNotSeeYou] not found in the node template [apacheLBGroovy]"

  Scenario: Trigger a custom command failure: operation not defined in interface
    When I trigger on the node template "apacheLBGroovy" the custom command "virtualCommand" of the interface "custom" for application "ALIEN"
    Then I should receive a RestResponse with an error code 504 and a message containing "Operation [virtualCommand] is not defined in the interface [custom] of the node [apacheLBGroovy]"
