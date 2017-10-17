Feature: Manage location resources

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I successfully create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I successfully enable the orchestrator "Mount doom orchestrator"
    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"

  @reset
  Scenario: Create a flavor
    When I create a resource of type "org.alien4cloud.nodes.mock.openstack.Flavor" named "Medium" related to the location "Mount doom orchestrator"/"Thark location"
    Then I should receive a RestResponse with no error
    When I get the location "Mount doom orchestrator"/"Thark location"
    Then I should receive a RestResponse with no error
    And The location should contains a resource with name "Medium" and type "org.alien4cloud.nodes.mock.openstack.Flavor"

  @reset
  Scenario: Create a image
    When I create a resource of type "org.alien4cloud.nodes.mock.openstack.Image" named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    Then I should receive a RestResponse with no error
    When I get the location "Mount doom orchestrator"/"Thark location"
    Then I should receive a RestResponse with no error
    And The location should contains a resource with name "Ubuntu" and type "org.alien4cloud.nodes.mock.openstack.Image"

  @reset
  Scenario: Create an on demand resource
    When I create a resource of type "org.alien4cloud.nodes.mock.Compute" named "Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    Then I should receive a RestResponse with no error
    When I get the location "Mount doom orchestrator"/"Thark location"
    Then I should receive a RestResponse with no error
    And The location should contains an on-demand resource with name "Small_Ubuntu" and type "org.alien4cloud.nodes.mock.Compute"

  @reset
  Scenario: Update a resource property
    When I create a resource of type "org.alien4cloud.nodes.mock.openstack.Flavor" named "Medium" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "2" for the resource named "Medium" related to the location "Mount doom orchestrator"/"Thark location"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Update a resource property of custom data type should succeed
    When I create a resource of type "org.alien4cloud.nodes.mock.Network" named "Network" related to the location "Mount doom orchestrator"/"Thark location"
#    And I update the property "subnet" to "2" for the resource named "Network" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the complex property "subnet" to """{"ip_version": "6"}""" for the resource named "Network" related to the location "Mount doom orchestrator"/"Thark location"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Update a resource capability property
    When I create a resource of type "org.alien4cloud.nodes.mock.openstack.Flavor" named "Medium" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the capability "host" property "disk_size" to "1 GB" for the resource named "Medium" related to the location "Mount doom orchestrator"/"Thark location"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Autogenerate resources
    Given I create a resource of type "org.alien4cloud.nodes.mock.openstack.Flavor" named "Medium" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "2" for the resource named "Medium" related to the location "Mount doom orchestrator"/"Thark location"
    And I create a resource of type "org.alien4cloud.nodes.mock.openstack.Image" named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "img1" for the resource named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    When I autogenerate the on-demand resources for the location "Mount doom orchestrator"/"Thark location"
    Then I should receive a RestResponse with no error
    When I get the location "Mount doom orchestrator"/"Thark location"
    Then I should receive a RestResponse with no error
    And The location should contains an on-demand resource with name "Medium_Ubuntu" and type "org.alien4cloud.nodes.mock.Compute"

  @reset
  Scenario: Update a non existing property of a resource should fail
    When I create a resource of type "org.alien4cloud.nodes.mock.openstack.Flavor" named "Medium" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "wrong-property-name" to "should-fail" for the resource named "Medium" related to the location "Mount doom orchestrator"/"Thark location"
    Then I should receive a RestResponse with an error code 504

  @reset
  Scenario: Update a resource capability property with wrong value type should fail
    When I create a resource of type "org.alien4cloud.nodes.mock.openstack.Flavor" named "Medium" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the capability "host" property "disk_size" to "should-fail" for the resource named "Medium" related to the location "Mount doom orchestrator"/"Thark location"
    Then I should receive a RestResponse with an error code 804

  @reset
  Scenario: Create a custom resource
    Given I upload the local archive "data/csars/node_replacement/node_replacement.yaml"
    When I create a resource of type "alien.test.nodes.JBoss" named "CustomJBoss" from archive "node_replacement" in version "0.1-SNAPSHOT" related to the location "Mount doom orchestrator"/"Thark location"
    Then I should receive a RestResponse with no error
    And The created resource response should contain a new dependency named "node_replacement" in version "0.1-SNAPSHOT"
    When I get the location "Mount doom orchestrator"/"Thark location"
    Then I should receive a RestResponse with no error
    And The location should contains an on-demand resource with name "CustomJBoss" and type "alien.test.nodes.JBoss"

  @reset
  Scenario: Delete a location resource should succeed
    Given I create a resource of type "org.alien4cloud.nodes.mock.openstack.Flavor" named "Medium" related to the location "Mount doom orchestrator"/"Thark location"
    And I create a resource of type "org.alien4cloud.nodes.mock.Compute" named "Compute" related to the location "Mount doom orchestrator"/"Thark location"
    And I get the location "Mount doom orchestrator"/"Thark location"
    And The location should contains a resource with name "Medium" and type "org.alien4cloud.nodes.mock.openstack.Flavor"
    And The location should contains an on-demand resource with name "Compute" and type "org.alien4cloud.nodes.mock.Compute"
    When I delete the location resource named "Medium" related to the location "Mount doom orchestrator"/"Thark location"
    Then I should receive a RestResponse with no error
    When I get the location "Mount doom orchestrator"/"Thark location"
    Then The location should not contain a resource with name "Medium" and type "org.alien4cloud.nodes.mock.openstack.Flavor"

  @reset
  Scenario: Delete a location on demand resource should succeed
    Given I create a resource of type "org.alien4cloud.nodes.mock.openstack.Flavor" named "Medium" related to the location "Mount doom orchestrator"/"Thark location"
    And I create a resource of type "org.alien4cloud.nodes.mock.Compute" named "Compute" related to the location "Mount doom orchestrator"/"Thark location"
    And I get the location "Mount doom orchestrator"/"Thark location"
    And The location should contains a resource with name "Medium" and type "org.alien4cloud.nodes.mock.openstack.Flavor"
    And The location should contains an on-demand resource with name "Compute" and type "org.alien4cloud.nodes.mock.Compute"
    When I delete the location resource named "Compute" related to the location "Mount doom orchestrator"/"Thark location"
    Then I should receive a RestResponse with no error
    When I get the location "Mount doom orchestrator"/"Thark location"
    Then The location should not contain an on-demand resource with name "Compute" and type "org.alien4cloud.nodes.mock.Compute"

  @reset
  Scenario: Deleting a custom resource should clean location dependencies
    Given I upload the local archive "data/csars/node_replacement/node_replacement.yaml"
    And I create a resource of type "alien.test.nodes.JBoss" named "CustomJBoss" from archive "node_replacement" in version "0.1-SNAPSHOT" related to the location "Mount doom orchestrator"/"Thark location"
    And I create a resource of type "alien.test.nodes.JBoss" named "CustomJBoss2" from archive "node_replacement" in version "0.1-SNAPSHOT" related to the location "Mount doom orchestrator"/"Thark location"
    And The created resource response should contain a new dependency named "node_replacement" in version "0.1-SNAPSHOT"
    When I delete the location resource named "CustomJBoss" related to the location "Mount doom orchestrator"/"Thark location"
    Then I should receive a RestResponse with no error
    When I get the location "Mount doom orchestrator"/"Thark location"
    And I register the rest response data as SPEL context of type "alien4cloud.rest.orchestrator.model.LocationDTO"
    Then The SPEL expression "location.dependencies?.isEmpty()" should return false
    Then The SPEL expression "location.dependencies.?[name == 'node_replacement' && version == '0.1-SNAPSHOT'].size()" should return 1

    When I delete the location resource named "CustomJBoss2" related to the location "Mount doom orchestrator"/"Thark location"
    Then I should receive a RestResponse with no error
    When I get the location "Mount doom orchestrator"/"Thark location"
    And I register the rest response data as SPEL context of type "alien4cloud.rest.orchestrator.model.LocationDTO"
    Then The SPEL expression "location.dependencies?.isEmpty()" should return false
    Then The SPEL expression "location.dependencies.?[name == 'node_replacement' && version == '0.1-SNAPSHOT'].size()" should return 0
