Feature: Topology editor: remove input

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload CSAR from path "../../alien4cloud/target/it-artifacts/tosca-normative-types-1.0.0-SNAPSHOT.csar"
    And I create an empty topology template

  Scenario: Remove a simple input should succeed
    Given I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | Simple input                                                     |
      | propertyDefinition.type | string                                                           |
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.inputs.DeleteInputOperation |
      | inputName | Simple input                                                        |
    Then No exception should be thrown
    And The SPEL int expression "inputs.size()" should return 0

  Scenario: Remove an input that does not exists should fail
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.inputs.DeleteInputOperation |
      | inputName | Simple input                                                        |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Remove an input that is associated to a node property should remove the association
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | software_component                                                    |
      | indexedNodeTypeId | tosca.nodes.SoftwareComponent:1.0.0-SNAPSHOT                          |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | Component version                                                |
      | propertyDefinition.type | version                                                          |
    And I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.SetNodePropertyAsInputOperation |
      | nodeName     | software_component                                                                   |
      | propertyName | component_version                                                                    |
      | inputName    | Component version                                                                    |
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.inputs.DeleteInputOperation |
      | inputName | Component version                                                        |
    Then No exception should be thrown
    And The SPEL int expression "inputs.size()" should return 0
    And The SPEL expression "nodeTemplates['software_component'].properties['component_version']" should return "null"

  Scenario: Remove an input that is associated to a node capability property should remove the association
    Given I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | Simple input                                                     |
      | propertyDefinition.type | string                                                           |

  Scenario: Remove an input that is associated to a relationship property should remove the association
    Given I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | Simple input                                                     |
      | propertyDefinition.type | string                                                           |
