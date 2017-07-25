Feature: Node matching and substitution
  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | frodon |
      | sam    |
      | tom    |
    And There is a "hobbits" group in the system
    And I add the user "sam" to the group "hobbits"
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"

    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"

    And I create a resource of type "org.alien4cloud.nodes.mock.openstack.Flavor" named "Small" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "1" for the resource named "Small" related to the location "Mount doom orchestrator"/"Thark location"
    And I create a resource of type "org.alien4cloud.nodes.mock.openstack.Image" named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "id" to "img1" for the resource named "Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I autogenerate the on-demand resources for the location "Mount doom orchestrator"/"Thark location"

    And I create a resource of type "org.alien4cloud.nodes.mock.Compute" named "Manual_Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "imageId" to "img1" for the resource named "Manual_Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"
    And I update the property "flavorId" to "1" for the resource named "Manual_Small_Ubuntu" related to the location "Mount doom orchestrator"/"Thark location"

    And I create a new application with name "ALIEN" and description "desc" and node templates
      | Compute | tosca.nodes.Compute:1.0.0-SNAPSHOT |
    And I add a role "APPLICATION_MANAGER" to group "hobbits" on the application "ALIEN"
    And I add a role "APPLICATION_MANAGER" to user "frodon" on the application "ALIEN"
    And I add a role "APPLICATION_MANAGER" to user "tom" on the application "ALIEN"
#    And I create an application environment of type "DEVELOPMENT" with name "DEV-ALIEN" and description "" for the newly created application
#    And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes of the environment "DEV-ALIEN"
    And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes

  @reset
  Scenario: Set a substitution for a node, using admin account
    When I substitute on the current application the node "Compute" with the location resource "Mount doom orchestrator"/"Thark location"/"Small_Ubuntu"
    Then I should receive a RestResponse with no error
    And The deployment topology should have the substituted nodes
      | Compute | Small_Ubuntu | org.alien4cloud.nodes.mock.Compute |
    When I substitute on the current application the node "Compute" with the location resource "Mount doom orchestrator"/"Thark location"/"Manual_Small_Ubuntu"
    Then I should receive a RestResponse with no error
    And The deployment topology should have the substituted nodes
      | Compute | Manual_Small_Ubuntu | org.alien4cloud.nodes.mock.Compute |

  @reset
  Scenario: Set a substitution for a node, user has access
    Given I successfully grant access to the resource type "LOCATION" named "Thark location" to the user "frodon"
    And I am authenticated with user named "frodon"
    When I substitute on the current application the node "Compute" with the location resource "Mount doom orchestrator"/"Thark location"/"Small_Ubuntu"
    Then I should receive a RestResponse with an error code 504
    When I substitute on the current application the node "Compute" with the location resource "Mount doom orchestrator"/"Thark location"/"Manual_Small_Ubuntu"
    Then I should receive a RestResponse with an error code 504

    When I authenticate with "ADMIN" role
    And I successfully grant access to the resource type "LOCATION_RESOURCE" named "Mount doom orchestrator/Thark location/Small_Ubuntu" to the user "frodon"

    When I am authenticated with user named "frodon"
    And I substitute on the current application the node "Compute" with the location resource "Mount doom orchestrator"/"Thark location"/"Small_Ubuntu"
    Then I should receive a RestResponse with no error
    And The deployment topology should have the substituted nodes
      | Compute | Small_Ubuntu | org.alien4cloud.nodes.mock.Compute |
    When I substitute on the current application the node "Compute" with the location resource "Mount doom orchestrator"/"Thark location"/"Manual_Small_Ubuntu"
    Then I should receive a RestResponse with an error code 504

  @reset
  Scenario: Set a substitution for a node, group has access
    Given I successfully grant access to the resource type "LOCATION" named "Thark location" to the group "hobbits"
    And I am authenticated with user named "sam"
    When I substitute on the current application the node "Compute" with the location resource "Mount doom orchestrator"/"Thark location"/"Small_Ubuntu"
    Then I should receive a RestResponse with an error code 504
    When I substitute on the current application the node "Compute" with the location resource "Mount doom orchestrator"/"Thark location"/"Manual_Small_Ubuntu"
    Then I should receive a RestResponse with an error code 504

    When I authenticate with "ADMIN" role
    And I successfully grant access to the resource type "LOCATION_RESOURCE" named "Mount doom orchestrator/Thark location/Manual_Small_Ubuntu" to the group "hobbits"

    When I am authenticated with user named "sam"
    And I substitute on the current application the node "Compute" with the location resource "Mount doom orchestrator"/"Thark location"/"Manual_Small_Ubuntu"
    Then I should receive a RestResponse with no error
    And The deployment topology should have the substituted nodes
      | Compute | Manual_Small_Ubuntu | org.alien4cloud.nodes.mock.Compute |
    When I substitute on the current application the node "Compute" with the location resource "Mount doom orchestrator"/"Thark location"/"Small_Ubuntu"
    Then I should receive a RestResponse with an error code 504

  @reset
  Scenario: Set a substitution for a node, application has access
    Given I successfully grant access to the resource type "LOCATION" named "Thark location" to the application "ALIEN"
    And I am authenticated with user named "tom"
    When I substitute on the current application the node "Compute" with the location resource "Mount doom orchestrator"/"Thark location"/"Small_Ubuntu"
    Then I should receive a RestResponse with an error code 504
    When I substitute on the current application the node "Compute" with the location resource "Mount doom orchestrator"/"Thark location"/"Manual_Small_Ubuntu"
    Then I should receive a RestResponse with an error code 504

    When I authenticate with "ADMIN" role
    And I successfully grant access to the resource type "LOCATION_RESOURCE" named "Mount doom orchestrator/Thark location/Small_Ubuntu" to the application "ALIEN"

    When I am authenticated with user named "tom"
    And I substitute on the current application the node "Compute" with the location resource "Mount doom orchestrator"/"Thark location"/"Small_Ubuntu"
    Then I should receive a RestResponse with no error
    And The deployment topology should have the substituted nodes
      | Compute | Small_Ubuntu | org.alien4cloud.nodes.mock.Compute |
    When I substitute on the current application the node "Compute" with the location resource "Mount doom orchestrator"/"Thark location"/"Manual_Small_Ubuntu"
    Then I should receive a RestResponse with an error code 504

  # grant access to the second resource to tom
    Given I authenticate with "ADMIN" role
    And I successfully grant access to the resource type "LOCATION" named "Thark location" to the user "tom"
    And I successfully grant access to the resource type "LOCATION_RESOURCE" named "Mount doom orchestrator/Thark location/Manual_Small_Ubuntu" to the user "tom"
    And I am authenticated with user named "tom"
    When I substitute on the current application the node "Compute" with the location resource "Mount doom orchestrator"/"Thark location"/"Small_Ubuntu"
    Then I should receive a RestResponse with no error
    And The deployment topology should have the substituted nodes
      | Compute | Small_Ubuntu | org.alien4cloud.nodes.mock.Compute |
    When I substitute on the current application the node "Compute" with the location resource "Mount doom orchestrator"/"Thark location"/"Manual_Small_Ubuntu"
    Then I should receive a RestResponse with no error
    And The deployment topology should have the substituted nodes
      | Compute | Manual_Small_Ubuntu | org.alien4cloud.nodes.mock.Compute |


  @reset
  Scenario: Set a substitution for a node, environment has access
    Given I successfully grant access to the resource type "LOCATION" named "Thark location" to the environment "Environment" of the application "ALIEN"
    And I am authenticated with user named "tom"
    When I substitute on the current application the node "Compute" with the location resource "Mount doom orchestrator"/"Thark location"/"Small_Ubuntu"
    Then I should receive a RestResponse with an error code 504
    When I substitute on the current application the node "Compute" with the location resource "Mount doom orchestrator"/"Thark location"/"Manual_Small_Ubuntu"
    Then I should receive a RestResponse with an error code 504

    When I authenticate with "ADMIN" role
    And I successfully grant access to the resource type "LOCATION_RESOURCE" named "Mount doom orchestrator/Thark location/Small_Ubuntu" to the environment "Environment" of the application "ALIEN"

    When I am authenticated with user named "tom"
    And I substitute on the current application the node "Compute" with the location resource "Mount doom orchestrator"/"Thark location"/"Small_Ubuntu"
    Then I should receive a RestResponse with no error
    And The deployment topology should have the substituted nodes
      | Compute | Small_Ubuntu | org.alien4cloud.nodes.mock.Compute |
    When I substitute on the current application the node "Compute" with the location resource "Mount doom orchestrator"/"Thark location"/"Manual_Small_Ubuntu"
    Then I should receive a RestResponse with an error code 504

  # grant access to the second resource to tom
    Given I authenticate with "ADMIN" role
    And I successfully grant access to the resource type "LOCATION" named "Thark location" to the user "tom"
    And I successfully grant access to the resource type "LOCATION_RESOURCE" named "Mount doom orchestrator/Thark location/Manual_Small_Ubuntu" to the user "tom"
    And I am authenticated with user named "tom"
    When I substitute on the current application the node "Compute" with the location resource "Mount doom orchestrator"/"Thark location"/"Small_Ubuntu"
    Then I should receive a RestResponse with no error
    And The deployment topology should have the substituted nodes
      | Compute | Small_Ubuntu | org.alien4cloud.nodes.mock.Compute |
    When I substitute on the current application the node "Compute" with the location resource "Mount doom orchestrator"/"Thark location"/"Manual_Small_Ubuntu"
    Then I should receive a RestResponse with no error
    And The deployment topology should have the substituted nodes
      | Compute | Manual_Small_Ubuntu | org.alien4cloud.nodes.mock.Compute |
