Feature: CSAR upload

  Background:
    Given I am authenticated with "COMPONENTS_MANAGER" role

  @reset
  Scenario: Upload valid CSAR
    Given I upload the archive "tosca base types 1.0"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Upload invalid CSAR : uploaded file is not a zipped file
    Given I upload the archive "unzipped"
    Then I should receive a RestResponse with an error code 201 and 1 compilation errors in 1 file(s)

  @reset
  Scenario: Upload invalid CSAR : definition file not found
    Given I upload the archive "invalid (definition file not found)"
    Then I should receive a RestResponse with an error code 201 and 1 compilation errors in 1 file(s)

  @reset
  Scenario: Upload invalid CSAR : definition file is not valid yaml file
    Given I upload the archive "invalid (definition file is not valid yaml file)"
    Then I should receive a RestResponse with an error code 201 and 1 compilation errors in 1 file(s)

  @reset
  Scenario: Upload invalid CSAR : definition file's declaration duplicated
    Given I upload the archive "invalid (definition file's declaration duplicated)"
    Then I should receive a RestResponse with an error code 201 and 1 compilation errors in 1 file(s)

  @reset
  Scenario: Upload invalid CSAR : ALIEN-META.yaml fail validation
    Given I upload the archive "invalid (ALIEN-META.yaml fail validation)"
    Then I should receive a RestResponse with an error code 201 and 1 compilation errors in 1 file(s)

  @reset
  Scenario: Upload invalid CSAR : ALIEN-META.yaml not found
    Given I upload the archive "invalid (ALIEN-META.yaml not found)"
    Then I should receive a RestResponse with an error code 201 and 1 compilation errors in 1 file(s)

  @reset
  Scenario: Upload invalid : ALIEN-META.yaml invalid
    Given I upload the archive "invalid (ALIEN-META.yaml invalid)"
    Then I should receive a RestResponse with an error code 201 and 1 compilation errors in 1 file(s)

  @reset
  Scenario: Upload invalid CSAR : icon not found
    Given I upload the archive "invalid (icon not found)"
    Then I should receive a RestResponse with an error code 201 and 1 compilation errors in 1 file(s)

  @reset
  Scenario: Upload invalid : icon invalid
    Given I upload the archive "invalid (icon invalid)"
    Then I should receive a RestResponse with an error code 201 and 1 compilation errors in 1 file(s)

  @reset
  Scenario: Upload CSAR that already exist in the repository
    Given I upload the archive "released"
    Then I should receive a RestResponse with no error
    When I upload the archive "released"
    Then I should receive a RestResponse with no error
    And  I there should be a parsing error level "INFO" and code "CSAR_ALREADY_INDEXED"
    When I upload the archive "released-bis"
    Then I should receive a RestResponse with an error code 502

  @reset
  Scenario: Upload invalid CSAR : dependency in definition do not exist
    Given I upload the archive "sample java types 1.0"
    Then I should receive a RestResponse with an error code 200 and 17 compilation errors in 1 file(s)

  @reset
  Scenario: Upload Snapshot version CSAR that already exist in the repository and check creation / lastUpdate dates
    Given I upload the archive "snapshot"
    When I try to get a component with id "tosca.nodes.Compute:1.0-SNAPSHOT"
    Then I should have last update date equals to creation date
    When I upload the archive "snapshot-bis"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "tosca-base-types:1.0-SNAPSHOT"
    When I try to get a component with id "tosca.nodes.Compute:1.0-SNAPSHOT"
    Then I should have last update date greater than creation date
