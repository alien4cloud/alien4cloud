# in these scenari, we will create a topology template, expose it as type (substitution) and then reuse it in an application topology
Feature: Topology composition

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I create a resource of type "alien.nodes.mock.openstack.Flavor" named "Small" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "1" for the resource named "Small" related to the location "Mount doom orchestrator"/"Thark location"
    And I create a resource of type "alien.nodes.mock.openstack.Image" named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "img1" for the resource named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I autogenerate the on-demand resources for the location "Mount doom orchestrator"/"Thark location"
    And I upload the archive "samples apache"
    And I upload the archive "samples mysql"
    And I upload the archive "samples php"
    And I upload the archive "samples wordpress"
    And I create a new topology template with name "net.sample.LAMP" and description "A Linux Apache Mysql PHP stack as a embedable topology template"
    And The RestResponse should contain a topology template id
    And I can get and register the topology for the last version of the registered topology template
    And I get the current topology
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | MyCompute                                                             |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | os_arch                                                          |
      | propertyDefinition.type | string                                                           |
    And I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodeCapabilityPropertyAsInputOperation |
      | nodeName       | MyCompute                                                                                             |
      | capabilityName | os                                                                                                    |
      | propertyName   | architecture                                                                                          |
      | inputName      | os_arch                                                                                               |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | os_type                                                          |
      | propertyDefinition.type | string                                                           |
    And I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodeCapabilityPropertyAsInputOperation |
      | nodeName       | MyCompute                                                                                             |
      | capabilityName | os                                                                                                    |
      | propertyName   | architecture                                                                                          |
      | inputName      | os_type                                                                                               |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | MyApache                                                              |
      | indexedNodeTypeId | alien.nodes.Apache:2.0.0-SNAPSHOT                                     |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | MyMysql                                                               |
      | indexedNodeTypeId | alien.nodes.Mysql:2.0.0-SNAPSHOT                                      |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | db_port                                                          |
      | propertyDefinition.type | integer                                                          |
    And I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodePropertyAsInputOperation |
      | nodeName     | MyMysql                                                                                     |
      | propertyName | port                                                                                        |
      | inputName    | db_port                                                                                     |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | db_name                                                          |
      | propertyDefinition.type | string                                                           |
    And I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodePropertyAsInputOperation |
      | nodeName     | MyMysql                                                                                     |
      | propertyName | name                                                                                        |
      | inputName    | db_name                                                                                     |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | db_user                                                          |
      | propertyDefinition.type | string                                                           |
    And I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodePropertyAsInputOperation |
      | nodeName     | MyMysql                                                                                     |
      | propertyName | db_user                                                                                     |
      | inputName    | db_user                                                                                     |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | db_password                                                      |
      | propertyDefinition.type | string                                                           |
    And I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodePropertyAsInputOperation |
      | nodeName     | MyMysql                                                                                     |
      | propertyName | db_password                                                                                 |
      | inputName    | db_password                                                                                 |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | MyPHP                                                                 |
      | indexedNodeTypeId | alien.nodes.PHP:2.0.0-SNAPSHOT                                        |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | MyMysql                                                                               |
      | relationshipName       | MyRelationship                                                                        |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0.0-SNAPSHOT                                                                        |
      | requirementName        | host                                                                                  |
      | target                 | MyCompute                                                                             |
      | targetedCapabilityName | host                                                                                  |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | MyApache                                                                              |
      | relationshipName       | MyRelationship                                                                        |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0.0-SNAPSHOT                                                                        |
      | requirementName        | host                                                                                  |
      | target                 | MyCompute                                                                             |
      | targetedCapabilityName | host                                                                                  |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | MyPHP                                                                                 |
      | relationshipName       | MyRelationship                                                                        |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0.0-SNAPSHOT                                                                        |
      | requirementName        | host                                                                                  |
      | target                 | MyCompute                                                                             |
      | targetedCapabilityName | host                                                                                  |
    And I save the topology
    And I pre register orchestrator properties
      | managementUrl | http://cloudifyurl:8099 |
      | numberBackup  | 1                       |
      | managerEmail  | admin@alien.fr          |

  @reset
  Scenario: Expose the template as a type and check type properties and attributes
    And I get the current topology
    Given I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId | tosca.nodes.Root                                                                  |
    Then I should receive a RestResponse with no error
    When I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.SetNodeAttributeAsOutputOperation |
      | nodeName      | MyCompute                                                                                      |
      | attributeName | ip_address                                                                                     |
    And I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.SetNodeCapabilityPropertyAsOutputOperation |
      | nodeName       | MyApache                                                                                                |
      | capabilityName | data_endpoint                                                                                           |
      | propertyName   | port                                                                                                    |
    And I save the topology
    When I try to get a component with id "net.sample.LAMP:0.1.0-SNAPSHOT"
    Then I should receive a RestResponse with no error
    And I should have a component with id "net.sample.LAMP:0.1.0-SNAPSHOT"
    When I register the rest response data as SPEL context of type "org.alien4cloud.tosca.model.types.NodeType"
    Then The SPEL expression "elementId" should return "net.sample.LAMP"
    And The SPEL expression "archiveName" should return "net.sample.LAMP"
    And The SPEL expression "archiveVersion" should return "0.1.0-SNAPSHOT"
    And The SPEL int expression "derivedFrom.size()" should return 1
    And The SPEL expression "derivedFrom[0]" should return "tosca.nodes.Root"
    And The SPEL boolean expression "properties.containsKey('os_arch')" should return true
    And The SPEL boolean expression "properties.containsKey('os_type')" should return true
    And The SPEL boolean expression "properties.containsKey('db_port')" should return true
    And The SPEL boolean expression "properties.containsKey('db_name')" should return true
    And The SPEL boolean expression "properties.containsKey('db_user')" should return true
    And The SPEL boolean expression "properties.containsKey('db_password')" should return true
    And The SPEL boolean expression "attributes.containsKey('ip_address')" should return true
    And The SPEL boolean expression "attributes.containsKey('port')" should return true

  @reset
  Scenario: Expose capabilities and check type capabilities
    And I get the current topology
    Given I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId | tosca.nodes.Root                                                                  |
    And I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | MyMysql                                                                                     |
      | substitutionCapabilityId | database_endpoint                                                                           |
      | capabilityId             | database_endpoint                                                                           |
    And I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.UpdateCapabilitySubstitutionTypeOperation |
      | substitutionCapabilityId | database_endpoint                                                                              |
      | newCapabilityId          | hostMysql                                                                                      |
    And I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | MyApache                                                                                    |
      | substitutionCapabilityId | host                                                                                        |
      | capabilityId             | host                                                                                        |
    And I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.UpdateCapabilitySubstitutionTypeOperation |
      | substitutionCapabilityId | host                                                                                           |
      | newCapabilityId          | hostApache                                                                                     |
    And I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | MyPHP                                                                                       |
      | substitutionCapabilityId | attachWebsite                                                                               |
      | capabilityId             | attachWebsite                                                                               |
    And I save the topology
    When I try to get a component with id "net.sample.LAMP:0.1.0-SNAPSHOT"
    Then I should receive a RestResponse with no error
    And I should have a component with id "net.sample.LAMP:0.1.0-SNAPSHOT"
    When I register the rest response data as SPEL context of type "org.alien4cloud.tosca.model.types.NodeType"
    Then The SPEL expression "capabilities.^[id == 'hostMysql'].type" should return "alien.capabilities.MysqlDatabaseEndpoint"
    And The SPEL expression "capabilities.^[id == 'hostApache'].type" should return "alien.capabilities.ApacheContainer"
    And The SPEL expression "capabilities.^[id == 'attachWebsite'].type" should return "alien.capabilities.PHPModule"

  @reset
  Scenario: Expose capabilities and use it in a topology
    And I get the current topology
    Given I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId | tosca.nodes.Root                                                                  |
    And I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | MyMysql                                                                                     |
      | substitutionCapabilityId | database_endpoint                                                                           |
      | capabilityId             | database_endpoint                                                                           |
    And I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | MyApache                                                                                    |
      | substitutionCapabilityId | hostApache                                                                                  |
      | capabilityId             | host                                                                                        |
    And I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | MyPHP                                                                                       |
      | substitutionCapabilityId | attachWebsite                                                                               |
      | capabilityId             | attachWebsite                                                                               |
    And I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.SetNodeAttributeAsOutputOperation |
      | nodeName      | MyCompute                                                                                      |
      | attributeName | ip_address                                                                                     |
    And I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.SetNodeCapabilityPropertyAsOutputOperation |
      | nodeName       | MyApache                                                                                                |
      | capabilityName | data_endpoint                                                                                           |
      | propertyName   | port                                                                                                    |
    And I save the topology
    Given I create a new application with name "myWebapp" and description "A webapp that use an embeded topology."
    And I get the current topology
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | myLAMP                                                                |
      | indexedNodeTypeId | net.sample.LAMP:0.1.0-SNAPSHOT                                        |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | myWordpress                                                           |
      | indexedNodeTypeId | alien.nodes.Wordpress:2.0.0-SNAPSHOT                                  |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | myWordpress                                                                           |
      | relationshipName       | hostOnApache                                                                          |
      | relationshipType       | alien.relationships.WordpressHostedOnApache                                           |
      | relationshipVersion    | 2.0.0-SNAPSHOT                                                                        |
      | requirementName        | host                                                                                  |
      | target                 | myLAMP                                                                                |
      | targetedCapabilityName | hostApache                                                                            |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | myWordpress                                                                           |
      | relationshipName       | connectToDb                                                                           |
      | relationshipType       | alien.relationships.WordpressConnectToMysql                                           |
      | relationshipVersion    | 2.0.0-SNAPSHOT                                                                        |
      | requirementName        | database                                                                              |
      | target                 | myLAMP                                                                                |
      | targetedCapabilityName | database_endpoint                                                                     |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | myWordpress                                                                           |
      | relationshipName       | connectToPhp                                                                          |
      | relationshipType       | alien.relationships.WordpressConnectToPHP                                             |
      | relationshipVersion    | 2.0.0-SNAPSHOT                                                                        |
      | requirementName        | php                                                                                   |
      | target                 | myLAMP                                                                                |
      | targetedCapabilityName | attachWebsite                                                                         |
    And I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodePropertyValueOperation |
      | nodeName      | myLAMP                                                                                |
      | propertyName  | os_arch                                                                               |
      | propertyValue | x86_64                                                                                |
    And I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodePropertyValueOperation |
      | nodeName      | myLAMP                                                                                |
      | propertyName  | os_type                                                                               |
      | propertyValue | linux                                                                                 |
    And I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodePropertyValueOperation |
      | nodeName      | myLAMP                                                                                |
      | propertyName  | db_name                                                                               |
      | propertyValue | name                                                                                  |
    And I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodePropertyValueOperation |
      | nodeName      | myLAMP                                                                                |
      | propertyName  | db_user                                                                               |
      | propertyValue | user                                                                                  |
    And I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodePropertyValueOperation |
      | nodeName      | myLAMP                                                                                |
      | propertyName  | db_port                                                                               |
      | propertyValue | 3660                                                                                  |
    And I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodePropertyValueOperation |
      | nodeName      | myLAMP                                                                                |
      | propertyName  | db_password                                                                           |
      | propertyValue | password                                                                              |
    And I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.SetNodeAttributeAsOutputOperation |
      | nodeName      | myLAMP                                                                                         |
      | attributeName | ip_address                                                                                     |
    And I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.SetNodeAttributeAsOutputOperation |
      | nodeName      | myLAMP                                                                                         |
      | attributeName | port                                                                                           |
    And I save the topology
    Given I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes
    When I deploy it
    Then I should receive a RestResponse with no error
    And The application's deployment must succeed
    When I get the deployment toology for the current application
    Then I should receive a RestResponse with no error
    When I register the rest response data as SPEL context of type2 "alien4cloud.rest.deployment.DeploymentTopologyDTO"
    Then The SPEL int expression "topology.nodeTemplates.size()" should return 5
    # the properties of the node MyCompute of the embeded topology are fed by the input -> the properties of the proxy node
    #And The SPEL expression "topology.nodeTemplates['myLAMP_MyCompute'].properties['os_arch'].value" should return "x86_64"
    #And The SPEL expression "topology.nodeTemplates['myLAMP_MyCompute'].properties['os_type'].value" should return "linux"
    # check that the relationships are correctly mapped
    # wordpress should be connected to the embeded apache
    And The SPEL expression "topology.nodeTemplates['myWordpress'].relationships.^[value.type == 'alien.relationships.WordpressHostedOnApache'].values().iterator().next().target" should return "myLAMP_MyApache"
    # the apache should be connected to the compute
    And The SPEL expression "topology.nodeTemplates['myLAMP_MyApache'].relationships.^[value.type == 'tosca.relationships.HostedOn'].values().iterator().next().target" should return "myLAMP_MyCompute"
    # the outputs should have been wired
#FIXME when it will be possible to expose two properties with the same name but in different node templates
#    And The SPEL expression "topology.outputProperties['myLAMP_MyMysql'][0]" should return "db_port"
    And The SPEL expression "topology.outputAttributes['myLAMP_MyCompute'][0]" should return "ip_address"
    And The SPEL expression "topology.outputCapabilityProperties['myLAMP_MyApache']['data_endpoint'][0]" should return "port"

  @reset
  Scenario: Recursive composition
# in this scenario we have 3 templates:
# - 1 mysql subsystem (mysql + compute)
# - 1 apache + php subsystem (apache + php + compute)
# - 1 LAMP that combine both
# An app will use this lamp to deploy a wordpress
    # The first topology template containing a MySql + Compute
    Given I create a new topology template with name "net.sample.MySqlSubsystem" and description "A Mysql + compute as a embedable topology template"
    And I should receive a RestResponse with no error
    And The RestResponse should contain a topology template id
    And I can get and register the topology for the last version of the registered topology template
    And I get the current topology
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | MyCompute                                                             |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | db_arch                                                          |
      | propertyDefinition.type | string                                                           |
    And I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodeCapabilityPropertyAsInputOperation |
      | nodeName       | MyCompute                                                                                             |
      | capabilityName | os                                                                                                    |
      | propertyName   | architecture                                                                                          |
      | inputName      | db_arch                                                                                               |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | db_type                                                          |
      | propertyDefinition.type | string                                                           |
    And I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodeCapabilityPropertyAsInputOperation |
      | nodeName       | MyCompute                                                                                             |
      | capabilityName | os                                                                                                    |
      | propertyName   | type                                                                                                  |
      | inputName      | db_type                                                                                               |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | MyMysql                                                               |
      | indexedNodeTypeId | alien.nodes.Mysql:2.0.0-SNAPSHOT                                      |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | MyMysql                                                                               |
      | relationshipName       | MyMysqlHostedOn                                                                       |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0.0-SNAPSHOT                                                                        |
      | requirementName        | host                                                                                  |
      | target                 | MyCompute                                                                             |
      | targetedCapabilityName | host                                                                                  |
    Given I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId | alien.nodes.Mysql                                                                 |
    And I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | MyMysql                                                                                     |
      | substitutionCapabilityId | database_endpoint                                                                           |
      | capabilityId             | database_endpoint                                                                           |
    And I save the topology
    # The second topology template containing a Apache + PHP + Compute
    Given I create a new topology template with name "net.sample.MyApacheSubsystem" and description "A Linux Apache PHP stack as a embedable topology template"
    And I should receive a RestResponse with no error
    And The RestResponse should contain a topology template id
    And I can get and register the topology for the last version of the registered topology template
    And I get the current topology
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | MyCompute                                                             |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | www_arch                                                         |
      | propertyDefinition.type | string                                                           |
    And I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodeCapabilityPropertyAsInputOperation |
      | nodeName       | MyCompute                                                                                             |
      | capabilityName | os                                                                                                    |
      | propertyName   | architecture                                                                                          |
      | inputName      | www_arch                                                                                              |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | www_type                                                         |
      | propertyDefinition.type | string                                                           |
    And I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodeCapabilityPropertyAsInputOperation |
      | nodeName       | MyCompute                                                                                             |
      | capabilityName | os                                                                                                    |
      | propertyName   | type                                                                                                  |
      | inputName      | www_type                                                                                              |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | MyApache                                                              |
      | indexedNodeTypeId | alien.nodes.Apache:2.0.0-SNAPSHOT                                     |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | MyPHP                                                                 |
      | indexedNodeTypeId | alien.nodes.PHP:2.0.0-SNAPSHOT                                        |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | MyApache                                                                              |
      | relationshipName       | MyRelationship                                                                        |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0.0-SNAPSHOT                                                                        |
      | requirementName        | host                                                                                  |
      | target                 | MyCompute                                                                             |
      | targetedCapabilityName | host                                                                                  |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | MyPHP                                                                                 |
      | relationshipName       | MyRelationship                                                                        |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0.0-SNAPSHOT                                                                        |
      | requirementName        | host                                                                                  |
      | target                 | MyCompute                                                                             |
      | targetedCapabilityName | host                                                                                  |
    Given I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId | tosca.nodes.Root                                                                  |
    And I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | MyApache                                                                                    |
      | substitutionCapabilityId | hostApache                                                                                  |
      | capabilityId             | host                                                                                        |
    And I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | MyPHP                                                                                       |
      | substitutionCapabilityId | attachWebsite                                                                               |
      | capabilityId             | attachWebsite                                                                               |
    And I save the topology
    # The third topology template combining the 2 others
    Given I create a new topology template with name "net.sample.LAMP2" and description "A Linux Apache Mysql PHP stack as a embedable topology template"
    And I should receive a RestResponse with no error
    And The RestResponse should contain a topology template id
    And I can get and register the topology for the last version of the registered topology template
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | WWW                                                                   |
      | indexedNodeTypeId | net.sample.MyApacheSubsystem:0.1.0-SNAPSHOT                           |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | sys_arch                                                         |
      | propertyDefinition.type | string                                                           |
    And I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodePropertyAsInputOperation |
      | nodeName     | WWW                                                                                         |
      | propertyName | www_arch                                                                                    |
      | inputName    | sys_arch                                                                                    |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | sys_type                                                         |
      | propertyDefinition.type | string                                                           |
    And I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodePropertyAsInputOperation |
      | nodeName     | WWW                                                                                         |
      | propertyName | www_type                                                                                    |
      | inputName    | sys_type                                                                                    |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | DB                                                                    |
      | indexedNodeTypeId | net.sample.MySqlSubsystem:0.1.0-SNAPSHOT                              |
    And I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodePropertyAsInputOperation |
      | nodeName     | DB                                                                                          |
      | propertyName | db_arch                                                                                     |
      | inputName    | sys_arch                                                                                    |
    And I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodePropertyAsInputOperation |
      | nodeName     | DB                                                                                          |
      | propertyName | db_type                                                                                     |
      | inputName    | sys_type                                                                                    |
    Given I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId | tosca.nodes.Root                                                                  |
    And I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | DB                                                                                          |
      | substitutionCapabilityId | hostMysql                                                                                   |
      | capabilityId             | database_endpoint                                                                           |
    And I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | WWW                                                                                         |
      | substitutionCapabilityId | hostApache                                                                                  |
      | capabilityId             | hostApache                                                                                  |
    And I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | WWW                                                                                         |
      | substitutionCapabilityId | attachWebsite                                                                               |
      | capabilityId             | attachWebsite                                                                               |
    And I save the topology
    # Now create the application that use this LAMP to deploy a wordpress
    Given I create a new application with name "myWebapp2" and description "A webapp that use 2 embeded topology."
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | myLAMP                                                                |
      | indexedNodeTypeId | net.sample.LAMP2:0.1.0-SNAPSHOT                                       |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | myWordpress                                                           |
      | indexedNodeTypeId | alien.nodes.Wordpress:2.0.0-SNAPSHOT                                  |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | myWordpress                                                                           |
      | relationshipName       | hostOnApache                                                                          |
      | relationshipType       | alien.relationships.WordpressHostedOnApache                                           |
      | relationshipVersion    | 2.0.0-SNAPSHOT                                                                        |
      | requirementName        | host                                                                                  |
      | target                 | myLAMP                                                                                |
      | targetedCapabilityName | hostApache                                                                            |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | myWordpress                                                                           |
      | relationshipName       | connectToDb                                                                           |
      | relationshipType       | alien.relationships.WordpressConnectToMysql                                           |
      | relationshipVersion    | 2.0.0-SNAPSHOT                                                                        |
      | requirementName        | database                                                                              |
      | target                 | myLAMP                                                                                |
      | targetedCapabilityName | hostMysql                                                                             |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | myWordpress                                                                           |
      | relationshipName       | connectToPhp                                                                          |
      | relationshipType       | alien.relationships.WordpressConnectToPHP                                             |
      | relationshipVersion    | 2.0.0-SNAPSHOT                                                                        |
      | requirementName        | php                                                                                   |
      | target                 | myLAMP                                                                                |
      | targetedCapabilityName | attachWebsite                                                                         |
    And I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodePropertyValueOperation |
      | nodeName      | myLAMP                                                                                |
      | propertyName  | sys_arch                                                                              |
      | propertyValue | x86_64                                                                                |
    And I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodePropertyValueOperation |
      | nodeName      | myLAMP                                                                                |
      | propertyName  | sys_type                                                                              |
      | propertyValue | linux                                                                                 |
    And I save the topology
    Given I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes
    When I deploy it
    Then I should receive a RestResponse with no error
    And The application's deployment must succeed
    When I get the deployment toology for the current application
    Then I should receive a RestResponse with no error
    When I register the rest response data as SPEL context of type2 "alien4cloud.rest.deployment.DeploymentTopologyDTO"
    Then The SPEL int expression "topology.nodeTemplates.size()" should return 6
    # the properties of the node MyCompute of the embeded topology are fed by the input -> the properties of the proxy node
    And The SPEL expression "topology.nodeTemplates['myLAMP_WWW_MyCompute'].capabilities['os'].properties['architecture'].value" should return "x86_64"
    And The SPEL expression "topology.nodeTemplates['myLAMP_WWW_MyCompute'].capabilities['os'].properties['type'].value" should return "linux"
    And The SPEL expression "topology.nodeTemplates['myLAMP_DB_MyCompute'].capabilities['os'].properties['architecture'].value" should return "x86_64"
    And The SPEL expression "topology.nodeTemplates['myLAMP_DB_MyCompute'].capabilities['os'].properties['type'].value" should return "linux"
    # check that the relationships are correctly mapped
    # wordpress should be connected to the embeded apache
    And The SPEL expression "topology.nodeTemplates['myWordpress'].relationships.^[value.type == 'alien.relationships.WordpressHostedOnApache'].values().iterator().next().target" should return "myLAMP_WWW_MyApache"
    # the apache should be connected to the compute
    And The SPEL expression "topology.nodeTemplates['myLAMP_WWW_MyApache'].relationships.^[value.type == 'tosca.relationships.HostedOn'].values().iterator().next().target" should return "myLAMP_WWW_MyCompute"

  @reset
  Scenario: Topology composition with interaction
# in this scenario we have 2 templates:
# - 1 mysql subsystem (only mysql)
# - 1 apache + php subsystem (apache + php + compute)
# An app will use those 2 subsystems to deploy a wordpress
    # The first topology template containing a MySql
    Given I create a new topology template with name "net.sample.MySqlSubsystem" and description "A Mysql as a embedable topology template"
    And I should receive a RestResponse with no error
    And The RestResponse should contain a topology template id
    And I can get and register the topology for the last version of the registered topology template
    And I get the current topology
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | MyMysql                                                               |
      | indexedNodeTypeId | alien.nodes.Mysql:2.0.0-SNAPSHOT                                      |
    Given I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId | alien.nodes.Mysql                                                                 |
    And I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | MyMysql                                                                                     |
      | substitutionCapabilityId | database_endpoint                                                                           |
      | capabilityId             | database_endpoint                                                                           |
    And I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.AddRequirementSubstitutionTypeOperation |
      | nodeTemplateName          | MyMysql                                                                                      |
      | substitutionRequirementId | host                                                                                         |
      | requirementId             | host                                                                                         |
    And I save the topology
    # The second topology template containing a Apache + PHP + Compute
    Given I create a new topology template with name "net.sample.MyApacheSubsystem" and description "A Linux Apache PHP stack as a embedable topology template"
    And I should receive a RestResponse with no error
    And The RestResponse should contain a topology template id
    And I can get and register the topology for the last version of the registered topology template
    And I get the current topology
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | MyCompute                                                             |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0.0-SNAPSHOT                                    |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | www_arch                                                         |
      | propertyDefinition.type | string                                                           |
    And I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodeCapabilityPropertyAsInputOperation |
      | nodeName       | MyCompute                                                                                             |
      | capabilityName | os                                                                                                    |
      | propertyName   | architecture                                                                                          |
      | inputName      | www_arch                                                                                              |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | www_type                                                         |
      | propertyDefinition.type | string                                                           |
    And I execute the operation
      | type           | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodeCapabilityPropertyAsInputOperation |
      | nodeName       | MyCompute                                                                                             |
      | capabilityName | os                                                                                                    |
      | propertyName   | type                                                                                                  |
      | inputName      | www_type                                                                                              |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | MyApache                                                              |
      | indexedNodeTypeId | alien.nodes.Apache:2.0.0-SNAPSHOT                                     |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | MyPHP                                                                 |
      | indexedNodeTypeId | alien.nodes.PHP:2.0.0-SNAPSHOT                                        |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | MyApache                                                                              |
      | relationshipName       | MyRelationship                                                                        |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0.0-SNAPSHOT                                                                        |
      | requirementName        | host                                                                                  |
      | target                 | MyCompute                                                                             |
      | targetedCapabilityName | host                                                                                  |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | MyPHP                                                                                 |
      | relationshipName       | MyRelationship                                                                        |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0.0-SNAPSHOT                                                                        |
      | requirementName        | host                                                                                  |
      | target                 | MyCompute                                                                             |
      | targetedCapabilityName | host                                                                                  |
    And I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId | tosca.nodes.Root                                                                  |
    And I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | MyApache                                                                                    |
      | substitutionCapabilityId | hostApache                                                                                  |
      | capabilityId             | host                                                                                        |
    And I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | MyCompute                                                                                   |
      | substitutionCapabilityId | host                                                                                        |
      | capabilityId             | host                                                                                        |
    And I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | MyPHP                                                                                       |
      | substitutionCapabilityId | attachWebsite                                                                               |
      | capabilityId             | attachWebsite                                                                               |
    And I save the topology
    # The third topology template combining the 2 others
    Given I create a new topology template with name "net.sample.LAMP2" and description "A Linux Apache Mysql PHP stack as a embedable topology template"
    And I should receive a RestResponse with no error
    And The RestResponse should contain a topology template id
    And I can get and register the topology for the last version of the registered topology template
    And I get the current topology
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | WWW                                                                   |
      | indexedNodeTypeId | net.sample.MyApacheSubsystem:0.1.0-SNAPSHOT                           |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | sys_arch                                                         |
      | propertyDefinition.type | string                                                           |
    And I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodePropertyAsInputOperation |
      | nodeName     | WWW                                                                                         |
      | propertyName | www_arch                                                                                    |
      | inputName    | sys_arch                                                                                    |
    And I execute the operation
      | type                    | org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation |
      | inputName               | sys_type                                                         |
      | propertyDefinition.type | string                                                           |
    And I execute the operation
      | type         | org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodePropertyAsInputOperation |
      | nodeName     | WWW                                                                                         |
      | propertyName | www_type                                                                                    |
      | inputName    | sys_type                                                                                    |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | DB                                                                    |
      | indexedNodeTypeId | net.sample.MySqlSubsystem:0.1.0-SNAPSHOT                              |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | DB                                                                                    |
      | relationshipName       | MyRelationship                                                                        |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0.0-SNAPSHOT                                                                        |
      | requirementName        | host                                                                                  |
      | target                 | WWW                                                                                   |
      | targetedCapabilityName | host                                                                                  |
    And I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId | tosca.nodes.Root                                                                  |
    And I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | DB                                                                                          |
      | substitutionCapabilityId | hostMysql                                                                                   |
      | capabilityId             | database_endpoint                                                                           |
    And I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | WWW                                                                                         |
      | substitutionCapabilityId | hostApache                                                                                  |
      | capabilityId             | hostApache                                                                                  |
    And I execute the operation
      | type                     | org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation |
      | nodeTemplateName         | WWW                                                                                         |
      | substitutionCapabilityId | attachWebsite                                                                               |
      | capabilityId             | attachWebsite                                                                               |
    And I save the topology
    # Now create the application that use this LAMP to deploy a wordpress
    Given I create a new application with name "myWebapp2" and description "A webapp that use 2 embeded topology."
    And I get the current topology
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | myLAMP                                                                |
      | indexedNodeTypeId | net.sample.LAMP2:0.1.0-SNAPSHOT                                       |
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | myWordpress                                                           |
      | indexedNodeTypeId | alien.nodes.Wordpress:2.0.0-SNAPSHOT                                  |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | myWordpress                                                                           |
      | relationshipName       | hostOnApache                                                                          |
      | relationshipType       | alien.relationships.WordpressHostedOnApache                                           |
      | relationshipVersion    | 2.0.0-SNAPSHOT                                                                        |
      | requirementName        | host                                                                                  |
      | target                 | myLAMP                                                                                |
      | targetedCapabilityName | hostApache                                                                            |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | myWordpress                                                                           |
      | relationshipName       | connectToDb                                                                           |
      | relationshipType       | alien.relationships.WordpressConnectToMysql                                           |
      | relationshipVersion    | 2.0.0-SNAPSHOT                                                                        |
      | requirementName        | database                                                                              |
      | target                 | myLAMP                                                                                |
      | targetedCapabilityName | hostMysql                                                                             |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | myWordpress                                                                           |
      | relationshipName       | connectToPhp                                                                          |
      | relationshipType       | alien.relationships.WordpressConnectToPHP                                             |
      | relationshipVersion    | 2.0.0-SNAPSHOT                                                                        |
      | requirementName        | php                                                                                   |
      | target                 | myLAMP                                                                                |
      | targetedCapabilityName | attachWebsite                                                                         |
    And I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodePropertyValueOperation |
      | nodeName      | myLAMP                                                                                |
      | propertyName  | sys_arch                                                                              |
      | propertyValue | x86_64                                                                                |
    And I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodePropertyValueOperation |
      | nodeName      | myLAMP                                                                                |
      | propertyName  | sys_type                                                                              |
      | propertyValue | linux                                                                                 |
    And I save the topology
    Given I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes
    When I deploy it
    Then I should receive a RestResponse with no error
    And The application's deployment must succeed
    When I get the deployment toology for the current application
    Then I should receive a RestResponse with no error
    When I register the rest response data as SPEL context of type2 "alien4cloud.rest.deployment.DeploymentTopologyDTO"
    Then The SPEL int expression "topology.nodeTemplates.size()" should return 5
    # the properties of the node MyCompute of the embeded topology are fed by the input -> the properties of the proxy node
    And The SPEL expression "topology.nodeTemplates['myLAMP_WWW_MyCompute'].capabilities['os'].properties['architecture'].value" should return "x86_64"
    And The SPEL expression "topology.nodeTemplates['myLAMP_WWW_MyCompute'].capabilities['os'].properties['type'].value" should return "linux"
    # check that the relationships are correctly mapped
    # wordpress should be connected to the embeded apache
    And The SPEL expression "topology.nodeTemplates['myWordpress'].relationships.^[value.type == 'alien.relationships.WordpressHostedOnApache'].values().iterator().next().target" should return "myLAMP_WWW_MyApache"
    # the apache should be connected to the compute
    And The SPEL expression "topology.nodeTemplates['myLAMP_WWW_MyApache'].relationships.^[value.type == 'tosca.relationships.HostedOn'].values().iterator().next().target" should return "myLAMP_WWW_MyCompute"
    # the mysql should be connected to the compute
    And The SPEL expression "topology.nodeTemplates['myLAMP_DB_MyMysql'].relationships.^[value.type == 'tosca.relationships.HostedOn'].values().iterator().next().target" should return "myLAMP_WWW_MyCompute"


  @reset
  Scenario: Cyclic reference
# When a topology template is exposed as a type, we forbid the use of this type in the same topology template
# (since it will cause endless recursive calls). Here we test this limitation.
    Given I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId | tosca.nodes.Root                                                                  |
    And I save the topology
    Then I should receive a RestResponse with no error
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | MyLampNode                                                            |
      | indexedNodeTypeId | net.sample.LAMP:0.1.0-SNAPSHOT                                        |
    Then I should receive a RestResponse with an error code 820

  @reset
  Scenario: Indirect cyclic reference
# Scenario:
# - net.sample.LAMP is exposed as a type
# - I cretae a template net.sample.LAMP2 that uses the type net.sample.LAMP and is exposed itself as a type
# - I try to add a node of type net.sample.LAMP2 in the topo net.sample.LAMP
# This is not allowed since it cause cyclic reference (LAMP -> LAMP2 -> LAMP)
    Given I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId | tosca.nodes.Root                                                                  |
    And I save the topology
    Then I should receive a RestResponse with no error
    Given I create a new topology template with name "net.sample.LAMP2" and description "A Linux Apache Mysql PHP stack as a embedable topology template"
    And I should receive a RestResponse with no error
    And The RestResponse should contain a topology template id
    And I can get and register the topology for the last version of the registered topology template
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | MyLampNode                                                            |
      | indexedNodeTypeId | net.sample.LAMP:0.1.0-SNAPSHOT                                        |
    And I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId | tosca.nodes.Root                                                                  |
    And I save the topology
    When I should be able to retrieve a topology with name "net.sample.LAMP" version "0.1.0-SNAPSHOT" and store it as a SPEL context
    And I get the current topology
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Lamp2                                                                 |
      | indexedNodeTypeId | net.sample.LAMP2:0.1.0-SNAPSHOT                                       |
    Then I should receive a RestResponse with an error code 820

  @reset
  Scenario: Delete referenced topology template
# A topology template that is exposed as a type and used in another topology can not be deleted
    Given I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId | tosca.nodes.Root                                                                  |
    And I save the topology
    And I create a new topology template version named "0.2.0-SNAPSHOT" based on the current version
    And I create a new application with name "myWebapp" and description "A webapp that use an embeded topology."
    And I get the current topology
    And I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | myLAMP                                                                |
      | indexedNodeTypeId | net.sample.LAMP:0.1.0-SNAPSHOT                                        |
    And I save the topology
    When I delete the topology template named "net.sample.LAMP"
    Then I should receive a RestResponse with an error code 507
    When I delete the topology template named "net.sample.LAMP" version "0.1.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 507
    Given I delete the application "myWebapp"
    When I delete the topology template named "net.sample.LAMP"
    Then I should receive a RestResponse with no error
