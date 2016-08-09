Feature: Workflow editor: reinitialize workflow

  ## WHAT SHOULD WE TEST HERE ?

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Reinitializing a standard workflow should succeed
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.workflow.ReinitializeWorkflowOperation |
      | workflowName | install                                                                        |
    Then No exception should be thrown
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.workflow.ReinitializeWorkflowOperation |
      | workflowName | uninstall                                                                      |
    Then No exception should be thrown
    ## TODO: WHAT SHOULD WE TEST HERE ?
#    And The SPEL int expression "workflows.size()" should return 3
#    And The SPEL expression "workflows['wf1_renamed'].name" should return "wf1_renamed"


  Scenario: reinitializing a non standard workflow should fail
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.workflow.CreateWorkflowOperation |
      | workflowName | wf1                                                                      |
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.workflow.ReinitializeWorkflowOperation |
      | workflowName | wf1                                                                            |
    Then an exception of type "alien4cloud.paas.wf.exception.BadWorkflowOperationException" should be thrown
