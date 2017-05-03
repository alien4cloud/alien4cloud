Feature: Update an application version exposed as a service

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | gandalf |

    And I add a role "APPLICATIONS_MANAGER" to user "gandalf"
    And I add a role "ARCHITECT" to user "gandalf"
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I successfully upload the local archive "data/webbApp-template"

    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"

    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"

    And I create a resource of type "org.alien4cloud.nodes.mock.Compute" named "Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "imageId" to "img1" for the resource named "Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "flavorId" to "1" for the resource named "Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"

    And I grant access to the resource type "LOCATION" named "Thark location" to the user "gandalf"
    And I successfully grant access to the resource type "LOCATION_RESOURCE" named "Mount doom orchestrator/Thark location/Small_Ubuntu" to the user "gandalf"

    And I am authenticated with user named "gandalf"
    And I create a new application with name "ALIEN" and description "" based on the template with name "WebApplicationTemplate"
    And I create an application version for application "ALIEN" with version "0.2.0-SNAPSHOT", description "null", topology template id "null" and previous version id "null"

    And I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId | org.alien4cloud.nodes.test.WebApplication                                         |
    And I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | WebApplication                                                                              |
      | substitutionCapabilityId | feature                                                                                     |
      | capabilityId             | feature                                                                                     |
    And I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | WebApplication                                                                              |
      | substitutionCapabilityId | app_endpoint                                                                                |
      | capabilityId             | app_endpoint                                                                                |
    And I successfully save the topology

    And I pre register orchestrator properties
      | managementUrl | http://cloudifyurl:8099 |
      | numberBackup  | 1                       |
      | managerEmail  | admin@alien.fr          |
    And I successfully create a service with name "MyService", from the application "ALIEN", environment "Environment"

  @reset
  Scenario: updating a version exposed as a service should fail
    When I update the application version for application "ALIEN" version id "ALIEN:0.1.0-SNAPSHOT" with new version "0.3.0-SNAPSHOT" and description "null"
    And I register the rest response data as SPEL context
    Then I should receive a RestResponse with an error code 508
    And The SPEL expression "#root[0]['resourceName']" should contains "MyService"

