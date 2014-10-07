Feature: trigger custom commands

Background:
  Given I am authenticated with "ADMIN" role
  And I upload a plugin
  And I create a cloud with name "mock cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
  And I enable the cloud "mock cloud"
  And There are these users in the system
    | sangoku |
  And I add a role "APPLICATIONS_MANAGER" to user "sangoku"
  And I add a role "COMPONENTS_MANAGER" to user "sangoku"
  And I add a role "CLOUD_DEPLOYER" to user "sangoku" on the resource type "CLOUD" named "mock cloud"
  And I am authenticated with user named "sangoku"
  And I upload the archive file that is "containing default tosca base types"
  And I upload the archive file that is "containing default java types"
  And I upload the archive file that is "containing default apacheLB types"
  Given I have an application "ALIEN" with a topology containing a nodeTemplate "apacheLBGroovy" related to "fastconnect.nodes.apacheLBGroovy:0.1"
  And I have added a node template "Compute" related to the "tosca.nodes.Compute:1.0" node type
  And I add a relationship of type "tosca.relationships.HostedOn" defined in archive "tosca-base-types" version "1.0" with source "apacheLBGroovy" and target "Compute" for requirement "host" of type "tosca.capabilities.Container" and target capability "compute"
  And I add a scaling policy to the node "Compute"
  And I deploy the application "ALIEN" with cloud "mock cloud" for the topology

Scenario: Trigger a custom command other than [updateWar,updateWarFile, addNode] on apache LB node and throw an operation failed error
  When I trigger on the node template "apacheLBGroovy" the custom command "removeNode" of the interface "custom" on the cloud "mock cloud"
  Then I should receive a RestResponse with an error code 371

Scenario: Trigger a custom command updateWarFile on an deployed application with success
  When I trigger on the node template "apacheLBGroovy" the custom command "updateWarFile" of the interface "custom" on the cloud "mock cloud"
  Then The operation response should contain the result "OK" for instance "1"

Scenario: Trigger a custom command updateWar on an deployed application with missing parameters error
  When I trigger on the node template "apacheLBGroovy" the custom command "updateWar" of the interface "custom" on the cloud "mock cloud"
  Then I should receive a RestResponse with an error code 805

Scenario: Trigger a custom command failure: interface not existing
  When I trigger on the node template "apacheLBGroovy" the custom command "addNode" of the interface "IDoNotSeeYou" on the cloud "mock cloud"
  Then I should receive a RestResponse with an error code 504 and a message containing "Interface [IDoNotSeeYou] not found in the node template [apacheLBGroovy]"

Scenario: Trigger a custom command failure: operation not defined in interface
  When I trigger on the node template "apacheLBGroovy" the custom command "virtualCommand" of the interface "custom" on the cloud "mock cloud"
  Then I should receive a RestResponse with an error code 504 and a message containing "Operation [virtualCommand] is not defined in the interface [custom] of the node [apacheLBGroovy]"
