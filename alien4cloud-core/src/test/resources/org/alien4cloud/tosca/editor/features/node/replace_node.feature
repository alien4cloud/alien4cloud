Feature: Topology editor: replace nodes templates

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload CSAR from path "../target/it-artifacts/node_replacement.csar"
    And I create an empty topology

  Scenario: Replace a nodetemplate in a topology
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    And No exception should be thrown
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | jvm                                                                   |
      | indexedNodeTypeId | alien.test.nodes.JVM:0.1-SNAPSHOT                                     |
    And No exception should be thrown
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | jvm                                                                                   |
      | relationshipName       | JVMHostedOnCompute                                                                    |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0                                                                                   |
      | requirementName        | host                                                                                  |
      | target                 | compute                                                                               |
      | targetedCapabilityName | host                                                                                  |
    And No exception should be thrown
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | app_server                                                            |
      | indexedNodeTypeId | alien.test.nodes.ApplicationServer:0.1-SNAPSHOT                       |
    And No exception should be thrown
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | app_server                                                                            |
      | relationshipName       | JVMHostedOnCompute                                                                    |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0                                                                                   |
      | requirementName        | host                                                                                  |
      | target                 | compute                                                                               |
      | targetedCapabilityName | host                                                                                  |
    And No exception should be thrown
    When I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodePropertyValueOperation |
      | nodeName      | app_server                                                                            |
      | propertyName  | component_version                                                                     |
      | propertyValue | 1.2.0                                                                                 |
    Then No exception should be thrown
    When I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateCapabilityPropertyValueOperation |
      | nodeName       | app_server                                                                                  |
      | capabilityName | app_server                                                                                  |
      | propertyName   | securized                                                                                   |
      | propertyValue  | true                                                                                        |
    Then No exception should be thrown
    When I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateCapabilityPropertyValueOperation |
      | nodeName       | app_server                                                                                  |
      | capabilityName | app_server                                                                                  |
      | propertyName   | protocol                                                                                    |
      | propertyValue  | http                                                                                        |
    Then No exception should be thrown
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | app_server                                                                            |
      | relationshipName       | AppServerDependsOnJVM                                                                 |
      | relationshipType       | alien.test.relationships.DependsOnJVM                                                 |
      | relationshipVersion    | 0.1-SNAPSHOT                                                                          |
      | requirementName        | jvm                                                                                   |
      | target                 | jvm                                                                                   |
      | targetedCapabilityName | jvm                                                                                   |
    And No exception should be thrown
    When I execute the operation
      | type             | org.alien4cloud.tosca.editor.operations.relationshiptemplate.UpdateRelationshipPropertyValueOperation |
      | nodeName         | app_server                                                                                            |
      | relationshipName | AppServerDependsOnJVM                                                                                 |
      | propertyName     | context                                                                                               |
      | propertyValue    | some value                                                                                            |
    Then No exception should be thrown
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | app                                                                   |
      | indexedNodeTypeId | alien.test.nodes.Application:0.1-SNAPSHOT                             |
    And No exception should be thrown
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | app                                                                                   |
      | relationshipName       | ApplicationHostedOnAppServer                                                          |
      | relationshipType       | alien.test.relationships.HostedOnAppServer                                            |
      | relationshipVersion    | 0.1-SNAPSHOT                                                                          |
      | requirementName        | server                                                                                |
      | target                 | app_server                                                                            |
      | targetedCapabilityName | app_server                                                                            |
    And No exception should be thrown
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.nodetemplate.ReplaceNodeOperation |
      | nodeName  | app_server                                                                |
      | newTypeId | alien.test.nodes.Tomcat:0.1-SNAPSHOT                                      |
    Then No exception should be thrown
    And The SPEL expression "nodeTemplates.size()" should return 4
    And The SPEL expression "nodeTemplates['app_server'].type" should return "alien.test.nodes.Tomcat"
    And The SPEL expression "nodeTemplates['app_server'].relationships['AppServerDependsOnJVM'].type" should return "alien.test.relationships.DependsOnJVM"
    And The SPEL expression "nodeTemplates['app_server'].relationships['AppServerDependsOnJVM'].target" should return "jvm"
    And The SPEL expression "nodeTemplates['app_server'].properties['component_version'].value" should return "1.2.0"
    And The SPEL expression "nodeTemplates['app_server'].capabilities['app_server'].properties['securized'].value" should return "true"
    And The SPEL expression "nodeTemplates['app_server'].capabilities['app_server'].properties['protocol'].value" should return "http"
    And The SPEL expression "nodeTemplates['app_server'].relationships['AppServerDependsOnJVM'].properties['context'].value" should return "some value"




