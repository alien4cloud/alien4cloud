Feature: Topology editor: delete file

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Delete a file at the root of the archive should succeed
    Given I upload a file located at "src/test/resources/org/alien4cloud/tosca/editor/features/upload_file.feature" to the archive path "upload_file.feature"
    When I execute the operation
      | type | org.alien4cloud.tosca.editor.operations.DeleteFileOperation |
      | path | upload_file.feature                                         |
    Then No exception should be thrown
    And The dto SPEL expression "archiveContentTree.children[0].children.size()" should return "3"

  Scenario: Delete a file in a folder of the archive should succeed
    When I upload a file located at "src/test/resources/org/alien4cloud/tosca/editor/features/upload_file.feature" to the archive path "new_folder/upload_file.feature"
    When I execute the operation
      | type | org.alien4cloud.tosca.editor.operations.DeleteFileOperation |
      | path | new_folder/upload_file.feature                              |
    Then No exception should be thrown
    And The dto SPEL expression "archiveContentTree.children[0].children.size()" should return "4"
    And The dto SPEL expression "archiveContentTree.children[0].children[1].children.size()" should return "0"

  Scenario: Delete a folder should succeed
    When I upload a file located at "src/test/resources/org/alien4cloud/tosca/editor/features/upload_file.feature" to the archive path "new_folder/upload_file.feature"
    When I execute the operation
      | type | org.alien4cloud.tosca.editor.operations.DeleteFileOperation |
      | path | new_folder                                                  |
    Then No exception should be thrown
    And The dto SPEL expression "archiveContentTree.children[0].children.size()" should return "3"

  Scenario: Delete a folder path ending by slash should succeed
    When I upload a file located at "src/test/resources/org/alien4cloud/tosca/editor/features/upload_file.feature" to the archive path "new_folder/upload_file.feature"
    When I execute the operation
      | type | org.alien4cloud.tosca.editor.operations.DeleteFileOperation |
      | path | new_folder/                                                 |
    Then No exception should be thrown
    And The dto SPEL expression "archiveContentTree.children[0].children.size()" should return "3"

  Scenario: Delete the yaml file should fail
    When I execute the operation
      | type | org.alien4cloud.tosca.editor.operations.DeleteFileOperation |
      | path | topology.yml                                         |
    Then an exception of type "org.alien4cloud.tosca.editor.exception.InvalidPathException" should be thrown

  Scenario: Delete a file that does not exists should fail
    When I execute the operation
      | type | org.alien4cloud.tosca.editor.operations.DeleteFileOperation |
      | path | upload_file.feature                                         |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown
