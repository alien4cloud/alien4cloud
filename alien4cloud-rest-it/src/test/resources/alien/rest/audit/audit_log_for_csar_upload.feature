Feature: Audit log for CSAR upload

  Background:
    Given I am authenticated with user named "admin"
    And I reset audit log configuration

  @reset
  Scenario: Audit log for a valid CSAR
    Given I upload the archive "tosca base types 1.0"
    Then I should receive a RestResponse with no error
    And I should have a latest audit trace with request parameters defined below:
     |  file  |  file  |
     |  csar  |  tosca-base-types:1.0 |

  @reset
  Scenario: Audit log for an invalid CSAR
    Given I upload the archive "unzipped"
    Then I should receive a RestResponse with an error code 201 and 1 compilation errors in 1 file(s)
    And I should have a latest audit trace with request parameters defined below:
     |  file  |  file  |
