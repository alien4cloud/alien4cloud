Feature: Workflow editor: rename workflow

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Renaming a workflow should succeed
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.workflow.CreateWorkflowOperation |
      | workflowName | wf1                                                                      |
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.workflow.RenameWorkflowOperation |
      | workflowName | wf1                                                                      |
      | newName      | wf1_renamed                                                              |
    Then No exception should be thrown
    And The SPEL int expression "workflows.size()" should return 3
    And The SPEL expression "workflows['wf1_renamed'].name" should return "wf1_renamed"

  Scenario: Renaming a workflow giving an existing workflow name should fail
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.workflow.CreateWorkflowOperation |
      | workflowName | wf1                                                                      |
    And I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.workflow.CreateWorkflowOperation |
      | workflowName | wf2                                                                      |
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.workflow.RenameWorkflowOperation |
      | workflowName | wf1                                                                      |
      | newName      | wf2                                                                      |
    Then an exception of type "alien4cloud.exception.AlreadyExistException" should be thrown
    When I get the edited topology
    Then The SPEL expression "workflows['wf1'].name" should return "wf1"
    And The SPEL expression "workflows['wf2'].name" should return "wf2"

  Scenario: Renaming a standard workflow should fail
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.workflow.RenameWorkflowOperation |
      | workflowName | install                                                                  |
      | newName      | should_fail                                                              |
    Then an exception of type "alien4cloud.paas.wf.exception.BadWorkflowOperationException" should be thrown
    When I get the edited topology
    Then The SPEL expression "workflows['install'].name" should return "install"
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.workflow.RenameWorkflowOperation |
      | workflowName | uninstall                                                                |
      | newName      | should_fail                                                              |
    Then an exception of type "alien4cloud.paas.wf.exception.BadWorkflowOperationException" should be thrown
    When I get the edited topology
    Then The SPEL expression "workflows['uninstall'].name" should return "uninstall"
