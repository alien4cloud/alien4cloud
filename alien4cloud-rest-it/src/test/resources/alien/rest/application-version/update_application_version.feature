Feature: Update operations on application version

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | luffy |
    And I add a role "APPLICATIONS_MANAGER" to user "luffy"
    And I add a role "COMPONENTS_MANAGER" to user "luffy"
    And I am authenticated with user named "luffy"
    And I create an application with name "watchmiddleearth", archive name "watchmiddleearth", description "Use my great eye to find frodo and the ring." and topology template id "null"
    And I should receive a RestResponse with no error

  @reset
  Scenario: Updating an application version with a new version should succeed
    And I successfully create an application version for application "watchmiddleearth" with version "0.2.0-SNAPSHOT", description "null", topology template id "null" and previous version id "null"
    When I update the application version for application "watchmiddleearth" version id "watchmiddleearth:0.2.0-SNAPSHOT" with new version "0.3.0-SNAPSHOT" and description "null"
    Then I should receive a RestResponse with no error

#  @reset
#  Scenario: Updating an application version with a new description should succeedd
#
#  @reset
#  Scenario: Updating an application version with a new version and description should succeedd

  @reset
  Scenario: Updating an application version with an existing version should fail
    And I successfully create an application version for application "watchmiddleearth" with version "0.2.0-SNAPSHOT", description "null", topology template id "null" and previous version id "null"
    When I update the application version for application "watchmiddleearth" version id "watchmiddleearth:0.2.0-SNAPSHOT" with new version "0.1.0-SNAPSHOT" and description "null"
    Then I should receive a RestResponse with an error code 502

  @reset
  Scenario: Updating the version of a released application version should fail
    And I successfully create an application version for application "watchmiddleearth" with version "0.2.0", description "null", topology template id "null" and previous version id "null"
    When I update the application version for application "watchmiddleearth" version id "watchmiddleearth:0.2.0" with new version "0.2.1-SNAPSHOT" and description "null"
    Then I should receive a RestResponse with an error code 608

  @reset
  Scenario: Updating the version of an application version linked to an environment should succeed
    And I successfully create an application topology version for application "watchmiddleearth" version "watchmiddleearth:0.1.0-SNAPSHOT" with qualifier "DEV", description "topology for development environment", topology template id "null" and previous version id "null"
    When I update the application version for application "watchmiddleearth" version id "watchmiddleearth:0.1.0-SNAPSHOT" with new version "0.1.1-SNAPSHOT" and description "null"
    Then I should receive a RestResponse with no error
    When I get the application version for application "watchmiddleearth" with id "watchmiddleearth:0.1.1-SNAPSHOT"
    And The application version should have an application topology version with version "0.1.1-SNAPSHOT"
    And The application version should have an application topology version with version "0.1.1-DEV-SNAPSHOT"
    ## check that the environment has well been updated
    When I get the application environment named "Environment"
    And I register the rest response data as SPEL context of type "alien4cloud.rest.application.model.ApplicationEnvironmentDTO"
    Then The SPEL expression "currentVersionName" should return "0.1.1-SNAPSHOT"


  @reset
  Scenario: Updating an application version should keep the deployment topology
    Given I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I successfully save the topology

    Given I am authenticated with "ADMIN" role
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"

    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"

    And I create a resource of type "org.alien4cloud.nodes.mock.Compute" named "Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "imageId" to "img1" for the resource named "Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "flavorId" to "1" for the resource named "Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"

    And I grant access to the resource type "LOCATION" named "Thark location" to the user "luffy"
    And I successfully grant access to the resource type "LOCATION_RESOURCE" named "Mount doom orchestrator/Thark location/Small_Ubuntu" to the user "luffy"
    And I am authenticated with user named "luffy"
    And I get the deployment topology for the current application
    And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes
    And I set the following orchestrator properties
      | managementUrl | http://cloudifyurl:8099 |
      | numberBackup  | 1                       |
      | managerEmail  | admin@alien.fr          |

    When I update the application version for application "watchmiddleearth" version id "watchmiddleearth:0.1.0-SNAPSHOT" with new version "0.2.0-SNAPSHOT" and description "null"
    Then I should receive a RestResponse with no error
    When I get the application version for application "watchmiddleearth" with id "watchmiddleearth:0.2.0-SNAPSHOT"
    And The application version should have an application topology version with version "0.2.0-SNAPSHOT"
        ## check that the environment has well been updated
    When I get the application environment named "Environment"
    And I register the rest response data as SPEL context of type "alien4cloud.rest.application.model.ApplicationEnvironmentDTO"
    Then The SPEL expression "currentVersionName" should return "0.2.0-SNAPSHOT"
  ## check that the deployment topology was kept
    When I get the deployment topology for the current application
#    And I register the rest response data as SPEL context of type "alien4cloud.deployment.DeploymentTopologyDTO"
    Then the deployment topology should have the following orchestrator properties
      | managementUrl | http://cloudifyurl:8099 |
      | numberBackup  | 1                       |
      | managerEmail  | admin@alien.fr          |
    And The deployment topology should have the substituted nodes
      | Compute | Small_Ubuntu | org.alien4cloud.nodes.mock.Compute |

  @reset
  Scenario: Updating the version of a deployed application version should fail
    Given I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I successfully save the topology

    Given I am authenticated with "ADMIN" role
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"

    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"

    And I create a resource of type "org.alien4cloud.nodes.mock.Compute" named "Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "imageId" to "img1" for the resource named "Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "flavorId" to "1" for the resource named "Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"

    And I grant access to the resource type "LOCATION" named "Thark location" to the user "luffy"
    And I successfully grant access to the resource type "LOCATION_RESOURCE" named "Mount doom orchestrator/Thark location/Small_Ubuntu" to the user "luffy"
    And I pre register orchestrator properties
      | managementUrl | http://cloudifyurl:8099 |
      | numberBackup  | 1                       |
      | managerEmail  | admin@alien.fr          |

    And I am authenticated with user named "luffy"
    And I get the deployment topology for the current application
    And I deploy the application "watchmiddleearth" on the location "Mount doom orchestrator"/"Thark location"
    And I wait for 10 seconds before continuing the test

    When I update the application version for application "watchmiddleearth" version id "watchmiddleearth:0.1.0-SNAPSHOT" with new version "0.2.0-SNAPSHOT" and description "null"
    And I register the rest response data as SPEL context
    Then I should receive a RestResponse with an error code 508
    And The SPEL expression "#root[0]['resourceName']" should contains "watchmiddleearth"
    And The SPEL expression "#root[0]['resourceName']" should contains "Environment"