Feature: CSAR upload with topology

  Background:
    Given I am authenticated with "ADMIN" role

  # In this file, some scenari are duplicated and prefixed with 'Re-'
  # The aim for such kind of duplicated scenario is to test the export YAML/TOSCA feature like this :
  # - a first scenario tests the import feature (a CSAR s upladed, then the topology is explored in order to check that it looks as expected)
  # - the 'Re-' scenario upload the same CSAR and then export the YAML
  # - a new CSAR is created with this exported YAML
  # - the built CSAR is imported and the whole test is replayed
  # By this way, we ensure that the imported then exported topology contain the same items.
  # The scenari that test failure cases are not duplicated.

  @reset
  Scenario: Upload CSAR containing apache types and embeded topology template
    Given I upload the archive "tosca base types 1.0"
    When I upload the archive "topology apache"
    Then I should receive a RestResponse with 2 alerts in 1 files : 0 errors 1 warnings and 1 infos
    And I should be able to retrieve a topology with name "apache-type" and store it as a SPEL context
    And The SPEL expression "dependencies.^[name == 'tosca-base-types'].version" should return "1.0"
    And The SPEL int expression "nodeTemplates.size()" should return 2
    And The SPEL expression "nodeTemplates['compute'].type" should return "tosca.nodes.Compute"
    And The SPEL int expression "nodeTemplates['compute'].properties.size()" should return 8
    And The SPEL expression "nodeTemplates['compute'].properties['os_distribution'].value" should return "ubuntu"
    And The SPEL expression "nodeTemplates['compute'].properties['os_type'].value" should return "linux"
    And The SPEL int expression "nodeTemplates['compute'].attributes.size()" should return 3
    And The SPEL boolean expression "nodeTemplates['compute'].attributes.containsKey('ip_address')" should return true
    And The SPEL boolean expression "nodeTemplates['compute'].attributes.containsKey('tosca_id')" should return true
    And The SPEL boolean expression "nodeTemplates['compute'].attributes.containsKey('tosca_name')" should return true
    And The SPEL int expression "nodeTemplates['compute'].capabilities.size()" should return 4
    And The SPEL boolean expression "nodeTemplates['compute'].capabilities.containsKey('feature')" should return true
    And The SPEL boolean expression "nodeTemplates['compute'].capabilities.containsKey('compute')" should return true
    And The SPEL boolean expression "nodeTemplates['compute'].capabilities.containsKey('scalable')" should return true
    And The SPEL int expression "nodeTemplates['compute'].requirements.size()" should return 2
    And The SPEL boolean expression "nodeTemplates['compute'].requirements.containsKey('dependency')" should return true
    And The SPEL boolean expression "nodeTemplates['compute'].requirements.containsKey('network')" should return true
    And The SPEL int expression "nodeTemplates['apache'].relationships.size()" should return 1
    And The SPEL expression "nodeTemplates['apache'].relationships['hostedOnCompute'].type" should return "tosca.relationships.HostedOn"
    And The SPEL expression "nodeTemplates['apache'].relationships['hostedOnCompute'].target" should return "compute"
    And The SPEL expression "nodeTemplates['apache'].relationships['hostedOnCompute'].requirementName" should return "host"
    And The SPEL expression "nodeTemplates['apache'].relationships['hostedOnCompute'].requirementType" should return "tosca.capabilities.Container"
    And The SPEL expression "nodeTemplates['apache'].relationships['hostedOnCompute'].targetedCapabilityName" should return "compute"
    And The SPEL int expression "nodeTemplates['apache'].relationships['hostedOnCompute'].properties.size()" should return 2
    And The SPEL expression "nodeTemplates['apache'].relationships['hostedOnCompute'].properties['password'].value" should return "mypassword"
    # this node has inherited capabilities
    And The SPEL int expression "nodeTemplates['apache'].capabilities.size()" should return 2
#
#  @reset
#  Scenario: Re-Upload CSAR containing apache types and embeded topology template
#    Given I upload the archive "tosca base types 1.0"
#    And I upload the archive "topology apache"
#    And I export the YAML from topology template named "apache-type" and build a test dataset named "apache-type-replay" changing the version from "1.1.0-SNAPSHOT" to "1.2.0-SNAPSHOT"
#    When I upload the archive "apache-type-replay"
#    Then I should receive a RestResponse with 1 alerts in 1 files : 0 errors 0 warnings and 1 infos
#    And I should be able to retrieve a topology with name "apache-type" version "1.2.0-SNAPSHOT" and store it as a SPEL context
#    And The SPEL expression "dependencies.^[name == 'tosca-base-types'].version" should return "1.0"
#    And The SPEL int expression "nodeTemplates.size()" should return 2
#    And The SPEL expression "nodeTemplates['compute'].type" should return "tosca.nodes.Compute"
#    And The SPEL int expression "nodeTemplates['compute'].properties.size()" should return 8
#    And The SPEL expression "nodeTemplates['compute'].properties['os_distribution'].value" should return "ubuntu"
#    And The SPEL expression "nodeTemplates['compute'].properties['os_type'].value" should return "linux"
#    And The SPEL int expression "nodeTemplates['compute'].attributes.size()" should return 3
#    And The SPEL boolean expression "nodeTemplates['compute'].attributes.containsKey('ip_address')" should return true
#    And The SPEL boolean expression "nodeTemplates['compute'].attributes.containsKey('tosca_id')" should return true
#    And The SPEL boolean expression "nodeTemplates['compute'].attributes.containsKey('tosca_name')" should return true
#    And The SPEL int expression "nodeTemplates['compute'].capabilities.size()" should return 4
#    And The SPEL boolean expression "nodeTemplates['compute'].capabilities.containsKey('feature')" should return true
#    And The SPEL boolean expression "nodeTemplates['compute'].capabilities.containsKey('compute')" should return true
#    And The SPEL boolean expression "nodeTemplates['compute'].capabilities.containsKey('scalable')" should return true
#    And The SPEL boolean expression "nodeTemplates['compute'].requirements.size() == 2" should return true
#    And The SPEL boolean expression "nodeTemplates['compute'].requirements.containsKey('dependency')" should return true
#    And The SPEL boolean expression "nodeTemplates['compute'].requirements.containsKey('network')" should return true
#    And The SPEL int expression "nodeTemplates['apache'].relationships.size()" should return 1
#    And The SPEL expression "nodeTemplates['apache'].relationships['hostedOnCompute'].type" should return "tosca.relationships.HostedOn"
#    And The SPEL expression "nodeTemplates['apache'].relationships['hostedOnCompute'].target" should return "compute"
#    And The SPEL expression "nodeTemplates['apache'].relationships['hostedOnCompute'].requirementName" should return "host"
#    And The SPEL expression "nodeTemplates['apache'].relationships['hostedOnCompute'].requirementType" should return "tosca.capabilities.Container"
#    And The SPEL expression "nodeTemplates['apache'].relationships['hostedOnCompute'].targetedCapabilityName" should return "compute"
#    And The SPEL int expression "nodeTemplates['apache'].relationships['hostedOnCompute'].properties.size()" should return 2
#    And The SPEL expression "nodeTemplates['apache'].relationships['hostedOnCompute'].properties['password'].value" should return "mypassword"
#
  @reset
  Scenario: Upload CSAR containing cutom types and embeded topology template using short notation for requirements
    Given I upload the archive "tosca base types 1.0"
    When I upload the archive "topology custom types"
    Then I should receive a RestResponse with 1 alerts in 1 files : 0 errors 0 warnings and 1 infos
    And I should be able to retrieve a topology with name "AllInclusiveArchive" and store it as a SPEL context
    And The SPEL expression "dependencies.^[name == 'AllInclusiveArchive'].version" should return "1.0.0-SNAPSHOT"
    And The SPEL expression "dependencies.^[name == 'tosca-base-types'].version" should return "1.0"
    And The SPEL int expression "nodeTemplates['software'].relationships.size()" should return 1
    And The SPEL expression "nodeTemplates['software'].relationships['myRelationTypeCompute'].type" should return "custom.relationships.MyRelationType"
    And The SPEL expression "nodeTemplates['software'].relationships['myRelationTypeCompute'].target" should return "compute"
    And The SPEL expression "nodeTemplates['software'].relationships['myRelationTypeCompute'].requirementName" should return "host"
    And The SPEL expression "nodeTemplates['software'].relationships['myRelationTypeCompute'].requirementType" should return "custom.capabilities.MyCapability"
    And The SPEL expression "nodeTemplates['software'].relationships['myRelationTypeCompute'].targetedCapabilityName" should return "host"
#
#  @reset
#  Scenario: Re-Upload CSAR containing cutom types and embeded topology template using short notation for requirements
#    Given I upload the archive "tosca base types 1.0"
#    And I upload the archive "topology custom types"
#    And I export the YAML from topology template named "AllInclusiveArchive" and build a test dataset named "AllInclusiveArchive-replay" changing the version from "1.0.0-SNAPSHOT" to "2.0.0-SNAPSHOT"
#    When I upload the archive "AllInclusiveArchive-replay"
#    Then I should receive a RestResponse with 1 alerts in 1 files : 0 errors 0 warnings and 1 infos
#    And I should be able to retrieve a topology with name "AllInclusiveArchive" version "2.0.0-SNAPSHOT" and store it as a SPEL context
#    And The SPEL expression "dependencies.^[name == 'AllInclusiveArchive'].version" should return "1.0.0-SNAPSHOT"
#    And The SPEL expression "dependencies.^[name == 'tosca-base-types'].version" should return "1.0"
#    And The SPEL int expression "nodeTemplates['software'].relationships.size()" should return 1
#    And The SPEL expression "nodeTemplates['software'].relationships['myRelationTypeCompute'].type" should return "custom.relationships.MyRelationType"
#    And The SPEL expression "nodeTemplates['software'].relationships['myRelationTypeCompute'].target" should return "compute"
#    And The SPEL expression "nodeTemplates['software'].relationships['myRelationTypeCompute'].requirementName" should return "host"
#    And The SPEL expression "nodeTemplates['software'].relationships['myRelationTypeCompute'].requirementType" should return "custom.capabilities.MyCapability"
#    And The SPEL expression "nodeTemplates['software'].relationships['myRelationTypeCompute'].targetedCapabilityName" should return "host"
#
#  @reset
#  Scenario: Upload twice a CSAR SNAPSHOT containing embeded topology template result in 2 topology templates
#    Given I upload the archive "tosca base types 1.0"
#    And I upload the archive "topology custom types"
#    When I upload the archive "topology custom types"
#    Then I should receive a RestResponse with 1 alerts in 1 files : 0 errors 0 warnings and 1 infos
#    And I should be able to retrieve a topology with name "AllInclusiveArchive" and store it as a SPEL context
#    And I should be able to retrieve a topology with name "AllInclusiveArchive" and store it as a SPEL context
#
  @reset
  Scenario: Upload a CSAR containing topology with a node referencing a unexisting type
    Given I upload the archive "tosca base types 1.0"
    When I upload the archive "topology-error-missingtype"
    Then I should receive a RestResponse with 1 alerts in 1 files : 1 errors 0 warnings and 0 infos

  @reset
  Scenario: Upload a CSAR containing topology with an unkown capability
    Given I upload the archive "tosca base types 1.0"
    When I upload the archive "topology-unknown-capability"
    Then I should receive a RestResponse with 1 alerts in 1 files : 1 errors 0 warnings and 0 infos
#
  @reset
  Scenario: Upload a CSAR containing topology with an unkown capability, short requirement notation
    Given I upload the archive "tosca base types 1.0"
    When I upload the archive "topology-unknown-capability-short-notation"
    Then I should receive a RestResponse with 1 alerts in 1 files : 1 errors 0 warnings and 0 infos

  @reset
  Scenario: Upload a CSAR containing topology with an unkown relationship type
    Given I upload the archive "tosca base types 1.0"
    When I upload the archive "topology-unknown-relationshiptype"
    Then I should receive a RestResponse with 1 alerts in 1 files : 1 errors 0 warnings and 0 infos

  @reset
  Scenario: Upload a CSAR containing topology with an unknown requirement target
    Given I upload the archive "tosca base types 1.0"
    When I upload the archive "topology-unknown-reqtarget"
    Then I should receive a RestResponse with 1 alerts in 1 files : 1 errors 0 warnings and 0 infos

  @reset
  Scenario: Upload a CSAR containing topology with an incorrect requirement name
    Given I upload the archive "tosca base types 1.0"
    When I upload the archive "topology-unknown-req"
    Then I should receive a RestResponse with 1 alerts in 1 files : 1 errors 0 warnings and 0 infos

  @reset
  Scenario: Upload CSAR containing embeded topology template with inputs
    Given I upload the archive "tosca base types 1.0"
    When I upload the archive "topology_inputs"
    Then I should receive a RestResponse with 1 alerts in 1 files : 0 errors 0 warnings and 1 infos
    And I should be able to retrieve a topology with name "topology-inputs" and store it as a SPEL context
    And The SPEL expression "inputs['os_type'].type" should return "string"
    And The SPEL int expression "inputs['os_type'].constraints[0].validValues.size()" should return 4
    And The SPEL expression "nodeTemplates['compute1'].properties['os_type'].function" should return "get_input"
    And The SPEL int expression "nodeTemplates['compute1'].properties['os_type'].parameters.size()" should return 1
    And The SPEL expression "nodeTemplates['compute1'].properties['os_type'].parameters[0]" should return "os_type"
    And The SPEL expression "nodeTemplates['compute2'].properties['os_type'].function" should return "get_input"
    And The SPEL int expression "nodeTemplates['compute2'].properties['os_type'].parameters.size()" should return 1
    And The SPEL expression "nodeTemplates['compute2'].properties['os_type'].parameters[0]" should return "os_type"
#
#  @reset
#  Scenario: Re-Upload CSAR containing embeded topology template with inputs
#    Given I upload the archive "tosca base types 1.0"
#    And I upload the archive "topology_inputs"
#    And I export the YAML from topology template named "topology-inputs" and build a test dataset named "topology-inputs-replay" changing the version from "1.0.0-SNAPSHOT" to "2.0.0-SNAPSHOT"
#    When I upload the archive "topology-inputs-replay"
#    Then I should receive a RestResponse with 1 alerts in 1 files : 0 errors 0 warnings and 1 infos
#    And I should be able to retrieve a topology with name "topology-inputs" version "2.0.0-SNAPSHOT" and store it as a SPEL context
#    And The SPEL expression "inputs['os_type'].type" should return "string"
#    And The SPEL int expression "inputs['os_type'].constraints[0].validValues.size()" should return 4
#    And The SPEL expression "nodeTemplates['compute1'].properties['os_type'].function" should return "get_input"
#    And The SPEL int expression "nodeTemplates['compute1'].properties['os_type'].parameters.size()" should return 1
#    And The SPEL expression "nodeTemplates['compute1'].properties['os_type'].parameters[0]" should return "os_type"
#    And The SPEL expression "nodeTemplates['compute2'].properties['os_type'].function" should return "get_input"
#    And The SPEL int expression "nodeTemplates['compute2'].properties['os_type'].parameters.size()" should return 1
#    And The SPEL expression "nodeTemplates['compute2'].properties['os_type'].parameters[0]" should return "os_type"
#
  @reset
  Scenario: Upload CSAR containing embeded topology template with outputs
    Given I upload the archive "tosca base types 1.0"
    When I upload the archive "topology_outputs"
    Then I should receive a RestResponse with 2 alerts in 1 files : 0 errors 1 warnings and 1 infos
    And I should be able to retrieve a topology with name "topology-outputs" and store it as a SPEL context
    And The SPEL int expression "outputProperties['apache'].size()" should return 1
    And The SPEL expression "outputProperties['apache'][0]" should return "port"
    And The SPEL int expression "outputProperties['apache'].size()" should return 1
    And The SPEL expression "outputAttributes['compute'][0]" should return "ip_address"
#
#  @reset
#  Scenario: Re-Upload CSAR containing embeded topology template with outputs
#    Given I upload the archive "tosca base types 1.0"
#    And I upload the archive "topology_outputs"
#    And I export the YAML from topology template named "topology-outputs" and build a test dataset named "topology-outputs-replay" changing the version from "1.0.0-SNAPSHOT" to "2.0.0-SNAPSHOT"
#    Given I upload the archive "topology-outputs-replay"
#    Then I should receive a RestResponse with 1 alerts in 1 files : 0 errors 0 warnings and 1 infos
#    And I should be able to retrieve a topology with name "topology-outputs" version "2.0.0-SNAPSHOT" and store it as a SPEL context
#    And The SPEL int expression "outputProperties['apache'].size()" should return 1
#    And The SPEL expression "outputProperties['apache'][0]" should return "port"
#    And The SPEL int expression "outputProperties['apache'].size()" should return 1
#    And The SPEL expression "outputAttributes['compute'][0]" should return "ip_address"

  @reset
  Scenario: Upload CSAR containing embeded topology template with capabilities properties
    Given I upload the archive "tosca base types 1.0"
    When I upload the archive "topology-capacility-prop"
    Then I should receive a RestResponse with 1 alerts in 1 files : 0 errors 0 warnings and 1 infos
    And I should be able to retrieve a topology with name "topology-capacility-prop" and store it as a SPEL context
    And The SPEL expression "nodeTemplates['compute1'].capabilities['compute'].properties['containee_types'].value" should return "something"
    And The SPEL expression "nodeTemplates['compute2'].capabilities['compute'].properties['containee_types'].function" should return "get_input"
    And The SPEL int expression "nodeTemplates['compute2'].capabilities['compute'].properties['containee_types'].parameters.size()" should return 1
    And The SPEL expression "nodeTemplates['compute2'].capabilities['compute'].properties['containee_types'].parameters[0]" should return "an_input"
#
#  @reset
#  Scenario: Re-Upload CSAR containing embeded topology template with capabilities properties
#    Given I upload the archive "tosca base types 1.0"
#    And I upload the archive "topology-capacility-prop"
#    And I export the YAML from topology template named "topology-capacility-prop" and build a test dataset named "topology-capacility-prop-replay" changing the version from "1.1.0-SNAPSHOT" to "1.2.0-SNAPSHOT"
#    When I upload the archive "topology-capacility-prop-replay"
#    Then I should receive a RestResponse with 1 alerts in 1 files : 0 errors 0 warnings and 1 infos
#    And I should be able to retrieve a topology with name "topology-capacility-prop" version "1.2.0-SNAPSHOT" and store it as a SPEL context
#    And The SPEL expression "nodeTemplates['compute1'].capabilities['compute'].properties['containee_types'].value" should return "something"
#    And The SPEL expression "nodeTemplates['compute2'].capabilities['compute'].properties['containee_types'].function" should return "get_input"
#    And The SPEL int expression "nodeTemplates['compute2'].capabilities['compute'].properties['containee_types'].parameters.size()" should return 1
#    And The SPEL expression "nodeTemplates['compute2'].capabilities['compute'].properties['containee_types'].parameters[0]" should return "an_input"
#
  @reset
  Scenario: Upload CSAR containing embeded topology template with unknown capabilities
    Given I upload the archive "tosca base types 1.0"
    When I upload the archive "topology-capacility-unkown"
    Then I should receive a RestResponse with 2 alerts in 1 files : 0 errors 1 warnings and 1 infos
    And I should be able to retrieve a topology with name "topology-capacility-unkown" and store it as a SPEL context

  @reset
  Scenario: Upload CSAR containing embeded topology template with unknown capabilities property
    Given I upload the archive "tosca base types 1.0"
    When I upload the archive "topology-capacility-prop-unkown"
    Then I should receive a RestResponse with 1 alerts in 1 files : 1 errors 0 warnings and 0 infos

  @reset
  Scenario: Upload CSAR containing embeded topology template with relationship property using get_input
    Given I upload the archive "tosca base types 1.0"
    When I upload the archive "topology-template-relationship-funtionprop"
    Then I should receive a RestResponse with 1 alerts in 1 files : 0 errors 0 warnings and 1 infos
    And I should be able to retrieve a topology with name "topology-template-relationship-funtionprop" and store it as a SPEL context
    And The SPEL expression "nodeTemplates['software'].relationships['hostedOnCompute'].properties['password'].function" should return "get_input"
    And The SPEL expression "nodeTemplates['software'].relationships['hostedOnCompute'].properties['password'].parameters[0]" should return "pwd"
#
#  @reset
#  Scenario: Re-Upload CSAR containing embeded topology template with relationship property using get_input
#    Given I upload the archive "tosca base types 1.0"
#    And I upload the archive "topology-template-relationship-funtionprop"
#    And I export the YAML from topology template named "topology-template-relationship-funtionprop" and build a test dataset named "topology-template-relationship-funtionprop-replay" changing the version from "1.0.0-SNAPSHOT" to "1.0.1-SNAPSHOT"
#    Given I upload the archive "topology-template-relationship-funtionprop-replay"
#    Then I should receive a RestResponse with 1 alerts in 1 files : 0 errors 0 warnings and 1 infos
#    And I should be able to retrieve a topology with name "topology-template-relationship-funtionprop" version "1.0.1-SNAPSHOT" and store it as a SPEL context
#    And The SPEL expression "nodeTemplates['software'].relationships['hostedOnCompute'].properties['password'].function" should return "get_input"
#    And The SPEL expression "nodeTemplates['software'].relationships['hostedOnCompute'].properties['password'].parameters[0]" should return "pwd"
#
  @reset
  Scenario: Upload CSAR containing embeded topology template with capability property using inputs and ouputs
    Given I upload the archive "tosca base types 1.0"
    When I upload the archive "topology-capability-io"
    Then I should receive a RestResponse with 1 alerts in 1 files : 0 errors 0 warnings and 1 infos
    And I should be able to retrieve a topology with name "topology-capability-io" and store it as a SPEL context
    And The SPEL expression "outputCapabilityProperties['Compute']['host'][0]" should return "valid_node_types"
    And The SPEL expression "nodeTemplates['Compute'].capabilities['host'].properties['valid_node_types'].function" should return "get_input"
    And The SPEL int expression "nodeTemplates['Compute'].capabilities['host'].properties['valid_node_types'].parameters.size()" should return 1
    And The SPEL expression "nodeTemplates['Compute'].capabilities['host'].properties['valid_node_types'].parameters[0]" should return "valid_node_types"
#
#  @reset
#  Scenario: Re-Upload CSAR containing embeded topology template with capability property using inputs and ouputs
#    Given I upload the archive "tosca base types 1.0"
#    And I upload the archive "topology-capability-io"
#    And I export the YAML from topology template named "topology-capability-io" and build a test dataset named "topology-capability-io-replay" changing the version from "0.1.0-SNAPSHOT" to "0.2.0-SNAPSHOT"
#    Given I upload the archive "topology-capability-io-replay"
#    Then I should receive a RestResponse with 1 alerts in 1 files : 0 errors 0 warnings and 1 infos
#    And I should be able to retrieve a topology with name "topology-capability-io" version "0.2.0-SNAPSHOT" and store it as a SPEL context
#    And The SPEL expression "outputCapabilityProperties['Compute']['host'][0]" should return "valid_node_types"
#    And The SPEL expression "nodeTemplates['Compute'].capabilities['host'].properties['valid_node_types'].function" should return "get_input"
#    And The SPEL int expression "nodeTemplates['Compute'].capabilities['host'].properties['valid_node_types'].parameters.size()" should return 1
#    And The SPEL expression "nodeTemplates['Compute'].capabilities['host'].properties['valid_node_types'].parameters[0]" should return "valid_node_types"

  @reset
  Scenario: Upload and delete CSAR containing only topology
    Given I upload the archive "tosca base types 1.0"
    And I upload the archive "topology_inputs"
    And I should receive a RestResponse with 1 alerts in 1 files : 0 errors 0 warnings and 1 infos
    When I delete a CSAR with id "topology-inputs:1.0.0-SNAPSHOT"
    Then I should receive a RestResponse with no error
    And I have no CSAR created with id "topology-inputs:1.0.0-SNAPSHOT"
#
#  # When the CSAR contains both topology and type, the topology has a dependency to the CSAR
#  # (since it may embed types contained in this CSAR)
#  # so we need to delete the topology template before deleting the CSAR
#  @reset
#  Scenario: Upload and delete CSAR containing types and topology
#    Given I upload the archive "tosca base types 1.0"
#    And I upload the archive "topology apache"
#    And I should receive a RestResponse with 2 alerts in 1 files : 0 errors 1 warnings and 1 infos
#    And I delete the topology template with name "apache-type"
#    And I should receive a RestResponse with no error
#    When I delete a CSAR with id "apache-type:1.1.0-SNAPSHOT"
#    Then I should receive a RestResponse with no error
#    And I have no CSAR created with id "apache-type:1.1.0-SNAPSHOT"
#
  @reset
  Scenario: Upload CSAR containing a type declaring an artifact
    Given I upload the archive "tosca base types 1.0"
    And I upload the archive "sample apache lb types 0.1"
    When I upload the archive "topology_artifact"
    Then I should receive a RestResponse with 1 alerts in 1 files : 0 errors 0 warnings and 1 infos
    And I should be able to retrieve a topology with name "topology_artifact" and store it as a SPEL context
    And The SPEL int expression "nodeTemplates['apache'].artifacts.size()" should return 1
    And The SPEL expression "nodeTemplates['apache'].artifacts['scripts'].artifactType" should return "fastconnect.artifacts.ResourceDirectory"
    And The SPEL expression "nodeTemplates['apache'].artifacts['scripts'].artifactRef" should return "scripts"

  # COMMENTED FOR REAL BEFORE... !!!!!!!!!

  #Scenario: Upload CSAR containing embeded topology template with groups and HA policies
  #  Given I upload the archive "tosca-normative-types"
  #  And I upload the archive "topology-groups"
  #  Then I should receive a RestResponse with 1 alerts in 1 files : 0 errors 0 warnings and 1 infos
  #  And I should be able to retrieve a topology with name "topology-groups" and store it as a SPEL context
  #  And The SPEL int expression "groups.size()" should return 2
  #  And The SPEL expression "groups['compute_scaling_group'].name" should return "compute_scaling_group"
  #  And The SPEL int expression "groups['compute_scaling_group'].members.size()" should return 1
  #  And The SPEL expression "groups['compute_scaling_group'].members[0]" should return "compute1"
  #  And The SPEL int expression "groups['compute_scaling_group'].policies.size()" should return 2
  #  And The SPEL expression "groups['compute_scaling_group'].policies[0].name" should return "my_scaling_ha_policy"
  #  And The SPEL expression "groups['compute_scaling_group'].policies[0].type" should return "tosca.policy.ha"
  #  And The SPEL expression "groups['compute_scaling_group'].policies[1].name" should return "another_ha_policy"
  #  And The SPEL expression "groups['compute_scaling_group'].policies[1].type" should return "tosca.policy.ha"
  #  And The SPEL expression "groups['compute_ha_group'].name" should return "compute_ha_group"
  #  And The SPEL int expression "groups['compute_ha_group'].members.size()" should return 2
  #  And The SPEL expression "groups['compute_ha_group'].members[0]" should return "compute1"
  #  And The SPEL expression "groups['compute_ha_group'].members[1]" should return "compute2"
  #  And The SPEL int expression "groups['compute_ha_group'].policies.size()" should return 1
  #  And The SPEL expression "groups['compute_ha_group'].policies[0].name" should return "my_scaling_ha_policy"
  #  And The SPEL expression "groups['compute_ha_group'].policies[0].type" should return "tosca.policy.ha"
  #  And The SPEL int expression "nodeTemplates['compute1'].groups.size()" should return 2
  #  And The SPEL expression "nodeTemplates['compute1'].groups[0]" should return "compute_scaling_group"
  #  And The SPEL expression "nodeTemplates['compute1'].groups[1]" should return "compute_ha_group"
  #  And The SPEL int expression "nodeTemplates['compute2'].groups.size()" should return 1
  #  And The SPEL expression "nodeTemplates['compute2'].groups[0]" should return "compute_ha_group"
  #
  #Scenario: Re-Upload CSAR containing embeded topology template with groups and HA policies
  #  Given I upload the archive "tosca-normative-types"
  #  And I upload the archive "topology-groups"
  #  And I export the YAML from topology template named "topology-groups" and build a test dataset named "topology-groups-replay" changing the version from "1.0.0-SNAPSHOT" to "1.0.1-SNAPSHOT"
  #  Given I upload the archive "topology-groups-replay"
  #  Then I should receive a RestResponse with 1 alerts in 1 files : 0 errors 0 warnings and 1 infos
  #  And I should be able to retrieve a topology with name "topology-groups" version "1.0.1-SNAPSHOT" and store it as a SPEL context
  #  And The SPEL int expression "groups.size()" should return 2
  #  And The SPEL expression "groups['compute_scaling_group'].name" should return "compute_scaling_group"
  #  And The SPEL int expression "groups['compute_scaling_group'].members.size()" should return 1
  #  And The SPEL expression "groups['compute_scaling_group'].members[0]" should return "compute1"
  #  And The SPEL int expression "groups['compute_scaling_group'].policies.size()" should return 2
  #  And The SPEL expression "groups['compute_scaling_group'].policies[0].name" should return "my_scaling_ha_policy"
  #  And The SPEL expression "groups['compute_scaling_group'].policies[0].type" should return "tosca.policy.ha"
  #  And The SPEL expression "groups['compute_scaling_group'].policies[1].name" should return "another_ha_policy"
  #  And The SPEL expression "groups['compute_scaling_group'].policies[1].type" should return "tosca.policy.ha"
  #  And The SPEL expression "groups['compute_ha_group'].name" should return "compute_ha_group"
  #  And The SPEL int expression "groups['compute_ha_group'].members.size()" should return 2
  #  And The SPEL expression "groups['compute_ha_group'].members[0]" should return "compute1"
  #  And The SPEL expression "groups['compute_ha_group'].members[1]" should return "compute2"
  #  And The SPEL int expression "groups['compute_ha_group'].policies.size()" should return 1
  #  And The SPEL expression "groups['compute_ha_group'].policies[0].name" should return "my_scaling_ha_policy"
  #  And The SPEL expression "groups['compute_ha_group'].policies[0].type" should return "tosca.policy.ha"
  #  And The SPEL int expression "nodeTemplates['compute1'].groups.size()" should return 2
  #  And The SPEL expression "nodeTemplates['compute1'].groups[0]" should return "compute_scaling_group"
  #  And The SPEL expression "nodeTemplates['compute1'].groups[1]" should return "compute_ha_group"
  #  And The SPEL int expression "nodeTemplates['compute2'].groups.size()" should return 1
  #  And The SPEL expression "nodeTemplates['compute2'].groups[0]" should return "compute_ha_group"
  #
  #Scenario: Upload CSAR containing embeded topology template with groups and HA policies containing unknown policy
  #  Given I upload the archive "tosca-normative-types"
  #  And I upload the archive "topology-groups-unknown-policy"
  #  Then I should receive a RestResponse with 1 alerts in 1 files : 1 errors 0 warnings and 0 infos
  #
  #Scenario: Upload CSAR containing embeded topology template with groups and HA policies containing unknown member
  #  Given I upload the archive "tosca-normative-types"
  #  And I upload the archive "topology-groups-unknown-member"
  #  Then I should receive a RestResponse with 2 alerts in 1 files : 0 errors 1 warnings and 1 infos
  #  And I should be able to retrieve a topology with name "topology-groups-unknown-member" and store it as a SPEL context
  #  And The SPEL int expression "groups.size()" should return 1
  #  And The SPEL expression "groups['compute_scaling_group'].name" should return "compute_scaling_group"
  #  And The SPEL int expression "groups['compute_scaling_group'].members.size()" should return 1
  #  And The SPEL expression "groups['compute_scaling_group'].members[0]" should return "compute1"
  #  And The SPEL int expression "groups['compute_scaling_group'].policies.size()" should return 1
  #  And The SPEL expression "groups['compute_scaling_group'].policies[0].name" should return "my_scaling_ha_policy"
  #  And The SPEL expression "groups['compute_scaling_group'].policies[0].type" should return "tosca.policy.ha"


  ## BACKKKKK

  @reset
  Scenario: Upload CSAR containing embedded topology template with missing referenced inputs
    Given I upload the archive "tosca base types 1.0"
    And I upload the archive "topology-missing-inputs"
    Then I should receive a RestResponse with 2 alerts in 1 files : 2 errors 0 warnings and 0 infos

  @reset
  Scenario: Upload a topology template with invalid node template name should rename it and throw informations
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I should receive a RestResponse with no error
    And I upload the archive "topology-invalid-node-name"
    Then I should receive a RestResponse with 3 alerts in 1 files : 0 errors 2 warnings and 1 infos
    And I should be able to retrieve a topology with name "invalid-node-name-template" version "1.0.0-SNAPSHOT" and store it as a SPEL context
    And The SPEL expression "nodeTemplates['Compute'].type" should return "tosca.nodes.Compute"
    And The SPEL expression "nodeTemplates['Data_base'].type" should return "tosca.nodes.Database"
