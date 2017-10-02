Feature: Topology editor: update policy targets

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Update the list of policy targets with existing target nodes should succeed
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute_1                                                             |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.policies.AddPolicyOperation |
      | policyName   | MyPolicy                                                            |
      | policyTypeId | tosca.policies.Placement:1.0.0-SNAPSHOT                             |
    When I execute the operation
      | type       | org.alien4cloud.tosca.editor.operations.policies.UpdatePolicyTargetsOperation |
      | policyName | MyPolicy                                                                      |
      | targets    | Compute, Compute_1                                                            |
    Then No exception should be thrown
    And The SPEL expression "policies.size()" should return 1
    And The SPEL expression "policies['MyPolicy'].type" should return "tosca.policies.Placement"
    And The SPEL expression "policies['MyPolicy'].targets.size()" should return 2

  Scenario: Update the list of policy targets with missing target nodes should fail
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.policies.AddPolicyOperation |
      | policyName   | MyPolicy                                                            |
      | policyTypeId | tosca.policies.Placement:1.0.0-SNAPSHOT                             |
    When I execute the operation
      | type       | org.alien4cloud.tosca.editor.operations.policies.UpdatePolicyTargetsOperation |
      | policyName | MyPolicy                                                                      |
      | targets    | Compute, Compute_1                                                            |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Update the list of policy targets for a missing policy should fail
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute_1                                                             |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.policies.AddPolicyOperation |
      | policyName   | MyPolicy                                                            |
      | policyTypeId | tosca.policies.Placement:1.0.0-SNAPSHOT                             |
    When I execute the operation
      | type       | org.alien4cloud.tosca.editor.operations.policies.UpdatePolicyTargetsOperation |
      | policyName | missingPolicy                                                                 |
      | targets    | Compute, Compute_1                                                            |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Remove a node template that is used in a policy should succeed
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute_1                                                             |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.policies.AddPolicyOperation |
      | policyName   | MyPolicy                                                            |
      | policyTypeId | tosca.policies.Placement:1.0.0-SNAPSHOT                             |
    And I execute the operation
      | type       | org.alien4cloud.tosca.editor.operations.policies.UpdatePolicyTargetsOperation |
      | policyName | MyPolicy                                                                      |
      | targets    | Compute, Compute_1                                                            |
    When I execute the operation
      | type     | org.alien4cloud.tosca.editor.operations.nodetemplate.DeleteNodeOperation |
      | nodeName | Compute_1                                                                |
    Then No exception should be thrown
    And The SPEL expression "policies.size()" should return 1
    And The SPEL expression "policies['MyPolicy'].type" should return "tosca.policies.Placement"
    And The SPEL expression "policies['MyPolicy'].targets.size()" should return 1
