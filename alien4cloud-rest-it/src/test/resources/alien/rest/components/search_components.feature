Feature: Components Search

  Background:
    Given I am authenticated with "COMPONENTS_MANAGER" role
    And There is 10 "node types" with base name "" indexed in ALIEN
    And There is 10 "relationship types" with base name "" indexed in ALIEN
    And There is 10 "capability types" with base name "" indexed in ALIEN

  @reset
  Scenario: Search for All
    When I search for all components type from 0 with result size of 100
    Then I should receive a RestResponse with no error
      And The response should contains 30 elements from various types.

  @reset
  Scenario: Search for Nodes should return the expected number of results
    Given There is 20 "node types" with base name "" indexed in ALIEN
    When I search for "node types" from 0 with result size of 100
    Then I should receive a RestResponse with no error
      And The response should contains 20 "node types".

  @reset
  Scenario: Search for Nodes with filter on capabilities should return only matching data
    Given There is 20 "node types" indexed in ALIEN with 10 of them having a "test.Capability" "capability"
    When I search for "node types" from 0 with result size of 100 and filter "capabilities.type" set to "test.Capability"
    Then I should receive a RestResponse with no error
      And The response should contains 10 "node types".
      And The "node types" in the response should all have the "test.Capability" "capability"

  @reset
  Scenario: Search for Nodes with filter on requirements should return only matching data
    Given There is 20 "node types" indexed in ALIEN with 10 of them having a "test.Requirement" "requirement"
    When I search for "node types" from 0 with result size of 100 and filter "requirements.type" set to "test.Requirement"
    Then I should receive a RestResponse with no error
      And The response should contains 10 "node types".
      And The "node types" in the response should all have the "test.Requirement" "requirement"

  @reset
  Scenario: Search for Nodes with filter on a default capability should return only matching data
    Given There is 20 "node types" indexed in ALIEN with 1 of them having a "test.Capability" "default capability"
    When I search for "node types" from 0 with result size of 100 and filter "defaultCapabilities" set to "test.Capability"
    Then I should receive a RestResponse with no error
      And The response should contains 1 "node types".

  @reset
  Scenario: Search for Relationships should return the expected number of results
    Given There is 20 "relationship types" with base name "" indexed in ALIEN
    When I search for "relationship types" from 0 with result size of 100
    Then I should receive a RestResponse with no error
      And The response should contains 20 "relationship types".

  @reset
  Scenario: Search for Relationships with filter on a validSources should return only matching data
    Given There is 20 "relationship types" indexed in ALIEN with 5 of them having a "test.ValidSource" "validSources"
    When I search for "relationship types" from 0 with result size of 100 and filter "validSources" set to "test.ValidSource"
    Then I should receive a RestResponse with no error
      And The response should contains 5 "relationship types".
      And The "relationship types" in the response should all have the "test.ValidSource" "validSource"

  @reset
  Scenario: Search for Capability should return the expected number of results
    Given There is 20 "capability types" with base name "" indexed in ALIEN
    When I search for "capability types" from 0 with result size of 100
    Then I should receive a RestResponse with no error
      And The response should contains 20 "capability types".

  ## Pagination is broken in the new elasticsearch with aggregations
#  @reset
#  Scenario: Searching for next elements should return other elements than first request
#    Given There is 20 "node types" with base name "" indexed in ALIEN
#      And I have already made a query to search the 10 first "node types"
#    When I search for "node types" from 10 with result size of 10
#    Then I should receive a RestResponse with no error
#      And The response should contains 10 other "node types".
