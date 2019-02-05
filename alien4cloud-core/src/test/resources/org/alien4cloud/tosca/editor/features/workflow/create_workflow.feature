Feature: Workflow editor: create new workflow

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Create a workflow should succeed
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.workflow.CreateWorkflowOperation |
      | workflowName | wf1                                                                      |
    Then No exception should be thrown
    And The SPEL expression "workflows.size()" should return 7
    And The SPEL expression "workflows['wf1'].name" should return "wf1"


  Scenario: Creating a workflow with a name containing special chars other than _ should fail
    Given I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.workflow.CreateWorkflowOperation |
      | workflowName | should fail                                                              |
    Then an exception of type "alien4cloud.exception.InvalidNameException" should be thrown
    When I get the edited topology
    Then The SPEL expression "workflows.size()" should return 6
    Then The SPEL expression "workflows['should fail']" should return "null"
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.workflow.CreateWorkflowOperation |
      | workflowName | should*fail££                                                            |
    Then an exception of type "alien4cloud.exception.InvalidNameException" should be thrown
    When I get the edited topology
    Then The SPEL expression "workflows.size()" should return 6
    Then The SPEL expression "workflows['should*fail££']" should return "null"
