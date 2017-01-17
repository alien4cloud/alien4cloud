Feature: Manage relationships template between node topology

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca base types 1.0"
    And I upload the archive "tosca-normative-types-wd06"
    And I should receive a RestResponse with no error
    And I upload the archive "sample java types 1.0"
    And I should receive a RestResponse with no error
    And There is a "node type" with element name "tosca.nodes.Compute" and archive version "1.0"
    And There is a "node type" with element name "fastconnect.nodes.Java" and archive version "1.0"
    And I create an application with name "watchmiddleearth", archive name "watchmiddleearth", description "Use my great eye to find frodo and the ring." and topology template id "null"

  @reset
  Scenario: Add a relationship between 2 nodes
    Given I have added a node template "Compute" related to the "tosca.nodes.Compute:1.0" node type
    	And I have added a node template "Java" related to the "fastconnect.nodes.Java:1.0" node type
    When I add a relationship of type "tosca.relationships.HostedOn" defined in archive "tosca-base-types" version "1.0" with source "Java" and target "Compute" for requirement "host" of type "tosca.capabilities.Container" and target capability "compute"
    	And I add a relationship of type "tosca.relationships.DependsOn" defined in archive "tosca-base-types" version "1.0" with source "Java" and target "Compute" for requirement "dependency" of type "tosca.capabilities.Feature" and target capability "feature"
    Then I should receive a RestResponse with no error
    	And I should have 1 relationship with source "Java" and target "Compute" for type "tosca.relationships.HostedOn" with requirement "host" of type "tosca.capabilities.Container"
      And I should have a relationship with type "tosca.relationships.HostedOn" from "Java" to "Compute" in ALIEN

  @reset
  Scenario: Add a relationship between 2 nodes when upperbound is already reached on source
    Given I have added a node template "Compute" related to the "tosca.nodes.Compute:1.0" node type
      And I have added a node template "Java" related to the "fastconnect.nodes.Java:1.0" node type
      And I have added a relationship "hostedOnCompute" of type "tosca.relationships.HostedOn" defined in archive "tosca-base-types" version "1.0" with source "Java" and target "Compute" for requirement "host" of type "tosca.capabilities.Container" and target capability "compute"
    When I add a relationship of type "tosca.relationships.HostedOn" defined in archive "tosca-base-types" version "1.0" with source "Java" and target "Compute" for requirement "host" of type "tosca.capabilities.Container" and target capability "compute"
    Then I should receive a RestResponse with an error code 810

  @reset
  Scenario: Add a relationship between 2 nodes when upperbound is already reached on target
    Given I have added a node template "Compute" related to the "tosca.nodes.Compute:1.0" node type
      And I have added a node template "Java" related to the "fastconnect.nodes.Java:1.0" node type
      And I have added a relationship "hostedOnCompute" of type "tosca.relationships.HostedOn" defined in archive "tosca-base-types" version "1.0" with source "Java" and target "Compute" for requirement "host" of type "tosca.capabilities.Container" and target capability "compute"
    When I add a relationship of type "tosca.relationships.HostedOn" defined in archive "tosca-base-types" version "1.0" with source "Java" and target "Compute" for requirement "host" of type "tosca.capabilities.Container" and target capability "compute"
    Then I should receive a RestResponse with an error code 810

  @reset
  Scenario: delete a relationship from a node template
    Given I have added a node template "Compute" related to the "tosca.nodes.Compute:1.0" node type
    	And I have added a node template "Java" related to the "fastconnect.nodes.Java:1.0" node type
      And I have added a relationship "hostedOnCompute" of type "tosca.relationships.HostedOn" defined in archive "tosca-base-types" version "1.0" with source "Java" and target "Compute" for requirement "host" of type "tosca.capabilities.Container" and target capability "compute"
    	And I have added a relationship "dependsOnCompute" of type "tosca.relationships.DependsOn" defined in archive "tosca-base-types" version "1.0" with source "Java" and target "Compute" for requirement "dependency" of type "tosca.capabilities.Feature" and target capability "feature"
    When I delete the relationship "dependsOnCompute" from the node template "Java"
    Then I should receive a RestResponse with no error
    When I try to retrieve the created topology
    Then I should receive a RestResponse with no error
      And I should have a relationship "hostedOnCompute" with type "tosca.relationships.HostedOn" from "Java" to "Compute" in ALIEN
      And I should not have the relationship "dependsOnCompute" in "Java" node template

  @reset
  Scenario: Add a relationship between 2 nodes: valid sources different of valid target
    Given I upload the archive "relationship test types"
      And There is a "node type" with element name "test.nodes.Compute" and archive version "1.0"
      And There is a "node type" with element name "test.nodes.Java" and archive version "1.0"
      And I have added a node template "Compute_test" related to the "test.nodes.Compute:1.0" node type
      And I have added a node template "Java_test" related to the "test.nodes.Java:1.0" node type
    When I add a relationship of type "test.relationships.HostedOn" defined in archive "tosca-base-types" version "1.0" with source "Java_test" and target "Compute_test" for requirement "hosted" of type "test.capabilities.Container" and target capability "hosting"
    Then I should receive a RestResponse with no error
      And I should have 1 relationship with source "Java_test" and target "Compute_test" for type "test.relationships.HostedOn" with requirement "hosted" of type "test.capabilities.Container"

  @reset
  Scenario: rename a relationship
    Given I have added a node template "Compute" related to the "tosca.nodes.Compute:1.0" node type
      And I have added a node template "Java" related to the "fastconnect.nodes.Java:1.0" node type
      And I have added a relationship "hostedOnCompute" of type "tosca.relationships.HostedOn" defined in archive "tosca-base-types" version "1.0" with source "Java" and target "Compute" for requirement "host" of type "tosca.capabilities.Container" and target capability "compute"
      And I have added a relationship "dependsOnCompute" of type "tosca.relationships.DependsOn" defined in archive "tosca-base-types" version "1.0" with source "Java" and target "Compute" for requirement "dependency" of type "tosca.capabilities.Container" and target capability "feature"
    When I rename the relationship "hostedOnCompute" into "hostedOnRenamed" from the node template "Java"
    Then I should receive a RestResponse with no error
      And I should have a relationship "hostedOnRenamed" with type "tosca.relationships.HostedOn" from "Java" to "Compute" in ALIEN

  @reset
  Scenario: update a relationship
    Given I have added a node template "Compute" related to the "tosca.nodes.Compute:1.0" node type
      And I have added a node template "Java" related to the "fastconnect.nodes.Java:1.0" node type
      And I have added a relationship "hostedOnCompute" of type "tosca.relationships.HostedOn" defined in archive "tosca-base-types" version "1.0" with source "Java" and target "Compute" for requirement "host" of type "tosca.capabilities.Container" and target capability "compute"
      And I have added a relationship "dependsOnCompute" of type "tosca.relationships.DependsOn" defined in archive "tosca-base-types" version "1.0" with source "Java" and target "Compute" for requirement "dependency" of type "tosca.capabilities.Container" and target capability "feature"
    When I update the "password" property of the relationship "hostedOnCompute" into "mypassword" from the node template "Java"
    Then I should receive a RestResponse with no error
      And I should have a relationship "hostedOnCompute" with type "tosca.relationships.HostedOn" from "Java" to "Compute" in ALIEN

  @reset
  Scenario: update a complexe property of relationship
    Given I have added a node template "Compute" related to the "tosca.nodes.Compute:1.0.0.wd06-SNAPSHOT" node type
    And I have added a node template "LoadBalancer" related to the "tosca.nodes.LoadBalancer:1.0.0.wd06-SNAPSHOT" node type
    And I have added a relationship "routesToCompute" of type "tosca.relationships.RoutesTo" defined in archive "tosca-normative-types" version "1.0.0.wd06-SNAPSHOT" with source "LoadBalancer" and target "Compute" for requirement "application" of type "tosca.capabilities.Endpoint" and target capability "endpoint"
    When I update the "credential" complex property of the relationship "routesToCompute" into """{"user": "sauron"}""" from the node template "LoadBalancer"
    Then I should receive a RestResponse with no error
