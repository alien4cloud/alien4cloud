Feature: List service resource

  Background:
    Given I am authenticated with "ADMIN" role
    Given I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"

  @reset
  Scenario: Listing services when none exists should succeed
    When I list services
    Then I should receive a RestResponse with no error
    And The SPEL expression "totalResults" should return 0
    And The SPEL expression "from" should return 0
    And The SPEL expression "to" should return 0

  @reset
  Scenario: Listing services should succeed
    Given I create 100 services each of them having 10 versions from type "tosca.nodes.Database", archive version "1.0.0-SNAPSHOT"
    When I list services
    Then I should receive a RestResponse with no error
    And The SPEL expression "totalResults" should return 1000
    And The SPEL expression "from" should return 0
    And The SPEL expression "to" should return 99
    When I list services from 0 count 1000
    Then I should receive a RestResponse with no error
    And The SPEL expression "totalResults" should return 1000
    And The SPEL expression "from" should return 0
    And The SPEL expression "to" should return 99

  @reset
  Scenario: Listing services and asking for more than 1000 results should fail
    When I list services from 0 count 10000
    Then I should receive a RestResponse with an error code 501

  @reset
  Scenario: Listing a service when not admin should fail
    Given There are these users in the system
      | sauron |
    And I add a role "APPLICATIONS_MANAGER" to user "sauron"
    And I am authenticated with user named "sauron"
    When I list services
    Then I should receive a RestResponse with an error code 102
