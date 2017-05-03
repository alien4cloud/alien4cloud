Feature: Delete an application environment

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | frodon |
      | golum  |
      | sauron |
    And I add a role "APPLICATIONS_MANAGER" to user "frodon"
    And I am authenticated with user named "frodon"

  @reset
  Scenario: Delete an application environment from its id
    Given I create an application with name "watchmiddleearth-3", archive name "watchmiddleearth3", description "Use my great eye to find frodo and the ring." and topology template id "null"
    Then I should receive a RestResponse with no error
    And The application should have a user "frodon" having "APPLICATION_MANAGER" role
    And I should receive a RestResponse with a string data "watchmiddleearth3"
    Given I create an application environment of type "DEVELOPMENT" with name "watchmiddleearth-env-mock-3" and description "Mock App Env 3" for the newly created application
    Then I should receive a RestResponse with no error
    When I delete the registered application environment named "watchmiddleearth-env-mock-3" from its id
    Then I should receive a RestResponse with no error
    And I should receive a RestResponse with a boolean data "true"

  @reset
  Scenario: APPLICATION_MANAGER should be able to delete an environment, others can't
    Given I create an application with name "watchmiddleearth", archive name "watchmiddleearth", description "..." and topology template id "null"
    And I should receive a RestResponse with no error
    And I add a role "APPLICATION_MANAGER" to user "golum" on the resource type "APPLICATION" named "watchmiddleearth"
    And I add a role "APPLICATION_DEVOPS" to user "sauron" on the resource type "APPLICATION" named "watchmiddleearth"
    And I am authenticated with user named "golum"
    And I create an application environment of type "OTHER" with name "other" and description "" for the newly created application
    When I am authenticated with user named "sauron"
    When I delete the registered application environment named "other" from its id
    Then I should receive a RestResponse with an error code 102
    When I am authenticated with user named "golum"
    When I delete the registered application environment named "other" from its id
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Delete an application environment exposed as a service should fail
    Given I am authenticated with "ADMIN" role
    And I add a role "ARCHITECT" to user "frodon"
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I successfully upload the local archive "data/webbApp-template"

    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"

    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"

    And I create a resource of type "org.alien4cloud.nodes.mock.Compute" named "Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "imageId" to "img1" for the resource named "Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "flavorId" to "1" for the resource named "Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"

    And I grant access to the resource type "LOCATION" named "Thark location" to the user "frodon"
    And I successfully grant access to the resource type "LOCATION_RESOURCE" named "Mount doom orchestrator/Thark location/Small_Ubuntu" to the user "frodon"

    And I am authenticated with user named "frodon"
    And I create a new application with name "ALIEN" and description "" based on the template with name "WebApplicationTemplate"

    And I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId | tosca.nodes.WebApplication                                                        |
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
    And I create an application environment of type "OTHER" with name "other" and description "" for the newly created application
    And I successfully create a service with name "MyService", from the application "ALIEN", environment "Environment"
    When I delete the registered application environment named "Environment" from its id
    Then I should receive a RestResponse with an error code 507
