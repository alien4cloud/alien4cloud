Feature: Policies substitution

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | frodon |
      | sam    |
      | tom    |
    And There is a "hobbits" group in the system
    And I add the user "sam" to the group "hobbits"
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I successfully create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I successfully enable the orchestrator "Mount doom orchestrator"

    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"

    And I create a policy resource of type "org.alien4cloud.mock.policies.AntiAffinity" named "AntiAffinity" related to the location "Mount doom orchestrator"/"Thark location"
    And I create a policy resource of type "org.alien4cloud.mock.policies.AntiAffinity" named "AntiAffinity2" related to the location "Mount doom orchestrator"/"Thark location"

    And I create a new application with name "ALIEN" and description "desc" and node templates
      | Compute | tosca.nodes.Compute:1.0.0-SNAPSHOT |
    And I get the current topology
    And I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.policies.AddPolicyOperation |
      | policyName   | MyPolicy                                                            |
      | policyTypeId | tosca.policies.Placement:1.0.0-SNAPSHOT                             |
    And I save the topology
    And I add a role "APPLICATION_MANAGER" to group "hobbits" on the application "ALIEN"
    And I add a role "APPLICATION_MANAGER" to user "frodon" on the application "ALIEN"
    And I add a role "APPLICATION_MANAGER" to user "tom" on the application "ALIEN"
#    And I create an application environment of type "DEVELOPMENT" with name "DEV-ALIEN" and description "" for the newly created application
#    And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes of the environment "DEV-ALIEN"
    And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes

  @reset
  Scenario: Set a substitution for a policy, using admin account
    When I substitute on the current application the policy "MyPolicy" with the location resource "Mount doom orchestrator"/"Thark location"/"AntiAffinity"
    Then I should receive a RestResponse with no error
    And The deployment topology should have the substituted policies
      | MyPolicy | AntiAffinity | org.alien4cloud.mock.policies.AntiAffinity |

  @reset
  Scenario: Set a substitution for a policy, user has access
    Given I successfully grant access to the resource type "LOCATION" named "Thark location" to the user "frodon"
    And I am authenticated with user named "frodon"
    When I substitute on the current application the policy "MyPolicy" with the location resource "Mount doom orchestrator"/"Thark location"/"AntiAffinity"
    Then I should receive a RestResponse with an error code 504
    When I substitute on the current application the policy "MyPolicy" with the location resource "Mount doom orchestrator"/"Thark location"/"AntiAffinity2"
    Then I should receive a RestResponse with an error code 504

    When I authenticate with "ADMIN" role
    And I successfully grant access to the resource type "LOCATION_POLICY" named "Mount doom orchestrator/Thark location/AntiAffinity" to the user "frodon"

    When I am authenticated with user named "frodon"
    When I substitute on the current application the policy "MyPolicy" with the location resource "Mount doom orchestrator"/"Thark location"/"AntiAffinity"
    Then I should receive a RestResponse with no error
    And The deployment topology should have the substituted policies
      | MyPolicy | AntiAffinity | org.alien4cloud.mock.policies.AntiAffinity |
    When I substitute on the current application the policy "MyPolicy" with the location resource "Mount doom orchestrator"/"Thark location"/"AntiAffinity2"
    Then I should receive a RestResponse with an error code 504

  @reset
  Scenario: Set a substitution for a policy, group has access
    Given I successfully grant access to the resource type "LOCATION" named "Thark location" to the group "hobbits"
    And I am authenticated with user named "sam"
    When I substitute on the current application the policy "MyPolicy" with the location resource "Mount doom orchestrator"/"Thark location"/"AntiAffinity"
    Then I should receive a RestResponse with an error code 504
    When I substitute on the current application the policy "MyPolicy" with the location resource "Mount doom orchestrator"/"Thark location"/"AntiAffinity2"
    Then I should receive a RestResponse with an error code 504

    When I authenticate with "ADMIN" role
    And I successfully grant access to the resource type "LOCATION_POLICY" named "Mount doom orchestrator/Thark location/AntiAffinity" to the group "hobbits"

    When I am authenticated with user named "sam"
    When I substitute on the current application the policy "MyPolicy" with the location resource "Mount doom orchestrator"/"Thark location"/"AntiAffinity"
    Then I should receive a RestResponse with no error
    And The deployment topology should have the substituted policies
      | MyPolicy | AntiAffinity | org.alien4cloud.mock.policies.AntiAffinity |
    When I substitute on the current application the policy "MyPolicy" with the location resource "Mount doom orchestrator"/"Thark location"/"AntiAffinity2"
    Then I should receive a RestResponse with an error code 504

  @reset
  Scenario: Set a substitution for a policy, application has access
    Given I successfully grant access to the resource type "LOCATION" named "Thark location" to the application "ALIEN"
    And I am authenticated with user named "tom"
    When I substitute on the current application the policy "MyPolicy" with the location resource "Mount doom orchestrator"/"Thark location"/"AntiAffinity"
    Then I should receive a RestResponse with an error code 504
    When I substitute on the current application the policy "MyPolicy" with the location resource "Mount doom orchestrator"/"Thark location"/"AntiAffinity2"
    Then I should receive a RestResponse with an error code 504

    When I authenticate with "ADMIN" role
    And I successfully grant access to the resource type "LOCATION_POLICY" named "Mount doom orchestrator/Thark location/AntiAffinity" to the application "ALIEN"

    When I am authenticated with user named "tom"
    When I substitute on the current application the policy "MyPolicy" with the location resource "Mount doom orchestrator"/"Thark location"/"AntiAffinity"
    Then I should receive a RestResponse with no error
    And The deployment topology should have the substituted policies
      | MyPolicy | AntiAffinity | org.alien4cloud.mock.policies.AntiAffinity |
    When I substitute on the current application the policy "MyPolicy" with the location resource "Mount doom orchestrator"/"Thark location"/"AntiAffinity2"
    Then I should receive a RestResponse with an error code 504

  # grant access to the second resource to tom
    Given I authenticate with "ADMIN" role
    And I successfully grant access to the resource type "LOCATION" named "Thark location" to the user "tom"
    And I successfully grant access to the resource type "LOCATION_POLICY" named "Mount doom orchestrator/Thark location/AntiAffinity2" to the user "tom"
    And I am authenticated with user named "tom"
    # application has been granted access, so sustitution with the first resource is possible
    When I substitute on the current application the policy "MyPolicy" with the location resource "Mount doom orchestrator"/"Thark location"/"AntiAffinity"
    Then I should receive a RestResponse with no error
    And The deployment topology should have the substituted policies
      | MyPolicy | AntiAffinity | org.alien4cloud.mock.policies.AntiAffinity |
    #tom should be able to substitute with the second resource as he has been granted access
    When I substitute on the current application the policy "MyPolicy" with the location resource "Mount doom orchestrator"/"Thark location"/"AntiAffinity2"
    Then I should receive a RestResponse with no error
    And The deployment topology should have the substituted policies
      | MyPolicy | AntiAffinity2 | org.alien4cloud.mock.policies.AntiAffinity |


  @reset
  Scenario: Set a substitution for a policy, environment has access
    Given I successfully grant access to the resource type "LOCATION" named "Thark location" to the environment "Environment" of the application "ALIEN"
    And I am authenticated with user named "tom"
    When I substitute on the current application the policy "MyPolicy" with the location resource "Mount doom orchestrator"/"Thark location"/"AntiAffinity"
    Then I should receive a RestResponse with an error code 504
    When I substitute on the current application the policy "MyPolicy" with the location resource "Mount doom orchestrator"/"Thark location"/"AntiAffinity2"
    Then I should receive a RestResponse with an error code 504

    When I authenticate with "ADMIN" role
    And I successfully grant access to the resource type "LOCATION_POLICY" named "Mount doom orchestrator/Thark location/AntiAffinity" to the environment "Environment" of the application "ALIEN"

    When I am authenticated with user named "tom"
    When I substitute on the current application the policy "MyPolicy" with the location resource "Mount doom orchestrator"/"Thark location"/"AntiAffinity"
    Then I should receive a RestResponse with no error
    And The deployment topology should have the substituted policies
      | MyPolicy | AntiAffinity | org.alien4cloud.mock.policies.AntiAffinity |
    When I substitute on the current application the policy "MyPolicy" with the location resource "Mount doom orchestrator"/"Thark location"/"AntiAffinity2"
    Then I should receive a RestResponse with an error code 504

  # grant access to the second resource to tom
    Given I authenticate with "ADMIN" role
    And I successfully grant access to the resource type "LOCATION" named "Thark location" to the user "tom"
    And I successfully grant access to the resource type "LOCATION_POLICY" named "Mount doom orchestrator/Thark location/AntiAffinity2" to the user "tom"
    And I am authenticated with user named "tom"
    # environment has been granted access, so sustitution with the first resource is possible
    When I substitute on the current application the policy "MyPolicy" with the location resource "Mount doom orchestrator"/"Thark location"/"AntiAffinity"
    Then I should receive a RestResponse with no error
    And The deployment topology should have the substituted policies
      | MyPolicy | AntiAffinity | org.alien4cloud.mock.policies.AntiAffinity |
    #tom should be able to substitute with the second resource as he has been granted access
    When I substitute on the current application the policy "MyPolicy" with the location resource "Mount doom orchestrator"/"Thark location"/"AntiAffinity2"
    Then I should receive a RestResponse with no error
    And The deployment topology should have the substituted policies
      | MyPolicy | AntiAffinity2 | org.alien4cloud.mock.policies.AntiAffinity |
