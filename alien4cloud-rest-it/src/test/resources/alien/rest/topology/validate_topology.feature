Feature: Check if topology is valid

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload the archive "alien-base-types 1.1.0"
    And I upload the archive "samples apache-load-balancer"
    And I upload the archive "samples tomcat-war"
    And I upload the archive "samples topology load-balancer-tomcat"

  @reset
  Scenario: checking if an empty topology is valid
    When I create a new application with name "watchmiddleearth" and description "Use my great eye to find frodo and the ring."
    And I check for the valid status of the topology
    Then I should receive a RestResponse with no error
    And the topology should not be valid
# Scenario: adding nodes templates and check if topology is valid
# Scenario: adding abstract nodes templates and check, should be valid
    When I get the current topology
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I save the topology
    And I check for the valid status of the topology
    Then I should receive a RestResponse with no error
    And the topology should be valid
# Scenario: adding nodetemplates without requirements lowerbounds satisfied and check if topology is valid
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Java                                                                  |
      | indexedNodeTypeId | org.alien4cloud.nodes.Java:2.0.0-SNAPSHOT                                       |
    And I save the topology
    And I check for the valid status of the topology
    Then I should receive a RestResponse with no error
    And the topology should not be valid
    And the node with requirements lowerbound not satisfied should be
      | Java | host |
#  Scenario: adding non abstract relationships and check if topology is valid
    When I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | Java                                                                                  |
      | relationshipName       | hostedOnCompute                                                                       |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0.0-SNAPSHOT                                                                        |
      | requirementName        | host                                                                                  |
      | target                 | Compute                                                                               |
      | targetedCapabilityName | host                                                                                  |
    And I save the topology
    And I check for the valid status of the topology
    Then I should receive a RestResponse with no error
    And the topology should be valid

  @reset
  Scenario: adding abstract relationships and check if topology is valid
    When I create a new application with name "load-balancer-cfy3" and description "Apache load balancer with CFY 3" based on the template with name "apache-load-balancer"
    And I check for the valid status of the topology
    Then I should receive a RestResponse with no error
    And the topology should be valid
    When I execute the operation
      | type             | org.alien4cloud.tosca.editor.operations.relationshiptemplate.DeleteRelationshipOperation |
      | nodeName         | War                                                                                      |
      | relationshipName | webApplicationConnectsToApacheLoadBalancerApacheLoadBalancer                             |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | War                                                                                   |
      | relationshipName       | webApplicationConnectsToApacheLoadBalancerApacheLoadBalancer                          |
      | relationshipType       | alien.relationships.WebApplicationConnectsToLoadBalancer                              |
      | relationshipVersion    | 1.2.0-SNAPSHOT                                                                        |
      | requirementName        | load_balancer                                                                         |
      | target                 | ApacheLoadBalancer                                                                    |
      | targetedCapabilityName | load_balancer                                                                         |
    And I save the topology
    And I check for the valid status of the topology
    Then I should receive a RestResponse with no error
    And the topology should not be valid

  @reset
  Scenario: Add a relationship between 2 nodes when upperbound is already reached on target
    When I create a new application with name "load-balancer-cfy3" and description "Apache load balancer with CFY 3" based on the template with name "apache-load-balancer"
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Tomcat2                                                               |
      | indexedNodeTypeId | org.alien4cloud.nodes.Tomcat:2.0.0-SNAPSHOT                                     |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | War                                                                                   |
      | relationshipName       | warHostedOnTomcatTomcat2                                                              |
      | relationshipType       | alien.relationships.WarHostedOnTomcat                                                 |
      | relationshipVersion    | 2.0.0-SNAPSHOT                                                                        |
      | requirementName        | host                                                                                  |
      | target                 | Tomcat                                                                                |
      | targetedCapabilityName | war_host                                                                              |
    Then I should receive a RestResponse with an error code 810

  @reset
  Scenario: Required properties not set then topology is not valid
    When I create a new application with name "load-balancer-cfy3" and description "Apache load balancer with CFY 3" based on the template with name "apache-load-balancer"
    When I check for the valid status of the topology
    Then I should receive a RestResponse with no error
    And the topology should be valid

    When I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodePropertyValueOperation |
      | nodeName      | War                                                                                   |
      | propertyName  | context_path                                                                          |
      | propertyValue |                                                                                       |
    And I save the topology
    And I check for the valid status of the topology
    Then I should receive a RestResponse with no error
    And the topology should not be valid
    And the node with required properties not set should be
      | War | context_path |
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Release while SNAPSHOT dependency remain should failed
    When I create a new application with name "load-balancer-cfy3" and description "Apache load balancer with CFY 3" based on the template with name "apache-load-balancer"
    And I update an application version with version "0.1.0-SNAPSHOT" to "0.1.0"
    Then I should receive a RestResponse with an error code 830

  @reset
  Scenario: Update a topology when the application version is released should fail
    Given I upload the archive "tosca base types 1.0"
    And I create a new application with name "watchmiddleearth" and description "Use my great eye to find frodo and the ring."
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    And I save the topology
    And I update an application version with version "0.1.0-SNAPSHOT" to "0.1.0"
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute2                                                              |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    Then I should receive a RestResponse with an error code 807

  @reset
  Scenario: Topology with undefined deployment artifact in node is not valid
    Given I upload the archive "topology-empty-deployment-artifact"
    And I create a new application with name "empty-deployment-artifact" and description "Demo empty artifact" based on the template with name "empty-deployment-artifact"
    When I check for the valid status of the topology
    Then the topology should not be valid
    And the nodes with missing artifacts should be
      | ArtifactDemo  | nested_uploaded_war  |
      | ArtifactDemo2 | nested_uploaded_war  |
      | ArtifactDemo2 | nested_uploaded_war2 |
    And I upload a file located at "src/test/resources/data/artifacts/myWar.war" to the archive path "nested/myWar.war"
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodeDeploymentArtifactOperation |
      | nodeName          | ArtifactDemo                                                                               |
      | artifactName      | nested_uploaded_war                                                                        |
      | artifactReference | nested/myWar.war                                                                           |
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodeDeploymentArtifactOperation |
      | nodeName          | ArtifactDemo2                                                                              |
      | artifactName      | nested_uploaded_war                                                                        |
      | artifactReference | nested/myWar.war                                                                           |
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodeDeploymentArtifactOperation |
      | nodeName          | ArtifactDemo2                                                                              |
      | artifactName      | nested_uploaded_war2                                                                       |
      | artifactReference | nested/myWar.war                                                                           |
    And I save the topology
    And I check for the valid status of the topology
    Then the topology should be valid
