Feature: Topology editor: capability service relationship

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Set a capability substitution service relationship
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | Compute                                                                                     |
      | capabilityId             | host                                                                                        |
      | substitutionCapabilityId | host                                                                                        |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.SetSubstitutionCapabilityServiceRelationshipOperation |
      | substitutionCapabilityId | host                                                                                                       |
      | relationshipType         | tosca.relationships.HostedOn                                                                               |
      | relationshipVersion      | 1.0                                                                                                        |
    Then No exception should be thrown
    And The SPEL expression "substitutionMapping.capabilities['host'].serviceRelationshipType" should return "tosca.relationships.HostedOn"

  Scenario: Set a capability substitution service relationship when no substitution should fail
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.SetSubstitutionCapabilityServiceRelationshipOperation |
      | substitutionCapabilityId | host                                                                                                       |
      | relationshipType         | tosca.relationships.HostedOn                                                                               |
      | relationshipVersion      | 1.0                                                                                                        |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Set a capability substitution service relationship on an unexposed capability should fail
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.SetSubstitutionCapabilityServiceRelationshipOperation |
      | substitutionCapabilityId | host                                                                                                       |
      | relationshipType         | tosca.relationships.HostedOn                                                                               |
      | relationshipVersion      | 1.0                                                                                                        |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Set a capability substitution service relationship with a missing relationship type should fail
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | Compute                                                                                     |
      | capabilityId             | host                                                                                        |
      | substitutionCapabilityId | host                                                                                        |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.SetSubstitutionCapabilityServiceRelationshipOperation |
      | substitutionCapabilityId | host                                                                                                       |
      | relationshipType         | tosca.relationships.UnknownRelationship                                                                    |
      | relationshipVersion      | 1.0                                                                                                        |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Reset a capability substitution service relationship
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | Compute                                                                                     |
      | capabilityId             | host                                                                                        |
      | substitutionCapabilityId | host                                                                                        |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.SetSubstitutionCapabilityServiceRelationshipOperation |
      | substitutionCapabilityId | host                                                                                                       |
      | relationshipType         | tosca.relationships.HostedOn                                                                               |
      | relationshipVersion      | 1.0                                                                                                        |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.SetSubstitutionCapabilityServiceRelationshipOperation |
      | substitutionCapabilityId | host                                                                                                       |
      | relationshipType         |                                                                                                            |
    Then No exception should be thrown
    And The SPEL expression "substitutionMapping.capabilities['host'].serviceRelationshipType" should return "null"


  Scenario: Reset a capability substitution service relationship with null
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | Compute                                                                                     |
      | capabilityId             | host                                                                                        |
      | substitutionCapabilityId | host                                                                                        |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.SetSubstitutionCapabilityServiceRelationshipOperation |
      | substitutionCapabilityId | host                                                                                                       |
      | relationshipType         | tosca.relationships.HostedOn                                                                               |
      | relationshipVersion      | 1.0                                                                                                        |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.SetSubstitutionCapabilityServiceRelationshipOperation |
      | substitutionCapabilityId | host                                                                                                       |
    Then No exception should be thrown
    And The SPEL expression "substitutionMapping.capabilities['host'].serviceRelationshipType" should return "null"