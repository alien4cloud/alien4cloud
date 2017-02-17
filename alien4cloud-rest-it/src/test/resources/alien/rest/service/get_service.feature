Feature: Get service resource

  Background:
    Given I am authenticated with "ADMIN" role

  @reset
  Scenario: Getting a service should succeed
    Given I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I create a service with name "MyBdService", version "1.0.0", type "tosca.nodes.Database", archive version "1.0.0-SNAPSHOT"
    When I get the last created service
    Then I should receive a RestResponse with no error
    And The SPEL expression "name" should return "MyBdService"
    And The SPEL expression "version" should return "1.0.0"
    And The SPEL expression "nodeInstance.nodeTemplate.type" should return "tosca.nodes.Database"

  @reset
  Scenario: Getting a service that does not exists should fail
    When I get a service with id "theserviceid"
    Then I should receive a RestResponse with an error code 504

  @reset
  Scenario: Getting a service when not admin should fail
    Given I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I create a service with name "MyBdService", version "1.0.0", type "tosca.nodes.Database", archive version "1.0.0-SNAPSHOT"
    And There are these users in the system
      | sauron |
    And I add a role "APPLICATIONS_MANAGER" to user "sauron"
    And I am authenticated with user named "sauron"
    When I get the last created service
    Then I should receive a RestResponse with an error code 102
