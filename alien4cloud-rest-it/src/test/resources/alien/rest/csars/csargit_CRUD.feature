Feature: Csargit crud feature

  Background: 
    Given I am authenticated with "ADMIN" role

  Scenario: Get an unexisting CsarGitRepository
    Given I get an unexisting CsarGitRepository with id "01"
    Then I should receive a RestResponse with an error code 504

  Scenario: Get a CsarGitRepository with empty id
    Given I get an unexisting CsarGitRepository with id ""
    Then I should receive a RestResponse with an error code 500

  Scenario: Get an unexisting CsarGitRepository by url
    Given I get an unexisting CsarGitRepository with url "https://test"
    Then I should receive a RestResponse with an error code 504

  Scenario: Get an unexisting CsarGitRepository by url
    Given I get an unexisting CsarGitRepository with url " "
    Then I should receive a RestResponse with an error code 504

  Scenario: Removing an unexisting CsarGitRepository by url
    Given I delete a csargit with empty url "eee"
    Then I should receive a RestResponse with an error code 504

  Scenario: Create a new csargit
    Given I have a csargit with the url "https://github.com/alien4cloud/samples" with username "admin" and stored "false" and password "admin"
    And I add locations to the csar
      | branchId | subPath |
      | master   |         |
    When I create a csargit
    Then I should receive a RestResponse with no error
    And I have a csargit created with url "https://github.com/alien4cloud/samples"

  Scenario: Create a new csargit which already exists
    Given I have a csargit with the url "https://github.com/alien4cloud/samples" with username "admin" and stored "false" and password "admin"
    And I add locations to the csar
      | branchId | subPath |
      | master   |         |
    When I create a csargit
    Then I should receive a RestResponse with an error code 501
    And I have no csargit created with url "https://github.com/alien4cloud/samples"

  Scenario: Create a new csargit with empty data
    Given I have a csargit with the url "" with username "" and stored "false" and password ""
    And I add locations to the csar
      | branchId | subPath |
      |          |         |
    When I create a csargit
    Then I should receive a RestResponse with an error code 501

  Scenario: Create a new csargit with empty locations
    Given I have a csargit with empty data and url "https://github.com/alien4cloud/empty" with username "" and stored "" and password ""
    And I add empty locations to the csar
      | branchId | subPath |
      |          |         |
    When I create a csargit
    Then I should receive a RestResponse with an error code 501

  Scenario: Import a csargit with a wrong url
    Given I have a csargit with the url "https://github.com/a" with username "admin" and stored "false" and password "admin"
    And I add locations to the csar
      | branchId | subPath |
      | master   |         |
    When I create a csargit
    And I trigger the import of a csar with url "https://github.com/a"
    Then I should receive a RestResponse with an error code 615

  Scenario: Import a private csargit with wrong credentials
    Given I have a csargit with the url "https://fastconnect.org/gitlab/alien-tosca-recipes/recipes" with username "toto" and stored "false" and password "toto"
    And I add locations to the csar
      | branchId | subPath |
      | master   | mongoDB |
    When I create a csargit
    And I trigger the import of a csar with url "https://fastconnect.org/gitlab/alien-tosca-recipes/recipes"
    Then I should receive a RestResponse with an error code 616

  Scenario: Delete a csargit
    Given I have a csargit with the url "https://github.com/alien4cloud/tosca-normative-types" with username "admin" and stored "false" and password "admin"
    And I add locations to the csar
      | branchId | subPath |
      | master   |         |
    When I create a csargit
    And I delete a csargit with id
    Then I should receive a RestResponse with no error

  Scenario: Delete a csargit with url
    Given I have a csargit with the url "https://github.com/alien4cloud/tosca-normative-types-2" with username "admin" and stored "false" and password "admin"
    And I add locations to the csar
      | branchId | subPath |
      | master   |         |
    When I create a csargit
    And I delete a csargit with url "https://github.com/alien4cloud/tosca-normative-types-2"
    Then I should receive a RestResponse with no error

  Scenario: Delete a csargit with a wrong url
    Given I have a csargit with the url "https://github.com//tosca-normative-types-3" with username "admin" and stored "false" and password "admin"
    And I add locations to the csar
      | branchId | subPath |
      | master   |         |
    When I create a csargit
    And I delete a csargit with wrong url "https://git.com/tosca-normative-type-3"
    Then I should receive a RestResponse with an error code 504

  Scenario: Update an existing CSAR
    Given I have a csargit with the url "https://github.com/alien4cloud/test" with username "admin" and stored "false" and password "admin"
    And I add locations to the csar
      | branchId | subPath |
      | master   |         |
    When I create a csargit
    Then I should receive a RestResponse with no error
    And I have a csargit created with url "https://github.com/alien4cloud/test"
    When I update a csargit with url "https://github.com/alien4cloud/test_updated" and username "toto" and password "password" and target url "https://github.com/alien4cloud/test"
    Then I should receive a RestResponse with no error
    And I have a csargit created with url "https://github.com/alien4cloud/test_updated"

  Scenario: Create and save a CSAR on disk
    Given I have a csargit with the url "https://github.com/alien4cloud/tosca-normative-types" with username "" and password ""
    And I add locations to the csar
      | branchId | subPath |
      | master   |         |
    When I create a csargit
    And I trigger the import of a csar with url "https://github.com/alien4cloud/tosca-normative-types"
    Then I should receive a RestResponse with no error
    And I trigger the import of a csar with url "https://github.com/alien4cloud/tosca-normative-types"
    Then I should receive a RestResponse with an error code 617
    And I trigger the import of a csar with url "https://github.com/alien4cloud/samples"
    Then I should receive a RestResponse with no error

  Scenario: Create a new csargit to use its id
    Given I have a csargit with the url "https://github.com/alien4cloud/new" with username "admin" and stored "false" and password "admin"
    And I add locations to the csar
      | branchId | subPath |
      | master   |         |
    When I create a csargit
    Then I should receive a RestResponse with no error
    And I have a csargit created with url "https://github.com/alien4cloud/new"
    Then I update a csar with url "https://github.com/alien4cloud/new" and username "new" and password "new"
    Then I should receive a RestResponse with no error

  Scenario: Get an existing CsarGitRepository by url
    Given I get an unexisting CsarGitRepository with url "https://github.com/alien4cloud/new"
    Then I should receive a RestResponse with no error

  Scenario: Get an unexisting CsarGitRepository with empty url
    Given I get an unexisting CsarGitRepository with url " "
    Then I should receive a RestResponse with an error code 504

  Scenario: Get an existing CsarGitRepository with id and url
    Given I get an existing CsarGitRepository with an url "https://github.com/alien4cloud/new"
    Then I should receive a RestResponse with no error
