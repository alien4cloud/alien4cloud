Feature: Perform matching on policies

  Background:
    Given I am authenticated with "ADMIN" role
    And I have uploaded the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I successfully create an orchestrator named "Mock orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I successfully enable the orchestrator "Mock orchestrator"
    And I create a location named "Mock aws location" and infrastructure type "Amazon" to the orchestrator "Mock orchestrator"
    Then I should receive a RestResponse with no error
    # Add a sample compute node location resource
    When I create a resource of type "org.alien4cloud.nodes.mock.aws.Compute" named "Small_Ubuntu" related to the location "Mock orchestrator"/"Mock aws location"
    Then I should receive a RestResponse with no error
    And I update the property "imageId" to "Small" for the resource named "Small_Ubuntu" related to the location "Mock orchestrator"/"Mock aws location"
    Then I should receive a RestResponse with no error
    And I update the property "flavorId" to "Ubuntu" for the resource named "Small_Ubuntu" related to the location "Mock orchestrator"/"Mock aws location"
    Then I should receive a RestResponse with no error
    # Add a sample placement node location resource
    When I create a policy resource of type "org.alien4cloud.mock.policies.AntiAffinity" named "SampleAntiAffinity" related to the location "Mock orchestrator"/"Mock aws location"
    Then I should receive a RestResponse with no error
    When I get the location "Mock orchestrator"/"Mock aws location"
    Then I should receive a RestResponse with no error
    And I update the complex list property "availability_zones" to """["az_1","az_2"]""" for the policy resource named "SampleAntiAffinity" related to the location "Mock orchestrator"/"Mock aws location"
    Then I should receive a RestResponse with no error
    # TODO Update zones properties
    And I create a new application with name "PolicyMatchingTest" and description "Application for policy matching test" and node templates
      | Compute   | tosca.nodes.Compute:1.0.0-SNAPSHOT |
      | Compute_1 | tosca.nodes.Compute:1.0.0-SNAPSHOT |

  @reset
  Scenario: Policy modifier should inject zone properties
    # Add an abstract placement policy to the topology with Compute and Compute_2 as targets
    And I get the current topology
    And I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.policies.AddPolicyOperation |
      | policyName   | MyPolicy                                                            |
      | policyTypeId | tosca.policies.Placement:1.0.0-SNAPSHOT                             |
    When I execute the operation
      | type       | org.alien4cloud.tosca.editor.operations.policies.UpdatePolicyTargetsOperation |
      | policyName | MyPolicy                                                                      |
      | targets    | Compute, Compute_1                                                            |
    And I save the topology
    When I Set a unique location policy to "Mock orchestrator"/"Mock aws location" for all nodes
    # Policy matching should have been done automatically, check that the matched node templates have zone defined
    Then The TopologyDTO SPEL expression "topology.nodeTemplates['Compute'].properties['zone'].value" should return "az_2"
    Then The TopologyDTO SPEL expression "topology.nodeTemplates['Compute_1'].properties['zone'].value" should return "az_1"
