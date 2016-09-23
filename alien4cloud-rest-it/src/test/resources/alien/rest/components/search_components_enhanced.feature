Feature: Components Search enhanced with special component naming

  Background:
    Given I am authenticated with "COMPONENTS_MANAGER" role
    And There is 10 "node types" with base name "" indexed in ALIEN
    And There is 5 "node types" with base name "alien.nodes.tests.Types" indexed in ALIEN

  @reset
  Scenario: Searching for next elements with doted name first
    When I make a basic "nodes" search for "node types" from 0 with result size of 10
    Then I should receive a RestResponse with no error
    And The response should contains 5 "node types".

  @reset
  Scenario: Searching for next elements with doted name second
    When I make a basic "tests" search for "node types" from 0 with result size of 10
    Then I should receive a RestResponse with no error
    And The response should contains 5 "node types".

  @reset
  Scenario: Searching for next elements with doted name and base node type name
    When I make a basic "node" search for "node types" from 0 with result size of 20
    Then I should receive a RestResponse with no error
      And The response should contains 15 elements
