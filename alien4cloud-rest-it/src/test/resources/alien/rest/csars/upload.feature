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
  Scenario: Upload released CSAR that depends on snapshot csar
    Given I upload the archive "snapshot dependency"
    Then I should receive a RestResponse with no error
    Given I upload the archive "released dependency"
    Then I should receive a RestResponse with no error
    When I upload the archive "released with snapshot dependency"
    Then I should receive a RestResponse with an error code 200
    And I should receive a RestResponse with 1 alerts in 1 files : 1 errors 0 warnings and 0 infos
    When I upload the archive "released with released dependency"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Upload invalid CSAR : dependency in definition does not exist
    Given I upload the archive "sample java types 1.0"
    Then I should receive a RestResponse with an error code 200 and 19 compilation errors in 1 file(s)

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

  @reset
  Scenario: Upload invalid CSAR : a node type is already defined in an other archive
    Given I upload the archive "tosca base types 1.0"
    Then I should receive a RestResponse with no error
    When I upload the archive "compute-1.0"
    Then I should receive a RestResponse with an error code 200

  @reset
  Scenario: Upload an archive with no conflicts
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca base types 1.0"
    When I upload the archive "dependencies c_v1"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "alien-tests-dependencies-c:1.0.0"
    When I upload the archive "dependencies d_v1"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "alien-tests-dependencies-d:1.0.0"
    When I upload the archive "dependencies b_v1"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "alien-tests-dependencies-b:1.0.0"
    When I upload the archive "dependencies a_v1"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "alien-tests-dependencies-a:1.0.0"
    When I upload the archive "dependencies scenario_1"
    Then I should receive a RestResponse with 1 alerts in 1 files : 0 errors 0 warnings and 1 infos
    And  I there should be a parsing error level "INFO" and code "TOPOLOGY_DETECTED"
    And I should have a CSAR with id "alien-tests-dependencies-scenario1:1.0.0-SNAPSHOT"
    And I have the CSAR "alien-tests-dependencies-scenario1" version "1.0.0" to contain a dependency to "alien-tests-dependencies-c" version "1.0.0"
    And I have the CSAR "alien-tests-dependencies-scenario1" version "1.0.0" to contain a dependency to "alien-tests-dependencies-d" version "1.0.0"
    And I have the CSAR "alien-tests-dependencies-scenario1" version "1.0.0" to contain a dependency to "alien-tests-dependencies-b" version "1.0.0"
    And I have the CSAR "alien-tests-dependencies-scenario1" version "1.0.0" to contain a dependency to "alien-tests-dependencies-a" version "1.0.0"

  @reset
  Scenario: Upload an archive with conflict between two transitive dependencies
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca base types 1.0"
    When I upload the archive "dependencies c_v2"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "alien-tests-dependencies-c:2.0.0"
    When I upload the archive "dependencies c_v1"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "alien-tests-dependencies-c:1.0.0"
    When I upload the archive "dependencies d_v2"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "alien-tests-dependencies-d:2.0.0"
    And I upload the archive "dependencies b_v2"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "alien-tests-dependencies-b:2.0.0"
    When I upload the archive "dependencies a_v1"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "alien-tests-dependencies-a:1.0.0"
    When I upload the archive "dependencies scenario_2"
    Then I should receive a RestResponse with 2 alerts in 1 files : 0 errors 1 warnings and 1 infos
    And  I there should be a parsing error level "WARNING" and code "DEPENDENCY_VERSION_CONFLICT"
    And  I there should be a parsing error level "INFO" and code "TOPOLOGY_DETECTED"
    And I should have a CSAR with id "alien-tests-dependencies-scenario2:1.0.0-SNAPSHOT"
    And I have the CSAR "alien-tests-dependencies-scenario2" version "1.0.0" to contain a dependency to "alien-tests-dependencies-c" version "2.0.0"
    And I have the CSAR "alien-tests-dependencies-scenario2" version "1.0.0" to contain a dependency to "alien-tests-dependencies-d" version "2.0.0"
    And I have the CSAR "alien-tests-dependencies-scenario2" version "1.0.0" to contain a dependency to "alien-tests-dependencies-b" version "2.0.0"
    And I have the CSAR "alien-tests-dependencies-scenario2" version "1.0.0" to contain a dependency to "alien-tests-dependencies-a" version "1.0.0"
    And The CSAR "alien-tests-dependencies-scenario2" version "1.0.0-SNAPSHOT" does not have a dependency to "alien-tests-dependencies-c" version "1.0.0"

  @reset
  Scenario: Upload an archive with conflict between transitive and direct dependency
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca base types 1.0"
    When I upload the archive "dependencies c_v2"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "alien-tests-dependencies-c:2.0.0"
    When I upload the archive "dependencies c_v1"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "alien-tests-dependencies-c:1.0.0"
    When I upload the archive "dependencies d_v2"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "alien-tests-dependencies-d:2.0.0"
    When I upload the archive "dependencies b_v2"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "alien-tests-dependencies-b:2.0.0"
    When I upload the archive "dependencies a_v1"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "alien-tests-dependencies-a:1.0.0"
    When I upload the archive "dependencies scenario_3"
    Then I should receive a RestResponse with 2 alerts in 1 files : 0 errors 1 warnings and 1 infos
    And  I there should be a parsing error level "WARNING" and code "TRANSITIVE_DEPENDENCY_VERSION_CONFLICT"
    And  I there should be a parsing error level "INFO" and code "TOPOLOGY_DETECTED"
    And I should have a CSAR with id "alien-tests-dependencies-scenario3:1.0.0-SNAPSHOT"
    And I have the CSAR "alien-tests-dependencies-scenario3" version "1.0.0" to contain a dependency to "alien-tests-dependencies-c" version "1.0.0"
    And I have the CSAR "alien-tests-dependencies-scenario3" version "1.0.0" to contain a dependency to "alien-tests-dependencies-d" version "2.0.0"
    And I have the CSAR "alien-tests-dependencies-scenario3" version "1.0.0" to contain a dependency to "alien-tests-dependencies-b" version "2.0.0"
    And I have the CSAR "alien-tests-dependencies-scenario3" version "1.0.0" to contain a dependency to "alien-tests-dependencies-a" version "1.0.0"
    And The CSAR "alien-tests-dependencies-scenario3" version "1.0.0-SNAPSHOT" does not have a dependency to "alien-tests-dependencies-c" version "2.0.0"

  @reset
  Scenario: Upload an archive with conflict between transitive and direct dependency resulting in a missing type (that could be resolved)
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca base types 1.0"
    When I upload the archive "dependencies c_v3"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "alien-tests-dependencies-c:3.0.0"
    When I upload the archive "dependencies c_v1"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "alien-tests-dependencies-c:1.0.0"
    When I upload the archive "dependencies d_v3"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "alien-tests-dependencies-d:3.0.0"
    When I upload the archive "dependencies b_v3"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "alien-tests-dependencies-b:3.0.0"
    When I upload the archive "dependencies a_v1"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "alien-tests-dependencies-a:1.0.0"
    When I upload the archive "dependencies scenario_4"
    Then I should receive a RestResponse with 2 alerts in 1 files : 1 errors 1 warnings and 0 infos
    And  I there should be a parsing error level "WARNING" and code "DEPENDENCY_VERSION_CONFLICT"
    And  I there should be a parsing error level "ERROR" and code "REQUIREMENT_CAPABILITY_NOT_FOUND"

  @reset
  Scenario: Upload an archive with conflict between transitive and direct dependency resulting in a missing type (that could not be resolved)
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca base types 1.0"
    When I upload the archive "dependencies c_v4"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "alien-tests-dependencies-c:4.0.0"
    When I upload the archive "dependencies c_v1"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "alien-tests-dependencies-c:1.0.0"
    When I upload the archive "dependencies d_v4"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "alien-tests-dependencies-d:4.0.0"
    When I upload the archive "dependencies b_v4"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "alien-tests-dependencies-b:4.0.0"
    When I upload the archive "dependencies a_v1"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "alien-tests-dependencies-a:1.0.0"
    When I upload the archive "dependencies scenario_5"
    Then I should receive a RestResponse with 2 alerts in 1 files : 1 errors 1 warnings and 0 infos
    And  I there should be a parsing error level "WARNING" and code "DEPENDENCY_VERSION_CONFLICT"
    And  I there should be a parsing error level "ERROR" and code "REQUIREMENT_CAPABILITY_NOT_FOUND"

  @reset
  Scenario: Upload an archive with conflict between two direct dependencies and two transitive dependency
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca base types 1.0"
    When I upload the archive "dependencies c_v2"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "alien-tests-dependencies-c:2.0.0"
    When I upload the archive "dependencies c_v1"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "alien-tests-dependencies-c:1.0.0"
    When I upload the archive "dependencies d_v2"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "alien-tests-dependencies-d:2.0.0"
    When I upload the archive "dependencies d_v1"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "alien-tests-dependencies-d:1.0.0"
    When I upload the archive "dependencies b_v2"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "alien-tests-dependencies-b:2.0.0"
    When I upload the archive "dependencies b_v1"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "alien-tests-dependencies-b:1.0.0"
    When I upload the archive "dependencies a_v1"
    Then I should receive a RestResponse with no error
    And I should have a CSAR with id "alien-tests-dependencies-a:1.0.0"
    When I upload the archive "dependencies scenario_6"
    Then I should receive a RestResponse with 3 alerts in 1 files : 0 errors 2 warnings and 1 infos
    And I there should be a parsing error level "WARNING" and code "DEPENDENCY_VERSION_CONFLICT"
    And I there should be a parsing error level "INFO" and code "TOPOLOGY_DETECTED"
    And I should have a CSAR with id "alien-tests-dependencies-scenario6:1.0.0-SNAPSHOT"
    And I have the CSAR "alien-tests-dependencies-scenario6" version "1.0.0" to contain a dependency to "alien-tests-dependencies-c" version "2.0.0"
    And I have the CSAR "alien-tests-dependencies-scenario6" version "1.0.0" to contain a dependency to "alien-tests-dependencies-d" version "2.0.0"
    And I have the CSAR "alien-tests-dependencies-scenario6" version "1.0.0" to contain a dependency to "alien-tests-dependencies-b" version "2.0.0"
    And I have the CSAR "alien-tests-dependencies-scenario6" version "1.0.0" to contain a dependency to "alien-tests-dependencies-a" version "1.0.0"
    And The CSAR "alien-tests-dependencies-scenario6" version "1.0.0-SNAPSHOT" does not have a dependency to "alien-tests-dependencies-c" version "1.0.0"
    And The CSAR "alien-tests-dependencies-scenario6" version "1.0.0-SNAPSHOT" does not have a dependency to "alien-tests-dependencies-d" version "1.0.0"
    And The CSAR "alien-tests-dependencies-scenario6" version "1.0.0-SNAPSHOT" does not have a dependency to "alien-tests-dependencies-b" version "1.0.0"
