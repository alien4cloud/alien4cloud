Feature: get runtime topology

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
    And I have an application "ALIEN" with a topology containing a nodeTemplate "apacheLBGroovy" related to "fastconnect.nodes.apacheLBGroovy:0.1"
    And I have added a node template "Compute" related to the "tosca.nodes.Compute:1.0" node type
    And I add a relationship of type "tosca.relationships.HostedOn" defined in archive "tosca-base-types" version "1.0" with source "apacheLBGroovy" and target "Compute" for requirement "host" of type "tosca.capabilities.Container" and target capability "compute"
    And I add a scaling policy to the node "Compute"
    And I deploy the application "ALIEN" with cloud "mock cloud" for the topology

  Scenario: Getting the runtime version of the deployed topology
    Given I have deleted a node template "apacheLBGroovy" from the topology
    When I ask the runtime topology of the application "ALIEN" on the cloud "mock cloud"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain a nodetemplate named "apacheLBGroovy" and type "fastconnect.nodes.apacheLBGroovy"
    And The RestResponse should contain a nodetemplate named "Compute" and type "tosca.nodes.Compute"
    When I try to retrieve the created topology
    Then I should receive a RestResponse with no error
    And The RestResponse should contain a nodetemplate named "Compute" and type "tosca.nodes.Compute"
    And The RestResponse should not contain a nodetemplate named "apacheLBGroovy"
