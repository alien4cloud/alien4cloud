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
    And I should be able to retrieve a topology with name "topology_template_name1" version "0.1.0-SNAPSHOT" and store it as a SPEL context

  @reset
  Scenario: Create two versions of a topology template
    Given I create a new topology template with name "topology_template" and description "My topology template description1" and node templates
      | NodeTemplateCompute | tosca.nodes.Compute:1.0 |
    Then I should receive a RestResponse with no error
    And I should be able to retrieve a topology with name "topology_template" version "0.1.0-SNAPSHOT" and store it as a SPEL context
    And The SPEL int expression "nodeTemplates.size()" should return 1
    When I create a new topology template with name "topology_template" description "My topology template description1" and version "0.2.0-SNAPSHOT"
    Then I should receive a RestResponse with no error
    And I should be able to retrieve a topology with name "topology_template" version "0.2.0-SNAPSHOT" and store it as a SPEL context
    And The SPEL boolean expression "nodeTemplates == null" should return true

  @reset
  Scenario: Create an new topology template version based on another
    Given I create a new topology template with name "topology_template" and description "My topology template description1" and node templates
      | NodeTemplateCompute | tosca.nodes.Compute:1.0 |
    Then I should receive a RestResponse with no error
    When I create a new topology with name "topology_template" version "0.2.0-SNAPSHOT" based on the version "0.1.0-SNAPSHOT"
    Then the topology named "topology_template" should have 2 versions
    And I should be able to retrieve a topology with name "topology_template" version "0.2.0-SNAPSHOT" and store it as a SPEL context
    And The SPEL int expression "nodeTemplates.size()" should return 1
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | NodeTemplateJava                                                      |
      | indexedNodeTypeId | fastconnect.nodes.Java:1.0                                            |
    And I save the topology
    Then I should be able to retrieve a topology with name "topology_template" version "0.2.0-SNAPSHOT" and store it as a SPEL context
    And The SPEL int expression "nodeTemplates.size()" should return 2
    And I should be able to retrieve a topology with name "topology_template" version "0.1.0-SNAPSHOT" and store it as a SPEL context
    And The SPEL int expression "nodeTemplates.size()" should return 1

  @reset
  Scenario: Delete a topology template version
    Given I create a new topology template with name "topology_template" and description "My topology template description1" and node templates
      | NodeTemplateCompute | tosca.nodes.Compute:1.0 |
    Then I should receive a RestResponse with no error
    And I create a new topology with name "topology_template" version "0.2.0-SNAPSHOT" based on the version "0.1.0-SNAPSHOT"
    When I delete a CSAR with id "topology_template:0.1.0-SNAPSHOT"
    Then I should receive a RestResponse with no error
    And the topology named "topology_template" should have 1 versions
    And I should be able to retrieve a topology with name "topology_template" version "0.2.0-SNAPSHOT" and store it as a SPEL context
    And The SPEL int expression "nodeTemplates.size()" should return 1

  @reset
  Scenario: Can not update a versionned template
    Given I create a new topology template with name "topology_template" and description "My topology template description1" and node templates
      | NodeTemplateCompute | tosca.nodes.Compute:1.0 |
    Then I should receive a RestResponse with no error
    And I create a new topology with name "topology_template" version "0.1.0" based on the version "0.1.0-SNAPSHOT"
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
