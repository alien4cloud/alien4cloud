Feature: trigger custom commands

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload a plugin
    And I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
    And I enable the cloud "Mount doom cloud"
    And I have already created a cloud image with name "Ubuntu Trusty", architecture "x86_64", type "linux", distribution "Ubuntu" and version "14.04.1"
    And I add the cloud image "Ubuntu Trusty" to the cloud "Mount doom cloud" and match it to paaS image "UBUNTU"
    And I add the flavor with name "small", number of CPUs 2, disk size 32 and memory size 2048 to the cloud "Mount doom cloud" and match it to paaS flavor "2"
    And There are these users in the system
      | sangoku |
    And I add a role "APPLICATIONS_MANAGER" to user "sangoku"
    And I add a role "COMPONENTS_MANAGER" to user "sangoku"
    And I add a role "CLOUD_DEPLOYER" to user "sangoku" on the resource type "CLOUD" named "Mount doom cloud"
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
