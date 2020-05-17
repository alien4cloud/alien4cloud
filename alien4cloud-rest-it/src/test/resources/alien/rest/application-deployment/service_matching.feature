Feature: Service matching and substitution

  # Note We use compute service even if this is not the best service example as it is good enough for matching testing

  Background:
    Given I am authenticated with "ADMIN" role
    # Initialize users for the test
    And There are these users in the system
      | frodon |
      | sam    |
      | tom    |
    And There is a "hobbits" group in the system
    And I add the user "sam" to the group "hobbits"
    # Initialize archive
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    # Initialize plugin, orchestrator and location
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I register "data" as "locationId"
    # Configure location resources
    And I create a resource of type "org.alien4cloud.nodes.mock.openstack.Flavor" named "Small" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "1" for the resource named "Small" related to the location "Mount doom orchestrator"/"Thark location"
    And I create a resource of type "org.alien4cloud.nodes.mock.openstack.Image" named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "img1" for the resource named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I autogenerate the on-demand resources for the location "Mount doom orchestrator"/"Thark location"
    And I create a resource of type "org.alien4cloud.nodes.mock.Compute" named "Manual_Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "imageId" to "img1" for the resource named "Manual_Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "flavorId" to "1" for the resource named "Manual_Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    # Create an application with a compute node
    And I create a new application with name "ALIEN" and description "desc" and node templates
      | Compute | tosca.nodes.Compute:1.0.0-SNAPSHOT |
    And I add a role "APPLICATION_MANAGER" to group "hobbits" on the application "ALIEN"
    And I add a role "APPLICATION_MANAGER" to user "frodon" on the application "ALIEN"
    And I add a role "APPLICATION_MANAGER" to user "tom" on the application "ALIEN"
    And I get the deployment topology for the current application
    And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes
    # Get the environment id
    And I POST "{}" to "/rest/v1/applications/ALIEN/environments/search"
    And I register "data[data][0][id]" as "environmentId"

  @reset
  Scenario: Matching against a service that is started should succeed
    And I create a service with name "MyHostService", version "1.0.0", type "tosca.nodes.Compute", archive version "1.0.0-SNAPSHOT"
    And I register path "data.id" with class "alien4cloud.model.service.ServiceResource" as "serviceId"
    And I should receive a RestResponse with no error
    # Change the state to started see src/test/resources/data/requests for the request json file)
    And I PATCH "services/patch_service_attr_state_started.json" to "/rest/v1/services/${serviceId}"
    And I should receive a RestResponse with no error
    # affect to the location
    And I PATCH "application-deployment/patch_service_location.json" to "/rest/v1/services/${serviceId}"
    And I should receive a RestResponse with no error
    # Get the topology and check matching options, we should have the service listed
    When I GET "/rest/v1/applications/ALIEN/environments/${environmentId}/deployment-topology"
    Then Available substitution should contains 3 proposal including 1 service

  @reset
  Scenario: Matching against a service that is not started should succeed
    And I create a service with name "MyHostService", version "1.0.0", type "tosca.nodes.Compute", archive version "1.0.0-SNAPSHOT"
    And I register path "data.id" with class "alien4cloud.model.service.ServiceResource" as "serviceId"
    And I should receive a RestResponse with no error
    # affect to the location
    And I PATCH "application-deployment/patch_service_location.json" to "/rest/v1/services/${serviceId}"
    And I should receive a RestResponse with no error
    # Get the topology and check matching options, we should have the service listed
    When I GET "/rest/v1/applications/ALIEN/environments/${environmentId}/deployment-topology"
    Then Available substitution should contains 3 proposal including 1 service

  @reset
  Scenario: Service not associated with the location should not match
    And I create a service with name "MyHostService", version "1.0.0", type "tosca.nodes.Compute", archive version "1.0.0-SNAPSHOT"
    And I register path "data.id" with class "alien4cloud.model.service.ServiceResource" as "serviceId"
    And I should receive a RestResponse with no error
    # Get the topology and check matching options, we should have the service listed
    When I GET "/rest/v1/applications/ALIEN/environments/${environmentId}/deployment-topology"
    Then Available substitution should contains 2 proposal including 0 service

  @reset
  Scenario: Matching against a service that is created after the first matching session should succeed
    And I create a service with name "MyHostService", version "1.0.0", type "tosca.nodes.Compute", archive version "1.0.0-SNAPSHOT"
    And I register path "data.id" with class "alien4cloud.model.service.ServiceResource" as "serviceId"
    And I should receive a RestResponse with no error
    # Get the topology and check matching options, we should have the service listed
    When I GET "/rest/v1/applications/ALIEN/environments/${environmentId}/deployment-topology"
    Then Available substitution should contains 2 proposal including 0 service
    # affect to the location
    And I PATCH "application-deployment/patch_service_location.json" to "/rest/v1/services/${serviceId}"
    And I should receive a RestResponse with no error
    # Get the topology and check matching options, we should have the service listed
    When I GET "/rest/v1/applications/ALIEN/environments/${environmentId}/deployment-topology"
    Then Available substitution should contains 3 proposal including 1 service

  @reset
  Scenario: Removed service should be removed from matching selection
    And I create a service with name "MyHostService", version "1.0.0", type "tosca.nodes.Compute", archive version "1.0.0-SNAPSHOT"
    And I register path "data.id" with class "alien4cloud.model.service.ServiceResource" as "serviceId"
    And I should receive a RestResponse with no error
    # affect to the location
    And I PATCH "application-deployment/patch_service_location.json" to "/rest/v1/services/${serviceId}"
    And I should receive a RestResponse with no error
    # Get the topology and check matching options, we should have the service listed
    When I GET "/rest/v1/applications/ALIEN/environments/${environmentId}/deployment-topology"
    Then Available substitution should contains 3 proposal including 1 service
    When I DELETE "/rest/v1/services/${serviceId}"
    Then I should receive a RestResponse with no error
    # Get the topology and check matching options, we should not have the service listed
    When I GET "/rest/v1/applications/ALIEN/environments/${environmentId}/deployment-topology"
    Then Available substitution should contains 2 proposal including 0 service
