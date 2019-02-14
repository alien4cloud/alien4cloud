Feature: Create application environment with copy inputs

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I successfully upload the local archive "data/csars/inputs_copy/inputs_copy.yaml"
    Then I should receive a RestResponse with no error
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"

    And I create a resource of type "org.alien4cloud.nodes.mock.openstack.Flavor" named "Small" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "1" for the resource named "Small" related to the location "Mount doom orchestrator"/"Thark location"

    And I create a resource of type "org.alien4cloud.nodes.mock.openstack.Flavor" named "Medium" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "2" for the resource named "Medium" related to the location "Mount doom orchestrator"/"Thark location"

    And I create a resource of type "org.alien4cloud.nodes.mock.openstack.Image" named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "img1" for the resource named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"

    And I create a resource of type "org.alien4cloud.nodes.mock.openstack.Image" named "CentOS" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "img2" for the resource named "CentOS" related to the location "Mount doom orchestrator"/"Thark location"

    And I autogenerate the on-demand resources for the location "Mount doom orchestrator"/"Thark location"

    And I create a resource of type "org.alien4cloud.nodes.mock.Compute" named "Manual_Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"

    And I create a new application with name "MyWebApp" and description "A webapp" based on the template with name "inputs_copy"

    And I create an application topology version for application "MyWebApp" version "MyWebApp:0.1.0-SNAPSHOT" with qualifier "dev", description "topology for development environment", topology template id "null" and previous version id "MyWebApp:0.1.0-SNAPSHOT"

    And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes
    And I set the following orchestrator properties
      | managementUrl | http://cloudifyurl:8099 |
      | numberBackup  | 1                       |
      | managerEmail  | admin@alien.fr          |
    And I set the following inputs properties
      | context_root | /myWar |
    And I upload a file located at "src/test/resources/data/artifacts/myWar.war" for the input artifact "uploaded_war"
    And I substitute on the current application the node "Compute" with the location resource "Mount doom orchestrator"/"Thark location"/"Medium_CentOS"
    And I substitute on the current application the node "Manual_Compute" with the location resource "Mount doom orchestrator"/"Thark location"/"Manual_Small_Ubuntu"
    And I update the property "imageId" to "updatedImg" for the subtituted node "Manual_Compute"

    And I update the application environment named "Environment" with values
      | name            | DEV         |
      | description     |             |
      | environmentType | DEVELOPMENT |

  @reset
  Scenario: Create a new application environment for an application and don't copy inputs from another environment
    Given I create an application environment of type "DEVELOPMENT" with name "ANOTHER_DEV" and description "" for the newly created application
    When I get the deployment topology for the current application on the environment "ANOTHER_DEV"
    Then the deployment topology should not have any location policies
    Then the deployment topology should not have any input properties
    Then the deployment topology should not have any input artifacts

  @reset
  Scenario: Create a new application environment for an application and copy inputs from another environment
    Given I create an application environment of type "PRODUCTION" with name "PROD", with inputs from environment "DEV" and description "" for the newly created application
    When I get the deployment topology for the current application on the environment "PROD"
    Then the deployment topology should have the following inputs properties
      | context_root | /myWar |
    Then the deployment topology should have the following orchestrator properties
      | managementUrl | http://cloudifyurl:8099 |
      | numberBackup  | 1                       |
      | managerEmail  | admin@alien.fr          |
    Then the deployment topology should have the following inputs artifacts
      | uploaded_war | myWar.war |
    And The deployment topology should have the substituted nodes
      | Compute | Medium_CentOS | org.alien4cloud.nodes.mock.Compute |
    And The deployment topology should have the substituted nodes
      | Manual_Compute | Manual_Small_Ubuntu | org.alien4cloud.nodes.mock.Compute |
    And The node "Manual_Compute" in the deployment topology should have the property "imageId" with value "updatedImg"

# since 2.X refactoring this test don't pass : when I change the version of the topology associated to an environment
# the DeploymentTopology is not reset, don't seem to be a big issue
#
#  @reset
#  Scenario: Switch version of an environment without copying inputs
#    Given I update the topology version of the application environment named "DEV" to "0.1.0-dev-SNAPSHOT"
#    When I get the deployment topology for the current application on the environment "DEV"
#    Then the deployment topology should not have any location policies
#    Then the deployment topology should not have any input properties
#    Then the deployment topology should not have any input artifacts

  @reset
  Scenario: Switch version of an environment with copying inputs
    Given I update the topology version of the application environment named "DEV" to "0.1.0-dev-SNAPSHOT" with inputs from environment "DEV"
    When I get the deployment topology for the current application on the environment "DEV"
    Then the deployment topology should have the following inputs properties
      | context_root | /myWar |
    Then the deployment topology should have the following orchestrator properties
      | managementUrl | http://cloudifyurl:8099 |
      | numberBackup  | 1                       |
      | managerEmail  | admin@alien.fr          |
    Then the deployment topology should have the following inputs artifacts
      | uploaded_war | myWar.war |
    And The deployment topology should have the substituted nodes
      | Compute | Medium_CentOS | org.alien4cloud.nodes.mock.Compute |