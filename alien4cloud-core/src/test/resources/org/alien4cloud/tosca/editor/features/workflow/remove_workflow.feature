Feature: Workflow editor: remove workflow

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Removing a workflow should succeed
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.workflow.CreateWorkflowOperation |
      | workflowName | wf1                                                                      |
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.workflow.RemoveWorkflowOperation |
      | workflowName | wf1                                                                      |
    Then No exception should be thrown
    And The SPEL int expression "workflows.size()" should return 2
    And The SPEL expression "workflows['wf1']" should return "null"

  Scenario: Removing a standard workflow should fail
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.workflow.RemoveWorkflowOperation |
      | workflowName | install                                                                  |
    Then an exception of type "alien4cloud.paas.wf.exception.BadWorkflowOperationException" should be thrown
    When I get the edited topology
    Then The SPEL expression "workflows['install'].name" should return "install"
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.workflow.RemoveWorkflowOperation |
      | workflowName | uninstall                                                                |
    Then an exception of type "alien4cloud.paas.wf.exception.BadWorkflowOperationException" should be thrown
    When I get the edited topology
    Then The SPEL expression "workflows['uninstall'].name" should return "uninstall"
