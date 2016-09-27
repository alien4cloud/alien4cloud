Feature: Topology editor: add substitution

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology template "TopologyTemplate1"

  Scenario: Set a topology template as substitute
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId   | tosca.nodes.Compute                                                               |
    Then No exception should be thrown
    And The SPEL expression "substitutionMapping.substitutionType.elementId" should return "tosca.nodes.Compute"
    When I save the topology
    Then I should be able to find a component with id "TopologyTemplate1:0.1.0-SNAPSHOT"

#  Scenario: Set a topology as substitute should failed
#    When I create an application "application"
#    And I execute the operation
#      | type      | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
#      | elementId | tosca.nodes.Compute                                                               |
#    Then an exception of type "java.lang.UnsupportedOperationException" should be thrown

  Scenario: Remove the topology template as substitute
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId   | tosca.nodes.Compute                                                               |
    Then No exception should be thrown
    And The SPEL expression "substitutionMapping.substitutionType.elementId" should return "tosca.nodes.Compute"
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.RemoveSubstitutionTypeOperation |
    Then No exception should be thrown
    And The SPEL expression "substitutionMapping" should return "null"
    When I save the topology
    Then I should not be able to find a component with id "TopologyTemplate1:0.1.0-SNAPSHOT"

  Scenario: Remove the substitute value of a non substitutable topology template should failed
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.RemoveSubstitutionTypeOperation |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Remove a substitutable topology template used in a topology should failed
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId | tosca.nodes.Compute                                                               |
    Then No exception should be thrown
    And The SPEL expression "substitutionMapping.substitutionType.elementId" should return "tosca.nodes.Compute"
    And I save the topology
    And I create an empty topology
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | TopologyTemplate1                                                     |
      | indexedNodeTypeId | TopologyTemplate1:0.1.0-SNAPSHOT                                      |
    And I save the topology
    When I execute the operation on the topology number 0
      | type | org.alien4cloud.tosca.editor.operations.substitution.RemoveSubstitutionTypeOperation |
    Then an exception of type "alien4cloud.exception.DeleteReferencedObjectException" should be thrown

