Feature: Deploy an application

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I create a resource of type "alien.nodes.mock.openstack.Flavor" named "Small" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "1" for the resource named "Small" related to the location "Mount doom orchestrator"/"Thark location"
    And I create a resource of type "alien.nodes.mock.openstack.Image" named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "img1" for the resource named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I autogenerate the on-demand resources for the location "Mount doom orchestrator"/"Thark location"

    And There are these users in the system
      | sangoku |
    And I add a role "APPLICATIONS_MANAGER" to user "sangoku"
    And I add a role "DEPLOYER" to user "sangoku" on the resource type "LOCATION" named "Thark location"
    And I am authenticated with user named "sangoku"

    And I pre register orchestrator properties
      | managementUrl | http://cloudifyurl:8099 |
      | numberBackup  | 1                       |
      | managerEmail  | admin@alien.fr          |
    And I have an application "ALIEN" with a topology containing a nodeTemplate "Compute" related to "tosca.nodes.Compute:1.0.0-SNAPSHOT"
    And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes
    And I deploy it
    And The application's deployment must succeed

  @reset
  Scenario: Deploy an application with success
    Given I search for log of the current application of type "deployment_status_change"
    And I sleep for 10 seconds so that indices are fully refreshed
    Then I should receive log entries that containing
      | deployment_status_change | Change deployment status to DEPLOYMENT_IN_PROGRESS |
      | deployment_status_change | Change deployment status to DEPLOYED               |
    Given I search for log of the current application of type "state_change"
    Then I should receive log entries that containing
      | state_change | Change state to creating    |
      | state_change | Change state to created     |
      | state_change | Change state to configuring |
      | state_change | Change state to configured  |
      | state_change | Change state to starting    |
      | state_change | Change state to started     |
    Given I search for log of the current application of type "state_change" and order by "timestamp" in "descending" order
    Then I should receive log entries that containing
      | state_change | Change state to started     |
      | state_change | Change state to starting    |
      | state_change | Change state to configured  |
      | state_change | Change state to configuring |
      | state_change | Change state to created     |
      | state_change | Change state to creating    |