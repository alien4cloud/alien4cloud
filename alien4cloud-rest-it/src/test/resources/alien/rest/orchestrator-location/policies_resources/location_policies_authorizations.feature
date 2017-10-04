Feature: Manage location policies resources authorizations

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | frodon |
      | sam    |
    And There is a "lordOfRing" group in the system
    And There is a "hobbits" group in the system
    And I add the user "frodon" to the group "lordOfRing"
    And I add the user "sam" to the group "hobbits"
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "middle_earth" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    Given I grant access to the resource type "LOCATION" named "middle_earth" to the user "frodon"
    Given I grant access to the resource type "LOCATION" named "middle_earth" to the group "lordOfRing"
    Given I create a policy resource of type "org.alien4cloud.policies.mock.MinimalPolicyType" named "MinimalPolicyType" related to the location "Mount doom orchestrator"/"middle_earth"
    Then I should receive a RestResponse with no error


  @reset
  Scenario: Add / Remove rights to a user on a location policy resource
    Given I grant access to the resource type "LOCATION_POLICY" named "MinimalPolicyType" to the user "frodon"
    Then I should have following list of users:
      | frodon |
    When I get the authorised users for the resource type "LOCATION_POLICY" named "MinimalPolicyType"
    Then I should have following list of users:
      | frodon |
    Given I grant access to the resource type "LOCATION_POLICY" named "MinimalPolicyType" to the user "sam"
    Then I should have following list of users:
      | frodon |
      | sam    |
    When I get the authorised users for the resource type "LOCATION_POLICY" named "MinimalPolicyType"
    Then I should have following list of users:
      | frodon |
      | sam    |
    Given I revoke access to the resource type "LOCATION_POLICY" named "MinimalPolicyType" from the user "sam"
    When I get the authorised users for the resource type "LOCATION_POLICY" named "MinimalPolicyType"
    Then I should have following list of users:
      | frodon |
    Given I revoke access to the resource type "LOCATION_POLICY" named "MinimalPolicyType" from the user "frodon"
    Then I should not have any authorized users

  @reset
  Scenario: Add / Remove rights to a group on a location policy resource
    Given I grant access to the resource type "LOCATION_POLICY" named "MinimalPolicyType" to the group "lordOfRing"
    Then I should have following list of groups:
      | lordOfRing |
    When I get the authorised groups for the resource type "LOCATION_POLICY" named "MinimalPolicyType"
    Then I should have following list of groups:
      | lordOfRing |
    Given I grant access to the resource type "LOCATION_POLICY" named "MinimalPolicyType" to the group "hobbits"
    When I get the authorised groups for the resource type "LOCATION_POLICY" named "MinimalPolicyType"
    Then I should have following list of groups:
      | lordOfRing |
      | hobbits    |
    Given I revoke access to the resource type "LOCATION_POLICY" named "MinimalPolicyType" from the group "lordOfRing"
    Then I should have following list of groups:
      | hobbits |
    When I get the authorised groups for the resource type "LOCATION_POLICY" named "MinimalPolicyType"
    Then I should have following list of groups:
      | hobbits |
    Given I revoke access to the resource type "LOCATION_POLICY" named "MinimalPolicyType" from the user "frodon"
    Then I should not have any authorized groups

  @reset
  Scenario: Add / Remove rights to a application on a location policy resource
    And I create an application with name "ALIEN", archive name "ALIEN", description "" and topology template id "null"
    When I create an application environment of type "DEVELOPMENT" with name "DEV-ALIEN" and description "" for the newly created application
    When I create an application environment of type "INTEGRATION_TESTS" with name "TST-ALIEN" and description "" for the newly created application
    When I create an application environment of type "PRODUCTION" with name "PRD-ALIEN" and description "" for the newly created application
    Given I grant access to the resource type "LOCATION" named "middle_earth" to the application "ALIEN"
    And I create an application with name "SDE", archive name "SDE", description "" and topology template id "null"
    Given I grant access to the resource type "LOCATION_POLICY" named "MinimalPolicyType" to the application "ALIEN"
    When I get the authorised applications for the resource type "LOCATION_POLICY" named "MinimalPolicyType"
    Then I should have following list of applications:
      | ALIEN |
    Given I grant access to the resource type "LOCATION_POLICY" named "MinimalPolicyType" to the application "SDE"
    When I get the authorised applications for the resource type "LOCATION_POLICY" named "MinimalPolicyType"
    Then I should have following list of applications:
      | ALIEN |
      | SDE   |
    Given I revoke access to the resource type "LOCATION_POLICY" named "MinimalPolicyType" from the application "ALIEN"
    When I get the authorised applications for the resource type "LOCATION_POLICY" named "MinimalPolicyType"
    Then I should have following list of applications:
      | SDE |
    Given I grant access to the resource type "LOCATION_POLICY" named "MinimalPolicyType" to the environment "DEV-ALIEN" of the application "ALIEN"
    When I get the authorised applications for the resource type "LOCATION_POLICY" named "MinimalPolicyType"
    Then I should have following list of environments:
      | DEV-ALIEN |
    Given I grant access to the resource type "LOCATION" named "middle_earth" to the environment "PRD-ALIEN" of the application "ALIEN"
    Given I grant access to the resource type "LOCATION_POLICY" named "MinimalPolicyType" to the environment "PRD-ALIEN" of the application "ALIEN"
    When I get the authorised applications for the resource type "LOCATION_POLICY" named "MinimalPolicyType"
    Then I should have following list of environments:
      | DEV-ALIEN |
      | PRD-ALIEN |
    And I should have following list of applications:
      | ALIEN |
      | SDE   |
    Given I revoke access to the resource type "LOCATION_POLICY" named "MinimalPolicyType" from the environment "DEV-ALIEN" of the application "ALIEN"
    When I get the authorised applications for the resource type "LOCATION_POLICY" named "MinimalPolicyType"
    Then I should have following list of environments:
      | PRD-ALIEN |
    And I should have following list of applications:
      | ALIEN |
      | SDE   |
    Given I revoke access to the resource type "LOCATION_POLICY" named "MinimalPolicyType" from the environment "PRD-ALIEN" of the application "ALIEN"
    When I get the authorised applications for the resource type "LOCATION_POLICY" named "MinimalPolicyType"
    Then I should not have any authorized environments
    Then I should have following list of applications:
      | SDE |
    Given I revoke access to the resource type "LOCATION_POLICY" named "MinimalPolicyType" from the application "SDE"
    When I get the authorised applications for the resource type "LOCATION_POLICY" named "MinimalPolicyType"
    Then I should not have any authorized applications
    Then I should not have any authorized environments

  @reset
  Scenario: Add / Remove rights to a application/environment_type on a location
    And I create an application with name "ALIEN", archive name "ALIEN", description "" and topology template id "null"
    When I create an application environment of type "DEVELOPMENT" with name "DEV-ALIEN" and description "" for the newly created application
    And I create an application environment of type "INTEGRATION_TESTS" with name "TST-ALIEN" and description "" for the newly created application
    And I create an application environment of type "PRODUCTION" with name "PRD-ALIEN" and description "" for the newly created application
    Given I grant access to the resource type "LOCATION_POLICY" named "MinimalPolicyType" to the environment type "INTEGRATION_TESTS" of the application "ALIEN"
    When I get the authorised applications for the resource type "LOCATION_POLICY" named "MinimalPolicyType"
    Then I should have following list of environment types:
      | INTEGRATION_TESTS |
    Then I should have following list of applications:
      | ALIEN |
    Given I revoke access to the resource type "LOCATION_POLICY" named "MinimalPolicyType" from the environment type "INTEGRATION_TESTS" of the application "ALIEN"
    When I get the authorised applications for the resource type "LOCATION_POLICY" named "MinimalPolicyType"
    Then I should not have any authorized environment types
    Then I should not have any authorized applications
