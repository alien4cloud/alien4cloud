Feature: Topology editor: capability substitution

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology


  #####################################
  ### Add
  #####################################

  Scenario: Add a capability substitution
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId   | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | Compute                                                                                     |
      | capabilityId             | host                                                                                        |
      | substitutionCapabilityId | host                                                                                        |
    And The SPEL expression "substitutionMapping.capabilities['host'].nodeTemplateName" should return "Compute"

  Scenario: Add a capability substitution on a wrong node should failed
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId   | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | Compute_failed                                                                              |
      | capabilityId             | host                                                                                        |
      | substitutionCapabilityId | host                                                                                        |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Add a capability substitution on a non substitute topology should failed
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | Compute                                                                                     |
      | capabilityId             | host                                                                                        |
      | substitutionCapabilityId | host                                                                                        |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Add a non existing capability as capability substitution should failed
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId   | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | Compute                                                                                     |
      | capabilityId             | host_failed                                                                                 |
      | substitutionCapabilityId | host                                                                                        |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Add capability as capability substitution with an already used substitutionCapabilityId should failed
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId   | tosca.nodes.Compute                                                               |
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
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | Compute                                                                                     |
      | capabilityId             | scalable                                                                                    |
      | substitutionCapabilityId | host                                                                                        |
    Then an exception of type "alien4cloud.exception.AlreadyExistException" should be thrown


  #####################################
  ### Remove
  #####################################

  Scenario: Remove a capability substitution
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId   | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | Compute                                                                                     |
      | capabilityId             | host                                                                                        |
      | substitutionCapabilityId | host                                                                                        |
    And The SPEL expression "substitutionMapping.capabilities['host'].nodeTemplateName" should return "Compute"
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.RemoveCapabilitySubstitutionTypeOperation |
      | substitutionCapabilityId | host                                                                                           |
    And The SPEL expression "substitutionMapping.capabilities['host']" should return "null"

  Scenario: Remove a non existing capability substitution when the topology is not an substitute should failed
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.RemoveCapabilitySubstitutionTypeOperation |
      | substitutionCapabilityId | host                                                                                           |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Remove a non existing capability substitution should failed
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId   | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.RemoveCapabilitySubstitutionTypeOperation |
      | substitutionCapabilityId | host                                                                                           |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Remove a non existing capability key substitution should failed
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId   | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | Compute                                                                                     |
      | capabilityId             | host                                                                                        |
      | substitutionCapabilityId | host                                                                                        |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.RemoveCapabilitySubstitutionTypeOperation |
      | substitutionCapabilityId | scalable                                                                                       |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown


  #####################################
  ### Update
  #####################################

  Scenario: Update a capability substitution
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId   | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | Compute                                                                                     |
      | capabilityId             | host                                                                                        |
      | substitutionCapabilityId | host                                                                                        |
    And The SPEL expression "substitutionMapping.capabilities['host'].nodeTemplateName" should return "Compute"
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.UpdateCapabilitySubstitutionTypeOperation |
      | substitutionCapabilityId | host                                                                                           |
      | newCapabilityId          | host_bis                                                                                       |
    And The SPEL expression "substitutionMapping.capabilities['host_bis'].nodeTemplateName" should return "Compute"

  Scenario: Update a non existing capability substitution when the topology is not an substitute should failed
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.UpdateCapabilitySubstitutionTypeOperation |
      | substitutionCapabilityId | host                                                                                           |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Update a non existing capability substitution should failed
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId   | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.UpdateCapabilitySubstitutionTypeOperation |
      | substitutionCapabilityId | host                                                                                           |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Update a non existing capability key substitution should failed
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId   | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | Compute                                                                                     |
      | capabilityId             | host                                                                                        |
      | substitutionCapabilityId | host                                                                                        |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.UpdateCapabilitySubstitutionTypeOperation |
      | substitutionCapabilityId | scalable                                                                                       |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Update capability key to an already used key substitution should failed
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId   | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | Compute                                                                                     |
      | capabilityId             | host                                                                                        |
      | substitutionCapabilityId | host                                                                                        |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | Compute                                                                                     |
      | capabilityId             | scalable                                                                                        |
      | substitutionCapabilityId | scalable                                                                                        |
    When I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.UpdateCapabilitySubstitutionTypeOperation |
      | substitutionCapabilityId | scalable                                                                                       |
      | newCapabilityId          | host                                                                                           |
    Then an exception of type "alien4cloud.exception.AlreadyExistException" should be thrown