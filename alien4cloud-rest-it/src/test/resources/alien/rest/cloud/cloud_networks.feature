Feature: Create cloud networks

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload a plugin
    And I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"

  Scenario: Add/remove network to cloud
    When I add the network with name "private" and CIDR "192.168.1.0/24" and IP version 4 and gateway "192.168.1.1" to the cloud "Mount doom cloud"
    Then I should receive a RestResponse with no error
    And The cloud with name "Mount doom cloud" should have 1 networks as resources:
      | private | 192.168.1.0/24 | 4 | 192.168.1.1 |
    When I add the network with name "public" and CIDR "177.68.1.0/24" and IP version 4 and gateway "177.68.1.1" to the cloud "Mount doom cloud"
    Then The cloud with name "Mount doom cloud" should have 2 networks as resources:
      | private | 192.168.1.0/24 | 4 | 192.168.1.1 |
      | public  | 177.68.1.0/24  | 4 | 177.68.1.1  |
    When I remove the network with name "private" from the cloud "Mount doom cloud"
    Then I should receive a RestResponse with no error
    And The cloud with name "Mount doom cloud" should have 1 networks as resources:
      | public | 177.68.1.0/24 | 4 | 177.68.1.1 |
    When I remove the network with name "public" from the cloud "Mount doom cloud"
    Then The cloud with name "Mount doom cloud" should not have any network as resources