Feature: Deployment setup feature.

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload a plugin
    And I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
    And I enable the cloud "Mount doom cloud"
    And I upload the archive "normative types 1.0.0-wd03"
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
    And I match the template composed of image "Windows 7" and flavor "small" of the cloud "Mount doom cloud" to the PaaS resource "SMALL_WINDOWS"
    And I match the template composed of image "Windows 7" and flavor "medium" of the cloud "Mount doom cloud" to the PaaS resource "MEDIUM_WINDOWS"
    And I match the template composed of image "Ubuntu Trusty" and flavor "small" of the cloud "Mount doom cloud" to the PaaS resource "SMALL_LINUX"
    And I match the template composed of image "Ubuntu Trusty" and flavor "medium" of the cloud "Mount doom cloud" to the PaaS resource "MEDIUM_LINUX"
    And I am authenticated with user named "sangoku"
    And I have an application "ALIEN" with a topology containing a nodeTemplate "Compute" related to "tosca.nodes.Compute:1.0"
    And I add a node template "Java" related to the "fastconnect.nodes.JavaChef:1.0" node type
    And I assign the cloud with name "Mount doom cloud" for the application

  Scenario: Select a compute template for a node
    When I select the the template composed of image "Ubuntu Trusty" and flavor "medium" for my node "Compute"
    Then I should receive a RestResponse with no error
    And The deployment setup of the application should contain following resources mapping:
      | Compute | Ubuntu Trusty | medium |

  Scenario: Modify node template's name, property and the deployment setup should be updated with the modification
    When I select the the template composed of image "Ubuntu Trusty" and flavor "medium" for my node "Compute"
    Then I should receive a RestResponse with no error
    And The deployment setup of the application should contain following resources mapping:
      | Compute | Ubuntu Trusty | medium |
    When I update the node template's name from "Compute" to "NewCompute"
    And I update the node template "NewCompute"'s property "os_type" to "linux"
    Then The deployment setup of the application should contain following resources mapping:
      | NewCompute | Ubuntu Trusty | small |
    When I select the the template composed of image "Ubuntu Trusty" and flavor "medium" for my node "Compute"
    And I update the node template "NewCompute"'s property "os_type" to "windows"
    Then The deployment setup of the application should contain following resources mapping:
      | NewCompute | Windows 7 | small |
    And I update the node template "NewCompute"'s property "os_version" to "unknown_os_version"
    Then The deployment setup of the application should contain no resources mapping
