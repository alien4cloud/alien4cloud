Feature: Manage application's authorizations on location

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "middle_earth" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I create an application with name "ALIEN", archive name "ALIEN", description "" and topology template id "null"
    When I create an application environment of type "DEVELOPMENT" with name "DEV-ALIEN" and description "" for the newly created application
    When I create an application environment of type "INTEGRATION_TESTS" with name "TST-ALIEN" and description "" for the newly created application
    When I create an application environment of type "PRODUCTION" with name "PRD-ALIEN" and description "" for the newly created application
    And I create an application with name "SDE", archive name "SDE", description "" and topology template id "null"

  @reset
  Scenario: Add / Remove rights to a application on a location
    Given I grant access to the resource type "LOCATION" named "middle_earth" to the application "ALIEN"
    When I get the authorised applications for the resource type "LOCATION" named "middle_earth"
    Then I should have following list of applications:
      | ALIEN |
    Given I grant access to the resource type "LOCATION" named "middle_earth" to the application "SDE"
    When I get the authorised applications for the resource type "LOCATION" named "middle_earth"
    Then I should have following list of applications:
      | ALIEN |
      | SDE   |
    Given I revoke access to the resource type "LOCATION" named "middle_earth" from the application "ALIEN"
    When I get the authorised applications for the resource type "LOCATION" named "middle_earth"
    Then I should have following list of applications:
      | SDE |
    Given I grant access to the resource type "LOCATION" named "middle_earth" to the environment "DEV-ALIEN" of the application "ALIEN"
    When I get the authorised applications for the resource type "LOCATION" named "middle_earth"
    Then I should have following list of environments:
      | DEV-ALIEN |
    Given I grant access to the resource type "LOCATION" named "middle_earth" to the environment "PRD-ALIEN" of the application "ALIEN"
    When I get the authorised applications for the resource type "LOCATION" named "middle_earth"
    Then I should have following list of environments:
      | DEV-ALIEN |
      | PRD-ALIEN |
    And I should have following list of applications:
      | ALIEN |
      | SDE   |
    Given I revoke access to the resource type "LOCATION" named "middle_earth" from the environment "DEV-ALIEN" of the application "ALIEN"
    When I get the authorised applications for the resource type "LOCATION" named "middle_earth"
    Then I should have following list of environments:
      | PRD-ALIEN |
    And I should have following list of applications:
      | ALIEN |
      | SDE   |
    Given I revoke access to the resource type "LOCATION" named "middle_earth" from the environment "PRD-ALIEN" of the application "ALIEN"
    When I get the authorised applications for the resource type "LOCATION" named "middle_earth"
    Then I should not have any authorized environments
    Then I should have following list of applications:
      | SDE |
    Given I revoke access to the resource type "LOCATION" named "middle_earth" from the application "SDE"
    When I get the authorised applications for the resource type "LOCATION" named "middle_earth"
    Then I should not have any authorized applications
    Then I should not have any authorized environments