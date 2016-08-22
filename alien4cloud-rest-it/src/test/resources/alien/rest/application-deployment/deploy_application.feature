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

  @reset
  Scenario: Deploy an application with success
    Given I create a new application with name "ALIEN" and description "" and node templates
      | Compute | tosca.nodes.Compute:1.0.0-SNAPSHOT |
    And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes
    When I deploy it
    Then I should receive a RestResponse with no error
    And The application's deployment must succeed

  @reset
  Scenario: Deploy an application with failure
    Given I create a new application with name "BAD-APPLICATION" and description "" and node templates
      | Compute | tosca.nodes.Compute:1.0.0-SNAPSHOT |
    And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes
    When I deploy it
    Then I should receive a RestResponse with no error
    And The application's deployment must fail

  @reset
  Scenario: Deploy an application with warning
    Given I create a new application with name "WARN-APPLICATION" and description "" and node templates
      | Compute | tosca.nodes.Compute:1.0.0-SNAPSHOT |
    And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes
    When I deploy it
    Then I should receive a RestResponse with no error
    And The application's deployment must finish with warning

  @reset
  Scenario: Create 2 applications without deploying and check application's statuses
    Given I have applications with names and descriptions
      | BAD APPLICATION | This Application should be in FAILURE status...  |
      | ALIEN           | This application should be in DEPLOYED status... |
    When I can get applications statuses
    Then I have expected applications statuses for "deployment" operation
      | BAD APPLICATION | UNDEPLOYED |
      | ALIEN           | UNDEPLOYED |
    And I should receive a RestResponse with no error

  @reset
  Scenario: Create 4 applications, deploy all and final check statuses
    Given I have applications with names and descriptions and a topology containing a nodeTemplate "Compute" related to "tosca.nodes.Compute:1.0.0-SNAPSHOT"
      | My Software Factory | This application should be in DEPLOYED status... |
      | WARN-APPLICATION    | This application should be in WARNING status...  |
      | BAD-APPLICATION     | This Application should be in FAILURE status...  |
      | ALIEN               | This application should be in DEPLOYED status... |
    Then I can get applications statuses
    When I deploy all applications on the location "Mount doom orchestrator"/"Thark location"
    Then I have expected applications statuses for "deployment" operation
      | BAD-APPLICATION     | FAILURE  |
      | ALIEN               | DEPLOYED |
      | WARN-APPLICATION    | WARNING  |
      | My Software Factory | DEPLOYED |
    And I should receive a RestResponse with no error

  @reset
  Scenario: deleting an deployed application should fail
    Given I create a new application with name "ALIEN" and description "" and node templates
      | Compute | tosca.nodes.Compute:1.0.0-SNAPSHOT |
    And I deploy the application "ALIEN" on the location "Mount doom orchestrator"/"Thark location"
    When I delete the application "ALIEN"
    Then I should receive a RestResponse with an error code 607
    And the application can be found in ALIEN
    And The application's deployment must succeed

  @reset
  Scenario: Create two app with similar names and deploy them on the same orchestrator should fail for the second app
    Given I create a new application with name "App Test" and description "" and node templates
      | Compute | tosca.nodes.Compute:1.0.0-SNAPSHOT |
    And I deploy the application "App Test" on the location "Mount doom orchestrator"/"Thark location"
    And I create a new application with name "App_Test" and description "" and node templates
      | Compute | tosca.nodes.Compute:1.0.0-SNAPSHOT |
    And I deploy the application "App_Test" on the location "Mount doom orchestrator"/"Thark location" without waiting for the end of deployment
    Then I should receive a RestResponse with an error code 613
    And I create a new application with name "App-Test" and description "" and node templates
      | Compute | tosca.nodes.Compute:1.0.0-SNAPSHOT |
    And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes
    When I deploy it
    Then I should receive a RestResponse with no error
