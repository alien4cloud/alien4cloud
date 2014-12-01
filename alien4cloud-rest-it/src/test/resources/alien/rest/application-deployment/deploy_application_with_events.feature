Feature: Listen to events of an deployed application.

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload a plugin
    And I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
    And I enable the cloud "Mount doom cloud"
    And I have already created a cloud image with name "Ubuntu Trusty", architecture "x86_64", type "linux", distribution "Ubuntu" and version "14.04.1"
    And I add the cloud image "Ubuntu Trusty" to the cloud "Mount doom cloud"
    And I add the flavor with name "small", number of CPUs 2, disk size 32 and memory size 2048 to the cloud "Mount doom cloud"
    And I match the template composed of image "Ubuntu Trusty" and flavor "small" of the cloud "Mount doom cloud" to the PaaS resource "SMALL_WINDOWS"
    And I upload the archive "tosca base types 1.0"
    And I should receive a RestResponse with no error
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
