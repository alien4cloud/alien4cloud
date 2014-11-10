Feature: get runtime topology

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload a plugin
    And I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
    And I enable the cloud "Mount doom cloud"
    And I have already created a cloud image with name "Ubuntu Trusty", architecture "x86_64", type "linux", distribution "Ubuntu" and version "14.04.1"
    And I add the cloud image "Ubuntu Trusty" to the cloud "Mount doom cloud"
    And I add the flavor with name "small", number of CPUs 2, disk size 32 and memory size 2048 to the cloud "Mount doom cloud"
    And I match the template composed of image "Ubuntu Trusty" and flavor "small" of the cloud "Mount doom cloud" to the PaaS resource "SMALL_LINUX"
    And There are these users in the system
      | sangoku |
    And I add a role "APPLICATIONS_MANAGER" to user "sangoku"
    And I add a role "COMPONENTS_MANAGER" to user "sangoku"
    And I add a role "CLOUD_DEPLOYER" to user "sangoku" on the resource type "CLOUD" named "Mount doom cloud"
    And I am authenticated with user named "sangoku"
    And I upload the archive file that is "containing default tosca base types"
    And I upload the archive file that is "containing default java types"
    And I upload the archive file that is "containing default apacheLB types"
    And I upload the archive file that is "csar file containing ubuntu types V0.1" 
    And I have an application "ALIEN" with a topology containing a nodeTemplate "apacheLBGroovy" related to "fastconnect.nodes.apacheLBGroovy:0.1"
    And I have added a node template "Ubuntu" related to the "alien.nodes.Ubuntu:0.1" node type
    And I add a relationship of type "tosca.relationships.HostedOn" defined in archive "tosca-base-types" version "1.0" with source "apacheLBGroovy" and target "Ubuntu" for requirement "host" of type "tosca.capabilities.Container" and target capability "Ubuntu"
    And I add a scaling policy to the node "Ubuntu"
    And I deploy the application "ALIEN" with cloud "Mount doom cloud" for the topology

  Scenario: Getting the runtime version of the deployed topology
    Given I have deleted a node template "apacheLBGroovy" from the topology
    When I ask the runtime topology of the application "ALIEN" on the cloud "Mount doom cloud"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain a nodetemplate named "apacheLBGroovy" and type "fastconnect.nodes.apacheLBGroovy"
    And The RestResponse should contain a nodetemplate named "Ubuntu" and type "alien.nodes.Ubuntu"
    When I try to retrieve the created topology
    Then I should receive a RestResponse with no error
    And The RestResponse should contain a nodetemplate named "Ubuntu" and type "alien.nodes.Ubuntu"
    And The RestResponse should not contain a nodetemplate named "apacheLBGroovy"
