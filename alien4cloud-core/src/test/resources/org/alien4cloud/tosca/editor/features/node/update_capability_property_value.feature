Feature: Topology editor: update node capability property value

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Updating a scalar property value of capability should succeed
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    When I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateCapabilityPropertyValueOperation |
      | nodeName       | compute                                                                                     |
      | capabilityName | host                                                                                        |
      | propertyName   | num_cpus                                                                                    |
      | propertyValue  | 10                                                                                          |
    Then No exception should be thrown
    And The SPEL expression "nodeTemplates['compute'].capabilities['host'].properties['num_cpus'].value" should return "10"

  Scenario: Updating a scalar property value of capability of wrong type should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    When I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateCapabilityPropertyValueOperation |
      | nodeName       | compute                                                                                     |
      | capabilityName | host                                                                                        |
      | propertyName   | num_cpus                                                                                    |
      | propertyValue  | AB                                                                                          |
    Then an exception of type "org.alien4cloud.tosca.editor.exception.PropertyValueException/org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException" should be thrown

  Scenario: Updating a scalar property value of capability with an unmatched constraint should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    When I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateCapabilityPropertyValueOperation |
      | nodeName       | compute                                                                                     |
      | capabilityName | host                                                                                        |
      | propertyName   | num_cpus                                                                                    |
      | propertyValue  | 0                                                                                           |
    Then an exception of type "org.alien4cloud.tosca.editor.exception.PropertyValueException/org.alien4cloud.tosca.exceptions.ConstraintViolationException" should be thrown

  Scenario: Updating a scalar property value of capability with a wrong name should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    When I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateCapabilityPropertyValueOperation |
      | nodeName       | compute                                                                                     |
      | capabilityName | host                                                                                        |
      | propertyName   | num_cpusFAILLED                                                                             |
      | propertyValue  | 0                                                                                           |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Updating a complex property value of capability should succeed
    Given I upload unzipped CSAR from path "src/test/resources/data/csars/capa_complex_props/capa_complex_props.yml"
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | CapaComplexProp                                                       |
      | indexedNodeTypeId | alien.test.nodes.CapaComplexProp:0.1-SNAPSHOT                         |
    When I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateCapabilityPropertyValueOperation |
      | nodeName       | CapaComplexProp                                                                             |
      | capabilityName | testCapa                                                                                    |
      | propertyName   | custom                                                                                      |
      | propertyValue  | {"name": "updated", "groups": ["updated_group"]}                                            |
    Then No exception should be thrown
    And The SPEL expression "nodeTemplates['CapaComplexProp'].capabilities['testCapa'].properties['custom'].value['name']" should return "updated"
    And The SPEL expression "nodeTemplates['CapaComplexProp'].capabilities['testCapa'].properties['custom'].value['groups'].size()" should return 1
    And The SPEL expression "nodeTemplates['CapaComplexProp'].capabilities['testCapa'].properties['custom'].value['groups'][0]" should return "updated_group"
