Feature: Topology editor: upload file

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Uploading a file at the root of the archive should succeed
    When I upload a file located at "src/test/resources/org/alien4cloud/tosca/editor/features/upload_file.feature" to the archive path "upload_file.feature"
    Then No exception should be thrown
    And The dto SPEL expression "archiveContentTree.children[0].children[3].name" should return "upload_file.feature"
    And The dto SPEL expression "archiveContentTree.children[0].children[3].leaf" should return "true"

  Scenario: Uploading a file in an archive new folder should succeed
    When I upload a file located at "src/test/resources/org/alien4cloud/tosca/editor/features/upload_file.feature" to the archive path "new_folder/upload_file.feature"
    Then No exception should be thrown
    And The dto SPEL expression "archiveContentTree.children[0].children[1].name" should return "new_folder"
    And The dto SPEL expression "archiveContentTree.children[0].children[1].leaf" should return "false"
    And The dto SPEL expression "archiveContentTree.children[0].children[1].children[0].name" should return "upload_file.feature"
    And The dto SPEL expression "archiveContentTree.children[0].children[1].children[0].leaf" should return "true"

  Scenario: Uploading a file to override an archive file should succeed
    When I upload a file located at "src/test/resources/org/alien4cloud/tosca/editor/features/upload_file.feature" to the archive path "new_folder/upload_file.feature"
    And I upload a file located at "src/test/resources/org/alien4cloud/tosca/editor/features/add_node.feature" to the archive path "new_folder/upload_file.feature"
    Then No exception should be thrown
    And The dto SPEL expression "archiveContentTree.children[0].children[1].name" should return "new_folder"
    And The dto SPEL expression "archiveContentTree.children[0].children[1].leaf" should return "false"
    And The dto SPEL expression "archiveContentTree.children[0].children[1].children[0].name" should return "upload_file.feature"
    And The dto SPEL expression "archiveContentTree.children[0].children[1].children[0].leaf" should return "true"

  Scenario: Uploading a file in place of a folder should fail
    When I upload a file located at "src/test/resources/org/alien4cloud/tosca/editor/features/upload_file.feature" to the archive path "new_folder/upload_file.feature"
    When I upload a file located at "src/test/resources/org/alien4cloud/tosca/editor/features/upload_file.feature" to the archive path "new_folder"
    Then an exception of type "org.alien4cloud.tosca.editor.exception.InvalidPathException" should be thrown

  Scenario: Uploading a file when the path uses file as folder (sub-file) of a folder should fail
    When I upload a file located at "src/test/resources/org/alien4cloud/tosca/editor/features/upload_file.feature" to the archive path "upload_file.feature"
    When I upload a file located at "src/test/resources/org/alien4cloud/tosca/editor/features/upload_file.feature" to the archive path "upload_file.feature/upload_file.feature"
    Then an exception of type "org.alien4cloud.tosca.editor.exception.InvalidPathException" should be thrown

#  Scenario: Creating an empty file should succeed
#    When I execute the operation
#      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
#      | nodeName          | Template1                                                             |
#      | indexedNodeTypeId | the.node.that.does.not.Exists:1.0                                     |
#    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown