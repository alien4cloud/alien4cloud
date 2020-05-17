Feature: inputs artifacts providing in deployment setup
# Tested features with this scenario:
#  - providing of input artifact via local filesystem upload
#  - providing of input artifact via topology archive file selection

  Background:

    Given I am authenticated with "ADMIN" role

    # Archives
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I successfully upload the local archive "data/csars/input_artifact/input_artifact.yml"

    And I upload a plugin

    # Orchestrator and location
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I create a resource of type "org.alien4cloud.nodes.mock.openstack.Flavor" named "Small" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "1" for the resource named "Small" related to the location "Mount doom orchestrator"/"Thark location"
    And I create a resource of type "org.alien4cloud.nodes.mock.openstack.Image" named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "img1" for the resource named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I autogenerate the on-demand resources for the location "Mount doom orchestrator"/"Thark location"
    When I create a new application with name "input-artifact-validation" and description "Input artifact validation" based on the template with name "input_artifact_test"
    And I get the current topology
#    When I upload a file located at "src/test/resources/data/artifacts/myWar.war" to the archive path "nested/myWar.war"
    And I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodeArtifactAsInputOperation |
      | nodeName     | ArtifactDemo                                                                                |
      | artifactName | war_file                                                                                    |
      | inputName    | war_file                                                                                    |
    And I save the topology
    And I get the deployment topology for the current application
    And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes
    And I set the following orchestrator properties
      | managerEmail  | toto@titi.fr            |
      | managementUrl | http://cloudifyurl:8099 |
      | numberBackup  | 1                       |

  @reset
  Scenario: Providing an input artifact via local filesystem upload should succeed
    Given I get the deployment topology for the current application
    And the deployment topology should not have any input artifacts
    When I upload a file located at "src/test/resources/data/artifacts/myWar.war" for the input artifact "war_file"
    And I get the deployment topology for the current application
    Then the deployment topology should have the following inputs artifacts
      | war_file | myWar.war |

  @reset
  Scenario: Providing an input artifact via local archive file selection should succeed
    Given I get the deployment topology for the current application
    And the deployment topology should not have any input artifacts
    When I upload a file located at "src/test/resources/data/artifacts/myWar.war" to the archive path "nested/myWarUpdated.war"
    When I select the file "nested/myWarUpdated.war" from the current topology archive for the input artifact "war_file"
    And I get the deployment topology for the current application
    Then the deployment topology should have the following inputs artifacts
      | war_file | nested/myWarUpdated.war |