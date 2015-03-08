Feature: CSAR upload with topology

Background:
  Given I am authenticated with "ADMIN" role

Scenario: Upload CSAR containing apache types and embeded topology template
  Given I upload the archive "tosca base types 1.0"
  When I upload the archive "topology apache"
  Then I should receive a RestResponse with 1 alerts in 1 files : 0 errors 0 warnings and 1 infos
  And If I search for topology templates I can find one with the name "apache-type-1.1.0-SNAPSHOT" and store the related topology as a SPEL context
  And The SPEL expression "dependencies.^[name == 'tosca-base-types'].version" should return "1.0"
  And The SPEL boolean expression "nodeTemplates.size() == 2" should return true
  And The SPEL expression "nodeTemplates['compute'].type" should return "tosca.nodes.Compute"
  And The SPEL boolean expression "nodeTemplates['compute'].properties.size() == 8" should return true
  And The SPEL expression "nodeTemplates['compute'].properties['os_distribution'].value" should return "ubuntu"
  And The SPEL expression "nodeTemplates['compute'].properties['os_type'].value" should return "linux"
  And The SPEL boolean expression "nodeTemplates['compute'].attributes.size() == 3" should return true
  And The SPEL boolean expression "nodeTemplates['compute'].attributes.containsKey('ip_address')" should return true
  And The SPEL boolean expression "nodeTemplates['compute'].attributes.containsKey('tosca_id')" should return true
  And The SPEL boolean expression "nodeTemplates['compute'].attributes.containsKey('tosca_name')" should return true
  And The SPEL boolean expression "nodeTemplates['compute'].capabilities.size() == 2" should return true
  And The SPEL boolean expression "nodeTemplates['compute'].capabilities.containsKey('feature')" should return true
  And The SPEL boolean expression "nodeTemplates['compute'].capabilities.containsKey('compute')" should return true
  And The SPEL boolean expression "nodeTemplates['compute'].requirements.size() == 2" should return true
  And The SPEL boolean expression "nodeTemplates['compute'].requirements.containsKey('dependency')" should return true
  And The SPEL boolean expression "nodeTemplates['compute'].requirements.containsKey('network')" should return true  
  And The SPEL boolean expression "nodeTemplates['apache'].relationships.size() == 1" should return true
  And The SPEL expression "nodeTemplates['apache'].relationships['hostedOnCompute'].type" should return "tosca.relationships.HostedOn"
  And The SPEL expression "nodeTemplates['apache'].relationships['hostedOnCompute'].target" should return "compute"
  And The SPEL expression "nodeTemplates['apache'].relationships['hostedOnCompute'].requirementName" should return "host"
  And The SPEL expression "nodeTemplates['apache'].relationships['hostedOnCompute'].requirementType" should return "tosca.capabilities.Container"
  And The SPEL expression "nodeTemplates['apache'].relationships['hostedOnCompute'].targetedCapabilityName" should return "compute"
  And The SPEL boolean expression "nodeTemplates['apache'].relationships['hostedOnCompute'].properties.size() == 1" should return true
  And The SPEL expression "nodeTemplates['apache'].relationships['hostedOnCompute'].properties['password'].value" should return "unfuckingbelievable"
  
  
Scenario: Upload CSAR containing cutom types and embeded topology template using short notation for requirements  
  Given I upload the archive "tosca-normative-types"
  When I upload the archive "topology custom types"
  Then I should receive a RestResponse with 1 alerts in 1 files : 0 errors 0 warnings and 1 infos
  And If I search for topology templates I can find one with the name "AllInclusiveArchive-1.0.0-SNAPSHOT" and store the related topology as a SPEL context
  And The SPEL expression "dependencies.^[name == 'AllInclusiveArchive'].version" should return "1.0.0-SNAPSHOT"
  And The SPEL expression "dependencies.^[name == 'tosca-normative-types'].version" should return "1.0.0.wd03-SNAPSHOT"
  And The SPEL boolean expression "nodeTemplates['software'].relationships.size() == 1" should return true
  And The SPEL expression "nodeTemplates['software'].relationships['myRelationTypeCompute'].type" should return "custom.relationships.MyRelationType"
  And The SPEL expression "nodeTemplates['software'].relationships['myRelationTypeCompute'].target" should return "compute"
  And The SPEL expression "nodeTemplates['software'].relationships['myRelationTypeCompute'].requirementName" should return "host"
  And The SPEL expression "nodeTemplates['software'].relationships['myRelationTypeCompute'].requirementType" should return "custom.capabilities.MyCapability"
  And The SPEL expression "nodeTemplates['software'].relationships['myRelationTypeCompute'].targetedCapabilityName" should return "host"  
  
Scenario: Upload twice a CSAR SNAPSHOT containing embeded topology template result in 2 topology templates  
  Given I upload the archive "tosca-normative-types"
  And I upload the archive "topology custom types"
  When I upload the archive "topology custom types"
  Then I should receive a RestResponse with 1 alerts in 1 files : 0 errors 0 warnings and 1 infos
  And If I search for topology templates I can find one with the name "AllInclusiveArchive-1.0.0-SNAPSHOT" and store the related topology as a SPEL context
  And If I search for topology templates I can find one with the name "AllInclusiveArchive-1.0.0-SNAPSHOT-1" and store the related topology as a SPEL context

Scenario: Upload a CSAR containing topology with a node referencing a unexisting type  
  Given I upload the archive "tosca-normative-types"
  When I upload the archive "topology-error-missingtype"
  Then I should receive a RestResponse with 2 alerts in 1 files : 1 errors 1 warnings and 0 infos
  
Scenario: Upload a CSAR containing topology with an unkown capability  
  Given I upload the archive "tosca-normative-types"
  When I upload the archive "topology-unknown-capability"
  Then I should receive a RestResponse with 2 alerts in 1 files : 1 errors 1 warnings and 0 infos    
  
Scenario: Upload a CSAR containing topology with an unkown capability, short requirement notation
  Given I upload the archive "tosca-normative-types"
  When I upload the archive "topology-unknown-capability-short-notation"
  Then I should receive a RestResponse with 2 alerts in 1 files : 1 errors 1 warnings and 0 infos     

Scenario: Upload a CSAR containing topology with an unkown relationship type
  Given I upload the archive "tosca-normative-types"
  When I upload the archive "topology-unknown-relationshiptype"
  Then I should receive a RestResponse with 2 alerts in 1 files : 1 errors 1 warnings and 0 infos  
    
Scenario: Upload a CSAR containing topology with an unknown requirement target  
  Given I upload the archive "tosca base types 1.0"
  When I upload the archive "topology-unknown-reqtarget"
  Then I should receive a RestResponse with 2 alerts in 1 files : 1 errors 1 warnings and 0 infos   
  
Scenario: Upload a CSAR containing topology with an incorrect requirement name  
  Given I upload the archive "tosca base types 1.0"
  When I upload the archive "topology-unknown-req"
  Then I should receive a RestResponse with 2 alerts in 1 files : 1 errors 1 warnings and 0 infos  
  
Scenario: Upload CSAR containing embeded topology template with inputs
  Given I upload the archive "tosca base types 1.0"
  When I upload the archive "topology_inputs"
  Then I should receive a RestResponse with 1 alerts in 1 files : 0 errors 0 warnings and 1 infos  
  And If I search for topology templates I can find one with the name "topology-inputs-1.0.0-SNAPSHOT" and store the related topology as a SPEL context
  And The SPEL expression "inputs['os_type'].type" should return "string"
  And The SPEL boolean expression "inputs['os_type'].constraints[0].validValues.size() == 4" should return true
  And The SPEL expression "nodeTemplates['compute1'].properties['os_type'].function" should return "get_input"
  And The SPEL boolean expression "nodeTemplates['compute1'].properties['os_type'].parameters.size() == 1" should return true
  And The SPEL expression "nodeTemplates['compute1'].properties['os_type'].parameters[0]" should return "os_type"
  And The SPEL expression "nodeTemplates['compute2'].properties['os_type'].function" should return "get_input"
  And The SPEL boolean expression "nodeTemplates['compute2'].properties['os_type'].parameters.size() == 1" should return true
  And The SPEL expression "nodeTemplates['compute2'].properties['os_type'].parameters[0]" should return "os_type"  
    
Scenario: Upload CSAR containing embeded topology template with outputs
  Given I upload the archive "tosca base types 1.0"
  When I upload the archive "topology_outputs"
  Then I should receive a RestResponse with 1 alerts in 1 files : 0 errors 0 warnings and 1 infos    
  And If I search for topology templates I can find one with the name "topology-outputs-1.0.0-SNAPSHOT" and store the related topology as a SPEL context
  And The SPEL boolean expression "outputProperties['apache'].size() == 1" should return true
  And The SPEL expression "outputProperties['apache'][0]" should return "port"
  And The SPEL boolean expression "outputProperties['apache'].size() == 1" should return true
  And The SPEL expression "outputAttributes['compute'][0]" should return "ip_address"
  