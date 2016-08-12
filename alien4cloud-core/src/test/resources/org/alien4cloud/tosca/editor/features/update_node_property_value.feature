Feature: Topology editor: update node property value

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

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

  Scenario: Updating a property value for a property that does not exists should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | software_component                                                    |
      | indexedNodeTypeId | tosca.nodes.SoftwareComponent:1.0.0-SNAPSHOT                          |
    When I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodePropertyValueOperation |
      | nodeName      | software_component                                                                    |
      | propertyName  | unknown_property                                                                      |
      | propertyValue | 1.2.0                                                                                 |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

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
