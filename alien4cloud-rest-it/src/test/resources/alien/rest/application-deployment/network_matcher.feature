Feature: Match topology's network to cloud's network.

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload a plugin
    And I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
    And I enable the cloud "Mount doom cloud"
    And I upload the archive "tosca base types 1.0"
    And There are these users in the system
      | sangoku |
    And I add a role "APPLICATIONS_MANAGER" to user "sangoku"
    And I add a role "CLOUD_DEPLOYER" to user "sangoku" on the resource type "CLOUD" named "Mount doom cloud"
    And I have already created a cloud image with name "Ubuntu Trusty", architecture "x86_64", type "linux", distribution "Ubuntu" and version "14.04.1"
    And I add the cloud image "Ubuntu Trusty" to the cloud "Mount doom cloud" and match it to paaS image "UBUNTU"
    And I add the flavor with name "medium", number of CPUs 4, disk size 64 and memory size 4096 to the cloud "Mount doom cloud" and match it to paaS flavor "3"
    And I add the network with name "private" and CIDR "192.168.1.0/24" and IP version 4 and gateway "192.168.1.1" to the cloud "Mount doom cloud"
    And I add the network with name "public" and CIDR "192.168.2.0/24" and IP version 4 and gateway "192.168.2.1" to the cloud "Mount doom cloud"
    And I match the network with name "private" of the cloud "Mount doom cloud" to the PaaS resource "alienPrivateNetwork"
    And I match the network with name "public" of the cloud "Mount doom cloud" to the PaaS resource "alienPublicNetwork"
    And I am authenticated with user named "sangoku"
    And I have an application "ALIEN" with a topology containing a nodeTemplate "Network" related to "tosca.nodes.Network:1.0"
    And I add a node template "Java" related to the "fastconnect.nodes.JavaChef:1.0" node type
    And I assign the cloud with name "Mount doom cloud" for the application

  Scenario: Match a topology for networks, filter by cidr
    When I match for resources for my application on the cloud
    Then I should receive a match result with 2 networks for the node "Network":
      | private | 192.168.1.0/24 | 4 | 192.168.1.1 | alienPrivateNetwork |
      | public  | 192.168.2.0/24 | 4 | 192.168.2.1 | alienPublicNetwork  |
    When I update the node template "Network"'s property "cidr" to "192.168.1.0/24"
    And I match for resources for my application on the cloud
    Then I should receive a match result with 1 networks for the node "Network":
      | private | 192.168.1.0/24 | 4 | 192.168.1.1 | alienPrivateNetwork |
    When I update the node template "Network"'s property "cidr" to "192.167.1.0/24"
    And I match for resources for my application on the cloud
    Then I should receive a match result with no networks for the node "Network"

  Scenario: Match a topology for networks, filter by name
    When I match for resources for my application on the cloud
    Then I should receive a match result with 2 networks for the node "Network":
      | private | 192.168.1.0/24 | 4 | 192.168.1.1 | alienPrivateNetwork |
      | public  | 192.168.2.0/24 | 4 | 192.168.2.1 | alienPublicNetwork  |
    When I update the node template "Network"'s property "network_name" to "private"
    And I match for resources for my application on the cloud
    Then I should receive a match result with 1 networks for the node "Network":
      | private | 192.168.1.0/24 | 4 | 192.168.1.1 | alienPrivateNetwork |
    When I update the node template "Network"'s property "network_name" to "unknown"
    And I match for resources for my application on the cloud
    Then I should receive a match result with no networks for the node "Network"

  Scenario: Match a topology for networks, filter by gateway ip
    When I match for resources for my application on the cloud
    Then I should receive a match result with 2 networks for the node "Network":
      | private | 192.168.1.0/24 | 4 | 192.168.1.1 | alienPrivateNetwork |
      | public  | 192.168.2.0/24 | 4 | 192.168.2.1 | alienPublicNetwork  |
    When I update the node template "Network"'s property "gateway_ip" to "192.168.1.1"
    And I match for resources for my application on the cloud
    Then I should receive a match result with 1 networks for the node "Network":
      | private | 192.168.1.0/24 | 4 | 192.168.1.1 | alienPrivateNetwork |
    When I update the node template "Network"'s property "gateway_ip" to "192.167.1.1"
    And I match for resources for my application on the cloud
    Then I should receive a match result with no networks for the node "Network"

  Scenario: Match a topology for networks, filter by ip version
    When I match for resources for my application on the cloud
    Then I should receive a match result with 2 networks for the node "Network":
      | private | 192.168.1.0/24 | 4 | 192.168.1.1 | alienPrivateNetwork |
      | public  | 192.168.2.0/24 | 4 | 192.168.2.1 | alienPublicNetwork  |
    When I update the node template "Network"'s property "ip_version" to "4"
    And I match for resources for my application on the cloud
    Then I should receive a match result with 2 networks for the node "Network":
      | private | 192.168.1.0/24 | 4 | 192.168.1.1 | alienPrivateNetwork |
      | public  | 192.168.2.0/24 | 4 | 192.168.2.1 | alienPublicNetwork  |
    When I update the node template "Network"'s property "ip_version" to "6"
    And I match for resources for my application on the cloud
    Then I should receive a match result with no networks for the node "Network"