Feature: Node substitution in the deployment topology.

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
  
    And I have an application "ALIEN" with a topology containing a nodeTemplate "Compute" related to "tosca.nodes.Compute:1.0.0.wd06-SNAPSHOT"
    
    And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes
    

  Scenario: Set a subsitution for a node
    When I substitute on the current application the node "Compute" with the location resource "Mount doom orchestrator"/"Thark location"/"Small_Ubuntu"
    Then I should receive a RestResponse with no error
    And The deployment topology sould have the substituted nodes
    	| Compute | Small_Ubuntu | alien.nodes.mock.Compute |
    When I substitute for the current application the node "Compute" with the location resource "Mount doom orchestrator"/"Thark location"/"Manual_Small_Ubuntu"
    Then I should receive a RestResponse with no error
    And The deployment topology sould have the substituted nodes
    	| Compute | Manual_Small_Ubuntu | alien.nodes.mock.Compute |
    	
  Scenario: Update a substituted node's property
  	Given I substitute on the current application the node "Compute" with the location resource "Mount doom orchestrator"/"Thark location"/"Manual_Small_Ubuntu"
  	When I update the property "imageId" to "updatedImg" for the subtituted node "Compute"
  	Then I should receive a RestResponse with no error
  	And The node "Compute" in the deployment topology should have the property "imageId" with value "updatedImg"
  	 
  Scenario: Update a substituted node's capability property
  	Given I substitute on the current application the node "Compute" with the location resource "Mount doom orchestrator"/"Thark location"/"Manual_Small_Ubuntu"
  	When I update the capability "scalable" property "max_instances" to "5" for the subtituted node "Compute"
  	Then I should receive a RestResponse with no error
  	And The the node "Compute" in the deployment topology should have the capability "scalable"'s property "max_instances" with value "5" 
  	
	
