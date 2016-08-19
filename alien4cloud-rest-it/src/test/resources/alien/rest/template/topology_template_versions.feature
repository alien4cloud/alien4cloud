Feature: Create topology template versions

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca base types 1.0"
    And I should receive a RestResponse with no error
    And I upload the archive "sample java types 1.0"
    And I should receive a RestResponse with no error
    And There is a "node type" with element name "tosca.nodes.Compute" and archive version "1.0"
    And There is a "node type" with element name "fastconnect.nodes.Java" and archive version "1.0"
    And There is a "mairon" user in the system
    And I add a role "ARCHITECT" to user "mairon"
    And I am authenticated with user named "mairon"

  @reset
  Scenario: Create an first topology template with default version number
    When I create a new topology template with name "topology_template_name1" and description "My topology template description1"
    Then I should receive a RestResponse with no error
    And If I search for topology templates I can find one with the name "topology_template_name1" version "0.1.0-SNAPSHOT" and store the related topology as a SPEL context

  @reset
  Scenario: Create an new topology template version
    Given I create a new topology template with name "topology_template" and description "My topology template description1" and node templates
      | NodeTemplateCompute | tosca.nodes.Compute:1.0 |
    Then I should receive a RestResponse with no error
    When I create a new topology template version named "0.2.0-SNAPSHOT"
    Then If I search for topology templates I can find one with the name "topology_template" version "0.2.0-SNAPSHOT" and store the related topology as a SPEL context
    And The SPEL boolean expression "nodeTemplates == null" should return true

  @reset
  Scenario: Create an new topology template version based on another
    Given I create a new topology template with name "topology_template" and description "My topology template description1" and node templates
      | NodeTemplateCompute | tosca.nodes.Compute:1.0 |
    Then I should receive a RestResponse with no error
    When I create a new topology template version named "0.2.0-SNAPSHOT" based on the current version
    Then the topology template named "topology_template" should have 2 versions
    And If I search for topology templates I can find one with the name "topology_template" version "0.2.0-SNAPSHOT" and store the related topology as a SPEL context
    And The SPEL int expression "nodeTemplates.size()" should return 1
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | NodeTemplateJava                                                      |
      | indexedNodeTypeId | fastconnect.nodes.Java:1.0                                            |
    And I save the topology
    Then If I search for topology templates I can find one with the name "topology_template" version "0.2.0-SNAPSHOT" and store the related topology as a SPEL context
    And The SPEL int expression "nodeTemplates.size()" should return 2
    And If I search for topology templates I can find one with the name "topology_template" version "0.1.0-SNAPSHOT" and store the related topology as a SPEL context
    And The SPEL int expression "nodeTemplates.size()" should return 1

  @reset
  Scenario: Delete a topology template version
    Given I create a new topology template with name "topology_template" and description "My topology template description1" and node templates
      | NodeTemplateCompute | tosca.nodes.Compute:1.0 |
    Then I should receive a RestResponse with no error
    And I create a new topology template version named "0.2.0-SNAPSHOT" based on the current version
    When I delete the topology template named "topology_template" version "0.1.0-SNAPSHOT"
    Then I should receive a RestResponse with no error
    And the topology template named "topology_template" should have 1 versions
    And If I search for topology templates I can find one with the name "topology_template" version "0.2.0-SNAPSHOT" and store the related topology as a SPEL context
    When I delete the topology template named "topology_template" version "0.2.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 610
    And the topology template named "topology_template" should have 1 versions
    When I delete the topology template named "topology_template"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Can not update a versionned template
    Given I create a new topology template with name "topology_template" and description "My topology template description1" and node templates
      | NodeTemplateCompute | tosca.nodes.Compute:1.0 |
    Then I should receive a RestResponse with no error
    And I create a new topology template version named "0.1.0" based on the current version
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | NodeTemplateJava                                                      |
      | indexedNodeTypeId | fastconnect.nodes.Java:1.0                                            |
    Then I should receive a RestResponse with an error code 807
    When I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | disk_size                                                        |
      | propertyDefinition.type | integer                                                          |
    Then I should receive a RestResponse with an error code 807
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId | tosca.nodes.Root                                                                  |
    Then I should receive a RestResponse with an error code 807
    When I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.groups.AddGroupMemberOperation |
      | nodeName  | NodeTemplateCompute                                                    |
      | groupName | HA_group                                                               |
    Then I should receive a RestResponse with an error code 807
