Feature: Workflow editor: create new workflow

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Create a workflow should succeed
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.workflow.CreateWorkflowOperation |
      | workflowName | wf1                                                                      |
    Then No exception should be thrown
    And The SPEL int expression "workflows.size()" should return 3
    And The SPEL expression "workflows['wf1'].name" should return "wf1"
