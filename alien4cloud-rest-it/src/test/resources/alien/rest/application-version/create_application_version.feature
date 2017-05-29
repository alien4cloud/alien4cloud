Feature: Create application version

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | sauron |
    And I add a role "APPLICATIONS_MANAGER" to user "sauron"
    And I am authenticated with user named "sauron"
    And I create an application with name "watchmiddleearth", archive name "watchmiddleearth", description "Use my great eye to find frodo and the ring." and topology template id "null"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Creating a new application version should succeed
    When I create an application version for application "watchmiddleearth" with version "0.2.0-SNAPSHOT", description "null", topology template id "null" and previous version id "null"
    Then I should receive a RestResponse with no error
    And I get the application version for application "watchmiddleearth" with id "watchmiddleearth:0.2.0-SNAPSHOT"
    And I should receive a RestResponse with no error
    # A default application topology version with no qualifier should have been created
    And The application version should have an application topology version with version "0.2.0-SNAPSHOT"

  @reset
  Scenario: Creating a new application version with description should succeed
    When I create an application version for application "watchmiddleearth" with version "0.2.0-SNAPSHOT", description "A great new version.", topology template id "null" and previous version id "null"
    Then I should receive a RestResponse with no error
    And I get the application version for application "watchmiddleearth" with id "watchmiddleearth:0.2.0-SNAPSHOT"
    And I should receive a RestResponse with no error
    # A default application topology version with no qualifier should have been created
    And The application version should have an application topology version with version "0.2.0-SNAPSHOT"

  @reset
  Scenario: Creating a new application version with no version number should fail
    When I create an application version for application "watchmiddleearth" with version "null", description "null", topology template id "null" and previous version id "null"
    Then I should receive a RestResponse with an error code 605

  @reset
  Scenario: Creating a new application version with an empty version number should fail
    When I create an application version for application "watchmiddleearth" with version "", description "null", topology template id "null" and previous version id "null"
    Then I should receive a RestResponse with an error code 605

  @reset
  Scenario: Creating a new application version with an invalid version format should fail
    When I create an application version for application "watchmiddleearth" with version "0.2..0-SNAPSHOT", description "null", topology template id "null" and previous version id "null"
    Then I should receive a RestResponse with an error code 605

  @reset
  Scenario: Creating a new application version with an already existing version number should fail
    When I create an application version for application "watchmiddleearth" with version "0.1.0-SNAPSHOT", description "null", topology template id "null" and previous version id "null"
    Then I should receive a RestResponse with an error code 502

  @reset
  Scenario: Creating a new application version when not application manager for the application should fail
    Given I am authenticated with "ADMIN" role
    And There is a "mairon" user in the system
    # give the global APPLICATIONS_MANAGER role but mairon is not manager on the application
    And I add a role "APPLICATIONS_MANAGER" to user "mairon"
    And I am authenticated with user named "mairon"
    When I create an application version for application "watchmiddleearth" with version "0.2.0-SNAPSHOT", description "null", topology template id "null" and previous version id "null"
    Then I should receive a RestResponse with an error code 102

  @reset
  Scenario: Creating a new application version from a topology template should succeed
    Given I am authenticated with "ADMIN" role
    # Prepare archives and user to create a topology template
    And I upload the archive "tosca base types 1.0"
    And I should receive a RestResponse with no error
    And I upload the archive "sample java types 1.0"
    And I should receive a RestResponse with no error
    And There is a "node type" with element name "tosca.nodes.Compute" and archive version "1.0"
    And There is a "node type" with element name "fastconnect.nodes.Java" and archive version "1.0"
    And There is a "mairon" user in the system
    And I add a role "ARCHITECT" to user "mairon"
    And I add a role "COMPONENTS_BROWSER" to user "sauron"
    # Create a topology template
    And I am authenticated with user named "mairon"
    And I create a new topology template with name "topology_template_java" and description "My topology template description1" and node templates
      | NodeTemplateCompute | tosca.nodes.Compute:1.0    |
      | NodeTemplateJava    | fastconnect.nodes.Java:1.0 |
    Then I should receive a RestResponse with no error
    # Create an application
    And I am authenticated with user named "sauron"
    When I create an application version for application "watchmiddleearth" with version "0.2.0-SNAPSHOT", description "A great new version.", topology template id "topology_template_java:0.1.0-SNAPSHOT" and previous version id "null"
    Then I should receive a RestResponse with no error
    And Topologies "watchmiddleearth:0.2.0-SNAPSHOT" and "topology_template_java:0.1.0-SNAPSHOT" have the same number of node templates with identical types.

  @reset
  Scenario: Creating a new application version from a topology that belongs to an application version should fail
    When I create an application version for application "watchmiddleearth" with version "0.2.0-SNAPSHOT", description "A great new version.", topology template id "watchmiddleearth:0.1.0-SNAPSHOT" and previous version id "null"
    Then I should receive a RestResponse with an error code 102

  @reset
  Scenario: Creating a new application version from a template that does not exists should fail
    When I create an application version for application "watchmiddleearth" with version "0.2.0-SNAPSHOT", description "A great new version.", topology template id "topology_template_java:0.1.0-SNAPSHOT" and previous version id "null"
    Then I should receive a RestResponse with an error code 504

  # scenarios below are related to the creation of application topology version within an application version
  @reset
  Scenario: Creating a new application topology version should succeed
    When I create an application topology version for application "watchmiddleearth" version "watchmiddleearth:0.1.0-SNAPSHOT" with qualifier "DEV", description "topology for development environment", topology template id "null" and previous version id "null"
    Then I should receive a RestResponse with no error
    And I get the application version for application "watchmiddleearth" with id "watchmiddleearth:0.1.0-SNAPSHOT"
    And I should receive a RestResponse with no error
    And The application version should have an application topology version with version "0.1.0-DEV-SNAPSHOT"

  @reset
  Scenario: Creating a new application topology version for an application version that does not exist should fail
    When I create an application topology version for application "watchmiddleearth" version "watchmiddleearth:0.2.0-SNAPSHOT" with qualifier "DEV", description "topology for development environment", topology template id "null" and previous version id "null"
    Then I should receive a RestResponse with an error code 504

  @reset
  Scenario: Creating a new application version with an already existing version number should fail
    When I create an application topology version for application "watchmiddleearth" version "watchmiddleearth:0.1.0-SNAPSHOT" with qualifier "", description "topology for development environment", topology template id "null" and previous version id "null"
    Then I should receive a RestResponse with an error code 502

  @reset
  Scenario: Creating a new application topology version when not application manager for the application should fail
    Given I am authenticated with "ADMIN" role
    And There is a "mairon" user in the system
    # give the global APPLICATIONS_MANAGER role but mairon is not manager on the application
    And I add a role "APPLICATIONS_MANAGER" to user "mairon"
    And I am authenticated with user named "mairon"
    When I create an application topology version for application "watchmiddleearth" version "watchmiddleearth:0.1.0-SNAPSHOT" with qualifier "DEV", description "topology for development environment", topology template id "null" and previous version id "null"
    Then I should receive a RestResponse with an error code 102

  @reset
  Scenario: Creating a new application topology version from a template should succeed
    Given I am authenticated with "ADMIN" role
    # Prepare archives and user to create a topology template
    And I upload the archive "tosca base types 1.0"
    And I should receive a RestResponse with no error
    And I upload the archive "sample java types 1.0"
    And I should receive a RestResponse with no error
    And There is a "node type" with element name "tosca.nodes.Compute" and archive version "1.0"
    And There is a "node type" with element name "fastconnect.nodes.Java" and archive version "1.0"
    And There is a "mairon" user in the system
    And I add a role "ARCHITECT" to user "mairon"
    And I add a role "COMPONENTS_BROWSER" to user "sauron"
    # Create a topology template
    And I am authenticated with user named "mairon"
    And I create a new topology template with name "topology_template_java" and description "My topology template description1" and node templates
      | NodeTemplateCompute | tosca.nodes.Compute:1.0    |
      | NodeTemplateJava    | fastconnect.nodes.Java:1.0 |
    Then I should receive a RestResponse with no error
    # Create an application
    And I am authenticated with user named "sauron"
    When I create an application topology version for application "watchmiddleearth" version "watchmiddleearth:0.1.0-SNAPSHOT" with qualifier "DEV", description "topology for development environment", topology template id "topology_template_java:0.1.0-SNAPSHOT" and previous version id "null"
    Then I should receive a RestResponse with no error
    And I get the application version for application "watchmiddleearth" with id "watchmiddleearth:0.1.0-SNAPSHOT"
    And I should receive a RestResponse with no error
    And The application version should have an application topology version with version "0.1.0-DEV-SNAPSHOT"
    And Topologies "watchmiddleearth:0.1.0-DEV-SNAPSHOT" and "topology_template_java:0.1.0-SNAPSHOT" have the same number of node templates with identical types.

  @reset
  Scenario: Creating a new application topology version from a template that does not exists should fail
    # Create an application
    When I create an application topology version for application "watchmiddleearth" version "watchmiddleearth:0.1.0-SNAPSHOT" with qualifier "DEV", description "topology for development environment", topology template id "topology_template_java:0.1.0-SNAPSHOT" and previous version id "null"
    Then I should receive a RestResponse with an error code 504

  @reset
  Scenario: Creating a new application topology version from a previous version should succeed
    Given I am authenticated with "ADMIN" role
      # Prepare archives and user to create a topology template
    And I upload the archive "tosca base types 1.0"
    And I should receive a RestResponse with no error
    And I upload the archive "sample java types 1.0"
    And I should receive a RestResponse with no error
    And There is a "node type" with element name "tosca.nodes.Compute" and archive version "1.0"
    And There is a "node type" with element name "fastconnect.nodes.Java" and archive version "1.0"
    And There is a "mairon" user in the system
    And I add a role "ARCHITECT" to user "mairon"
    And I add a role "COMPONENTS_BROWSER" to user "sauron"
      # Create a topology template
    And I am authenticated with user named "mairon"
    And I create a new topology template with name "topology_template_java" and description "My topology template description1" and node templates
      | NodeTemplateCompute | tosca.nodes.Compute:1.0    |
      | NodeTemplateJava    | fastconnect.nodes.Java:1.0 |
    And I should receive a RestResponse with no error
      # Create an application
    And I am authenticated with user named "sauron"
    And I create an application topology version for application "watchmiddleearth" version "watchmiddleearth:0.1.0-SNAPSHOT" with qualifier "DEV", description "topology for development environment", topology template id "topology_template_java:0.1.0-SNAPSHOT" and previous version id "null"
    And I should receive a RestResponse with no error
    And Topologies "watchmiddleearth:0.1.0-DEV-SNAPSHOT" and "topology_template_java:0.1.0-SNAPSHOT" have the same number of node templates with identical types.
    When I create an application topology version for application "watchmiddleearth" version "watchmiddleearth:0.1.0-SNAPSHOT" with qualifier "PROD", description "topology for development environment", topology template id "null" and previous version id "watchmiddleearth:0.1.0-DEV-SNAPSHOT"
    Then I should receive a RestResponse with no error
    And I get the application version for application "watchmiddleearth" with id "watchmiddleearth:0.1.0-SNAPSHOT"
    And I should receive a RestResponse with no error
    And The application version should have an application topology version with version "0.1.0-SNAPSHOT"
    And The application version should have an application topology version with version "0.1.0-DEV-SNAPSHOT"
    And The application version should have an application topology version with version "0.1.0-PROD-SNAPSHOT"
    And Topologies "watchmiddleearth:0.1.0-DEV-SNAPSHOT" and "watchmiddleearth:0.1.0-PROD-SNAPSHOT" have the same number of node templates with identical types.

  @reset
  Scenario: Creating a new application topology version from a previous version that does not exists should fail
    # Create an application
    When I create an application topology version for application "watchmiddleearth" version "watchmiddleearth:0.1.0-SNAPSHOT" with qualifier "DEV", description "topology for development environment", topology template id "null" and previous version id "watchmiddleearth:0.2.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 504

  @reset
  Scenario: Creating a new application topology version from a previous version from another application should fail
    Given I create an application with name "attackgondor", archive name "attackgondor", description "Send Mordor armies to march on Gondor." and topology template id "null"
    When I create an application topology version for application "watchmiddleearth" version "watchmiddleearth:0.1.0-SNAPSHOT" with qualifier "DEV", description "topology for development environment", topology template id "null" and previous version id "attackgondor:0.1.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 102

  # Scenario below depends on the application topology version creation feature
  @reset
  Scenario: Creating a new application version from a previous version should succeed
    Given I am authenticated with "ADMIN" role
      # Prepare archives and user to create a topology template
    And I upload the archive "tosca base types 1.0"
    And I should receive a RestResponse with no error
    And I upload the archive "sample java types 1.0"
    And I should receive a RestResponse with no error
    And There is a "node type" with element name "tosca.nodes.Compute" and archive version "1.0"
    And There is a "node type" with element name "fastconnect.nodes.Java" and archive version "1.0"
    And There is a "mairon" user in the system
    And I add a role "ARCHITECT" to user "mairon"
    And I add a role "COMPONENTS_BROWSER" to user "sauron"
      # Create a topology template
    And I am authenticated with user named "mairon"
    And I create a new topology template with name "topology_template_java" and description "My topology template description1" and node templates
      | NodeTemplateCompute | tosca.nodes.Compute:1.0    |
      | NodeTemplateJava    | fastconnect.nodes.Java:1.0 |
    And I should receive a RestResponse with no error
      # Create an application
    And I am authenticated with user named "sauron"
    Given I create an application topology version for application "watchmiddleearth" version "watchmiddleearth:0.1.0-SNAPSHOT" with qualifier "DEV", description "topology for development environment", topology template id "null" and previous version id "null"
    And I create an application topology version for application "watchmiddleearth" version "watchmiddleearth:0.1.0-SNAPSHOT" with qualifier "PROD", description "topology for development environment", topology template id "topology_template_java:0.1.0-SNAPSHOT" and previous version id "null"
    When I create an application version for application "watchmiddleearth" with version "0.2.0-SNAPSHOT", description "A great new version.", topology template id "null" and previous version id "watchmiddleearth:0.1.0-SNAPSHOT"
    Then I should receive a RestResponse with no error
    And I get the application version for application "watchmiddleearth" with id "watchmiddleearth:0.2.0-SNAPSHOT"
    And The application version should have an application topology version with version "0.2.0-SNAPSHOT"
    And The application version should have an application topology version with version "0.2.0-DEV-SNAPSHOT"
    And The application version should have an application topology version with version "0.2.0-PROD-SNAPSHOT"
    And Topologies "watchmiddleearth:0.1.0-DEV-SNAPSHOT" and "watchmiddleearth:0.2.0-DEV-SNAPSHOT" have the same number of node templates with identical types.
    And Topologies "watchmiddleearth:0.1.0-PROD-SNAPSHOT" and "watchmiddleearth:0.2.0-PROD-SNAPSHOT" have the same number of node templates with identical types.

  @reset
  Scenario: Creating a new application version from a previous version that does not exists should fail
    When I create an application version for application "watchmiddleearth" with version "0.2.0-SNAPSHOT", description "A great new version.", topology template id "null" and previous version id "watchmiddleearth:0.1.0"
    Then I should receive a RestResponse with an error code 504

  @reset
  Scenario: Creating a new application version from a previous version that belongs to another application should fail
    Given I create an application with name "attackgondor", archive name "attackgondor", description "Send Mordor armies to march on Gondor." and topology template id "null"
    When I create an application version for application "watchmiddleearth" with version "0.2.0-SNAPSHOT", description "A great new version.", topology template id "null" and previous version id "attackgondor:0.1.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 102

  @reset
  Scenario: Creating a new application topology version with a snapshot in qualifier name should fail
    # Create an application
    When I create an application topology version for application "watchmiddleearth" version "watchmiddleearth:0.1.0-SNAPSHOT" with qualifier "SNAPSHOT", description "topology for development environment", topology template id "null" and previous version id "null"
    Then I should receive a RestResponse with an error code 605
    When I create an application topology version for application "watchmiddleearth" version "watchmiddleearth:0.1.0-SNAPSHOT" with qualifier "1.0-SNAPSHOT", description "topology for development environment", topology template id "null" and previous version id "null"
    Then I should receive a RestResponse with an error code 605
    When I create an application topology version for application "watchmiddleearth" version "watchmiddleearth:0.1.0-SNAPSHOT" with qualifier "snapshot", description "topology for development environment", topology template id "null" and previous version id "null"
    Then I should receive a RestResponse with an error code 605
    When I create an application topology version for application "watchmiddleearth" version "watchmiddleearth:0.1.0-SNAPSHOT" with qualifier "SnapshoT", description "topology for development environment", topology template id "null" and previous version id "null"
    Then I should receive a RestResponse with an error code 605
