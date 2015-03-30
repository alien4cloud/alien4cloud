Feature: Create cloud

Background:
  Given I am authenticated with "ADMIN" role
  And I upload a plugin

Scenario: Create a cloud
  When I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
  Then I should receive a RestResponse with no error
  When I list clouds
  Then I should receive a RestResponse with no error
  And Response should contains 1 cloud
  And Response should contains a cloud with name "Mount doom cloud"

Scenario: Create a cloud with existing name should fail
  When I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
  And I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
  Then I should receive a RestResponse with an error code 502
  When I list clouds
  Then I should receive a RestResponse with no error
  And Response should contains 1 cloud
  And Response should contains a cloud with name "Mount doom cloud"
 
Scenario: Clone a cloud
  Given I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
  And I enable "Mount doom cloud"
  And I update cloud named "Mount doom cloud" iaas type to "OPENSTACK"
  And I update cloud named "Mount doom cloud" environment type to "DEVELOPMENT"
  And I have already created a cloud image with name "Windows 7", architecture "x86_64", type "windows", distribution "Windows" and version "7.0"
  And I add the cloud image "Windows 7" to the cloud "Mount doom cloud"
  And I match the image "Windows 7" of the cloud "Mount doom cloud" to the PaaS resource "WINDOWS"
  And I add the flavor with name "small", number of CPUs 2, disk size 32 and memory size 2048 to the cloud "Mount doom cloud"
  And I match the flavor "small" of the cloud "Mount doom cloud" to the PaaS resource "2"
  And I add the network with name "private" and CIDR "192.168.1.0/24" and IP version 4 and gateway "192.168.1.1" to the cloud "Mount doom cloud"
  And I match the network with name "private" of the cloud "Mount doom cloud" to the PaaS resource "alienPrivateNetwork"
  And I add the storage with id "STORAGE1" and device "/etc/dev1" and size 1024 to the cloud "Mount doom cloud"
  And I match the storage with name "STORAGE1" of the cloud "Mount doom cloud" to the PaaS resource "alienSTORAGE2"
  And I update configuration for cloud "Mount doom cloud"
  And There are these users in the system
    | user |
  And There is a "lordOfRing" group in the system
  And There is a "hobbits" group in the system
  And I add the user "user" to the group "lordOfRing"
  And I add the user "user" to the group "hobbits"
  And I add a role "CLOUD_DEPLOYER" to group "lordOfRing" on the resource type "CLOUD" named "Mount doom cloud"
  When I clone the cloud with name "Mount doom cloud"
  And I get the cloud by name "Mount doom cloud-1"
  And I register the rest response data as SPEL context of type "alien4cloud.model.cloud.Cloud"
  Then The SPEL expression "name" should return "Mount doom cloud-1"
  And The SPEL expression "paasPluginId" should return "alien4cloud-mock-paas-provider:1.0"
  And The SPEL expression "paasPluginBean" should return "mock-paas-provider"
  And The SPEL expression "iaaSType" should return "openstack"
  And The SPEL expression "environmentType" should return "DEVELOPMENT"
  # the cloned cloud is not enabled
  And The SPEL boolean expression "!enabled" should return true 
  # all the stuffs bellow should have been cloned
  And The SPEL boolean expression "images.size() == 1" should return true 
  And The SPEL expression "imageMapping[images[0]]" should return "WINDOWS"
  And The SPEL boolean expression "flavors.size() == 1" should return true 
  And The SPEL expression "flavors[0].id" should return "small"
  And The SPEL expression "flavors[0].numCPUs" should return "2"
  And The SPEL expression "flavors[0].diskSize" should return "32"
  And The SPEL expression "flavors[0].memSize" should return "2048"
  And The SPEL expression "flavorMapping['small']" should return "2"
  And The SPEL boolean expression "computeTemplates.size() == 1" should return true 
  And The SPEL boolean expression "computeTemplates[0].cloudImageId == images[0]" should return true
  And The SPEL expression "computeTemplates[0].cloudImageFlavorId" should return "small"
  And The SPEL boolean expression "computeTemplates[0].enabled" should return true
  And The SPEL boolean expression "networks.size() == 1" should return true 
  And The SPEL expression "networks[0].id" should return "private"
  And The SPEL expression "networks[0].ipVersion" should return "4"
  And The SPEL expression "networks[0].cidr" should return "192.168.1.0/24"
  And The SPEL expression "networks[0].gatewayIp" should return "192.168.1.1"
  And The SPEL expression "networkMapping['private']" should return "alienPrivateNetwork"
  And The SPEL boolean expression "storages.size() == 1" should return true 
  And The SPEL expression "storages[0].id" should return "STORAGE1"
  And The SPEL expression "storages[0].size" should return "1024"
  And The SPEL expression "storages[0].device" should return "/etc/dev1"
  And The SPEL expression "storageMapping['STORAGE1']" should return "alienSTORAGE2"
  And The SPEL expression "groupRoles.entrySet()[0].value[0]" should return "CLOUD_DEPLOYER"
  When I get configuration for cloud "Mount doom cloud-1"
  And I register the rest response data as SPEL context of type "alien4cloud.it.plugin.ProviderConfig"
  And The SPEL expression "firstArgument" should return "firstArgument"
  And The SPEL expression "secondArgument" should return "secondArgument"
  
Scenario: Clone a cloud twice
  Given I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
  And I clone the cloud with name "Mount doom cloud"
  When I clone the cloud with name "Mount doom cloud"
  Then I should receive a RestResponse with no error  
  When I get the cloud by name "Mount doom cloud-2"
  Then I should receive a RestResponse with no error  