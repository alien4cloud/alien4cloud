Feature: Update substituted policy property

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I successfully create an orchestrator named "Mock orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I successfully enable the orchestrator "Mock orchestrator"

    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mock orchestrator"

    And I create a policy resource of type "org.alien4cloud.policies.mock.SimpleConditionPolicyType" named "SimpleConditionPolicyType" related to the location "Mock orchestrator"/"Thark location"
    And I update the property "sample_property" to "doNotUpdate" for the policy resource named "SimpleConditionPolicyType" related to the location "Mock orchestrator"/"Thark location"
    And I create a policy resource of type "org.alien4cloud.policies.mock.SimpleConditionPolicyType" named "SimpleConditionPolicyType2" related to the location "Mock orchestrator"/"Thark location"

    And I create a new application with name "ALIEN" and description "desc" and node templates
      | Compute | tosca.nodes.Compute:1.0.0-SNAPSHOT |
    And I get the current topology
    And I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.policies.AddPolicyOperation |
      | policyName   | MyPolicy                                                            |
      | policyTypeId | tosca.policies.Root:1.0.0-SNAPSHOT                                  |
    And I execute the operation
      | type       | org.alien4cloud.tosca.editor.operations.policies.UpdatePolicyTargetsOperation |
      | policyName | MyPolicy                                                                      |
      | targets    | Compute                                                                       |
    And I save the topology
    And I get the deployment topology for the current application
    And I Set a unique location policy to "Mock orchestrator"/"Thark location" for all nodes

  @reset
  Scenario: Update a substituted policy's property
    Given I substitute on the current application the policy "MyPolicy" with the location resource "Mock orchestrator"/"Thark location"/"SimpleConditionPolicyType2"
    And I should receive a RestResponse with no error
    When I update the property "sample_property" to "toto" for the substituted policy "MyPolicy"
    Then I should receive a RestResponse with no error
    When I register the rest response data as SPEL context of type "alien4cloud.deployment.DeploymentTopologyDTO"
    Then The SPEL expression "topology.policies['MyPolicy'].properties['sample_property'].value" should return "toto"

  @reset
  Scenario: Update a substituted policy's property should fail if configured by admin
    Given I substitute on the current application the policy "MyPolicy" with the location resource "Mock orchestrator"/"Thark location"/"SimpleConditionPolicyType"
    When I update the property "sample_property" to "toto" for the substituted policy "MyPolicy"
    Then I should receive a RestResponse with an error code 800
    When I ask for the deployment topology of the application "ALIEN"
    And I register the rest response data as SPEL context of type "alien4cloud.deployment.DeploymentTopologyDTO"
    Then The SPEL expression "topology.policies['MyPolicy'].properties['sample_property'].value" should return "doNotUpdate"

  @reset
  Scenario: Set a substituted policy's property as a secret
    Given I substitute on the current application the policy "MyPolicy" with the location resource "Mock orchestrator"/"Thark location"/"SimpleConditionPolicyType2"
    And I should receive a RestResponse with no error
    When I update the property "sample_property" to a secret with a secret path "kv/sample_property" for the substituted policy "MyPolicy"
    Then I should receive a RestResponse with no error
    When I register the rest response data as SPEL context of type "alien4cloud.deployment.DeploymentTopologyDTO"
    Then The SPEL expression "topology.policies['MyPolicy'].properties['sample_property'].function" should return "get_secret"
    Then The SPEL expression "topology.policies['MyPolicy'].properties['sample_property'].parameters[0]" should return "kv/sample_property"

  @reset
  Scenario: Update a substituted policy's property should fail if configured by admin
    Given I substitute on the current application the policy "MyPolicy" with the location resource "Mock orchestrator"/"Thark location"/"SimpleConditionPolicyType"
    When I update the property "sample_property" to a secret with a secret path "kv/sample_property" for the substituted policy "MyPolicy"
    Then I should receive a RestResponse with an error code 800
    When I ask for the deployment topology of the application "ALIEN"
    And I register the rest response data as SPEL context of type "alien4cloud.deployment.DeploymentTopologyDTO"
    Then The SPEL expression "topology.policies['MyPolicy'].properties['sample_property'].value" should return "doNotUpdate"
