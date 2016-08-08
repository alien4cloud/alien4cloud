Feature: Topology editor: requirement substitution

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology template "TopologyTemplate1"


  #####################################
  ### Add
  #####################################

  Scenario: Add a requirement substitution
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId   | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.AddRequirementSubstitutionTypeOperation |
      | nodeTemplateName          | Compute                                                                                      |
      | requirementId             | network                                                                                      |
      | substitutionRequirementId | network                                                                                      |
    And The SPEL expression "substitutionMapping.requirements['network'].nodeTemplateName" should return "Compute"

  Scenario: Add a non existing requirement as requirement substitution should failed
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId   | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.AddRequirementSubstitutionTypeOperation |
      | nodeTemplateName          | Compute                                                                                      |
      | requirementId             | network_failed                                                                               |
      | substitutionRequirementId | network                                                                                      |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Add requirement as requirement substitution with an already used substitutionRequirementId should failed
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId   | tosca.nodes.Compute                                                               |
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
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.AddRequirementSubstitutionTypeOperation |
      | nodeTemplateName          | Compute                                                                                      |
      | requirementId             | dependency                                                                                   |
      | substitutionRequirementId | network                                                                                      |
    Then an exception of type "alien4cloud.exception.AlreadyExistException" should be thrown

  Scenario: Add a requirement substitution on a wrong node should failed
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId   | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.AddRequirementSubstitutionTypeOperation |
      | nodeTemplateName          | Compute_failed                                                                               |
      | requirementId             | network                                                                                         |
      | substitutionRequirementId | network                                                                                         |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Add a requirement substitution on a non substitute topology should failed
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.AddRequirementSubstitutionTypeOperation |
      | nodeTemplateName          | Compute                                                                                      |
      | requirementId             | network                                                                                      |
      | substitutionRequirementId | network                                                                                      |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown


  #####################################
  ### Remove
  #####################################

  Scenario: Remove a requirement substitution
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId   | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.AddRequirementSubstitutionTypeOperation |
      | nodeTemplateName          | Compute                                                                                      |
      | requirementId             | network                                                                                      |
      | substitutionRequirementId | network                                                                                      |
    And The SPEL expression "substitutionMapping.requirements['network'].nodeTemplateName" should return "Compute"
    When I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.RemoveRequirementSubstitutionTypeOperation |
      | substitutionRequirementId | network                                                                                         |
    And The SPEL expression "substitutionMapping.requirements['network']" should return "null"

  Scenario: Remove a non existing requirement substitution should failed
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId   | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.RemoveRequirementSubstitutionTypeOperation |
      | substitutionRequirementId | network                                                                                         |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Remove a non existing requirement substitution when the topology is not an substitute should failed
    When I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.RemoveRequirementSubstitutionTypeOperation   |
      | substitutionRequirementId | network                                                                                           |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Remove a non existing requirement key substitution should failed
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId   | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.AddRequirementSubstitutionTypeOperation |
      | nodeTemplateName          | Compute                                                                                      |
      | requirementId             | network                                                                                      |
      | substitutionRequirementId | network                                                                                      |
    When I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.RemoveRequirementSubstitutionTypeOperation |
      | substitutionRequirementId | dependency                                                                                      |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown


  #####################################
  ### Update
  #####################################

  Scenario: Update a requirement substitution
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId   | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.AddRequirementSubstitutionTypeOperation |
      | nodeTemplateName          | Compute                                                                                      |
      | requirementId             | network                                                                                      |
      | substitutionRequirementId | network                                                                                      |
    And The SPEL expression "substitutionMapping.requirements['network'].nodeTemplateName" should return "Compute"
    When I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.UpdateRequirementSubstitutionTypeOperation |
      | substitutionRequirementId | network                                                                                         |
      | newRequirementId          | network_bis                                                                                     |
    And The SPEL expression "substitutionMapping.requirements['network_bis'].nodeTemplateName" should return "Compute"

  Scenario: Update a non existing requirement substitution when the topology is not an substitute should failed
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.UpdateRequirementSubstitutionTypeOperation |
      | substitutionRequirementId | network                                                                                           |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Update a non existing requirement substitution should failed
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId   | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.UpdateRequirementSubstitutionTypeOperation |
      | substitutionRequirementId | network                                                                                           |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Update a non existing requirement key substitution should failed
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId   | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddRequirementSubstitutionTypeOperation |
      | nodeTemplateName         | Compute                                                                                     |
      | requirementId             | network                                                                                        |
      | substitutionRequirementId | network                                                                                        |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.UpdateRequirementSubstitutionTypeOperation |
      | substitutionRequirementId | dependency                                                                                       |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Update requirement key to an already used key substitution should failed
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId   | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddRequirementSubstitutionTypeOperation |
      | nodeTemplateName         | Compute                                                                                     |
      | requirementId             | network                                                                                        |
      | substitutionRequirementId | network                                                                                        |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddRequirementSubstitutionTypeOperation |
      | nodeTemplateName         | Compute                                                                                     |
      | requirementId             | dependency                                                                                        |
      | substitutionRequirementId | dependency                                                                                        |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.UpdateRequirementSubstitutionTypeOperation |
      | substitutionRequirementId | dependency                                                                                       |
      | newRequirementId          | network                                                                                           |
    Then an exception of type "alien4cloud.exception.AlreadyExistException" should be thrown