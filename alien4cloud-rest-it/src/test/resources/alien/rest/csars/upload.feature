Feature: CSAR upload

Background:
  Given I am authenticated with "COMPONENTS_MANAGER" role

Scenario: Upload valid CSAR
  Given I have a CSAR folder that is "valid"
  When I upload it
  Then I should receive a RestResponse with no error

Scenario: Upload invalid CSAR (uploaded file is not a zipped file)
  Given I have a CSAR file that is "unzipped"
  When I upload it
  Then I should receive a RestResponse with an error code 201 and 1 compilation errors in 1 file(s)

Scenario: Upload invalid CSAR (invalid (definition file not found))
  Given I have a CSAR folder that is "invalid (definition file not found)"
  When I upload it
  Then I should receive a RestResponse with an error code 201 and 1 compilation errors in 1 file(s)

Scenario: Upload invalid CSAR (invalid (definition file is not valid yaml file))
  Given I have a CSAR folder that is "invalid (definition file is not valid yaml file)"
  When I upload it
  Then I should receive a RestResponse with an error code 201 and 1 compilation errors in 1 file(s)

Scenario: Upload invalid CSAR (invalid (definition file's declaration duplicated))
  Given I have a CSAR folder that is "invalid (definition file's declaration duplicated)"
  When I upload it
  Then I should receive a RestResponse with an error code 201 and 1 compilation errors in 1 file(s)

Scenario: Upload invalid CSAR (invalid (ALIEN-META.yaml fail validation))
  Given I have a CSAR folder that is "invalid (ALIEN-META.yaml fail validation)"
  When I upload it
  Then I should receive a RestResponse with an error code 201 and 1 compilation errors in 1 file(s)

Scenario: Upload invalid CSAR (ALIEN-META.yaml not found)
  Given I have a CSAR folder that is "invalid (ALIEN-META.yaml not found)"
  When I upload it
  Then I should receive a RestResponse with an error code 201 and 1 compilation errors in 1 file(s)

Scenario: Upload invalid (ALIEN-META.yaml invalid)
  Given I have a CSAR folder that is "invalid (ALIEN-META.yaml invalid)"
  When I upload it
  Then I should receive a RestResponse with an error code 201 and 1 compilation errors in 1 file(s)

Scenario: Upload invalid CSAR (icon not found)
  Given I have a CSAR folder that is "invalid (icon not found)"
  When I upload it
  Then I should receive a RestResponse with an error code 201 and 1 compilation errors in 1 file(s)

Scenario: Upload invalid (icon invalid)
  Given I have a CSAR folder that is "invalid (icon invalid)"
  When I upload it
  Then I should receive a RestResponse with an error code 201 and 1 compilation errors in 1 file(s)

Scenario: Upload CSAR that already exist in the repository
  Given I have a CSAR folder that is "valid"
  And The CSAR is already uploaded in the system
  When I upload it
  Then I should receive a RestResponse with an error code 401

Scenario: Upload invalid CSAR (dependency in definition do not exist)
  Given I have a CSAR folder that is "invalid (dependency in definition do not exist)"
  When I upload it
  Then I should receive a RestResponse with an error code 201 and 3 compilation errors in 1 file(s)

Scenario: Upload Snapshot version CSAR that already exist in the repository
  Given I have a CSAR folder that is "snapshot"
  And The CSAR is already uploaded in the system
  When I upload it
  Then I should receive a RestResponse with no error
  
Scenario: Upload Snapshot version CSAR that already exist in the repository and check creation / lastUpdate dates
  Given I have a CSAR folder that is "snapshot"
  And The CSAR is already uploaded in the system
  When I try to get a component with id "test.java.app:1.0-SNAPSHOT"
  Then I should have last update date equals to creation date
  When I upload it
  Then I should receive a RestResponse with no error
  When I try to get a component with id "test.java.app:1.0-SNAPSHOT" 
  Then I should have last update date greater than creation date
