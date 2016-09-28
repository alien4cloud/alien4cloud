Feature: GIT repository usage

  Background:
    Given I am authenticated with "COMPONENTS_MANAGER" role

  @reset
  Scenario: Get an unexisting GIT repository
      Given I get the GIT repository with id "01"
      Then I should receive a RestResponse with an error code 504

  @reset
  Scenario: Add GIT repository
   Given I add a GIT repository with url "https://github.com/alien4cloud/tosca-normative-types.git" usr "" pwd "" stored "false" and locations
        | branchId | subPath |
        | master   |         |
     And I should receive a RestResponse with no error
    When I list all the git repositories
    Then I should have 1 git repository in the list
     And I can find a GIT repository with url "https://github.com/alien4cloud/tosca-normative-types.git" usr "" pwd "" stored "false" and locations
        | branchId | subPath |
        | master   |         |

  @reset
  Scenario: Create a new csargit with empty data
     Given I add a GIT repository with url "" usr "" pwd "" stored "false" and locations
        | branchId | subPath |
        |          |         |
      Then I should receive a RestResponse with an error code 501

  @reset
  Scenario: Create a new csargit with empty locations
     Given I add a GIT repository with url "https://github.com/alien4cloud/empty" usr "" pwd "" stored "false" and locations
        | branchId | subPath |
      Then I should receive a RestResponse with an error code 501

  @reset
  Scenario: Add GIT repository twice
   Given I add a GIT repository with url "https://github.com/alien4cloud/tosca-normative-types.git" usr "" pwd "" stored "false" and locations
        | branchId | subPath |
        | master   |         |
     And I should receive a RestResponse with no error
    When I add a GIT repository with url "https://github.com/alien4cloud/tosca-normative-types.git" usr "" pwd "" stored "false" and locations
        | branchId | subPath |
        | master   |         |
    Then I should receive a RestResponse with an error code 502

  @reset
  Scenario: Update GIT repository
   Given I add a GIT repository with url "https://github.com/alien4cloud/tosca-normative-types.git" usr "" pwd "" stored "false" and locations
        | branchId | subPath |
        | master   |         |
     And I should receive a RestResponse with no error
   Given I get the GIT repo with url "https://github.com/alien4cloud/tosca-normative-types.git"
    When I update the GIT repository with url "https://pinrlinpinpin" usr "tutu" pwd "toto" stored "true" and locations
        | branchId | subPath |
        | dev      | stuff   |
    Then I should receive a RestResponse with no error
    When I list all the git repositories
    Then I should have 1 git repository in the list
     And I can find a GIT repository with url "https://pinrlinpinpin" usr "tutu" pwd "toto" stored "true" and locations
        | branchId | subPath |
        | dev      | stuff   |

  @reset
  Scenario: Update GIT repository using existing URL
   Given I add a GIT repository with url "https://github.com/alien4cloud/tosca-normative-types.git" usr "" pwd "" stored "false" and locations
        | branchId | subPath |
        | master   |         |
     And I should receive a RestResponse with no error
   Given I add a GIT repository with url "https://pinrlinpinpin" usr "" pwd "" stored "false" and locations
        | branchId | subPath |
        | master   |         |
     And I should receive a RestResponse with no error
    When I list all the git repositories
    Then I should have 2 git repository in the list
   Given I get the GIT repo with url "https://pinrlinpinpin"
    When I update the GIT repository with url "https://github.com/alien4cloud/tosca-normative-types.git" usr "tutu" pwd "toto" stored "true" and locations
        | branchId | subPath |
        | dev      | stuff   |
    Then I should receive a RestResponse with an error code 502

  @reset
  Scenario: Remove a GIT repository
   Given I add a GIT repository with url "https://github.com/alien4cloud/tosca-normative-types.git" usr "" pwd "" stored "false" and locations
        | branchId | subPath |
        | master   |         |
     And I should receive a RestResponse with no error
   Given I add a GIT repository with url "https://pinrlinpinpin" usr "" pwd "" stored "false" and locations
        | branchId | subPath |
        | master   |         |
     And I should receive a RestResponse with no error
    When I list all the git repositories
    Then I should have 2 git repository in the list
   Given I get the GIT repo with url "https://pinrlinpinpin"
    When I delete the GIT repository
    Then I should receive a RestResponse with no error
    When I list all the git repositories
    Then I should have 1 git repository in the list
     And I can find a GIT repository with url "https://github.com/alien4cloud/tosca-normative-types.git" usr "" pwd "" stored "false" and locations
        | branchId | subPath |
        | master   |         |

  @reset
  Scenario: Remove an unexisting GIT repository
      Given I delete the GIT repository with id "eee"
      Then I should receive a RestResponse with an error code 504

  @reset
  Scenario: Add GIT repository and import it
   Given I add a GIT repository with url "https://github.com/alien4cloud/tosca-normative-types.git" usr "" pwd "" stored "false" and locations
        | branchId | subPath |
        | 1.0.0    |         |
     And I get the GIT repo with url "https://github.com/alien4cloud/tosca-normative-types.git"
     And I import the GIT repository
     And I can find 1 CSAR
     And I should have a CSAR with id "tosca-normative-types:1.0.0.wd03-SNAPSHOT"
     And I add a GIT repository with url "https://github.com/alien4cloud/samples.git" usr "" pwd "" stored "false" and locations
        | branchId | subPath |
        | 1.0.0    | apache  |
   Given I get the GIT repo with url "https://github.com/alien4cloud/samples.git"
    When I try to get a component with id "alien.nodes.Apache:2.0.0-SNAPSHOT"
    Then I should receive a RestResponse with no error
     And I should not have any component
    When I import the GIT repository
    Then I should receive a RestResponse with no error
     And I can find 2 CSAR
     And I should have a CSAR with id "apache-type:2.0.0-SNAPSHOT"
    When I try to get a component with id "alien.nodes.Apache:2.0.0-SNAPSHOT"
    Then I should receive a RestResponse with no error
     And I should have a component with id "alien.nodes.Apache:2.0.0-SNAPSHOT"

  @reset
  Scenario: Import a csargit with a wrong url
   Given I add a GIT repository with url "https://github.com/a" usr "" pwd "" stored "false" and locations
        | branchId | subPath |
        | 1.0.0    |         |
     And I get the GIT repo with url "https://github.com/a"
    When I import the GIT repository
    Then I should receive a RestResponse with an error code 615

  @reset
  Scenario: Import a private csargit with wrong credentials
   Given I add a GIT repository with url "https://fastconnect.org/gitlab/alien-tosca-recipes/recipes" usr "toto" pwd "toto" stored "false" and locations
        | branchId | subPath |
        | master   | mongoDB |
     And I get the GIT repo with url "https://fastconnect.org/gitlab/alien-tosca-recipes/recipes"
    When I import the GIT repository
    Then I should receive a RestResponse with an error code 615

  @reset
  Scenario: Import a CSAR from Gitlab
    Given I add a GIT repository with url "https://fastconnect.org/gitlab/benoitph/alien-samples.git" usr "" pwd "" stored "false" and locations
      | branchId | subPath |
      | master   |         |
    And I get the GIT repo with url "https://fastconnect.org/gitlab/benoitph/alien-samples.git"
    When I import the GIT repository
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Add GIT repository and import it, then add an archive and reimport it
   Given I add a GIT repository with url "https://github.com/alien4cloud/tosca-normative-types.git" usr "" pwd "" stored "false" and locations
        | branchId | subPath |
        | 1.0.0    |         |
     And I get the GIT repo with url "https://github.com/alien4cloud/tosca-normative-types.git"
     And I import the GIT repository
     And I add a GIT repository with url "https://github.com/alien4cloud/samples.git" usr "" pwd "" stored "false" and locations
        | branchId | subPath |
        | 1.0.0    | apache  |
   Given I get the GIT repo with url "https://github.com/alien4cloud/samples.git"
    When I import the GIT repository
    Then I should receive a RestResponse with no error
    When I try to get a component with id "alien.nodes.PHP:2.0.0-SNAPSHOT"
    Then I should receive a RestResponse with no error
     And I should not have any component
    When I update the GIT repository with url "https://github.com/alien4cloud/samples.git" usr "" pwd "" stored "false" and locations
        | branchId | subPath |
        | 1.0.0    | apache  |
        | 1.0.0    | php     |
     And I import the GIT repository
     And I can find 3 CSAR
     And I should have a CSAR with id "php-type:2.0.0-SNAPSHOT"
    When I try to get a component with id "alien.nodes.PHP:2.0.0-SNAPSHOT"
    Then I should receive a RestResponse with no error
     And I should have a component with id "alien.nodes.PHP:2.0.0-SNAPSHOT"

  @reset
  Scenario: Add GIT repository with all archives and import it
   Given I add a GIT repository with url "https://github.com/alien4cloud/tosca-normative-types.git" usr "" pwd "" stored "false" and locations
        | branchId | subPath |
        | 1.0.0    |         |
     And I get the GIT repo with url "https://github.com/alien4cloud/tosca-normative-types.git"
     And I import the GIT repository
     And I add a GIT repository with url "https://github.com/alien4cloud/alien4cloud-extended-types.git" usr "" pwd "" stored "false" and locations
        | branchId | subPath |
        | 1.0.0    |         |
     And I get the GIT repo with url "https://github.com/alien4cloud/alien4cloud-extended-types.git"
     And I import the GIT repository
     And I can find 3 CSAR

  # Fixme: fail if we invert both location (1.0.0 then master) !
  @reset
  Scenario: Add GIT repository with two branch and import it
   Given I add a GIT repository with url "https://github.com/alien4cloud/tosca-normative-types.git" usr "" pwd "" stored "false" and locations
        | branchId | subPath |
        | master   |         |
        | 1.0.0    |         |
     And I get the GIT repo with url "https://github.com/alien4cloud/tosca-normative-types.git"
     And I import the GIT repository
     And I can find 2 CSAR
