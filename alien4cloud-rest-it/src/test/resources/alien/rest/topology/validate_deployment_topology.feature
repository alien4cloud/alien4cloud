Feature: Check if a deployment topology is valid

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload the archive "topology-input-artifact"
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
    And I create a new application with name "input-artifact" and description "Demo input artifact" based on the template with name "input-artifact"
    And I get the deployment topology for the current application
    And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes

  @reset
  Scenario: Check deployment topology's validity
    When I check for the valid status of the deployment topology
    Then the deployment topology should not be valid
    And the missing inputs artifacts should be
      | uploaded_war |
    When I upload a file located at "src/test/resources/data/artifacts/myWar.war" for the input artifact "uploaded_war"
    And I set the following orchestrator properties
      | managementUrl | http://cloudifyurl:8099 |
      | managerEmail  | a@b.c                   |
      | numberBackup  | 1                       |
    And I check for the valid status of the deployment topology
    Then the deployment topology should be valid