Feature: Match location for a deployment configuration

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
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I create a location named "Elf location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I create a new application with name "ALIEN" and description "desc" and node templates
      | Compute | tosca.nodes.Compute:1.0.0-SNAPSHOT |
    And I create an application environment of type "DEVELOPMENT" with name "DEV-ALIEN" and description "" for the newly created application

  @reset
  Scenario: Get locations matchings for this topology, using admin account
    When I ask for the locations matching for the current application
    Then I should receive a match result with 2 locations
      | Thark location |
      | Elf location   |

  @reset
  Scenario: Get locations matchings for this topology, user has access
    Given I am authenticated with user named "frodon"
    When I ask for the locations matching for the current application
    Then I should receive a match result with no locations

    When I authenticate with "ADMIN" role
    And I grant access to the resource type "LOCATION" named "Thark location" to the user "frodon"
    Then I should receive a RestResponse with no error

    When I am authenticated with user named "frodon"
    When I ask for the locations matching for the current application
    Then I should receive a match result with 1 locations
      | Thark location |


  @reset
  Scenario: Get locations matchings for this topology, group has access
    Given I am authenticated with user named "sam"
    When I ask for the locations matching for the current application
    Then I should receive a match result with no locations

    When I authenticate with "ADMIN" role
    And I grant access to the resource type "LOCATION" named "Elf location" to the group "hobbits"
    Then I should receive a RestResponse with no error

    When I am authenticated with user named "sam"
    When I ask for the locations matching for the current application
    Then I should receive a match result with 1 locations
      | Elf location |


  @reset
  Scenario: Get locations matchings for this topology, application has access
    Given I am authenticated with user named "tom"
    When I ask for the locations matching for the current application
    Then I should receive a match result with no locations

    When I authenticate with "ADMIN" role
    And I grant access to the resource type "LOCATION" named "Elf location" to the application "ALIEN"
    Then I should receive a RestResponse with no error

    When I am authenticated with user named "tom"
    And I ask for the locations matching for the environment "DEV-ALIEN" of the application "ALIEN"
    Then I should receive a RestResponse with an error code 102

    When I authenticate with "ADMIN" role
    And I add a role "DEPLOYMENT_MANAGER" to user "tom" on the resource type "ENVIRONMENT" named "DEV-ALIEN"
    Then I should receive a RestResponse with no error

    When I am authenticated with user named "tom"
    When I ask for the locations matching for the environment "DEV-ALIEN" of the application "ALIEN"
    Then I should receive a match result with 1 locations
      | Elf location |


  @reset
  Scenario: Get locations matchings for this topology, environment has access
    Given I am authenticated with user named "tom"
    When I ask for the locations matching for the current application
    Then I should receive a match result with no locations

    When I authenticate with "ADMIN" role
    And I grant access to the resource type "LOCATION" named "Thark location" to the environment "DEV-ALIEN" of the application "ALIEN"
    And I add a role "DEPLOYMENT_MANAGER" to user "tom" on the resource type "ENVIRONMENT" named "DEV-ALIEN"

    When I am authenticated with user named "tom"
    When I ask for the locations matching for the environment "DEV-ALIEN" of the application "ALIEN"
    Then I should receive a match result with 1 locations
      | Thark location |