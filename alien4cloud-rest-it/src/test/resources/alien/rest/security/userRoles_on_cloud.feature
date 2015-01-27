Feature: Deployment on cloud must be secured

Background:
  Given I am authenticated with "ADMIN" role
  And There are these users in the system
    | sangoku |
    | krilin  |
  And I add a role "APPLICATIONS_MANAGER" to user "sangoku"
  And I add a role "APPLICATIONS_MANAGER" to user "krilin"
  And I upload a plugin
  And I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
  And I enable the cloud "Mount doom cloud"
  And I have already created a cloud image with name "Ubuntu Trusty", architecture "x86_64", type "linux", distribution "Ubuntu" and version "14.04.1"
  And I add the cloud image "Ubuntu Trusty" to the cloud "Mount doom cloud"
  And I add the flavor with name "small", number of CPUs 2, disk size 32 and memory size 2048 to the cloud "Mount doom cloud"
  And I match the template composed of image "Ubuntu Trusty" and flavor "small" of the cloud "Mount doom cloud" to the PaaS resource "SMALL_LINUX"
  And I add a role "CLOUD_DEPLOYER" to user "sangoku" on the resource type "CLOUD" named "Mount doom cloud"
  And I upload the archive "tosca base types 1.0"
  And I should receive a RestResponse with no error

Scenario: Deploy on a cloud without given right must fail
  Given I am authenticated with user named "krilin"
  And I have an application "ALIEN_CLOUD_FAIL_NO_RIGHT" with a topology containing a nodeTemplate "compute" related to "tosca.nodes.Compute:1.0"
  And I must have an environment named "Environment" for application "ALIEN_CLOUD_FAIL_NO_RIGHT"
  And I update the environment named "Environment" to use cloud "Mount doom cloud" for application "ALIEN_CLOUD_FAIL_NO_RIGHT"
  When I deploy the application "ALIEN_CLOUD_FAIL_NO_RIGHT" with cloud "Mount doom cloud" for the topology without waiting for the end of deployment
  Then I should receive a RestResponse with an error code 606

Scenario: Deploy on a cloud with right given must succeed
  Given I am authenticated with user named "sangoku"
  And I have an application "ALIEN_CLOUD_SUCCESS_WITH_RIGHT" with a topology containing a nodeTemplate "compute" related to "tosca.nodes.Compute:1.0"
  And I deploy the application "ALIEN_CLOUD_SUCCESS_WITH_RIGHT" with cloud "Mount doom cloud" for the topology
  Then I should receive a RestResponse with no error
