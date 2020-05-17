Feature: Delete application version

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | sauron |
    And I add a role "APPLICATIONS_MANAGER" to user "sauron"
    And I am authenticated with user named "sauron"
    And I create an application with name "watchmiddleearth", archive name "watchmiddleearth", description "Use my great eye to find frodo and the ring." and topology template id "null"
    And I should receive a RestResponse with no error

  @reset
  Scenario: Deleting an application version should succeed
    And I create an application version for application "watchmiddleearth" with version "0.2.0-SNAPSHOT", description "null", topology template id "null" and previous version id "null"
    And I should receive a RestResponse with no error
    When I delete an application version for application "watchmiddleearth" with version id "watchmiddleearth:0.2.0-SNAPSHOT"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Deleting the last application version should fail
    When I delete an application version for application "watchmiddleearth" with version id "watchmiddleearth:0.1.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 610

  @reset
  Scenario: Deleting an application version that does not exists should fail
    When I delete an application version for application "watchmiddleearth" with version id "watchmiddleearth:0.2.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 504

  @reset
  Scenario: Deleting an application version when one of it's version is used should fail
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I create a resource of type "org.alien4cloud.nodes.mock.openstack.Flavor" named "Small" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "1" for the resource named "Small" related to the location "Mount doom orchestrator"/"Thark location"
    And I create a resource of type "org.alien4cloud.nodes.mock.openstack.Image" named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "img1" for the resource named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I autogenerate the on-demand resources for the location "Mount doom orchestrator"/"Thark location"
    And I pre register orchestrator properties
      | managementUrl | http://cloudifyurl:8099 |
      | numberBackup  | 1                       |
      | managerEmail  | admin@alien.fr          |
    And I create a new application with name "ONE-PIECE" and description "" and node templates
      | Compute | tosca.nodes.Compute:1.0.0-SNAPSHOT |
    And I should receive a RestResponse with no error
    And I create an application version for application "ONEPIECE" with version "0.2.0-SNAPSHOT", description "null", topology template id "null" and previous version id "null"
    And I should receive a RestResponse with no error
    And I get the deployment topology for the current application
    And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes
    And I should receive a RestResponse with no error
#    And I deploy it
    And I should receive a RestResponse with no error
    When I delete an application version for application "ONEPIECE" with version id "ONEPIECE:0.1.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 507

  @reset
  Scenario: Deleting an application topology version should succeed
    And I create an application topology version for application "watchmiddleearth" version "watchmiddleearth:0.1.0-SNAPSHOT" with qualifier "DEV", description "topology for development environment", topology template id "null" and previous version id "null"
    And I should receive a RestResponse with no error
    And I get the application version for application "watchmiddleearth" with id "watchmiddleearth:0.1.0-SNAPSHOT"
    And I should receive a RestResponse with no error
    And The application version should have an application topology version with version "0.1.0-DEV-SNAPSHOT"
    When I delete the application topology version for application "watchmiddleearth", version id "watchmiddleearth:0.1.0-SNAPSHOT" with topology version id "0.1.0-DEV-SNAPSHOT"
    Then I should receive a RestResponse with no error
    And I get the application version for application "watchmiddleearth" with id "watchmiddleearth:0.1.0-SNAPSHOT"
    And I should receive a RestResponse with no error
    And The application version should not have an application topology version with version "0.1.0-DEV-SNAPSHOT"

  @reset
  Scenario: Deleting the last application topology version should fail
    When I delete the application topology version for application "watchmiddleearth", version id "watchmiddleearth:0.1.0-SNAPSHOT" with topology version id "0.1.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 610

  @reset
  Scenario: Deleting an application topology version that does not exists should fail
    When I delete the application topology version for application "watchmiddleearth", version id "watchmiddleearth:0.1.0-SNAPSHOT" with topology version id "0.1.0-DEV-SNAPSHOT"
    Then I should receive a RestResponse with an error code 504

  @reset
  Scenario: Deleting an application topology version that is used should fail
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I create a resource of type "org.alien4cloud.nodes.mock.openstack.Flavor" named "Small" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "1" for the resource named "Small" related to the location "Mount doom orchestrator"/"Thark location"
    And I create a resource of type "org.alien4cloud.nodes.mock.openstack.Image" named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "img1" for the resource named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I autogenerate the on-demand resources for the location "Mount doom orchestrator"/"Thark location"
    And I pre register orchestrator properties
      | managementUrl | http://cloudifyurl:8099 |
      | numberBackup  | 1                       |
      | managerEmail  | admin@alien.fr          |
    And I create a new application with name "ONE-PIECE" and description "" and node templates
      | Compute | tosca.nodes.Compute:1.0.0-SNAPSHOT |
    And I should receive a RestResponse with no error
    And I create an application topology version for application "ONEPIECE" version "ONEPIECE:0.1.0-SNAPSHOT" with qualifier "DEV", description "topology for development environment", topology template id "null" and previous version id "null"
    And I should receive a RestResponse with no error
    And I get the deployment topology for the current application
    And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes
    And I should receive a RestResponse with no error
#    And I deploy it
    And I should receive a RestResponse with no error
    When I delete the application topology version for application "ONEPIECE", version id "ONEPIECE:0.1.0-SNAPSHOT" with topology version id "0.1.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 507
