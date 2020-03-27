Feature: Update a service resource associated to an application environment on deployment process

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

  @reset
  Scenario: Managed service should be properly updated on deployment success
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

    And I pre register orchestrator properties
      | managementUrl | http://cloudifyurl:8099 |
      | numberBackup  | 1                       |
      | managerEmail  | admin@alien.fr          |

    And I successfully create a service with name "MyService", from the application "ALIEN", environment "Environment"
    Given I am authenticated with "ADMIN" role
    When I get the last created service
    Then The SPEL expression "environmentId != null" should return true
    Then The SPEL expression "deploymentId == null" should return true
    Then The SPEL expression "getState()" should return "initial"
    And The SPEL expression "nodeInstance.attributeValues['ip_address']" should return "null"
    And The SPEL expression "nodeInstance.attributeValues['tosca_id']" should return "null"
    #deploy the appli
    Given I am authenticated with user named "gandalf"
    And I get the deployment topology for the current application
    And I deploy the application "ALIEN" on the location "Mount doom orchestrator"/"Thark location"
    And I wait for 10 seconds before continuing the test
    #check the service state and attributes
    And I am authenticated with "ADMIN" role
    When I get the last created service
    Then The SPEL expression "environmentId != null" should return true
    Then The SPEL expression "getState()" should return "started"
    And The SPEL expression "nodeInstance.attributeValues['ip_address'] != null" should return true
    And The SPEL expression "nodeInstance.attributeValues['tosca_id'] != null" should return true

  @reset
  Scenario: Managed service should be properly updated on deployment failure
    And I create a new application with name "BAD-APPLICATION" and description "" based on the template with name "WebApplicationTemplate"

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

    And I pre register orchestrator properties
      | managementUrl | http://cloudifyurl:8099 |
      | numberBackup  | 1                       |
      | managerEmail  | admin@alien.fr          |

    And I successfully create a service with name "MyService", from the application "BAD-APPLICATION", environment "Environment"
    Given I am authenticated with "ADMIN" role
    When I get the last created service
    Then The SPEL expression "environmentId != null" should return true
    Then The SPEL expression "deploymentId == null" should return true
    Then The SPEL expression "getState()" should return "initial"
    And The SPEL expression "nodeInstance.attributeValues['ip_address']" should return "null"
    And The SPEL expression "nodeInstance.attributeValues['tosca_id']" should return "null"
    #deploy the appli
    Given I am authenticated with user named "gandalf"
    And I get the deployment topology for the current application
    And I deploy the application "BAD-APPLICATION" on the location "Mount doom orchestrator"/"Thark location" without waiting for the end of deployment
    And I wait for 10 seconds before continuing the test
    #check the service state and attributes
    And I am authenticated with "ADMIN" role
    When I get the last created service
    Then The SPEL expression "environmentId != null" should return true
    Then The SPEL expression "deploymentId != null" should return true
    Then The SPEL expression "getState()" should return "error"
    And The SPEL expression "nodeInstance.attributeValues['ip_address'] != null" should return true
    And The SPEL expression "nodeInstance.attributeValues['tosca_id'] != null" should return true


  @reset
  Scenario: Managed service should be properly cleaned on undeployment
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

    And I pre register orchestrator properties
      | managementUrl | http://cloudifyurl:8099 |
      | numberBackup  | 1                       |
      | managerEmail  | admin@alien.fr          |

    And I successfully create a service with name "MyService", from the application "ALIEN", environment "Environment"
#deploy the appli
    Given I am authenticated with user named "gandalf"
    And I get the deployment topology for the current application
    And I deploy the application "ALIEN" on the location "Mount doom orchestrator"/"Thark location"
    And I wait for 10 seconds before continuing the test
#check the service state and attributes
    And I am authenticated with "ADMIN" role
    When I get the last created service
    Then The SPEL expression "environmentId != null" should return true
    Then The SPEL expression "getState()" should return "started"
    And The SPEL expression "nodeInstance.attributeValues['ip_address'] != null" should return true
    And The SPEL expression "nodeInstance.attributeValues['tosca_id'] != null" should return true

    #undeploy
    Given I am authenticated with user named "gandalf"
    And I undeploy application "ALIEN", environment "Environment"
    And I wait for 10 seconds before continuing the test
    And I am authenticated with "ADMIN" role
    When I get the last created service
    Then The SPEL expression "environmentId != null" should return true
    Then The SPEL expression "deploymentId == null" should return true
    Then The SPEL expression "getState()" should return "initial"
    And The SPEL expression "nodeInstance.attributeValues['ip_address']" should return "null"
    And The SPEL expression "nodeInstance.attributeValues['tosca_id']" should return "null"

