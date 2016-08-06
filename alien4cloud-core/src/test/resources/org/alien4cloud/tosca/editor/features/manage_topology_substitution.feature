Feature: Topology editor: add substitution

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology template "TopologyTemplate1"

  Scenario: Set a topology template as substitute
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | topologyId  |                                                                                   |
      | elementId   | tosca.nodes.Compute                                                               |
    Then No exception should be thrown
    And The SPEL expression "substitutionMapping.substitutionType.elementId" should return "tosca.nodes.Compute"

  Scenario: Remove the topology template as substitute
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | topologyId  |                                                                                   |
      | elementId   | tosca.nodes.Compute                                                               |
    Then No exception should be thrown
    And The SPEL expression "substitutionMapping.substitutionType.elementId" should return "tosca.nodes.Compute"
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.RemoveSubstitutionTypeOperation |
      | topologyId  |                                                                                      |
    Then No exception should be thrown
    And The SPEL expression "substitutionMapping" should return "null"