Feature: Topology editor: requirement service relationship

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Set a requirement substitution service relationship
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.AddRequirementSubstitutionTypeOperation |
      | nodeTemplateName          | Compute                                                                                      |
      | requirementId             | network                                                                                      |
      | substitutionRequirementId | network                                                                                      |
    When I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.SetSubstitutionRequirementServiceRelationshipOperation |
      | substitutionRequirementId | network                                                                                                     |
      | relationshipType          | tosca.relationships.Network                                                                                 |
      | relationshipVersion       | 1.0                                                                                                         |
    Then No exception should be thrown
    And The SPEL expression "substitutionMapping.requirements['network'].serviceRelationshipType" should return "tosca.relationships.Network"

  Scenario: Set a requirement substitution service relationship when no substitution should fail
    When I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.SetSubstitutionRequirementServiceRelationshipOperation |
      | substitutionRequirementId | network                                                                                                     |
      | relationshipType          | tosca.relationships.Network                                                                                 |
      | relationshipVersion       | 1.0                                                                                                         |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Set a requirement substitution service relationship on an unexposed capability should fail
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.SetSubstitutionRequirementServiceRelationshipOperation |
      | substitutionRequirementId | network                                                                                                     |
      | relationshipType          | tosca.relationships.Network                                                                                 |
      | relationshipVersion       | 1.0                                                                                                         |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Set a requirement substitution service relationship with a missing relationship type should fail
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.AddRequirementSubstitutionTypeOperation |
      | nodeTemplateName          | Compute                                                                                      |
      | requirementId             | network                                                                                      |
      | substitutionRequirementId | network                                                                                      |
    When I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.SetSubstitutionRequirementServiceRelationshipOperation |
      | substitutionRequirementId | network                                                                                                     |
      | relationshipType          | tosca.relationships.UnknownRelationship                                                                     |
      | relationshipVersion       | 1.0                                                                                                         |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Reset a requirement substitution service relationship
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.AddRequirementSubstitutionTypeOperation |
      | nodeTemplateName          | Compute                                                                                      |
      | requirementId             | network                                                                                      |
      | substitutionRequirementId | network                                                                                      |
    When I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.SetSubstitutionRequirementServiceRelationshipOperation |
      | substitutionRequirementId | network                                                                                                     |
      | relationshipType          |                                                                                                             |
    Then No exception should be thrown
    And The SPEL expression "substitutionMapping.requirements['network'].serviceRelationshipType" should return "null"

  Scenario: Reset a requirement substitution service relationship with null
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.AddRequirementSubstitutionTypeOperation |
      | nodeTemplateName          | Compute                                                                                      |
      | requirementId             | network                                                                                      |
      | substitutionRequirementId | network                                                                                      |
    When I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.SetSubstitutionRequirementServiceRelationshipOperation |
      | substitutionRequirementId | network                                                                                                     |
    Then No exception should be thrown
    And The SPEL expression "substitutionMapping.requirements['network'].serviceRelationshipType" should return "null"