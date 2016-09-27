Feature: Node type suggestions

  Background:
    Given I am authenticated with "ADMIN" role
    And I have uploaded the archive "tosca base types 1.0"
    And I have uploaded the archive "valid-csar-with-test"

  @reset
  Scenario: node type suggestion request should return the expected result
    When I ask suggestions for node type with "java"
    Then I should receive a RestResponse with no error
    And The suggestion response should contains 4 elements
