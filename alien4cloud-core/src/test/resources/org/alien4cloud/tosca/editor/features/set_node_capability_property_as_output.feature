Feature: Topology editor: set node capability's property as output

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Set a node capability's property as output should succeed
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | compute                                                    			  |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                         			  |
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.SetNodeCapabilityPropertyAsOutputOperation |
      | nodeName     | compute                                                                   |
      | capabilityName     | scalable                                                                   |
      | propertyName | max_instances                                                                    |
    Then No exception should be thrown
    And The SPEL int expression "outputCapabilityProperties.size()" should return 1
    And The SPEL int expression "outputCapabilityProperties['compute'].size()" should return 1
    And The SPEL int expression "outputCapabilityProperties['compute']['scalable'].size()" should return 1
    And The SPEL int expression "outputCapabilityProperties['compute']['scalable'].?[#this == 'max_instances'].size()" should return 1
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.SetNodeCapabilityPropertyAsOutputOperation |
      | nodeName     | compute                                                                   |
      | capabilityName     | scalable                                                                   |
      | propertyName | min_instances                                                                    |
    Then No exception should be thrown
    And The SPEL int expression "outputCapabilityProperties.size()" should return 1
    And The SPEL int expression "outputCapabilityProperties['compute'].size()" should return 1
    And The SPEL int expression "outputCapabilityProperties['compute']['scalable'].size()" should return 2
    And The SPEL int expression "outputCapabilityProperties['compute']['scalable'].?[#this == 'min_instances'].size()" should return 1

  Scenario: Set as output a property of a capability that doesn't exists in a node should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | compute                                                    			  |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                         			  |
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.SetNodeCapabilityPropertyAsOutputOperation |
      | nodeName     | compute                                                                   |
      | capabilityName     | i_do_not_exist                                                                   |
      | propertyName | max_instances                                                                    |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown
    When I get the edited topology
    Then The SPEL expression "outputCapabilityProperties" should return "null"

  Scenario: Set as output a capability's property that doesn't exists in a node should fail
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | compute                                                    			  |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                         			  |
    When I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.SetNodeCapabilityPropertyAsOutputOperation |
      | nodeName     | compute                                                                   |
      | capabilityName     | scalable                                                                   |
      | propertyName | i_do_not_exist                                                                    |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown
    When I get the edited topology
    Then The SPEL expression "outputCapabilityProperties" should return "null"

