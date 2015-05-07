Feature: This is not a test, it reuses integration test step to set up Alien with all data

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

    # Cloudify 2
    And I upload a plugin from "../../alien4cloud-cloudify2-provider"
    And I create a cloud with name "Cloudify 2" from cloudify 2 PaaS provider
    And I update cloudify 2 manager's url to "http://129.185.67.27:8100" for cloud with name "Cloudify 2"
    And I enable the cloud "Cloudify 2"
    And I add the cloud image "Ubuntu Trusty" to the cloud "Cloudify 2" and match it to paaS image "RegionOne/2b4475df-b6d6-49b7-a062-a3a20d45ab7c"
    And I add the flavor with name "small", number of CPUs 2, disk size 34359738368 and memory size 2147483648 to the cloud "Cloudify 2" and match it to paaS flavor "RegionOne/2"
    And I add the network with name "private" and CIDR "192.168.1.0/24" and IP version 4 and gateway "192.168.1.1" to the cloud "Cloudify 2"
    And I match the network with name "private" of the cloud "Cloudify 2" to the PaaS resource "APPLICATION_NET"
    And I add the storage with id "SmallBlock" and device "/dev/vdb" and size 1073741824 to the cloud "Cloudify 2"
    And I match the storage with name "SmallBlock" of the cloud "Cloudify 2" to the PaaS resource "SMALL_BLOCK"
    And I add the availability zone with id "paris" and description "Data-center at Paris" to the cloud "Cloudify 2"
    And I match the availability zone with name "paris" of the cloud "Cloudify 2" to the PaaS resource "Fastconnect"
    And I add the availability zone with id "toulouse" and description "Data-center at Toulouse" to the cloud "Cloudify 2"
    And I match the availability zone with name "toulouse" of the cloud "Cloudify 2" to the PaaS resource "A4C-zone"

    # Application CFY 2
    And I create a new application with name "wordpress-cfy2" and description "Wordpress with CFY 2" based on the template with name "wordpress-template-1.1.0-SNAPSHOT"
    And I assign the cloud with name "Cloudify 2" for the application
    And I set the input property "os_arch" of the topology to "x86_64"
    And I set the input property "os_type" of the topology to "linux"
    And I add a node template "DbStorage" related to the "alien.nodes.ConfigurableBlockStorage:1.0-SNAPSHOT" node type
    And I update the node template "DbStorage"'s property "location" to "/var/mysql"
    And I update the node template "DbStorage"'s property "device" to "/dev/vdb"
    And I update the node template "DbStorage"'s property "file_system" to "ext4"
    And I update the node template "mysql"'s property "storage_path" to "/var/mysql"
    And I add a relationship of type "tosca.relationships.AttachTo" defined in archive "tosca-normative-types" version "1.0.0.wd03-SNAPSHOT" with source "DbStorage" and target "computeDb" for requirement "attachment" of type "tosca.capabilities.Attachment" and target capability "attach"
    And I add a node template "privateNetwork" related to the "tosca.nodes.Network:1.0.0.wd03-SNAPSHOT" node type
    And I add a relationship of type "tosca.relationships.Network" defined in archive "tosca-normative-types" version "1.0.0.wd03-SNAPSHOT" with source "computeDb" and target "privateNetwork" for requirement "network" of type "tosca.capabilities.Connectivity" and target capability "connection"
    And I add a relationship of type "tosca.relationships.Network" defined in archive "tosca-normative-types" version "1.0.0.wd03-SNAPSHOT" with source "computeWww" and target "privateNetwork" for requirement "network" of type "tosca.capabilities.Connectivity" and target capability "connection"
    And I set the input property "os_arch" of the topology to "x86_64"
    And I set the input property "os_type" of the topology to "linux"
    And I select the network with name "private" for my node "privateNetwork"