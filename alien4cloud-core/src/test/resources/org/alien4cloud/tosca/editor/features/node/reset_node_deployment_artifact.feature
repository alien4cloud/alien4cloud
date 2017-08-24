Feature: Topology editor: set deployment artifact

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Reset an artifact should succeed
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | war_node                                                              |
      | indexedNodeTypeId | fastconnect.nodes.War:1.0                                             |
    And I upload a file located at "src/test/resources/org/alien4cloud/tosca/editor/features/upload_file.feature" to the archive path "upload_file.feature"
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodeDeploymentArtifactOperation |
      | nodeName          | war_node                                                                                   |
      | artifactName      | war                                                                                        |
      | artifactReference | upload_file.feature                                                                        |
    Then No exception should be thrown
    And The SPEL expression "nodeTemplates['war_node'].artifacts['war'].artifactRef" should return "upload_file.feature"
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.ResetNodeDeploymentArtifactOperation  |
      | nodeName          | war_node                                                                                   |
      | artifactName      | war                                                                                        |
    Then No exception should be thrown
    And The SPEL expression "nodeTemplates['war_node'].artifacts['war'].artifactRef" should return "null"

  Scenario: Reset an artifact on wrong artifact name should failed
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | war_node                                                              |
      | indexedNodeTypeId | fastconnect.nodes.War:1.0                                             |
    And I upload a file located at "src/test/resources/org/alien4cloud/tosca/editor/features/upload_file.feature" to the archive path "upload_file.feature"
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodeDeploymentArtifactOperation |
      | nodeName          | war_node                                                                                   |
      | artifactName      | war                                                                                        |
      | artifactReference | upload_file.feature                                                                        |
    Then No exception should be thrown
    And The SPEL expression "nodeTemplates['war_node'].artifacts['war'].artifactRef" should return "upload_file.feature"
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.ResetNodeDeploymentArtifactOperation  |
      | nodeName          | war_node                                                                                   |
      | artifactName      | war2                                                                                        |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown