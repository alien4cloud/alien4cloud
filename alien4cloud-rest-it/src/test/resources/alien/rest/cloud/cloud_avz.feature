Feature: Create cloud storages

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload a plugin
    And I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
    And I enable the cloud "Mount doom cloud"
    And I upload the archive "tosca-normative-types"
    And I upload the archive "alien-base-types"
    And I upload the archive "alien-extended-storage-types"
    And There are these users in the system
      | sangoku |
    And I add a role "APPLICATIONS_MANAGER" to user "sangoku"
    And I add a role "CLOUD_DEPLOYER" to user "sangoku" on the resource type "CLOUD" named "Mount doom cloud"
    And I have already created a cloud image with name "Ubuntu Trusty", architecture "x86_64", type "linux", distribution "Ubuntu" and version "14.04.1"
    And I add the cloud image "Ubuntu Trusty" to the cloud "Mount doom cloud" and match it to paaS image "UBUNTU"
    And I add the flavor with name "medium", number of CPUs 4, disk size 64 and memory size 4096 to the cloud "Mount doom cloud" and match it to paaS flavor "3"
    And I add the availability zone with id "paris" and description "Data-center at Paris" to the cloud "Mount doom cloud"
    And I add the availability zone with id "toulouse" and description "Data-center at Toulouse" to the cloud "Mount doom cloud"
    And I match the availability zone with name "paris" of the cloud "Mount doom cloud" to the PaaS resource "paris-zone"
    And I match the availability zone with name "toulouse" of the cloud "Mount doom cloud" to the PaaS resource "toulouse-zone"
    And I am authenticated with user named "sangoku"
    And I have an application "ALIEN" with a topology containing a nodeTemplate "Compute1" related to "tosca.nodes.Compute:1.0"
    And I add a node template "Compute2" related to the "tosca.nodes.Compute:1.0" node type
    And I add a group with name "HA_group" whose members are "Compute1, Compute2"
    And I assign the cloud with name "Mount doom cloud" for the application

  Scenario: Match a topology for storage, no filter
    When I match for resources for my application on the cloud
    Then I should receive a match result with 2 availability zones for the group "HA_group":
      | paris    | Data-center at Paris    |
      | toulouse | Data-center at Toulouse |

  Scenario: Should be able to add and remove a storage
    When I match for resources for my application on the cloud
    Then I should receive a match result with 2 availability zones for the group "HA_group":
      | paris    | Data-center at Paris    |
      | toulouse | Data-center at Toulouse |
    And I am authenticated with "ADMIN" role
    And I add the availability zone with id "grenoble" and description "Data-center at Grenoble" to the cloud "Mount doom cloud"
    And I match the availability zone with name "grenoble" of the cloud "Mount doom cloud" to the PaaS resource "grenoble-zone"
    And I match for resources for my application on the cloud
    Then I should receive a match result with 3 availability zones for the group "HA_group":
      | paris    | Data-center at Paris    |
      | toulouse | Data-center at Toulouse |
      | grenoble | Data-center at Grenoble |
    Then I remove the availability zone with name "grenoble" from the cloud "Mount doom cloud"
    When I match for resources for my application on the cloud
    Then I should receive a match result with 2 availability zones for the group "HA_group":
      | paris    | Data-center at Paris    |
      | toulouse | Data-center at Toulouse |