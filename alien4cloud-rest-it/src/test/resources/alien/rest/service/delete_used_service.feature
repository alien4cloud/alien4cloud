Feature: Delete a service used in a deployment

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | gandalf |
    And I add a role "APPLICATIONS_MANAGER" to user "gandalf"
    And I add a role "ARCHITECT" to user "gandalf"
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"

    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"

    And I create a resource of type "org.alien4cloud.nodes.mock.Compute" named "Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "imageId" to "img1" for the resource named "Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "flavorId" to "1" for the resource named "Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"

    And I successfully grant access to the resource type "LOCATION" named "Thark location" to the user "gandalf"
    And I successfully grant access to the resource type "LOCATION_RESOURCE" named "Mount doom orchestrator/Thark location/Small_Ubuntu" to the user "gandalf"

    And I successfully create a service with name "MyStorage", version "1.0.0", type "org.alien4cloud.nodes.mock.BlockStorage", archive version "1.0"
    And I register path "data.id" with class "alien4cloud.model.service.ServiceResource" as "serviceId"
    And I set the property "size" to "1 gib" for the service "MyStorage"
    And I successfully start the service "MyStorage"
    And I authorize these locations to use the service "MyStorage"
      | Mount doom orchestrator/Thark location |
    And I successfully grant access to the resource type "SERVICE" named "MyStorage" to the user "gandalf"

    And I am authenticated with user named "gandalf"
    And I create a new application with name "ALIEN" and description "desc" and node templates
      | Compute      | tosca.nodes.Compute:1.0.0-SNAPSHOT      |
      | BlockStorage | tosca.nodes.BlockStorage:1.0.0-SNAPSHOT |
    And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes

    And I pre register orchestrator properties
      | managementUrl | http://cloudifyurl:8099 |
      | numberBackup  | 1                       |
      | managerEmail  | admin@alien.fr          |

    And I substitute on the current application the node "BlockStorage" with the service resource "MyStorage"
    And I should receive a RestResponse with no error
    And I deploy it
    Then I should receive a RestResponse with no error
    And The application's deployment must succeed

  @reset
  Scenario: Deleting a service used should fail
    Given I am authenticated with "ADMIN" role
    When I DELETE "/rest/v1/services/${serviceId}"
    Then I should receive a RestResponse with an error code 508

  @reset
  Scenario: Make sure we can delete a service used for a deployment after undeploying it
    Given I undeploy application "ALIEN", environment "Environment"
    And I should receive a RestResponse with no error
    And I am authenticated with "ADMIN" role
    When I DELETE "/rest/v1/services/${serviceId}"
    Then I should receive a RestResponse with no error
    When I get the last created service
    Then I should receive a RestResponse with an error code 504
