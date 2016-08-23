Feature: upload CSAR with artifact

  Background:
    Given I am authenticated with "COMPONENTS_MANAGER" role

  @reset
  Scenario: Upload valid CSAR with artifact type
    Given I upload the archive "tosca base types 1.0"
    Then I should receive a RestResponse with no error
    When I upload the archive "sample java types 1.0"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Upload valid CSAR with artifact reference to an existing file
    Given I upload the archive "tosca base types 1.0"
    Then I should receive a RestResponse with no error
    When I upload the archive "artifact java types 1.0"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Upload an invalid CSAR with artifact reference to an unexisting file
    Given I upload the archive "tosca base types 1.0"
    Then I should receive a RestResponse with no error
    When I upload the archive "artifact java types 1.0 wrong path"
    Then I should receive a RestResponse with an error code 200

  @reset
  Scenario: Upload an invalid CSAR with artifact reference to an unexisting type
    Given I upload the archive "tosca base types 1.0"
    Then I should receive a RestResponse with no error
    When I upload the archive "artifact java types 1.0 wrong type"
    Then I should receive a RestResponse with an error code 200
