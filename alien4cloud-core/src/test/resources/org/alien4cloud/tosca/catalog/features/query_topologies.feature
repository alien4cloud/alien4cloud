#Feature: Tosca Catalog: query topologies
##
##  Background:
##    Given I am authenticated with "ADMIN" role
##
##  Scenario: Add a node that exists in the repository should succeed
##    When I execute the operation
##      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
##      | nodeName          | Template1                                                             |
##      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
##    Then No exception should be thrown
##    And The SPEL expression "nodeTemplates.size()" should return 1
##    And The SPEL expression "nodeTemplates['Template1'].type" should return "tosca.nodes.Compute"
