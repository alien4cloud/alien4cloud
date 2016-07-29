Feature: Topology editor: nodes templates

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload CSAR from path "../../alien4cloud/target/it-artifacts/tosca-normative-types-1.0.0-SNAPSHOT.csar"
    And I create an empty topology template

  Scenario: Updating a scalar property value should succeed
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | software_component                                                    |
      | indexedNodeTypeId | tosca.nodes.SoftwareComponent:1.0.0-SNAPSHOT                          |
    When I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodePropertyValueOperation |
      | nodeName      | software_component                                                                    |
      | propertyName  | component_version                                                                     |
      | propertyValue | 1.2.0                                                                                 |
    Then No exception should be thrown
    And The SPEL expression "nodeTemplates['software_component'].properties['component_version'].value" should return "1.2.0"

  Scenario: Updating a scalar property value of wrong type should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | software_component                                                    |
      | indexedNodeTypeId | tosca.nodes.SoftwareComponent:1.0.0-SNAPSHOT                          |
    When I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodePropertyValueOperation |
      | nodeName      | software_component                                                                    |
      | propertyName  | component_version                                                                     |
      | propertyValue | toto                                                                                  |
    Then an exception of type "org.alien4cloud.tosca.editor.exception.PropertyValueException/alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException" should be thrown

  Scenario: Updating a scalar property value with an unmatched constraint should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | software_component                                                    |
      | indexedNodeTypeId | tosca.nodes.Network:1.0.0-SNAPSHOT                                    |
    When I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodePropertyValueOperation |
      | nodeName      | software_component                                                                    |
      | propertyName  | ip_version                                                                            |
      | propertyValue | 2                                                                                     |
    Then an exception of type "org.alien4cloud.tosca.editor.exception.PropertyValueException/alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException" should be thrown
