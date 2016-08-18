Feature: Un-Deploy an application

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

  @reset
  Scenario: Create 1 application, deploy it, check statuses, undeploy it and check statuses
    Given I have applications with names and descriptions and a topology containing a nodeTemplate "Compute" related to "tosca.nodes.Compute:1.0.0-SNAPSHOT"
      | The great eye | This application should be in DEPLOYED status... |
    And I can get applications statuses
    When I deploy all applications on the location "Mount doom orchestrator"/"Thark location"
    Then I have expected applications statuses for "deployment" operation
      | The great eye | DEPLOYED |

    When I undeploy all environments for applications
    Then I should receive a RestResponse with no error
    And I have expected applications statuses for "undeployment" operation
      | The great eye | UNDEPLOYED |

    When I ask for detailed deployments for orchestrator "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
    And the response should contains 1 deployments DTO and applications with an end date set
      | The great eye |

  @reset
  Scenario: Create 1 application, deploy it, undeploy it, disable the associate orchestrator and check the deployment topology
    Given I am authenticated with "ADMIN" role
    And I create a new application with name "The great eye" and description "This application should be in DEPLOYED status..." and node templates
      | Compute | tosca.nodes.Compute:1.0.0-SNAPSHOT |
    When I deploy all applications on the location "Mount doom orchestrator"/"Thark location"
    And The application's deployment must succeed
    When I undeploy all environments for applications
    Then I should receive a RestResponse with no error
    And I disable "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
    When I ask for the deployment topology of the application "The great eye"
    Then I should receive a RestResponse with no error
