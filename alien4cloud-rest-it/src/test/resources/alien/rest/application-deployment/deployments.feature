Feature: get deployments

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

    And I create an orchestrator named "Mount doom orchestrator 2" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator 2"
    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator 2"
    And I create a resource of type "alien.nodes.mock.openstack.Flavor" named "Small" related to the location "Mount doom orchestrator 2"/"Thark location"
    And I update the property "id" to "1" for the resource named "Small" related to the location "Mount doom orchestrator 2"/"Thark location"
    And I create a resource of type "alien.nodes.mock.openstack.Image" named "Ubuntu" related to the location "Mount doom orchestrator 2"/"Thark location"
    And I update the property "id" to "img1" for the resource named "Ubuntu" related to the location "Mount doom orchestrator 2"/"Thark location"
    And I autogenerate the on-demand resources for the location "Mount doom orchestrator 2"/"Thark location"
    And I pre register orchestrator properties
      | managementUrl | http://cloudifyurl:8099 |
      | numberBackup  | 1                       |
      | managerEmail  | admin@alien.fr          |
    And I create a new application with name "The great eye" and description "" and node templates
      | Compute | tosca.nodes.Compute:1.0.0-SNAPSHOT |
    And I Set a unique location policy to "Mount doom orchestrator 2"/"Thark location" for all nodes
    And I deploy it

  @reset
  Scenario: Ask for detailed deployment object of one cloud
    Given I have applications with names and descriptions and a topology containing a nodeTemplate "Compute" related to "tosca.nodes.Compute:1.0.0-SNAPSHOT"
      | ALIEN_1 | ALIEN 1 |
      | ALIEN_2 | ALIEN 2 |
    And I deploy all applications on the location "Mount doom orchestrator"/"Thark location"
    When I ask for detailed deployments for orchestrator "Mount doom orchestrator 2"
    Then I should receive a RestResponse with no error
    And the response should contains 1 deployments DTO and applications
      | The great eye |
    When I ask for detailed deployments for orchestrator "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
    And the response should contains 2 deployments DTO and applications
      | ALIEN_1 |
      | ALIEN_2 |

  @reset
  Scenario: Ask for detailed deployment object of all cloud
    Given I have applications with names and descriptions and a topology containing a nodeTemplate "Compute" related to "tosca.nodes.Compute:1.0.0-SNAPSHOT"
      | ALIEN_1 | ALIEN 1 |
      | ALIEN_2 | ALIEN 2 |
    And I deploy all applications on the location "Mount doom orchestrator"/"Thark location"
    When I ask for detailed deployments for all orchestrators
    Then I should receive a RestResponse with no error
    And the response should contains 3 deployments DTO and applications
      | The great eye |
      | ALIEN_1       |
      | ALIEN_2       |

  @reset
  Scenario: ask for detailed deployment object for an application
    When I create a new application with name "ALIEN_1 great eye" and description "ALIEN_1" and node templates
      | Compute | tosca.nodes.Compute:1.0.0-SNAPSHOT |
    Then I should not get a deployment if I ask one for application "ALIEN_1" on orchestrator "Mount doom orchestrator"
