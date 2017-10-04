Feature: Manage location resources authorizations

  Background:
    Given I am authenticated with "ADMIN" role
    # Init archives
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    # Init a service
    And I create a service with name "MyBdService", version "1.0.0", type "tosca.nodes.Database", archive version "1.0.0-SNAPSHOT"
    # Init users
    And There are these users in the system
      | frodon |
      | sam    |
    And There is a "lordOfRing" group in the system
    And There is a "hobbits" group in the system
    And I add the user "frodon" to the group "lordOfRing"
    And I add the user "sam" to the group "hobbits"

  @reset
  Scenario: Add / Remove rights to a user on a service
    Given I grant access to the resource type "SERVICE" named "MyBdService" to the user "frodon"
    And I grant access to the resource type "SERVICE" named "MyBdService" to the user "sam"
    When I get the authorised users for the resource type "SERVICE" named "MyBdService"
    Then I should have following list of users:
      | frodon |
      | sam    |
    When I revoke access to the resource type "SERVICE" named "MyBdService" from the user "frodon"
    Then I should have following list of users:
      | sam |

  @reset
  Scenario: Add / Remove rights to a group on a service
    Given I grant access to the resource type "SERVICE" named "MyBdService" to the group "lordOfRing"
    And I grant access to the resource type "SERVICE" named "MyBdService" to the group "hobbits"
    When I get the authorised groups for the resource type "SERVICE" named "MyBdService"
    Then I should have following list of groups:
      | lordOfRing |
      | hobbits    |
    When I revoke access to the resource type "SERVICE" named "MyBdService" from the group "hobbits"
    Then I should have following list of groups:
      | lordOfRing |

  @reset
  Scenario: Add / Remove rights to a application on a service
    Given I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "middle_earth" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I grant access to the resource type "LOCATION" named "middle_earth" to the user "frodon"
    And I grant access to the resource type "LOCATION" named "middle_earth" to the group "lordOfRing"
    And I should receive a RestResponse with no error

    Given I create an application with name "ALIEN", archive name "ALIEN", description "" and topology template id "null"
    And I create an application environment of type "DEVELOPMENT" with name "DEV-ALIEN" and description "" for the newly created application
    And I create an application environment of type "INTEGRATION_TESTS" with name "TST-ALIEN" and description "" for the newly created application
    And I create an application environment of type "PRODUCTION" with name "PRD-ALIEN" and description "" for the newly created application
    And I grant access to the resource type "LOCATION" named "middle_earth" to the application "ALIEN"

    When I grant access to the resource type "SERVICE" named "MyBdService" to the application "ALIEN"
    Then I get the authorised applications for the resource type "SERVICE" named "MyBdService"
    And I should have following list of applications:
      | ALIEN |
    When I revoke access to the resource type "SERVICE" named "MyBdService" from the application "ALIEN"
    Then I should not have any authorized applications

  @reset
  Scenario: Add / Remove rights to a application environment on a service
    Given I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "middle_earth" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I grant access to the resource type "LOCATION" named "middle_earth" to the user "frodon"
    And I grant access to the resource type "LOCATION" named "middle_earth" to the group "lordOfRing"
    And I should receive a RestResponse with no error

    Given I create an application with name "ALIEN", archive name "ALIEN", description "" and topology template id "null"
    And I create an application environment of type "DEVELOPMENT" with name "DEV-ALIEN" and description "" for the newly created application
    And I create an application environment of type "INTEGRATION_TESTS" with name "TST-ALIEN" and description "" for the newly created application
    And I create an application environment of type "PRODUCTION" with name "PRD-ALIEN" and description "" for the newly created application
    And I grant access to the resource type "LOCATION" named "middle_earth" to the application "ALIEN"

    When I grant access to the resource type "SERVICE" named "MyBdService" to the environment "DEV-ALIEN" of the application "ALIEN"
    Then I get the authorised applications for the resource type "SERVICE" named "MyBdService"
    Then I should have following list of environments:
      | DEV-ALIEN |
    When I revoke access to the resource type "SERVICE" named "MyBdService" from the environment "DEV-ALIEN" of the application "ALIEN"
    Then I should not have any authorized environments

  @reset
  Scenario: Grant / Revoke rights to a application environment type on a service
    Given I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "middle_earth" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"

    Given I create an application with name "ALIEN", archive name "ALIEN", description "" and topology template id "null"
    And I create an application environment of type "DEVELOPMENT" with name "DEV-ALIEN" and description "" for the newly created application
    And I create an application environment of type "INTEGRATION_TESTS" with name "TST-ALIEN" and description "" for the newly created application
    And I create an application environment of type "PRODUCTION" with name "PRD-ALIEN" and description "" for the newly created application
    And I grant access to the resource type "SERVICE" named "MyBdService" to the environment type "DEVELOPMENT" of the application "ALIEN"
    And I grant access to the resource type "SERVICE" named "MyBdService" to the environment type "INTEGRATION_TESTS" of the application "ALIEN"
    Then I get the authorised applications for the resource type "SERVICE" named "MyBdService"
    Then I should have following list of environment types:
      | DEVELOPMENT |
      | INTEGRATION_TESTS |
    When I revoke access to the resource type "SERVICE" named "MyBdService" from the environment type "INTEGRATION_TESTS" of the application "ALIEN"
    Then I get the authorised applications for the resource type "SERVICE" named "MyBdService"
    Then I should have following list of environment types:
      | DEVELOPMENT |
    When I revoke access to the resource type "SERVICE" named "MyBdService" from the environment type "DEVELOPMENT" of the application "ALIEN"
    Then I get the authorised applications for the resource type "SERVICE" named "MyBdService"
    Then I should not have any authorized environments