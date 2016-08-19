#Feature: Listen to events of an deployed application
#
#  Background:
#    Given I am authenticated with "ADMIN" role
#    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
#    And I upload a plugin
#    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"
#    And I enable the orchestrator "Mount doom orchestrator"
#    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
#    And I create a resource of type "alien.nodes.mock.openstack.Flavor" named "Small" related to the location "Mount doom orchestrator"/"Thark location"
#    And I update the property "id" to "1" for the resource named "Small" related to the location "Mount doom orchestrator"/"Thark location"
#    And I create a resource of type "alien.nodes.mock.openstack.Image" named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
#    And I update the property "id" to "img1" for the resource named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
#    And I autogenerate the on-demand resources for the location "Mount doom orchestrator"/"Thark location"
#
#    And There are these users in the system
#      | sangoku |
#    And I add a role "APPLICATIONS_MANAGER" to user "sangoku"
#    And I add a role "DEPLOYER" to user "sangoku" on the resource type "LOCATION" named "Thark location"
#    And I am authenticated with user named "sangoku"
#    And I pre register orchestrator properties
#      | managementUrl | http://cloudifyurl:8099 |
#      | numberBackup  | 1                       |
#      | managerEmail  | admin@alien.fr          |
#
#
#  @reset
#  Scenario: Deploy an application and listen to events
#    Given I create a new application with name "ALIEN" and description "" and node templates
#      | Compute | tosca.nodes.Compute:1.0.0-SNAPSHOT |
#    And I deploy the application "ALIEN" on the location "Mount doom orchestrator"/"Thark location" without waiting for the end of deployment
#    When I start listening to "instance-state" event
#    And I start listening to "deployment-status" event
#    Then I should receive "deployment-status" events that contains
#      | DEPLOYMENT_IN_PROGRESS |
#      | DEPLOYED               |
#    And I should receive "instance-state" events that contains
#      | initial     |
#      | creating    |
#      | created     |
#      | configuring |
#      | configured  |
#      | starting    |
#      | started     |

#  Scenario: Deploy an application with blockstorage and listen to events
#    Given I have an application "BLOCKSTORAGE-APPLICATION" with a topology containing a nodeTemplate "Compute" related to "tosca.nodes.Compute:1.0.0-SNAPSHOT"
#    And I deploy the application "BLOCKSTORAGE-APPLICATION" on the location "Mount doom orchestrator"/"Thark location" without waiting for the end of deployment
#    When I start listening to "instance-state" event
#    And I start listening to "persistent" event
#    And I start listening to "deployment-status" event
#    Then I should receive "deployment-status" events that containing
#      | DEPLOYMENT_IN_PROGRESS |
#      | DEPLOYED               |
#    Then I should receive "instance-state" events that containing
#      | initial     |
#      | creating    |
#      | created    |
#      | configuring |
#      | configured  |
#      | starting    |
#      | started     |
#    And I should receive persistent resources events containing the following nodes and properties
#      | Compute | volume_id |
