Feature: Mixing multiple versions of components

  Background:
    Given I am authenticated with "COMPONENTS_MANAGER" role

  Scenario: Upload CSAR with multiple versions
    Given I upload the archive file that is "csar file containing base types"
    And I upload the archive file that is "csar file containing java types"
    And I upload the archive file that is "csar file containing base types V2"
    And I upload the archive file that is "csar file containing java types V2"
    And I upload the archive file that is "csar file containing base types V3"
    And I upload the archive file that is "csar file containing java types V3"
    When I search for "node types" from 0 with result size of 1000
    Then I should receive a RestResponse with no error
    And The response should contains 24 elements from various types of version "3.0" and older versions are
      | 1.0 |
      | 2.0 |

  Scenario: Upload CSAR with multiple versions in irregular order
    Given I upload the archive file that is "csar file containing base types"
    And I upload the archive file that is "csar file containing java types"
    And I upload the archive file that is "csar file containing base types V3"
    And I upload the archive file that is "csar file containing java types V3"
    And I upload the archive file that is "csar file containing base types V2"
    And I upload the archive file that is "csar file containing java types V2"
    When I search for "node types" from 0 with result size of 1000
    Then I should receive a RestResponse with no error
    And The response should contains 24 elements from various types of version "3.0" and older versions are
      | 1.0 |
      | 2.0 |

  Scenario: Upload CSAR with multiple versions
    Given I upload the archive file that is "csar file containing base types"
    And I upload the archive file that is "csar file containing java types"
    And I upload the archive file that is "csar file containing base types V2"
    And I upload the archive file that is "csar file containing java types V2"
    And I upload the archive file that is "csar file containing base types V3"
    And I upload the archive file that is "csar file containing java types V3"
    When I search for "relationship types" from 0 with result size of 1000
    Then I should receive a RestResponse with no error
    And The response should contains 4 elements from various types of version "3.0" and older versions are
      | 1.0 |
      | 2.0 |