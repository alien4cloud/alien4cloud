# in these scenari, we will create a topology template, expose it as type (substitution) and then reuse it in an application topology
Feature: Topology composition

  Background:
    Given I am authenticated with "ADMIN" role
      And I upload the archive "tosca-normative-types"
      And I upload the archive "tosca-normative-types-wd06"
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
    Given I create a new topology template with name "net.sample.LAMP" and description "A Linux Apache Mysql PHP stack as a embedable topology template"
      And The RestResponse should contain a topology template id
      And I can get and register the topology for the last version of the registered topology template
    Given I add a node template "MyCompute" related to the "tosca.nodes.Compute:1.0.0.wd06-SNAPSHOT" node type
      And I define the capability "os" property "architecture" of the node "MyCompute" as input property
      And I rename the input "architecture" to "os_arch"
      And I set the property "architecture" of capability "os" the node "MyCompute" as input property name "os_arch"
      And I define the capability "os" property "type" of the node "MyCompute" as input property
      And I rename the input "type" to "os_type"
      And I set the property "type" of capability "os" the node "MyCompute" as input property name "os_type"  
      And I add a node template "MyApache" related to the "alien.nodes.Apache:2.0.0-SNAPSHOT" node type
      And I add a node template "MyMysql" related to the "alien.nodes.Mysql:2.0.0-SNAPSHOT" node type
      And I define the property "db_port" of the node "MyMysql" of typeId "alien.nodes.Mysql:2.0.0-SNAPSHOT" as input property
      And I associate the property "db_port" of a node template "MyMysql" to the input "db_port"
      And I define the property "db_name" of the node "MyMysql" of typeId "alien.nodes.Mysql:2.0.0-SNAPSHOT" as input property
      And I associate the property "db_name" of a node template "MyMysql" to the input "db_name"
      And I define the property "db_user" of the node "MyMysql" of typeId "alien.nodes.Mysql:2.0.0-SNAPSHOT" as input property
      And I associate the property "db_user" of a node template "MyMysql" to the input "db_user"
      And I define the property "db_password" of the node "MyMysql" of typeId "alien.nodes.Mysql:2.0.0-SNAPSHOT" as input property
      And I associate the property "db_password" of a node template "MyMysql" to the input "db_password"
      And I define the property "db_port" of the node "MyMysql" as output property
      And I add a node template "MyPHP" related to the "alien.nodes.PHP:2.0.0-SNAPSHOT" node type
      And I add a relationship of type "tosca.relationships.HostedOn" defined in archive "tosca-normative-types" version "1.0.0.wd03-SNAPSHOT" with source "MyMysql" and target "MyCompute" for requirement "host" of type "tosca.nodes.Compute" and target capability "host"
      And I add a relationship of type "tosca.relationships.HostedOn" defined in archive "tosca-normative-types" version "1.0.0.wd03-SNAPSHOT" with source "MyApache" and target "MyCompute" for requirement "host" of type "tosca.nodes.Compute" and target capability "host"
      And I add a relationship of type "tosca.relationships.HostedOn" defined in archive "tosca-normative-types" version "1.0.0.wd03-SNAPSHOT" with source "MyPHP" and target "MyCompute" for requirement "host" of type "tosca.nodes.Compute" and target capability "host"
      And I pre register orchestrator properties
        | managementUrl | http://cloudifyurl:8099 |
        | numberBackup  | 1                       |
        | managerEmail  | admin@alien.fr          |

  Scenario: Expose the template as a type and check type properties and attributes
    Given I expose the template as type "tosca.nodes.Root"
    Then I should receive a RestResponse with no error
    Given I define the attribute "ip_address" of the node "MyCompute" as output attribute
    And I define the property "port" of the capability "app_endpoint" of the node "MyApache" as output property
    When I try to get a component with id "net.sample.LAMP:0.1.0-SNAPSHOT"
    Then I should receive a RestResponse with no error
    And I should have a component with id "net.sample.LAMP:0.1.0-SNAPSHOT"
    When I register the rest response data as SPEL context of type "alien4cloud.model.components.IndexedNodeType"
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
    And The SPEL boolean expression "attributes.containsKey('db_port')" should return true
    And The SPEL boolean expression "attributes.containsKey('port')" should return true

  Scenario: Expose capabilities and check type capabilities
    Given I expose the template as type "tosca.nodes.Root"
    And I expose the capability "database_endpoint" for the node "MyMysql"
    And I rename the exposed capability "database_endpoint" to "hostMysql"
    And I expose the capability "host" for the node "MyApache"
    And I rename the exposed capability "host" to "hostApache"
    And I expose the capability "attachWebsite" for the node "MyPHP"
    When I try to get a component with id "net.sample.LAMP:0.1.0-SNAPSHOT"
    Then I should receive a RestResponse with no error
    And I should have a component with id "net.sample.LAMP:0.1.0-SNAPSHOT"
    When I register the rest response data as SPEL context of type "alien4cloud.model.components.IndexedNodeType"
    Then The SPEL expression "capabilities.^[id == 'hostMysql'].type" should return "alien.capabilities.MysqlDatabaseEndpoint"
    And The SPEL expression "capabilities.^[id == 'hostApache'].type" should return "alien.capabilities.ApacheContainer"
    And The SPEL expression "capabilities.^[id == 'attachWebsite'].type" should return "alien.capabilities.PHPModule"

  Scenario: Expose capabilities and use it in a topology
    Given I expose the template as type "tosca.nodes.Root"
    And I expose the capability "database_endpoint" for the node "MyMysql"
    And I expose the capability "host" for the node "MyApache"
    And I rename the exposed capability "host" to "hostApache"
    And I expose the capability "attachWebsite" for the node "MyPHP"
    And I define the attribute "ip_address" of the node "MyCompute" as output attribute
    And I define the property "port" of the capability "app_endpoint" of the node "MyApache" as output property
    Given I create a new application with name "myWebapp" and description "A webapp that use an embeded topology."
    And I add a node template "myLAMP" related to the "net.sample.LAMP:0.1.0-SNAPSHOT" node type
    And I add a node template "myWordpress" related to the "alien.nodes.Wordpress:2.0.0-SNAPSHOT" node type
    And I add a relationship of type "alien.relationships.WordpressHostedOnApache" defined in archive "wordpress-type" version "2.0.0-SNAPSHOT" with source "myWordpress" and target "myLAMP" for requirement "host" of type "alien.capabilities.ApacheContainer" and target capability "hostApache"
    And I add a relationship of type "alien.relationships.WordpressConnectToMysql" defined in archive "wordpress-type" version "2.0.0-SNAPSHOT" with source "myWordpress" and target "myLAMP" for requirement "database" of type "alien.capabilities.MysqlDatabaseEndpoint" and target capability "database_endpoint"
    And I add a relationship of type "alien.relationships.WordpressConnectToPHP" defined in archive "wordpress-type" version "2.0.0-SNAPSHOT" with source "myWordpress" and target "myLAMP" for requirement "php" of type "alien.capabilities.PHPModule" and target capability "attachWebsite"
    And I update the node template "myLAMP"'s property "os_arch" to "x86_64"
    And I update the node template "myLAMP"'s property "os_type" to "linux"
    And I define the attribute "ip_address" of the node "myLAMP" as output attribute
    And I define the attribute "db_port" of the node "myLAMP" as output attribute
    And I define the attribute "port" of the node "myLAMP" as output attribute
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
    And The SPEL expression "topology.outputProperties['myLAMP_MyMysql'][0]" should return "db_port"
    And The SPEL expression "topology.outputAttributes['myLAMP_MyCompute'][0]" should return "ip_address"
    And The SPEL expression "topology.outputCapabilityProperties['myLAMP_MyApache']['app_endpoint'][0]" should return "port"

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
    And I add a node template "MyCompute" related to the "tosca.nodes.Compute:1.0.0.wd06-SNAPSHOT" node type
    And I define the capability "os" property "architecture" of the node "MyCompute" as input property
    And I rename the input "architecture" to "db_arch"
    And I set the property "architecture" of capability "os" the node "MyCompute" as input property name "db_arch"
    And I define the capability "os" property "type" of the node "MyCompute" as input property
    And I rename the input "type" to "db_type"
    And I set the property "type" of capability "os" the node "MyCompute" as input property name "db_type"
    And I add a node template "MyMysql" related to the "alien.nodes.Mysql:2.0.0-SNAPSHOT" node type
    And I add a relationship of type "tosca.relationships.HostedOn" defined in archive "tosca-normative-types" version "1.0.0.wd03-SNAPSHOT" with source "MyMysql" and target "MyCompute" for requirement "host" of type "tosca.nodes.Compute" and target capability "host"
    And I expose the template as type "alien.nodes.Mysql"
    And I expose the capability "database_endpoint" for the node "MyMysql"
    # The second topology template containing a Apache + PHP + Compute
    Given I create a new topology template with name "net.sample.MyApacheSubsystem" and description "A Linux Apache PHP stack as a embedable topology template"
    And I should receive a RestResponse with no error
    And The RestResponse should contain a topology template id
    And I can get and register the topology for the last version of the registered topology template
    And I add a node template "MyCompute" related to the "tosca.nodes.Compute:1.0.0.wd06-SNAPSHOT" node type
    And I define the capability "os" property "architecture" of the node "MyCompute" as input property
    And I rename the input "architecture" to "www_arch"
    And I set the property "architecture" of capability "os" the node "MyCompute" as input property name "www_arch"
    And I define the capability "os" property "type" of the node "MyCompute" as input property
    And I rename the input "type" to "www_type"
    And I set the property "type" of capability "os" the node "MyCompute" as input property name "www_type"        
    And I add a node template "MyApache" related to the "alien.nodes.Apache:2.0.0-SNAPSHOT" node type
    And I add a node template "MyPHP" related to the "alien.nodes.PHP:2.0.0-SNAPSHOT" node type
    And I add a relationship of type "tosca.relationships.HostedOn" defined in archive "tosca-normative-types" version "1.0.0.wd03-SNAPSHOT" with source "MyApache" and target "MyCompute" for requirement "host" of type "tosca.nodes.Compute" and target capability "host"
    And I add a relationship of type "tosca.relationships.HostedOn" defined in archive "tosca-normative-types" version "1.0.0.wd03-SNAPSHOT" with source "MyPHP" and target "MyCompute" for requirement "host" of type "tosca.nodes.Compute" and target capability "host"
    And I expose the template as type "tosca.nodes.Root"
    And I expose the capability "host" for the node "MyApache"
    And I rename the exposed capability "host" to "hostApache"
    And I expose the capability "attachWebsite" for the node "MyPHP"
    # The third topology template combining the 2 others
    Given I create a new topology template with name "net.sample.LAMP2" and description "A Linux Apache Mysql PHP stack as a embedable topology template"
    And I should receive a RestResponse with no error
    And The RestResponse should contain a topology template id
    And I can get and register the topology for the last version of the registered topology template
    And I add a node template "WWW" related to the "net.sample.MyApacheSubsystem:0.1.0-SNAPSHOT" node type
    And I define the property "www_arch" of the node "WWW" of typeId "net.sample.MyApacheSubsystem:0.1.0-SNAPSHOT" as input property
    And I rename the input "www_arch" to "sys_arch"
    And I associate the property "www_arch" of a node template "WWW" to the input "sys_arch"
    And I define the property "www_type" of the node "WWW" of typeId "net.sample.MyApacheSubsystem:0.1.0-SNAPSHOT" as input property
    And I rename the input "www_type" to "sys_type"
    And I associate the property "www_type" of a node template "WWW" to the input "sys_type"
    And I add a node template "DB" related to the "net.sample.MySqlSubsystem:0.1.0-SNAPSHOT" node type
    And I associate the property "db_arch" of a node template "DB" to the input "sys_arch"
    And I associate the property "db_type" of a node template "DB" to the input "sys_type"
    And I expose the template as type "tosca.nodes.Root"
    And I expose the capability "database_endpoint" for the node "DB"
    And I rename the exposed capability "database_endpoint" to "hostMysql"
    And I expose the capability "hostApache" for the node "WWW"
    And I expose the capability "attachWebsite" for the node "WWW"
    # Now create the application that use this LAMP to deploy a wordpress
    Given I create a new application with name "myWebapp2" and description "A webapp that use 2 embeded topology."
    And I add a node template "myLAMP" related to the "net.sample.LAMP2:0.1.0-SNAPSHOT" node type
    And I add a node template "myWordpress" related to the "alien.nodes.Wordpress:2.0.0-SNAPSHOT" node type
    And I add a relationship of type "alien.relationships.WordpressHostedOnApache" defined in archive "wordpress-type" version "2.0.0-SNAPSHOT" with source "myWordpress" and target "myLAMP" for requirement "host" of type "alien.capabilities.ApacheContainer" and target capability "hostApache"
    And I add a relationship of type "alien.relationships.WordpressConnectToMysql" defined in archive "wordpress-type" version "2.0.0-SNAPSHOT" with source "myWordpress" and target "myLAMP" for requirement "database" of type "alien.capabilities.MysqlDatabaseEndpoint" and target capability "hostMysql"
    And I add a relationship of type "alien.relationships.WordpressConnectToPHP" defined in archive "wordpress-type" version "2.0.0-SNAPSHOT" with source "myWordpress" and target "myLAMP" for requirement "php" of type "alien.capabilities.PHPModule" and target capability "attachWebsite"
    And I update the node template "myLAMP"'s property "sys_arch" to "x86_64"
    And I update the node template "myLAMP"'s property "sys_type" to "linux"
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
    And I add a node template "MyMysql" related to the "alien.nodes.Mysql:2.0.0-SNAPSHOT" node type
    And I expose the template as type "alien.nodes.Mysql"
    And I expose the capability "database_endpoint" for the node "MyMysql"
    And I expose the requirement "host" for the node "MyMysql"
    # The second topology template containing a Apache + PHP + Compute
    Given I create a new topology template with name "net.sample.MyApacheSubsystem" and description "A Linux Apache PHP stack as a embedable topology template"
    And I should receive a RestResponse with no error
    And The RestResponse should contain a topology template id
    And I can get and register the topology for the last version of the registered topology template
    And I add a node template "MyCompute" related to the "tosca.nodes.Compute:1.0.0.wd06-SNAPSHOT" node type
    And I define the capability "os" property "architecture" of the node "MyCompute" as input property
    And I rename the input "architecture" to "www_arch"
    And I set the property "architecture" of capability "os" the node "MyCompute" as input property name "www_arch"
    And I define the capability "os" property "type" of the node "MyCompute" as input property
    And I rename the input "type" to "www_type"
    And I set the property "type" of capability "os" the node "MyCompute" as input property name "www_type"
    And I add a node template "MyApache" related to the "alien.nodes.Apache:2.0.0-SNAPSHOT" node type
    And I add a node template "MyPHP" related to the "alien.nodes.PHP:2.0.0-SNAPSHOT" node type
    And I add a relationship of type "tosca.relationships.HostedOn" defined in archive "tosca-normative-types" version "1.0.0.wd03-SNAPSHOT" with source "MyApache" and target "MyCompute" for requirement "host" of type "tosca.nodes.Compute" and target capability "host"
    And I add a relationship of type "tosca.relationships.HostedOn" defined in archive "tosca-normative-types" version "1.0.0.wd03-SNAPSHOT" with source "MyPHP" and target "MyCompute" for requirement "host" of type "tosca.nodes.Compute" and target capability "host"
    And I expose the template as type "tosca.nodes.Root"
    And I expose the capability "host" for the node "MyApache"
    And I rename the exposed capability "host" to "hostApache"
    And I expose the capability "host" for the node "MyCompute"
    And I expose the capability "attachWebsite" for the node "MyPHP"
    # The third topology template combining the 2 others
    Given I create a new topology template with name "net.sample.LAMP2" and description "A Linux Apache Mysql PHP stack as a embedable topology template"
    And I should receive a RestResponse with no error
    And The RestResponse should contain a topology template id
    And I can get and register the topology for the last version of the registered topology template
    And I add a node template "WWW" related to the "net.sample.MyApacheSubsystem:0.1.0-SNAPSHOT" node type
    And I define the property "www_arch" of the node "WWW" of typeId "net.sample.MyApacheSubsystem:0.1.0-SNAPSHOT" as input property
    And I rename the input "www_arch" to "sys_arch"
    And I associate the property "www_arch" of a node template "WWW" to the input "sys_arch"
    And I define the property "www_type" of the node "WWW" of typeId "net.sample.MyApacheSubsystem:0.1.0-SNAPSHOT" as input property
    And I rename the input "www_type" to "sys_type"
    And I associate the property "www_type" of a node template "WWW" to the input "sys_type"
    And I add a node template "DB" related to the "net.sample.MySqlSubsystem:0.1.0-SNAPSHOT" node type
    And I add a relationship of type "tosca.relationships.HostedOn" defined in archive "tosca-normative-types" version "1.0.0.wd03-SNAPSHOT" with source "DB" and target "WWW" for requirement "host" of type "tosca.nodes.Compute" and target capability "host"
    And I expose the template as type "tosca.nodes.Root"
    And I expose the capability "database_endpoint" for the node "DB"
    And I rename the exposed capability "database_endpoint" to "hostMysql"
    And I expose the capability "hostApache" for the node "WWW"
    And I expose the capability "attachWebsite" for the node "WWW"
    # Now create the application that use this LAMP to deploy a wordpress
    Given I create a new application with name "myWebapp2" and description "A webapp that use 2 embeded topology."
    And I add a node template "myLAMP" related to the "net.sample.LAMP2:0.1.0-SNAPSHOT" node type
    And I add a node template "myWordpress" related to the "alien.nodes.Wordpress:2.0.0-SNAPSHOT" node type
    And I add a relationship of type "alien.relationships.WordpressHostedOnApache" defined in archive "wordpress-type" version "2.0.0-SNAPSHOT" with source "myWordpress" and target "myLAMP" for requirement "host" of type "alien.capabilities.ApacheContainer" and target capability "hostApache"
    And I add a relationship of type "alien.relationships.WordpressConnectToMysql" defined in archive "wordpress-type" version "2.0.0-SNAPSHOT" with source "myWordpress" and target "myLAMP" for requirement "database" of type "alien.capabilities.MysqlDatabaseEndpoint" and target capability "hostMysql"
    And I add a relationship of type "alien.relationships.WordpressConnectToPHP" defined in archive "wordpress-type" version "2.0.0-SNAPSHOT" with source "myWordpress" and target "myLAMP" for requirement "php" of type "alien.capabilities.PHPModule" and target capability "attachWebsite"
    And I update the node template "myLAMP"'s property "sys_arch" to "x86_64"
    And I update the node template "myLAMP"'s property "sys_type" to "linux"
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

Scenario: Cyclic reference
# When a topology template is exposed as a type, we forbid the use of this type in the same topology template
# (since it will cause endless recursive calls). Here we test this limitation.
    Given I expose the template as type "tosca.nodes.Root"
    Then I should receive a RestResponse with no error
    When I add a node template "MyCompute" related to the "net.sample.LAMP:0.1.0-SNAPSHOT" node type
    Then I should receive a RestResponse with an error code 820

Scenario: Indirect cyclic reference
# Scenario:
# - net.sample.LAMP is exposed as a type
# - I cretae a template net.sample.LAMP2 that uses the type net.sample.LAMP and is exposed itself as a type
# - I try to add a node of type net.sample.LAMP2 in the topo net.sample.LAMP
# This is not allowed since it cause cyclic reference (LAMP -> LAMP2 -> LAMP)
    Given I expose the template as type "tosca.nodes.Root"
    Then I should receive a RestResponse with no error
    Given I create a new topology template with name "net.sample.LAMP2" and description "A Linux Apache Mysql PHP stack as a embedable topology template"
    And I should receive a RestResponse with no error
    And The RestResponse should contain a topology template id
    And I can get and register the topology for the last version of the registered topology template
    And I add a node template "Lamp" related to the "net.sample.LAMP:0.1.0-SNAPSHOT" node type
    And I expose the template as type "tosca.nodes.Root"
    When If I search for topology templates I can find one with the name "net.sample.LAMP" version "0.1.0-SNAPSHOT" and store the related topology as a SPEL context
    And I add a node template "Lamp2" related to the "net.sample.LAMP2:0.1.0-SNAPSHOT" node type
    Then I should receive a RestResponse with an error code 820

Scenario: Delete referenced topology template
# A topology template that is exposed as a type and used in another topology can not be deleted
    Given I expose the template as type "tosca.nodes.Root"
    And I create a new topology template version named "0.2.0-SNAPSHOT" based on the current version
    And I create a new application with name "myWebapp" and description "A webapp that use an embeded topology."
    And I add a node template "myLAMP" related to the "net.sample.LAMP:0.1.0-SNAPSHOT" node type
    When I delete the topology template named "net.sample.LAMP"
    Then I should receive a RestResponse with an error code 507
    When I delete the topology template named "net.sample.LAMP" version "0.1.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 507
    Given I delete the application "myWebapp"
    When I delete the topology template named "net.sample.LAMP"
    Then I should receive a RestResponse with no error
