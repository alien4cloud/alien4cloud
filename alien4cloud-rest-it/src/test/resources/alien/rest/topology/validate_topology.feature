Feature: Check if topology is valid

  Background:
    Given I am authenticated with "APPLICATIONS_MANAGER" role
      And I create a new application with name "watchmiddleearth" and description "Use my great eye to find frodo and the ring."
      And I already had a csar with name "myCsar" and version "1.0-SNAPSHOT"
      And I add to the csar "myCsar" "1.0-SNAPSHOT" the component "rootNodeType"
      And I create "capabilities" in an archive name "myCsar" version "1.0-SNAPSHOT"
        |tosca.capabilities.Container|
        |tosca.capabilities.Feature|
        |fastconnect.capabilities.Runner|
        |tosca.capabilities.Java|
      And i create a relationshiptype "test.HostedOn" in an archive name "myCsar" version "1.0-SNAPSHOT" with properties
        |validSource     | tosca.capabilities.Container|
        |validTarget     | tosca.capabilities.Container|
        |abstract        |false|
      And i create a relationshiptype "test.DependsOn" in an archive name "myCsar" version "1.0-SNAPSHOT" with properties
        |validSource     | tosca.capabilities.Feature|
        |validTarget     | tosca.capabilities.Feature|
        |abstract        |true|

  @reset
  Scenario: checking if an empty topology is valid
    Given I am authenticated with "APPLICATIONS_MANAGER" role
    When I check for the valid status of the topology
    Then I should receive a RestResponse with no error
      And the topology should not be valid

  @reset
  Scenario: adding nodes templates and check if topology is valid
    Given I am authenticated with "APPLICATIONS_MANAGER" role
      And I add to the csar "myCsar" "1.0-SNAPSHOT" the components
        |computeNodeType|
        |javaNodeType|
      And I have added a node template "Compute" related to the "fastconnect.nodes.Compute:1.0-SNAPSHOT" node type
    When I check for the valid status of the topology
    Then I should receive a RestResponse with no error
      And the topology should be valid
    When I add a node template "Java" related to the "fastconnect.nodes.Java:1.0-SNAPSHOT" node type
      And I check for the valid status of the topology
    Then I should receive a RestResponse with no error
      And the topology should be valid

  @reset
  Scenario: adding non abstract relationships and check if topology is valid
    Given I am authenticated with "APPLICATIONS_MANAGER" role
      And I add to the csar "myCsar" "1.0-SNAPSHOT" the components
        |computeNodeType|
        |javaNodeType|
        |javaChefNodeType|
      And I have added a node template "Compute" related to the "fastconnect.nodes.Compute:1.0-SNAPSHOT" node type
      And I have added a node template "compute_2" related to the "fastconnect.nodes.Compute:1.0-SNAPSHOT" node type
      And I have added a node template "JavaChef" related to the "fastconnect.nodes.JavaChef:1.0-SNAPSHOT" node type
    When I add a relationship of type "test.HostedOn" defined in archive "myCsar" version "1.0-SNAPSHOT" with source "JavaChef" and target "Compute" for requirement "container" of type "tosca.capabilities.Container" and target capability "container"
      And I add a relationship of type "test.HostedOn" defined in archive "myCsar" version "1.0-SNAPSHOT" with source "JavaChef" and target "compute_2" for requirement "compute" of type "tosca.capabilities.Container" and target capability "container"
      And I check for the valid status of the topology
    Then I should receive a RestResponse with no error
      And the topology should be valid

  @reset
  Scenario: adding abstract relationships and check if topology is valid
    Given I am authenticated with "APPLICATIONS_MANAGER" role
      And I add to the csar "myCsar" "1.0-SNAPSHOT" the components
        |computeNodeType|
        |javaNodeType|
        |javaChefNodeType|
      And I have added a node template "Compute" related to the "fastconnect.nodes.Compute:1.0-SNAPSHOT" node type
      And I have added a node template "JavaChef" related to the "fastconnect.nodes.JavaChef:1.0-SNAPSHOT" node type
    When I add a relationship of type "test.DependsOn" defined in archive "myCsar" version "1.0-SNAPSHOT" with source "JavaChef" and target "Compute" for requirement "featureYup" of type "tosca.capabilities.Feature" and target capability "feature"
      And I check for the valid status of the topology
    Then I should receive a RestResponse with no error
      And the topology should not be valid

  @reset
  Scenario: adding abstract nodes templates and check, should be valid
    Given I am authenticated with "APPLICATIONS_MANAGER" role
      And I add to the csar "myCsar" "1.0-SNAPSHOT" the components
        |applicationServerNodeType|
        |computeNodeType|
        |javaNodeType|
        |javaChefNodeType|
        |javaFakeNodeType|
        |tierNodeType|
      And I have added a node template "ApplicationServer" related to the "fastconnect.nodes.ApplicationServer:1.0-SNAPSHOT" node type
      And I have added a node template "Java" related to the "fastconnect.nodes.Java:1.0-SNAPSHOT" node type
      And I have added a node template "Tier" related to the "tosca.nodes.Tier:1.0-SNAPSHOT" node type
      And I add a relationship of type "test.HostedOn" defined in archive "myCsar" version "1.0-SNAPSHOT" with source "Java" and target "ApplicationServer" for requirement "compute" of type "tosca.capabilities.Container" and target capability "container"
      And I add a relationship of type "test.DependsOn" defined in archive "myCsar" version "1.0-SNAPSHOT" with source "Java" and target "Tier" for requirement "dependency" of type "tosca.capabilities.Feature" and target capability "feature"
    When I check for the valid status of the topology
    Then I should receive a RestResponse with no error
      And the topology should be valid

  @reset
  Scenario: adding nodetemplates without requirements lowerbounds satisfied and check if topology is valid
    Given I am authenticated with "APPLICATIONS_MANAGER" role
      And I add to the csar "myCsar" "1.0-SNAPSHOT" the components
        |computeNodeType|
        |javaNodeType|
        |javaChefNodeType|
      And I have added a node template "Compute" related to the "fastconnect.nodes.Compute:1.0-SNAPSHOT" node type
      And I have added a node template "JavaChef" related to the "fastconnect.nodes.JavaChef:1.0-SNAPSHOT" node type
    When I check for the valid status of the topology
    Then I should receive a RestResponse with no error
      And the topology should not be valid
      And the node with requirements lowerbound not satisfied should be
        |JavaChef|container,compute|

  @reset
  Scenario: Add a relationship between 2 nodes when upperbound is already reached on target
    Given I am authenticated with "APPLICATIONS_MANAGER" role
      And I add to the csar "myCsar" "1.0-SNAPSHOT" the components
        |computeModifiedNodeType|
        |javaNodeType|
        |javaChefNodeType|
      And I have added a node template "ComputeModified" related to the "fastconnect.nodes.ComputeModified:1.0-SNAPSHOT" node type
      And I have added a node template "JavaChef" related to the "fastconnect.nodes.JavaChef:1.0-SNAPSHOT" node type
      And I have added a node template "Java" related to the "fastconnect.nodes.Java:1.0-SNAPSHOT" node type
      And I have added a relationship "hostedOnCompute" of type "test.HostedOn" defined in archive "myCsar" version "1.0-SNAPSHOT" with source "Java" and target "ComputeModified" for requirement "compute" of type "tosca.capabilities.Container" and target capability "container"
    When I add a relationship "hostedOnCompute" of type "test.HostedOn" defined in archive "myCsar" version "1.0-SNAPSHOT" with source "JavaChef" and target "ComputeModified" for requirement "container" of type "tosca.capabilities.Container" and target capability "container"
    Then I should receive a RestResponse with an error code 810

  @reset
  Scenario: adding nodetemplate with required properties not set and check if topology is valid
    Given I am authenticated with "APPLICATIONS_MANAGER" role
      And I add to the csar "myCsar" "1.0-SNAPSHOT" the components
         |javaFakeNodeType|
      And I have added a node template "JavaFake" related to the "fastconnect.nodes.JavaFake:1.0-SNAPSHOT" node type
    When I check for the valid status of the topology
    Then I should receive a RestResponse with no error
      And the topology should not be valid
      And the node with required properties not set should be
        |JavaFake|os_name|
    When I update the node template "JavaFake"'s property "os_name" to "Linux"
      Then I should receive a RestResponse with no error
    When I check for the valid status of the topology
      Then I should receive a RestResponse with no error
      And the topology should be valid

  @reset
  Scenario: Release while SNAPSHOT dependency remain should failed
    Given I am authenticated with "APPLICATIONS_MANAGER" role
      And I add to the csar "myCsar" "1.0-SNAPSHOT" the components
        |computeModifiedNodeType|
        |javaNodeType|
      And I have added a node template "ComputeModified" related to the "fastconnect.nodes.ComputeModified:1.0-SNAPSHOT" node type
    When I update an application version with version "0.1.0-SNAPSHOT" to "0.2"
    Then I should receive a RestResponse with an error code 830

  @reset
  Scenario: Update a topology when the application version is released should failed
    Given I am authenticated with "APPLICATIONS_MANAGER" role
      And I add to the csar "myCsar" "1.0-SNAPSHOT" the components
        |computeModifiedNodeType|
        |javaNodeType|
      And I update an application version with version "0.1.0-SNAPSHOT" to "0.2"
      And I have added a node template "Java" related to the "fastconnect.nodes.Java:1.0-SNAPSHOT" node type without check
    Then I should receive a RestResponse with an error code 807
