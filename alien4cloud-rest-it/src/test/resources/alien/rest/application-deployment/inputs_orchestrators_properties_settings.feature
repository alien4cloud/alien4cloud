Feature: inputs and orchestrator proerties settings in deployment topology.

  Background: 
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | frodon |
    And I upload the archive "tosca-normative-types-wd06"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    
    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I create a resource of type "alien.nodes.mock.openstack.Flavor" named "Small" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "1" for the resource named "Small" related to the location "Mount doom orchestrator"/"Thark location"
    And I create a resource of type "alien.nodes.mock.openstack.Image" named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "img1" for the resource named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I create a resource of type "alien.nodes.mock.openstack.Image" named "Debian" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "img2" for the resource named "Debian" related to the location "Mount doom orchestrator"/"Thark location"
  	And I autogenerate the on-demand resources for the location "Mount doom orchestrator"/"Thark location"
    And I create a resource of type "alien.nodes.mock.Compute" named "Manual_Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "imageId" to "img1" for the resource named "Manual_Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "flavorId" to "1" for the resource named "Manual_Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
  	And I add a role "DEPLOYER" to user "frodon" on the resource type "LOCATION" named "Thark location"
  
#    And I have an application "ALIEN" with a topology containing a nodeTemplate "Compute" related to "tosca.nodes.Compute:1.0.0.wd06-SNAPSHOT"
    And I have an application "ALIEN" with a topology containing a nodeTemplate "WebServer" related to "tosca.nodes.WebServer:1.0.0.wd06-SNAPSHOT"
  	And I add a role "APPLICATION_MANAGER" to user "frodon" on the resource type "APPLICATION" named "ALIEN"
    
    And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes
    
    

  Scenario: Setting values to input properties
  	Given I define and associate the property "component_version" of the node "WebServer" as input property
    And I am authenticated with user named "frodon"
    When I set the input property "component_version" of the deployment to "3.0"
    Then I should receive a RestResponse with no error
    And The deployment topology sould have the input "component_version" with value "3.0"
    And the following nodes properties values sould be "3.0"
    	| WebServer | component_version |
