Feature: Match topology's node to cloud resources.

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload a plugin
    And I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
    And I enable the cloud "Mount doom cloud"
    And I upload the archive "tosca base types 1.0"
    And I should receive a RestResponse with no error
    And There are these users in the system
      | sangoku |
    And I add a role "APPLICATIONS_MANAGER" to user "sangoku"
    And I add a role "CLOUD_DEPLOYER" to user "sangoku" on the resource type "CLOUD" named "Mount doom cloud"
    And I have already created a cloud image with name "Windows 7", architecture "x86_64", type "windows", distribution "Windows" and version "7.0"
    And I have already created a cloud image with name "Ubuntu Trusty", architecture "x86_64", type "linux", distribution "Ubuntu" and version "14.04.1"
    And I add the cloud image "Windows 7" to the cloud "Mount doom cloud"
    And I add the cloud image "Ubuntu Trusty" to the cloud "Mount doom cloud"
    And I add the flavor with name "small", number of CPUs 2, disk size 32 and memory size 2048 to the cloud "Mount doom cloud"
    And I add the flavor with name "medium", number of CPUs 4, disk size 64 and memory size 4096 to the cloud "Mount doom cloud"
    And I am authenticated with user named "sangoku"
    And I have an application "ALIEN" with a topology containing a nodeTemplate "Compute" related to "tosca.nodes.Compute:1.0"
    And I add a node template "Java" related to the "fastconnect.nodes.JavaChef:1.0" node type
    And I assign the cloud with name "Mount doom cloud" for the application

  Scenario: If no matching has been done for templates, then templates should not be available for matching
    When I match for resources for my application on the cloud
    Then I should receive an empty match result
    And I am authenticated with "ADMIN" role
    And I match the template composed of image "Windows 7" and flavor "small" of the cloud "Mount doom cloud" to the PaaS resource "SMALL_WINDOWS"
    And I am authenticated with user named "sangoku"
    And I match for resources for my application on the cloud
    Then I should receive a match result with 1 compute templates for the node "Compute":
      | Windows 7 | small |
    And I am authenticated with "ADMIN" role
    And I match the template composed of image "Windows 7" and flavor "medium" of the cloud "Mount doom cloud" to the PaaS resource "MEDIUM_WINDOWS"
    And I match the template composed of image "Ubuntu Trusty" and flavor "small" of the cloud "Mount doom cloud" to the PaaS resource "SMALL_LINUX"
    And I match the template composed of image "Ubuntu Trusty" and flavor "medium" of the cloud "Mount doom cloud" to the PaaS resource "MEDIUM_LINUX"
    And I am authenticated with user named "sangoku"
    And I match for resources for my application on the cloud
    Then I should receive a match result with 4 compute templates for the node "Compute":
      | Windows 7     | small  |
      | Windows 7     | medium |
      | Ubuntu Trusty | small  |
      | Ubuntu Trusty | medium |

  Scenario: If application has unmatched resource so it cannot be deployed
    When I match for resources for my application on the cloud
    Then I should receive an empty match result
    When I deploy the application "ALIEN" with cloud "Mount doom cloud" for the topology without waiting for the end of deployment
    Then I should receive a RestResponse with an error code 603
