Feature: Service creation

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"

  @reset
  Scenario: Creating a new service
    When I create a service with name "MyBdService", version "1.0.0", type "tosca.nodes.Database", archive name "tosca-normative-types", archive version "1.0.0-SNAPSHOT", deploymentId "null"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Creating a new service with no name should fail
    When I create a service with name "null", version "1.0.0", type "tosca.nodes.Database", archive name "tosca-normative-types", archive version "1.0.0-SNAPSHOT", deploymentId "null"
    Then I should receive a RestResponse with an error code 501

  @reset
  Scenario: Creating a new service with empty name should fail
    When I create a service with name "", version "1.0.0", type "tosca.nodes.Database", archive name "tosca-normative-types", archive version "1.0.0-SNAPSHOT", deploymentId "null"
    Then I should receive a RestResponse with an error code 501

  @reset
  Scenario: Creating a new service with no valid version should fail
    When I create a service with name "MyBdService", version "anyVersion", type "tosca.nodes.Database", archive name "tosca-normative-types", archive version "1.0.0-SNAPSHOT", deploymentId "null"
    Then I should receive a RestResponse with an error code 605

  @reset
  Scenario: Creating a new service with unkonwn type should fail
    When I create a service with name "", version "1.0.0", type "tosca.nodes.NonExistingType", archive name "tosca-normative-types", archive version "1.0.0-SNAPSHOT", deploymentId "null"
    Then I should receive a RestResponse with an error code 501

  @reset
  Scenario: Creating a new service with unkonwn archive version should fail
    When I create a service with name "", version "1.0.0", type "tosca.nodes.Database", archive name "tosca-normative-types", archive version "10.0.0-SNAPSHOT", deploymentId "null"
    Then I should receive a RestResponse with an error code 501

  @reset
  Scenario: Creating a new service with existing name & version should fail
    Given I create a service with name "MyBdService", version "1.0.0", type "tosca.nodes.Database", archive name "tosca-normative-types", archive version "1.0.0-SNAPSHOT", deploymentId "null"
    When I create a service with name "MyBdService", version "1.0.0", type "tosca.nodes.Database", archive name "tosca-normative-types", archive version "1.0.0-SNAPSHOT", deploymentId "null"
    Then I should receive a RestResponse with an error code 502

  @reset
  Scenario: Creating a new service with existing name but different version should sucess
    Given I create a service with name "MyBdService", version "1.0.0", type "tosca.nodes.Database", archive name "tosca-normative-types", archive version "1.0.0-SNAPSHOT", deploymentId "null"
    When I create a service with name "MyBdService", version "1.0.2", type "tosca.nodes.Database", archive name "tosca-normative-types", archive version "1.0.0-SNAPSHOT", deploymentId "null"
    Then I should receive a RestResponse with no error