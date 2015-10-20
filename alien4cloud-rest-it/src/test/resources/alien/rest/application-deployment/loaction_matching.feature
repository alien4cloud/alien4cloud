Feature: Match locations and set location policies.

  Background: 
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | frodon |
    And I upload the archive "tosca-normative-types-wd06"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I create a location named "Thark location 2" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I have an application "ALIEN" with a topology containing a nodeTemplate "Compute" related to "tosca.nodes.Compute:1.0.0.wd06-SNAPSHOT"

  Scenario: Get locations matchings for this topology, using admin account
    When I ask for the locations matching for the current application
    Then I should receive a match result with 2 locations
      | Thark location   |
      | Thark location 2 |
	
	Scenario: Get locations matchings for this topology, using a common account
		Given I am authenticated with user named "frodon"
		When I ask for the locations matching for the current application
		Then I should receive a match result with no locations
		
		When I authenticate with "ADMIN" role
		And I add a role "DEPLOYER" to user "frodon" on the resource type "LOCATION" named "Thark location"
		Then I should receive a RestResponse with no error
		
		When I am authenticated with user named "frodon"
		When I ask for the locations matching for the current application
		Then I should receive a match result with 1 locations
			| Thark location |
			
	Scenario: Set location policy for all groups in the topology
		Given I add a role "DEPLOYER" to user "frodon" on the resource type "LOCATION" named "Thark location"
#		When I Set the following location policies with orchestrator "Mount doom orchestrator" for groups
#			| _A4C_ALL | Thark location |
		When I Set a unique location policy with location "Thark location" on orchestrator "Mount doom orchestrator" for all nodes
		Then I should receive a RestResponse with no error
		And the deployment topology shoud have the following location policies
			| _A4C_ALL |  Mount doom orchestrator | Thark location |
