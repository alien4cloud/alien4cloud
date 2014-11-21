Feature: Mixing multiple versions of components

  Background:
    Given I am authenticated with "COMPONENTS_MANAGER" role

  Scenario: Upload CSAR with multiple versions
    Given I upload the archive "normative types 1.0.0-wd03"
    And I should receive a RestResponse with no error
    And I upload the archive "normative types 1.0.1-wd03"
    And I should receive a RestResponse with no error
    And I upload the archive "normative types 1.0.2-wd03"
    And I should receive a RestResponse with no error
    And I upload the archive "sample java types 1.0"
    And I should receive a RestResponse with no error
    And I upload the archive "sample java types 2.0"
    And I should receive a RestResponse with no error
    And I upload the archive "sample java types 3.0"
    And I should receive a RestResponse with no error
    When I search for "node types" from 0 with result size of 1000
    Then I should receive a RestResponse with no error
    And The response should contains 14 elements from various types of version "3.0" and older versions are
      | 1.0 |
      | 2.0 |
    When I search for "relationship types" from 0 with result size of 1000
    Then I should receive a RestResponse with no error
      And The response should contains 5 elements from various types of version "3.0" and older versions are
        | 1.0 |
        | 2.0 |

  Scenario: Upload CSAR with multiple versions in irregular order
    Given I upload the archive "normative types 1.0.0-wd03"
    And I should receive a RestResponse with no error
    And I upload the archive "normative types 1.0.2-wd03"
    And I should receive a RestResponse with no error
    And I upload the archive "normative types 1.0.1-wd03"
    And I should receive a RestResponse with no error
    And I upload the archive "sample java types 1.0"
    And I should receive a RestResponse with no error
    And I upload the archive "sample java types 3.0"
    And I should receive a RestResponse with no error
    And I upload the archive "sample java types 2.0"
    And I should receive a RestResponse with no error
    When I search for "node types" from 0 with result size of 1000
    Then I should receive a RestResponse with no error
    And The response should contains 14 elements from various types of version "3.0" and older versions are
      | 1.0 |
      | 2.0 |
