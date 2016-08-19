Feature: CRUD operations on application version

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | lufy |
    And I add a role "APPLICATIONS_MANAGER" to user "lufy"
    And I am authenticated with user named "lufy"

  @reset
  Scenario: Create an application version with failure
    Given I create a new application with name "ALIEN" and description "Test application"
    And I create an application version with version "0.3..0-SNAPSHOT-SHOULD-FAILED"
    Then I should receive a RestResponse with an error code 605

  @reset
  Scenario: Create an application version with the same name version raise a conflict
    Given I create a new application with name "ALIEN" and description "Test application"
    And I create an application version with version "0.1.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 502

  @reset
  Scenario: Create an application version with success
    Given I create a new application with name "ALIEN" and description "Test application"
    And I create an application version with version "0.3.0-SNAPSHOT"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Delete an application version when it' the last should failled
    Given I create a new application with name "ALIEN" and description "Test application"
    And I delete an application version with name "0.1.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 610

  @reset
  Scenario: Delete an application version with failure
    Given I create a new application with name "ALIEN" and description "Test application"
    And I delete an application version with name "0.2.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 504

  @reset
  Scenario: Delete an application version with failure when application is deployed
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I create a resource of type "alien.nodes.mock.openstack.Flavor" named "Small" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "1" for the resource named "Small" related to the location "Mount doom orchestrator"/"Thark location"
    And I create a resource of type "alien.nodes.mock.openstack.Image" named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "img1" for the resource named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I autogenerate the on-demand resources for the location "Mount doom orchestrator"/"Thark location"
    And I pre register orchestrator properties
      | managementUrl | http://cloudifyurl:8099 |
      | numberBackup  | 1                       |
      | managerEmail  | admin@alien.fr          |
    And I create a new application with name "ALIEN" and description "" and node templates
      | Compute | tosca.nodes.Compute:1.0.0-SNAPSHOT |
    And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes
    When I deploy it
    And I delete an application version with name "0.1.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 507

  @reset
  Scenario: Delete an application version with success
    Given I create a new application with name "ALIEN" and description "Test application"
    And I create an application version with version "0.2.0-SNAPSHOT"
    Then I should receive a RestResponse with no error
    And I delete an application version with name "0.2.0-SNAPSHOT"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Search for application versions
    Given I create a new application with name "ALIEN" and description "Test application"
    And I create an application version with version "0.3.0-SNAPSHOT"
    Then I should receive a RestResponse with no error
    When I search for application versions
    Then I should receive 2 application versions in the search result

  @reset
  Scenario: Update an application version with success
    Given I create a new application with name "ALIEN" and description "Test application"
    And I create an application version with version "0.2.0-SNAPSHOT"
    Then I should receive a RestResponse with no error
    And I update an application version with version "0.2.0-SNAPSHOT" to "0.4.0-SNAPSHOT"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Update an application version with an existing name should be failed
    Given I create a new application with name "ALIEN" and description "Test application"
    And I create an application version with version "0.2.0-SNAPSHOT"
    Then I should receive a RestResponse with no error
    And I update an application version with version "0.1.0-SNAPSHOT" to "0.2.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 502

  @reset
  Scenario: Update a released application version should be failed
    Given I create a new application with name "ALIEN" and description "Test application"
    And I create an application version with version "0.2.0"
    Then I should receive a RestResponse with no error
    And I update an application version with version "0.2.0" to "0.2.1"
    Then I should receive a RestResponse with an error code 608
