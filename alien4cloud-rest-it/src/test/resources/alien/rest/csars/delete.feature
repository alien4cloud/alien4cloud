Feature: CSAR delete

  Background:
    Given I am authenticated with "COMPONENTS_MANAGER" role
    And I upload the archive "tosca base types 1.0"
    And I should receive a RestResponse with no error
    Given I upload the archive "valid-csar-with-test"
    And I should receive a RestResponse with no error
    
  Scenario: Try do delete a CSAR that is a target dependency
    When I delete a CSAR with id "tosca-base-types:1.0"
    Then I should receive a RestResponse with an error code 507
  
  Scenario: Try do delete a CSAR that is used in a topology  
  	Given I am authenticated with "COMPONENTS_MANAGER" role
  	And I upload a plugin
    And I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
    And I enable the cloud "Mount doom cloud"
    And I have already created a cloud image with name "Ubuntu Trusty", architecture "x86_64", type "linux", distribution "Ubuntu" and version "14.04.1"
    And I add the cloud image "Ubuntu Trusty" to the cloud "Mount doom cloud" and match it to paaS image "UBUNTU"
    And I add the flavor with name "small", number of CPUs 2, disk size 32 and memory size 2048 to the cloud "Mount doom cloud" and match it to paaS flavor "2"
  	Given I am authenticated with "APPLICATIONS_MANAGER" role
    And I create a new application with name "watchmiddleearth" and description "Use my great eye to find frodo and the ring."
    And I add a node template "Compute" related to the "tosca.nodes.Compute:1.0" node type
    And I add a node template "Java" related to the "fastconnect.nodes.Java:2.0-SNAPSHOT" node type
    And I have added a relationship "hostedOnCompute" of type "tosca.relationships.HostedOn" defined in archive "tosca-base-types" version "1.0" with source "Java" and target "Compute" for requirement "host" of type "tosca.capabilities.Container" and target capability "compute"
    When I delete a CSAR with id "topology-test:2.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 507
    
  Scenario: Update a snapshot CSAR with less nodes
    Given I am authenticated with "COMPONENTS_MANAGER" role
    Given I upload the archive "valid-csar-with-update1"
    And I should receive a RestResponse with no error
    # in the update2 (3.0-SNAPSHOT), the War is removed 
    Given I upload the archive "valid-csar-with-update2"
    And I should receive a RestResponse with no error
    When I search for "node types" using query "war" from 0 with result size of 1000
    Then I should receive a RestResponse with no error
    And The response should contains 1 elements from various types of version "2.0-SNAPSHOT"
    
  Scenario: Delete a CSAR version and ensure that the highest version is well managed
    Given I am authenticated with "COMPONENTS_MANAGER" role
    Given I upload the archive "valid-csar-with-update1"
    And I should receive a RestResponse with no error
    Given I upload the archive "valid-csar-with-update3"
    And I should receive a RestResponse with no error
    When I search for "node types" using query "war" from 0 with result size of 1000
    Then I should receive a RestResponse with no error
    And The response should contains 1 elements from various types of version "4.0-SNAPSHOT" and older versions are
      | 2.0-SNAPSHOT |
      | 3.0-SNAPSHOT |   
    When I delete a CSAR with id "topology-test:4.0-SNAPSHOT"  
    Then I should receive a RestResponse with no error
    When I search for "node types" using query "war" from 0 with result size of 1000
    Then I should receive a RestResponse with no error
    And The response should contains 1 elements from various types of version "3.0-SNAPSHOT" and older versions are
      | 2.0-SNAPSHOT |    
    
  Scenario: Delete a CSAR version and ensure that the older versions are well managed
    Given I am authenticated with "COMPONENTS_MANAGER" role
    Given I upload the archive "valid-csar-with-update1"
    And I should receive a RestResponse with no error
    Given I upload the archive "valid-csar-with-update3"
    And I should receive a RestResponse with no error
    When I search for "node types" using query "war" from 0 with result size of 1000
    Then I should receive a RestResponse with no error
    And The response should contains 1 elements from various types of version "4.0-SNAPSHOT" and older versions are
      | 2.0-SNAPSHOT |
      | 3.0-SNAPSHOT |   
    When I delete a CSAR with id "topology-test:3.0-SNAPSHOT"  
    Then I should receive a RestResponse with no error
    When I search for "node types" using query "war" from 0 with result size of 1000
    Then I should receive a RestResponse with no error
    And The response should contains 1 elements from various types of version "4.0-SNAPSHOT" and older versions are
      | 2.0-SNAPSHOT |
