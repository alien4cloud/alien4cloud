Feature: Listen to events of an deployed application.

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload a plugin
    And I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
    And I enable the cloud "Mount doom cloud"
    And I upload the archive file that is "containing default tosca base types"
    And There are these users in the system
      | sangoku |
    And I add a role "APPLICATIONS_MANAGER" to user "sangoku"
    And I add a role "CLOUD_DEPLOYER" to user "sangoku" on the resource type "CLOUD" named "Mount doom cloud"
    And I am authenticated with user named "sangoku"
    And I have an application "ALIEN" with a topology containing a nodeTemplate "compute" related to "tosca.nodes.Compute:1.0"

  Scenario: Deploy an application and listen to events
    Given I deploy the application "ALIEN" with cloud "Mount doom cloud" for the topology without waiting for the end of deployment
    When I start listening to "instance-state" event
    And I start listening to "deployment-status" event
    Then I should receive "deployment-status" events that containing
      | DEPLOYMENT_IN_PROGRESS |
      | DEPLOYED               |
    And I should receive "instance-state" events that containing
      | init        |
      | creating    |
      | created     |
      | configuring |
      | configured  |
      | starting    |
      | started     |
      
   Scenario: Deploy an application with blockstorage and listen to events
#   	Given I have an application with name "BLOCKSTORAGE-APPLICATION" 
	Given I have an application "BLOCKSTORAGE-APPLICATION" with a topology containing a nodeTemplate "compute" related to "tosca.nodes.Compute:1.0"
    Given I deploy the application "BLOCKSTORAGE-APPLICATION" with cloud "Mount doom cloud" for the topology without waiting for the end of deployment
    When I start listening to "instance-state" event
    And I start listening to "storage" event
    Then I should receive "instance-state" events that containing
      | init        |
      | creating    |
      | configuring |
      | configured  |
      | starting    |
      | started     |
    And I should receive "storage" events that containing
      | created |
