Feature: Manage location resources

Background:
  Given I am authenticated with "ADMIN" role
  And I upload the archive "tosca-normative-types 1.0.0.wd06-SNAPSHOT"
  And I upload a plugin
  And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"
  And I enable the orchestrator "Mount doom orchestrator"
  And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"

Scenario: Create a flavor
  When I create a flavor named "Medium" to the location "Thark location"
    Then I should receive a RestResponse with no error
  When I list resources of the location "Thark location"
    Then I should receive a RestResponse with no error
    And Response should contains 1 resource
    And Response should contains a resource with name "Medium" and resource type "alien.nodes.mock.openstack.Flavor"

Scenario: Create a image
  When I create a image named "Ubuntu" to the location "Thark location"
    Then I should receive a RestResponse with no error
  When I list resources of the location "Thark location"
    Then I should receive a RestResponse with no error
    And Response should contains 1 resource
    And Response should contains a resource with name "Ubuntu" and resource type "alien.nodes.mock.openstack.Image"
    
Scenario: Update a ressource
  When I create a flavor named "Medium" to the location "Thark location"
  And I update the property "disk_size" to "10" of a resource type "alien.nodes.mock.openstack.Flavor" named "Medium"
    Then I should receive a RestResponse with no error
    
Scenario: Update a ressource with a non-existing property should fail
  When I create a flavor named "Medium" to the location "Thark location"
  And I update the property "wrong-property-name" to "should-fail" of a resource type "alien.nodes.mock.openstack.Flavor" named "Medium"
    Then I should receive a RestResponse with an error code 504

Scenario: Update a ressource with wrong value type should fail
  When I create a flavor named "Medium" to the location "Thark location"
  And I update the property "disk_size" to "should-fail" of a resource type "alien.nodes.mock.openstack.Flavor" named "Medium"
    Then I should receive a RestResponse with an error code 800