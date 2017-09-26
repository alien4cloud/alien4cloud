Feature: Topology editor: duplicate node

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Duplicate a node template in a topology
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Template                                                              |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type     | org.alien4cloud.tosca.editor.operations.nodetemplate.DuplicateNodeOperation |
      | nodeName | Template                                                                    |
    Then No exception should be thrown
    And The SPEL expression "nodeTemplates.size()" should return 2
    And The SPEL expression "nodeTemplates['Template'].type" should return "tosca.nodes.Compute"
    And The SPEL expression "nodeTemplates['Template_copy'].type" should return "tosca.nodes.Compute"

  Scenario: Duplicating a non existing node template in an empty topology should fail
    When I execute the operation
      | type     | org.alien4cloud.tosca.editor.operations.nodetemplate.DuplicateNodeOperation |
      | nodeName | missingNode                                                                 |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Duplicating a non existing node template in a topology should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Template                                                              |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type     | org.alien4cloud.tosca.editor.operations.nodetemplate.DuplicateNodeOperation |
      | nodeName | missingNode                                                                 |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Duplicating a node template in a topology should also copy outputs
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Template                                                              |
      | indexedNodeTypeId | tosca.nodes.WebServer:1.0.0-SNAPSHOT                                  |
    And I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.SetNodePropertyAsOutputOperation |
      | nodeName     | Template                                                                                      |
      | propertyName | component_version                                                                             |
    And I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.SetNodeAttributeAsOutputOperation |
      | nodeName      | Template                                                                                       |
      | attributeName | tosca_name                                                                                     |
    And I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.SetNodeCapabilityPropertyAsOutputOperation |
      | nodeName       | Template                                                                                                |
      | capabilityName | data_endpoint                                                                                           |
      | propertyName   | port                                                                                                    |

    When I execute the operation
      | type     | org.alien4cloud.tosca.editor.operations.nodetemplate.DuplicateNodeOperation |
      | nodeName | Template                                                                    |
    Then No exception should be thrown
    And The SPEL expression "nodeTemplates.size()" should return 2
    And The SPEL expression "nodeTemplates['Template'].type" should return "tosca.nodes.WebServer"
    And The SPEL expression "nodeTemplates['Template_copy'].type" should return "tosca.nodes.WebServer"

    #output properties
    And The SPEL expression "outputProperties.size()" should return 2
    And The SPEL expression "outputProperties['Template'].size()" should return 1
    And The SPEL expression "outputProperties['Template'].?[#this == 'component_version'].size()" should return 1
    And The SPEL expression "outputProperties['Template_copy'].size()" should return 1
    And The SPEL expression "outputProperties['Template_copy'].?[#this == 'component_version'].size()" should return 1

    #outputs attributes
    And The SPEL expression "outputAttributes.size()" should return 2
    And The SPEL expression "outputAttributes['Template'].size()" should return 1
    And The SPEL expression "outputAttributes['Template'].?[#this == 'tosca_name'].size()" should return 1
    And The SPEL expression "outputAttributes['Template_copy'].size()" should return 1
    And The SPEL expression "outputAttributes['Template_copy'].?[#this == 'tosca_name'].size()" should return 1

    #outputs capabilities properties
    And The SPEL expression "outputCapabilityProperties.size()" should return 2
    And The SPEL expression "outputCapabilityProperties['Template'].size()" should return 1
    And The SPEL expression "outputCapabilityProperties['Template']['data_endpoint'].size()" should return 1
    And The SPEL expression "outputCapabilityProperties['Template']['data_endpoint'].?[#this == 'port'].size()" should return 1
    And The SPEL expression "outputCapabilityProperties['Template_copy'].size()" should return 1
    And The SPEL expression "outputCapabilityProperties['Template_copy']['data_endpoint'].size()" should return 1
    And The SPEL expression "outputCapabilityProperties['Template_copy']['data_endpoint'].?[#this == 'port'].size()" should return 1


  Scenario: Duplicating a node template should also copy its hosted nodes
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Java                                                                  |
      | indexedNodeTypeId | fastconnect.nodes.Java:1.0                                            |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | Java                                                                                  |
      | relationshipName       | MyRelationship                                                                        |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0                                                                                   |
      | requirementName        | host                                                                                  |
      | target                 | Compute                                                                               |
      | targetedCapabilityName | host                                                                                  |
    When I execute the operation
      | type     | org.alien4cloud.tosca.editor.operations.nodetemplate.DuplicateNodeOperation |
      | nodeName | Compute                                                                     |
    Then No exception should be thrown
    And The SPEL expression "nodeTemplates.size()" should return 4
    And The SPEL expression "nodeTemplates['Compute_copy'].type" should return "tosca.nodes.Compute"
    And The SPEL expression "nodeTemplates['Java_copy'].type" should return "fastconnect.nodes.Java"
    And The SPEL expression "nodeTemplates['Java_copy'].relationships.size()" should return 1
    And The SPEL expression "nodeTemplates['Java_copy'].relationships['MyRelationship_copy'].target" should return "Compute_copy"

    ##Duplicate a hosted node only
    When I execute the operation
      | type     | org.alien4cloud.tosca.editor.operations.nodetemplate.DuplicateNodeOperation |
      | nodeName | Java_copy                                                                   |
    Then No exception should be thrown
    And The SPEL expression "nodeTemplates.size()" should return 5
    And The SPEL expression "nodeTemplates['Java_copy_copy'].type" should return "fastconnect.nodes.Java"
    And The SPEL expression "nodeTemplates['Java_copy_copy'].relationships" should return "null"


  Scenario: Duplicating a node template should also copy its hosted nodes, along with internal relationships
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Java                                                                  |
      | indexedNodeTypeId | fastconnect.nodes.Java:1.0                                            |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Java2                                                                 |
      | indexedNodeTypeId | fastconnect.nodes.Java:1.0                                            |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Java3                                                                 |
      | indexedNodeTypeId | fastconnect.nodes.Java:1.0                                            |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | Java                                                                                  |
      | relationshipName       | MyRelationship                                                                        |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0                                                                                   |
      | requirementName        | host                                                                                  |
      | target                 | Compute                                                                               |
      | targetedCapabilityName | host                                                                                  |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | Java2                                                                                 |
      | relationshipName       | MyRelationship                                                                        |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0                                                                                   |
      | requirementName        | host                                                                                  |
      | target                 | Compute                                                                               |
      | targetedCapabilityName | host                                                                                  |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | Java                                                                                  |
      | relationshipName       | MyRelationship2                                                                       |
      | relationshipType       | tosca.relationships.DependsOn                                                         |
      | relationshipVersion    | 1.0                                                                                   |
      | requirementName        | dependency                                                                            |
      | target                 | Java2                                                                                 |
      | targetedCapabilityName | feature                                                                               |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | Java                                                                                  |
      | relationshipName       | MyRelationship3                                                                       |
      | relationshipType       | tosca.relationships.DependsOn                                                         |
      | relationshipVersion    | 1.0                                                                                   |
      | requirementName        | dependency                                                                            |
      | target                 | Java3                                                                                 |
      | targetedCapabilityName | feature                                                                               |
    When I execute the operation
      | type     | org.alien4cloud.tosca.editor.operations.nodetemplate.DuplicateNodeOperation |
      | nodeName | Compute                                                                     |
    Then No exception should be thrown
    And The SPEL expression "nodeTemplates.size()" should return 7
    And The SPEL expression "nodeTemplates['Compute_copy'].type" should return "tosca.nodes.Compute"
    And The SPEL expression "nodeTemplates['Java_copy'].type" should return "fastconnect.nodes.Java"
    And The SPEL expression "nodeTemplates['Java2_copy'].type" should return "fastconnect.nodes.Java"

    ## There should be only 2 relationships in Java_copy node, targeting Compute_copy and Java2_copy.
    And The SPEL expression "nodeTemplates['Java_copy'].relationships.size()" should return 2
    And The SPEL expression "nodeTemplates['Java_copy'].relationships['MyRelationship_copy'].target" should return "Compute_copy"
    And The SPEL expression "nodeTemplates['Java_copy'].relationships['MyRelationship2_copy'].target" should return "Java2_copy"
    And The SPEL expression "nodeTemplates['Java2_copy'].relationships.size()" should return 1
    And The SPEL expression "nodeTemplates['Java2_copy'].relationships['MyRelationship_copy'].target" should return "Compute_copy"

  Scenario: Duplicating a node template should not impact the original node
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Java                                                                  |
      | indexedNodeTypeId | fastconnect.nodes.Java:1.0                                            |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Java2                                                                 |
      | indexedNodeTypeId | fastconnect.nodes.Java:1.0                                            |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | Java                                                                                  |
      | relationshipName       | MyRelationship                                                                        |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0                                                                                   |
      | requirementName        | host                                                                                  |
      | target                 | Compute                                                                               |
      | targetedCapabilityName | host                                                                                  |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | Java                                                                                  |
      | relationshipName       | MyRelationship2                                                                       |
      | relationshipType       | tosca.relationships.DependsOn                                                         |
      | relationshipVersion    | 1.0                                                                                   |
      | requirementName        | dependency                                                                            |
      | target                 | Java2                                                                                 |
      | targetedCapabilityName | feature                                                                               |
    And The SPEL expression "nodeTemplates['Java'].relationships.size()" should return 2
    When I execute the operation
      | type     | org.alien4cloud.tosca.editor.operations.nodetemplate.DuplicateNodeOperation |
      | nodeName | Compute                                                                     |
    Then No exception should be thrown
    And The SPEL expression "nodeTemplates.size()" should return 5
    And The SPEL expression "nodeTemplates['Compute_copy'].type" should return "tosca.nodes.Compute"
    And The SPEL expression "nodeTemplates['Java_copy'].type" should return "fastconnect.nodes.Java"
    And The SPEL expression "nodeTemplates['Java_copy'].relationships.size()" should return 1
    And The SPEL expression "nodeTemplates['Java_copy'].relationships['MyRelationship_copy'].target" should return "Compute_copy"
    ## relationships on the original nodes should not change
    And The SPEL expression "nodeTemplates['Java'].relationships.size()" should return 2
    And The SPEL expression "nodeTemplates['Java'].relationships['MyRelationship'].target" should return "Compute"
    And The SPEL expression "nodeTemplates['Java'].relationships['MyRelationship2'].target" should return "Java2"

    #TODO check workflow
