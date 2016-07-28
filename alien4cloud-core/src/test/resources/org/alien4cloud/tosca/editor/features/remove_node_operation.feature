Feature: Topology editor: nodes templates

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload CSAR from path "../../alien4cloud/target/it-artifacts/tosca-base-types-1.0.csar"
    And I upload CSAR from path "../../alien4cloud/target/it-artifacts/java-types-1.0.csar"
    And I create an empty topology template

#@Ignore
  Scenario: Remove a nodetemplate from a topology
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Template1                                                             |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type     | org.alien4cloud.tosca.editor.operations.nodetemplate.DeleteNodeOperation |
      | nodeName | Template1                                                                |
    Then No exception should be thrown
    And The SPEL int expression "nodeTemplates.size()" should return 0


#@Ignore
  Scenario: Remove a non existing nodetemplate from a topology should fail
    Given I build the operation: add a node template "Template1" related to the "tosca.nodes.Compute:1.0" node type
    And I execute the current operation on the current topology
    When I build the operation: delete a node template "Template0" from the topology
    And I execute the current operation on the current topology
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown