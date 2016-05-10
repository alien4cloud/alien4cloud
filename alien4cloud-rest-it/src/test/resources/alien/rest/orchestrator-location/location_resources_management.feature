Feature: Manage location resources

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"

  @reset
  Scenario: Create a flavor
    When I create a resource of type "alien.nodes.mock.openstack.Flavor" named "Medium" related to the location "Mount doom orchestrator"/"Thark location"
      Then I should receive a RestResponse with no error
    When I get the location "Mount doom orchestrator"/"Thark location"
      Then I should receive a RestResponse with no error
      And The location should contains a resource with name "Medium" and type "alien.nodes.mock.openstack.Flavor"

  @reset
  Scenario: Create a image
    When I create a resource of type "alien.nodes.mock.openstack.Image" named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
      Then I should receive a RestResponse with no error
    When I get the location "Mount doom orchestrator"/"Thark location"
      Then I should receive a RestResponse with no error
      And The location should contains a resource with name "Ubuntu" and type "alien.nodes.mock.openstack.Image"

  @reset
  Scenario: Update a ressource property
    When I create a resource of type "alien.nodes.mock.openstack.Flavor" named "Medium" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "2" for the resource named "Medium" related to the location "Mount doom orchestrator"/"Thark location"
      Then I should receive a RestResponse with no error

  @reset
  Scenario: Update a ressource capability property
    When I create a resource of type "alien.nodes.mock.openstack.Flavor" named "Medium" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the capability "host" property "disk_size" to "1 GB" for the resource named "Medium" related to the location "Mount doom orchestrator"/"Thark location"
      Then I should receive a RestResponse with no error

  @reset
  Scenario: Autogenerate resources
    Given I create a resource of type "alien.nodes.mock.openstack.Flavor" named "Medium" related to the location "Mount doom orchestrator"/"Thark location"
      And I update the property "id" to "2" for the resource named "Medium" related to the location "Mount doom orchestrator"/"Thark location"
      And I create a resource of type "alien.nodes.mock.openstack.Image" named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
      And I update the property "id" to "img1" for the resource named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    When I autogenerate the on-demand resources for the location "Mount doom orchestrator"/"Thark location"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Update a ressource with a non-existing property should fail
    When I create a resource of type "alien.nodes.mock.openstack.Flavor" named "Medium" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "wrong-property-name" to "should-fail" for the resource named "Medium" related to the location "Mount doom orchestrator"/"Thark location"
      Then I should receive a RestResponse with an error code 504

  @reset
  Scenario: Update a ressource with wrong value type should fail
    When I create a resource of type "alien.nodes.mock.openstack.Flavor" named "Medium" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the capability "host" property "disk_size" to "should-fail" for the resource named "Medium" related to the location "Mount doom orchestrator"/"Thark location"
      Then I should receive a RestResponse with an error code 800
