Feature: Search service resource

  Background:
    Given I am authenticated with "ADMIN" role
    Given I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"

  @reset
  Scenario: Searching services when none exists should succeed
    When I POST "empty.json" to "/rest/v1/services/adv/search"
    Then I should receive a RestResponse with no error
    And I register service list result for SPEL
    And The SPEL expression "totalResults" should return 0
    And The SPEL expression "from" should return 0
    And The SPEL expression "to" should return 0

  @reset
  Scenario: Searching services with pagination should succeed
    Given I create 100 services each of them having 10 versions from type "tosca.nodes.Database", archive version "1.0.0-SNAPSHOT"
    When I POST "empty.json" to "/rest/v1/services/adv/search"
    Then I should receive a RestResponse with no error
    And I register service list result for SPEL
    And The SPEL expression "totalResults" should return 1000
    And The SPEL expression "from" should return 0
    And The SPEL expression "to" should return 49
    When I POST "services/search_size_1000.json" to "/rest/v1/services/adv/search"
    Then I should receive a RestResponse with no error
    And I register service list result for SPEL
    And The SPEL expression "totalResults" should return 1000
    And The SPEL expression "from" should return 0
    And The SPEL expression "to" should return 999
    When I POST "services/search_from_50.json" to "/rest/v1/services/adv/search"
    Then I should receive a RestResponse with no error
    And I register service list result for SPEL
    And The SPEL expression "totalResults" should return 1000
    And The SPEL expression "from" should return 50
    And The SPEL expression "to" should return 99
    When I POST "services/search_from_50_size_100.json" to "/rest/v1/services/adv/search"
    Then I should receive a RestResponse with no error
    And I register service list result for SPEL
    And The SPEL expression "totalResults" should return 1000
    And The SPEL expression "from" should return 50
    And The SPEL expression "to" should return 149

#  @reset
#  Scenario: Searching services and asking for more than 1000 results should fail
#    When I POST "services/search_size_10000.json" to "/rest/v1/services/adv/search"
#    Then I should receive a RestResponse with an error code 501

  @reset
  Scenario: Searching a service when not admin should fail
    Given There are these users in the system
      | sauron |
    And I add a role "APPLICATIONS_MANAGER" to user "sauron"
    And I am authenticated with user named "sauron"
    When I POST "empty.json" to "/rest/v1/services/adv/search"
    Then I should receive a RestResponse with an error code 102
