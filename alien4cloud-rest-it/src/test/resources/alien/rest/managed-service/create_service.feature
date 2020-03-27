Feature: Create a service resource from an environment

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


  @reset
  Scenario: Creating a new service from an undeployed environment should succeed
    When I create a service with name "MyService", from the application "ALIEN", environment "Environment"
    Then I should receive a RestResponse with no error
    Given I am authenticated with "ADMIN" role
    When I get the last created service
    Then The SPEL expression "environmentId != null" should return true

  #TODO improve this scenario later, since now there is no real difference between services created from deployed / undeployed environment
  @reset
  Scenario: Creating a new service from a deployed environment should succeed
    Given I get the deployment topology for the current application
    And I deploy the application "ALIEN" on the location "Mount doom orchestrator"/"Thark location"
    When I create a service with name "MyService", from the deployed application "ALIEN", environment "Environment"
    Then I should receive a RestResponse with no error
    Given I am authenticated with "ADMIN" role
    When I get the last created service
    Then The SPEL expression "environmentId != null" should return true
    Then The SPEL expression "getState()" should return "started"

  @reset
  Scenario: Creating a new service from an deployed environment when this one is not deployed should fail
    When I create a service with name "MyService", from the deployed application "ALIEN", environment "Environment"
    Then I should receive a RestResponse with an error code 504

  @reset
  Scenario: Creating a new managed service with no name should fail
    When I create a service with name "null", from the application "ALIEN", environment "Environment"
    Then I should receive a RestResponse with an error code 501

  @reset
  Scenario: Creating a new managed service with empty name should fail
    When I create a service with name "", from the application "ALIEN", environment "Environment"
    Then I should receive a RestResponse with an error code 501

  @reset
  Scenario: Creating a new managed service with existing name should fail
    Given I successfully create a service with name "MyService", from the application "ALIEN", environment "Environment"
    When I create a service with name "MyService", from the application "ALIEN", environment "Environment"
    Then I should receive a RestResponse with an error code 502

  @reset
  Scenario: Creating a new managed service when already created one on an environment should fail
    Given I get the deployment topology for the current application
    And I deploy the application "ALIEN" on the location "Mount doom orchestrator"/"Thark location"
    Given I successfully create a service with name "MyService", from the application "ALIEN", environment "Environment"
    When I create a service with name "MyService_2", from the application "ALIEN", environment "Environment"
    Then I should receive a RestResponse with an error code 502

  @reset
  Scenario: Creating a new managed service when not deployment manager on the environment should fail
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | sauron |
    And I add a role "APPLICATIONS_MANAGER" to user "sauron"
    And I am authenticated with user named "sauron"
    When I create a service with name "MyService", from the application "ALIEN", environment "Environment"
    Then I should receive a RestResponse with an error code 102

  @reset
  Scenario: Creating a new managed service when application manager should succeed
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | sauron |
    And I add a role "APPLICATION_MANAGER" to user "sauron" on the application "ALIEN"
    And I am authenticated with user named "sauron"
    When I create a service with name "MyService", from the application "ALIEN", environment "Environment"
    Then I should receive a RestResponse with no error
    Given I am authenticated with "ADMIN" role
    When I get the last created service
    Then The SPEL expression "environmentId != null" should return true

  @reset
  Scenario: Creating a new managed service when deployment manager should succeed
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | sauron |
    And I am authenticated with user named "gandalf"
    And I add a role "DEPLOYMENT_MANAGER" to user "sauron" on the resource type "ENVIRONMENT" named "Environment"
    And I am authenticated with user named "sauron"
    When I create a service with name "MyService", from the application "ALIEN", environment "Environment"
    Then I should receive a RestResponse with no error
    Given I am authenticated with "ADMIN" role
    When I get the last created service
    Then The SPEL expression "environmentId != null" should return true