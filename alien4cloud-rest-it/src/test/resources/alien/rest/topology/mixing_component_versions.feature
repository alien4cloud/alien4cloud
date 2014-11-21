Feature: Mixing multiple versions of components

  Background:
    Given I am authenticated with "COMPONENTS_MANAGER" role
    And I upload the archive "normative types 1.0.0-wd03"
    And I should receive a RestResponse with no error
    And I upload the archive "normative types 1.0.1-wd03"
    And I should receive a RestResponse with no error
    And I upload the archive "normative types 1.0.2-wd03"
    And I should receive a RestResponse with no error
    And I upload the archive "sample java types 1.0"
    And I should receive a RestResponse with no error
    And I upload the archive "sample java types 2.0"
    And I should receive a RestResponse with no error
    And I upload the archive "sample java types 3.0"
    And I should receive a RestResponse with no error
    And I am authenticated with "APPLICATIONS_MANAGER" role
    And I create a new application with name "watchmiddleearth" and description "Use my great eye to find frodo and the ring."

  Scenario: Add a relationship between 2 nodes with mixing versions, ALIEN must be enough intelligent to upgrade versions
    Given I have added a node template "Compute" related to the "tosca.nodes.Compute:1.0" node type
    And I have added a node template "Java" related to the "fastconnect.nodes.Java:3.0" node type
    When I add a relationship of type "tosca.relationships.HostedOn" defined in archive "tosca-base-types" version "3.0" with source "Java" and target "Compute" for requirement "host" of type "tosca.capabilities.Container" and target capability "compute"
    And I add a relationship of type "tosca.relationships.DependsOn" defined in archive "tosca-base-types" version "1.0" with source "Java" and target "Compute" for requirement "dependency" of type "tosca.capabilities.Feature" and target capability "feature"
    Then I should receive a RestResponse with no error
    And I should have 1 relationship with source "Java" and target "Compute" for type "tosca.relationships.HostedOn" with requirement "host" of type "tosca.capabilities.Container"
    And I should have a relationship with type "tosca.relationships.HostedOn" from "Java" to "Compute" in ALIEN
    And The topology should have as dependencies
      | tosca-base-types | 3.0 |
      | java-types       | 3.0 |
