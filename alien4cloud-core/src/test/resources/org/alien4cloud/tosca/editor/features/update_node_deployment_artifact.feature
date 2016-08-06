Feature: Topology editor: set deployment artifact

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Setting an artifact to a temporary file from the archive root should succeed
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

  Scenario: Setting an artifact to a temporary file from an archive directory should succeed
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | war_node                                                              |
      | indexedNodeTypeId | fastconnect.nodes.War:1.0                                             |
    And I upload a file located at "src/test/resources/org/alien4cloud/tosca/editor/features/upload_file.feature" to the archive path "new_folder/upload_file.feature"
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | war_node                                                              |
      | indexedNodeTypeId | fastconnect.nodes.War:1.0                                             |

  Scenario: Setting an artifact to a file that is not in the archive should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | war_node                                                              |
      | indexedNodeTypeId | fastconnect.nodes.War:1.0                                             |
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodeDeploymentArtifactOperation |
      | nodeName          | war_node                                                                                   |
      | artifactName      | war                                                                                        |
      | artifactReference | upload_file.feature                                                                        |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown
