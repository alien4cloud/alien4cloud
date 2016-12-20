Feature: This is not a test, it reuses integration test step to set up Alien with all data

  @reset
  Scenario: Setup Alien
    Given I am authenticated with "ADMIN" role
    And I have already created a cloud image with name "Ubuntu Trusty", architecture "x86_64", type "linux", distribution "Ubuntu" and version "14.04.1"

    # Archives
    And I upload the archive "tosca-normative-types"
    And I upload the archive "alien-base-types"
    And I upload the archive "alien-extended-storage-types"
    And I upload the archive "samples apache"
    And I upload the archive "samples mysql"
    And I upload the archive "samples php"
    And I upload the archive "samples wordpress"
    And I upload the archive "samples topology wordpress"

    # Mock cloud
    And I upload a plugin
    And I create a cloud with name "Mock Cloud" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-paas-provider"
    And I enable the cloud "Mock Cloud"
    And I add the cloud image "Ubuntu Trusty" to the cloud "Mock Cloud" and match it to paaS image "UBUNTU"
    And I add the flavor with name "small", number of CPUs 2, disk size 34359738368 and memory size 2147483648 to the cloud "Mock Cloud" and match it to paaS flavor "SMALL"
    And I add the network with name "private" and CIDR "192.168.1.0/24" and IP version 4 and gateway "192.168.1.1" to the cloud "Mock Cloud"
    And I match the network with name "private" of the cloud "Mock Cloud" to the PaaS resource "APPLICATION_NET"
    And I add the public network with name "public" to the cloud "Mock Cloud" and match it to paaS network "net-pub"
    And I add the storage with id "SmallBlock" and device "/dev/vdb" and size 1073741824 to the cloud "Mock Cloud"
    And I match the storage with name "SmallBlock" of the cloud "Mock Cloud" to the PaaS resource "SMALL_BLOCK"
    And I add the availability zone with id "grenoble" and description "Data-center at Grenoble" to the cloud "Mock Cloud"
    And I match the availability zone with name "grenoble" of the cloud "Mock Cloud" to the PaaS resource "grenoble-zone"
    And I add the availability zone with id "paris" and description "Data-center at Paris" to the cloud "Mock Cloud"
    And I match the availability zone with name "paris" of the cloud "Mock Cloud" to the PaaS resource "paris-zone"
    And I add the availability zone with id "toulouse" and description "Data-center at Toulouse" to the cloud "Mock Cloud"
    And I match the availability zone with name "toulouse" of the cloud "Mock Cloud" to the PaaS resource "toulouse-zone"

    # Mock Application
    And I create a new application with name "wordpress-mock" and description "Wordpress with Mock" based on the template with name "wordpress-template"
    And I assign the cloud with name "Mock Cloud" for the application
    And I add a node template "DbStorage" related to the "alien.nodes.ConfigurableBlockStorage:1.0-SNAPSHOT" node type
    And I update the node template "DbStorage"'s property "location" to "/var/mysql"
    And I update the node template "DbStorage"'s property "device" to "/dev/vdb"
    And I update the node template "DbStorage"'s property "file_system" to "ext4"
    And I update the node template "mysql"'s property "storage_path" to "/var/mysql"
    And I add a relationship of type "tosca.relationships.AttachTo" defined in archive "tosca-normative-types" version "1.0.0.wd03-SNAPSHOT" with source "DbStorage" and target "computeDb" for requirement "attachment" of type "tosca.capabilities.Attachment" and target capability "attach"
    And I add a node template "internet" related to the "tosca.nodes.Network:1.0.0.wd03-SNAPSHOT" node type
    And I add a relationship of type "tosca.relationships.Network" defined in archive "tosca-normative-types" version "1.0.0.wd03-SNAPSHOT" with source "computeWww" and target "internet" for requirement "network" of type "tosca.capabilities.Connectivity" and target capability "connection"
    And I add a node template "privateNetwork" related to the "tosca.nodes.Network:1.0.0.wd03-SNAPSHOT" node type
    And I add a relationship of type "tosca.relationships.Network" defined in archive "tosca-normative-types" version "1.0.0.wd03-SNAPSHOT" with source "computeDb" and target "privateNetwork" for requirement "network" of type "tosca.capabilities.Connectivity" and target capability "connection"
    And I add a relationship of type "tosca.relationships.Network" defined in archive "tosca-normative-types" version "1.0.0.wd03-SNAPSHOT" with source "computeWww" and target "privateNetwork" for requirement "network" of type "tosca.capabilities.Connectivity" and target capability "connection"
    And I set the input property "os_arch" of the topology to "x86_64"
    And I set the input property "os_type" of the topology to "linux"
    And I select the network with name "public" for my node "internet"
    And I select the network with name "private" for my node "privateNetwork"
