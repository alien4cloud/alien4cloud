Feature: Replace node templates

  Background:
    Given I am authenticated with "APPLICATIONS_MANAGER" role
      And I create a new application with name "watchmiddleearth" and description "Use my great eye to find frodo and the ring."
      And I already had a csar with name "myCsar" and version "1.0-SNAPSHOT"
      And I add to the csar "myCsar" "1.0-SNAPSHOT" the components
        |rootNodeType|
        |applicationServerNodeType|
        |computeNodeType|
        |javaNodeType|
        |javaChefNodeType|
        |javaFakeNodeType|
        |tierNodeType|
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
  Scenario: asking for possible replacements for an abstract node template
    Given I am authenticated with "APPLICATIONS_MANAGER" role
      And I have added a node template "Compute" related to the "fastconnect.nodes.Compute:1.0-SNAPSHOT" node type
      And I have added a node template "Java" related to the "fastconnect.nodes.Java:1.0-SNAPSHOT" node type
      And I add a relationship of type "test.HostedOn" defined in archive "myCsar" version "1.0-SNAPSHOT" with source "Java" and target "Compute" for requirement "compute" of type "tosca.capabilities.Container" and target capability "container"
    When I ask for replacements for the node "Java"
    Then I should receive a RestResponse with no error
      And the possible replacements nodes types should be
        |fastconnect.nodes.JavaChef|
        |fastconnect.nodes.JavaFake|
    When I add a relationship of type "test.DependsOn" defined in archive "myCsar" version "1.0-SNAPSHOT" with source "Java" and target "Compute" for requirement "dependency" of type "tosca.capabilities.Feature" and target capability "feature"
      And I ask for replacements for the node "Java"
    Then I should receive a RestResponse with no error
      And the possible replacements nodes types should be "fastconnect.nodes.JavaChef"

  @reset
  Scenario: asking for possible replacements for a target node template
    Given I am authenticated with "APPLICATIONS_MANAGER" role
      And I have added a node template "ApplicationServer" related to the "fastconnect.nodes.ApplicationServer:1.0-SNAPSHOT" node type
      And I have added a node template "Java" related to the "fastconnect.nodes.Java:1.0-SNAPSHOT" node type
      And I add a relationship of type "test.HostedOn" defined in archive "myCsar" version "1.0-SNAPSHOT" with source "Java" and target "ApplicationServer" for requirement "compute" of type "tosca.capabilities.Container" and target capability "container"
    When I ask for replacements for the node "ApplicationServer"
    Then I should receive a RestResponse with no error
      And the possible replacements nodes types should be
        |fastconnect.nodes.Compute|

  @reset
  Scenario: replacing a node template only as a source
    Given I am authenticated with "APPLICATIONS_MANAGER" role
      And I have added a node template "Compute" related to the "fastconnect.nodes.Compute:1.0-SNAPSHOT" node type
      And I have added a node template "Java" related to the "fastconnect.nodes.Java:1.0-SNAPSHOT" node type
      And I add a relationship "hostedOnCompute" of type "test.HostedOn" defined in archive "myCsar" version "1.0-SNAPSHOT" with source "Java" and target "Compute" for requirement "compute" of type "tosca.capabilities.Container" and target capability "container"
      And I add a relationship "dependsOnOnCompute" of type "test.DependsOn" defined in archive "myCsar" version "1.0-SNAPSHOT" with source "Java" and target "Compute" for requirement "dependency" of type "tosca.capabilities.Feature" and target capability "feature"
    When I ask for replacements for the node "Java"
    Then I should receive a RestResponse with no error
      And the possible replacements nodes types should be "fastconnect.nodes.JavaChef"
    When I replace the node template "Java" with a node "JavaChef" related to the "fastconnect.nodes.JavaChef:1.0-SNAPSHOT" node type
      Then I should receive a RestResponse with no error
      And The RestResponse should contain a node type with "fastconnect.nodes.JavaChef:1.0-SNAPSHOT" id

  @reset
  Scenario: replacing a node template only as target
    Given I am authenticated with "APPLICATIONS_MANAGER" role
      And I add to the csar "myCsar" "1.0-SNAPSHOT" the components
        |tomcatNodeType|
      And I have added a node template "Compute" related to the "fastconnect.nodes.Compute:1.0-SNAPSHOT" node type
      And I have added a node template "Java" related to the "fastconnect.nodes.Java:1.0-SNAPSHOT" node type
      And I have added a node template "Tomcat" related to the "fastconnect.nodes.Tomcat:1.0-SNAPSHOT" node type
      And I add a relationship "hostedOnCompute" of type "test.HostedOn" defined in archive "myCsar" version "1.0-SNAPSHOT" with source "Tomcat" and target "Compute" for requirement "compute" of type "tosca.capabilities.Container" and target capability "container"
      And I add a relationship "dependsOnJava" of type "test.DependsOn" defined in archive "myCsar" version "1.0-SNAPSHOT" with source "Tomcat" and target "Java" for requirement "dependency" of type "tosca.capabilities.Feature" and target capability "feature"
    When I ask for replacements for the node "Java"
    Then I should receive a RestResponse with no error
      And there should be the followings in replacements nodes types
        |fastconnect.nodes.JavaChef|
    When I replace the node template "Java" with a node "JavaChef" related to the "fastconnect.nodes.JavaChef:1.0-SNAPSHOT" node type
      Then I should receive a RestResponse with no error
      And The RestResponse should contain a node type with "fastconnect.nodes.JavaChef:1.0-SNAPSHOT" id
      And I should have a relationship "dependsOnJavaChef" with type "test.DependsOn" from "Tomcat" to "JavaChef" in ALIEN

  @reset
  Scenario: replacing a node template as a source and target
    Given I am authenticated with "APPLICATIONS_MANAGER" role
      And I add to the csar "myCsar" "1.0-SNAPSHOT" the components
        |tomcatNodeType|
      And I have added a node template "Compute" related to the "fastconnect.nodes.Compute:1.0-SNAPSHOT" node type
      And I have added a node template "Java" related to the "fastconnect.nodes.Java:1.0-SNAPSHOT" node type
      And I have added a node template "Tomcat" related to the "fastconnect.nodes.Tomcat:1.0-SNAPSHOT" node type
      And I add a relationship "hostedOnCompute" of type "test.HostedOn" defined in archive "myCsar" version "1.0-SNAPSHOT" with source "Java" and target "Compute" for requirement "compute" of type "tosca.capabilities.Container" and target capability "container"
      And I add a relationship "hostedOnCompute" of type "test.HostedOn" defined in archive "myCsar" version "1.0-SNAPSHOT" with source "Tomcat" and target "Compute" for requirement "compute" of type "tosca.capabilities.Container" and target capability "container"
      And I add a relationship "dependsOnJava" of type "test.DependsOn" defined in archive "myCsar" version "1.0-SNAPSHOT" with source "Tomcat" and target "Java" for requirement "dependency" of type "tosca.capabilities.Feature" and target capability "feature"
    When I ask for replacements for the node "Java"
    Then I should receive a RestResponse with no error
      And there should be the followings in replacements nodes types
        |fastconnect.nodes.JavaChef|
    When I replace the node template "Java" with a node "JavaChef" related to the "fastconnect.nodes.JavaChef:1.0-SNAPSHOT" node type
      Then I should receive a RestResponse with no error
      And The RestResponse should contain a node type with "fastconnect.nodes.JavaChef:1.0-SNAPSHOT" id
      And I should have a relationship "dependsOnJavaChef" with type "test.DependsOn" from "Tomcat" to "JavaChef" in ALIEN
