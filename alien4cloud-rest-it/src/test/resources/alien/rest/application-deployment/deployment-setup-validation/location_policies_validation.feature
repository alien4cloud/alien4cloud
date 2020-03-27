Feature: location policy validation in deployment setup

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"

    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I create a resource of type "org.alien4cloud.nodes.mock.openstack.Flavor" named "Small" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "1" for the resource named "Small" related to the location "Mount doom orchestrator"/"Thark location"
    And I create a resource of type "org.alien4cloud.nodes.mock.openstack.Image" named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "img1" for the resource named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I autogenerate the on-demand resources for the location "Mount doom orchestrator"/"Thark location"
    And I create a new application with name "ALIEN" and description "ALIEN_1" and node templates
      | Compute | tosca.nodes.Compute:1.0.0-SNAPSHOT |

  @reset
  Scenario: Successfully setting a location policy should be considered valid
    Given I get the deployment topology for the current application
    And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes
    And I set the following orchestrator properties
      | managerEmail  | toto@titi.fr            |
      | managementUrl | http://cloudifyurl:8099 |
      | numberBackup  | 1                       |
    When I check for the valid status of the deployment topology
    Then the deployment topology should be valid

  @reset
  Scenario: Missing location policy should be considered invalid
    When I check for the valid status of the deployment topology
    Then the deployment topology should not be valid
    And there should be a missing location policy task

  @reset
  Scenario: Selected disabled location should be considered invalid
    Given I get the deployment topology for the current application
    And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes
    And I set the following orchestrator properties
      | managerEmail  | toto@titi.fr            |
      | managementUrl | http://cloudifyurl:8099 |
      | numberBackup  | 1                       |
    When I disable the orchestrator "Mount doom orchestrator"
    And I check for the valid status of the deployment topology
    Then the deployment topology should not be valid
    And there should be an unavailable location task with code "LOCATION_DISABLED" and the following orchestrators and locations
      | Mount doom orchestrator | Thark location |

  @reset
  Scenario: Selected unauthorized location should be considered invalid
    Given There is a "frodon" user in the system
    And I add a role "APPLICATION_MANAGER" to user "frodon" on the resource type "APPLICATION" named "ALIEN"
    And I grant access to the resource type "LOCATION" named "Thark location" to the user "frodon"
    And I am authenticated with user named "frodon"
    And I get the deployment topology for the current application
    And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes
    And I set the following orchestrator properties
      | managerEmail  | toto@titi.fr            |
      | managementUrl | http://cloudifyurl:8099 |
      | numberBackup  | 1                       |
    And  I am authenticated with "ADMIN" role
    And I revoke access to the resource type "LOCATION" named "Thark location" from the user "frodon"
    And I am authenticated with user named "frodon"

    When I check for the valid status of the deployment topology
    Then the deployment topology should not be valid
    And there should be a missing location policy task
#    And there should be an unavailable location task with code "LOCATION_UNAUTHORIZED" and the following orchestrators and locations
#      | Mount doom orchestrator | Thark location |