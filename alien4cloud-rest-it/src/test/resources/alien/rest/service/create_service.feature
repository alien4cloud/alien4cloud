Feature: Create service resource

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"

  @reset
  Scenario: Creating a new service should succeed
    When I create a service with name "MyBdService", version "1.0.0", type "tosca.nodes.Database", archive version "1.0.0-SNAPSHOT"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Creating a new service with no name should fail
    When I create a service with name "null", version "1.0.0", type "tosca.nodes.Database", archive version "1.0.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 501

  @reset
  Scenario: Creating a new service with empty name should fail
    When I create a service with name "", version "1.0.0", type "tosca.nodes.Database", archive version "1.0.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 501

  @reset
  Scenario: Creating a new service with no name version fail
    When I create a service with name "MyBdService", version "null", type "tosca.nodes.Database", archive version "1.0.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 501

  @reset
  Scenario: Creating a new service with empty version should fail
    When I create a service with name "MyBdService", version "", type "tosca.nodes.Database", archive version "1.0.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 501

  @reset
  Scenario: Creating a new service with an invalid version should fail
    When I create a service with name "MyBdService", version "anyVersion", type "tosca.nodes.Database", archive version "1.0.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 605

  @reset
  Scenario: Creating a new service with unknown type should fail
    When I create a service with name "MyBdService", version "1.0.0", type "tosca.nodes.NonExistingType", archive version "1.0.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 504

  @reset
  Scenario: Creating a new service with unknown archive version should fail
    When I create a service with name "MyBdService", version "1.0.0", type "tosca.nodes.Database", archive version "10.0.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 504

  @reset
  Scenario: Creating a new service with existing name & version should fail
    Given I create a service with name "MyBdService", version "1.0.0", type "tosca.nodes.Database", archive version "1.0.0-SNAPSHOT"
    When I create a service with name "MyBdService", version "1.0.0", type "tosca.nodes.Database", archive version "1.0.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 502

  @reset
  Scenario: Creating a new service with existing name but different version should succeed
    Given I create a service with name "MyBdService", version "1.0.0", type "tosca.nodes.Database", archive version "1.0.0-SNAPSHOT"
    When I create a service with name "MyBdService", version "1.0.2", type "tosca.nodes.Database", archive version "1.0.0-SNAPSHOT"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Creating a new service when not admin should fail
    And There are these users in the system
      | sauron |
    And I add a role "APPLICATIONS_MANAGER" to user "sauron"
    And I am authenticated with user named "sauron"
    When I create a service with name "MyBdService", version "1.0.0", type "tosca.nodes.Database", archive version "1.0.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 102
