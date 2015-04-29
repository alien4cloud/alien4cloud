Feature: This is not a test, it reuses integration test step to set up Alien with all data

  Scenario: Setup Alien
    Given I am authenticated with "ADMIN" role
    And I have already created a cloud image with name "Ubuntu Trusty", architecture "x86_64", type "linux", distribution "Ubuntu" and version "14.04.1"

    # Mock cloud
    And I upload a plugin
    And I create a cloud with name "Mock Cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
    And I enable the cloud "Mock Cloud"
    And I add the cloud image "Ubuntu Trusty" to the cloud "Mock Cloud" and match it to paaS image "UBUNTU"
    And I add the flavor with name "small", number of CPUs 2, disk size 32 and memory size 2048 to the cloud "Mock Cloud" and match it to paaS flavor "SMALL"
    And I add the network with name "private" and CIDR "192.168.1.0/24" and IP version 4 and gateway "192.168.1.1" to the cloud "Mock Cloud"
    And I match the network with name "private" of the cloud "Mock Cloud" to the PaaS resource "APPLICATION_NET"
    And I add the storage with id "SmallBlock" and device "/dev/vdb" and size 1073741824 to the cloud "Mock Cloud"
    And I match the storage with name "SmallBlock" of the cloud "Mock Cloud" to the PaaS resource "SMALL_BLOCK"
    And I add the availability zone with id "grenoble" and description "Data-center at Grenoble" to the cloud "Mock Cloud"
    And I match the availability zone with name "grenoble" of the cloud "Mock Cloud" to the PaaS resource "grenoble-zone"
    And I add the availability zone with id "paris" and description "Data-center at Paris" to the cloud "Mock Cloud"
    And I match the availability zone with name "paris" of the cloud "Mock Cloud" to the PaaS resource "paris-zone"
    And I add the availability zone with id "toulouse" and description "Data-center at Toulouse" to the cloud "Mock Cloud"
    And I match the availability zone with name "toulouse" of the cloud "Mock Cloud" to the PaaS resource "toulouse-zone"

    # Cloudify 2
    And I upload a plugin from "../../alien4cloud-cloudify2-provider"
    And I create a cloud with name "Cloudify 2" from cloudify 2 PaaS provider
    And I update cloudify 2 manager's url to "http://129.185.67.27:8100" for cloud with name "Cloudify 2"
    And I enable the cloud "Cloudify 2"
    And I add the cloud image "Ubuntu Trusty" to the cloud "Cloudify 2" and match it to paaS image "RegionOne/2b4475df-b6d6-49b7-a062-a3a20d45ab7c"
    And I add the flavor with name "small", number of CPUs 2, disk size 32 and memory size 2048 to the cloud "Cloudify 2" and match it to paaS flavor "RegionOne/2"
    And I add the network with name "private" and CIDR "192.168.1.0/24" and IP version 4 and gateway "192.168.1.1" to the cloud "Cloudify 2"
    And I match the network with name "private" of the cloud "Cloudify 2" to the PaaS resource "APPLICATION_NET"
    And I add the storage with id "SmallBlock" and device "/dev/vdb" and size 1073741824 to the cloud "Cloudify 2"
    And I match the storage with name "SmallBlock" of the cloud "Cloudify 2" to the PaaS resource "SMALL_BLOCK"
    And I add the availability zone with id "paris" and description "Data-center at Paris" to the cloud "Cloudify 2"
    And I match the availability zone with name "paris" of the cloud "Cloudify 2" to the PaaS resource "Fastconnect"
    And I add the availability zone with id "toulouse" and description "Data-center at Toulouse" to the cloud "Cloudify 2"
    And I match the availability zone with name "toulouse" of the cloud "Cloudify 2" to the PaaS resource "A4C-zone"

    # Cloudify 3
    And I upload a plugin from "../../alien4cloud-cloudify3-provider"
    And I create a cloud with name "Cloudify 3" from cloudify 3 PaaS provider
    And I update cloudify 3 manager's url to "http://129.185.67.85:8100" for cloud with name "Cloudify 3"
    And I enable the cloud "Cloudify 3"
    And I add the cloud image "Ubuntu Trusty" to the cloud "Cloudify 3" and match it to paaS image "727df994-2e1b-404e-9276-b248223a835d"
    And I add the flavor with name "small", number of CPUs 2, disk size 32 and memory size 2048 to the cloud "Cloudify 3" and match it to paaS flavor "2"
    And I add the network with name "private" and CIDR "192.168.1.0/24" and IP version 4 and gateway "192.168.1.1" to the cloud "Cloudify 3"
    And I add the public network with name "public" to the cloud "Cloudify 3" and match it to paaS network "net-pub"
    And I add the storage with id "SmallBlock" and device "/dev/vdb" and size 1073741824 to the cloud "Cloudify 3"
    And I add the availability zone with id "paris" and description "Data-center at Paris" to the cloud "Cloudify 3"
    And I match the availability zone with name "paris" of the cloud "Cloudify 3" to the PaaS resource "Fastconnect"
    And I add the availability zone with id "toulouse" and description "Data-center at Toulouse" to the cloud "Cloudify 3"
    And I match the availability zone with name "toulouse" of the cloud "Cloudify 3" to the PaaS resource "A4C-zone"

    # Archives
    And I upload the archive "tosca-normative-types"
    And I upload the archive "alien-base-types"
    And I upload the archive "alien-extended-storage-types"
    And I upload the archive "samples apache"
    And I upload the archive "samples wordpress"
    And I upload the archive "samples mysql"
    And I upload the archive "samples php"
    And I upload the archive "samples topology apache"
    And I upload the archive "samples topology wordpress"