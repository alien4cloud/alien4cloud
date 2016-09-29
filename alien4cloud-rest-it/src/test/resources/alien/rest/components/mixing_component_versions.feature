Feature: Mixing multiple versions of components

  Background:
    Given I am authenticated with "COMPONENTS_MANAGER" role

  @reset
  Scenario: Upload CSAR with multiple versions
    Given I have uploaded the archive "tosca base types 1.0"
    And I have uploaded the archive "tosca base types 2.0"
    And I have uploaded the archive "tosca base types 3.0"
    And I have uploaded the archive "sample java types 1.0"
    And I have uploaded the archive "sample java types 2.0"
    And I have uploaded the archive "sample java types 3.0"
    When I search for "node types" from 0 with result size of 1000
    Then I should receive a RestResponse with no error
    And The response should contains 24 elements from various types of version "3.0" and older versions are
      | 1.0 |
      | 2.0 |
    When I search for "relationship types" from 0 with result size of 1000
    Then I should receive a RestResponse with no error
      And The response should contains 5 elements from various types of version "3.0" and older versions are
        | 1.0 |
        | 2.0 |

  @reset
  Scenario: Upload CSAR with multiple versions in irregular order
    Given I upload the archive "tosca base types 1.0"
    And I should receive a RestResponse with no error
    And I upload the archive "tosca base types 3.0"
    And I should receive a RestResponse with no error
    And I upload the archive "tosca base types 2.0"
    And I should receive a RestResponse with no error
    And I upload the archive "sample java types 1.0"
    And I should receive a RestResponse with no error
    And I upload the archive "sample java types 3.0"
    And I should receive a RestResponse with no error
    And I upload the archive "sample java types 2.0"
    And I should receive a RestResponse with no error
    When I search for "node types" from 0 with result size of 1000
    Then I should receive a RestResponse with no error
    And The response should contains 24 elements from various types of version "3.0" and older versions are
      | 1.0 |
      | 2.0 |
