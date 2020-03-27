Feature: Create a service resource from an environment (advanced topologies)
  # This feature is similar to Create a service resource from an environment but has a different background.

  Background:
    Given I am authenticated with "ADMIN" role
    And I setup alien with
      | user          | sauron,APPLICATIONS_MANAGER                                               |
      | archives      | tosca-normative-types-1.0.0-SNAPSHOT,topology-single-ubuntu-compute-subst |
      | orchestrators | orc                                                                       |
      | location      | orc,loc,OpenStack,sauron                                                  |
      | resource      | orc,loc,org.alien4cloud.nodes.mock.openstack.Flavor,Small,id,small        |
      | resource      | orc,loc,org.alien4cloud.nodes.mock.openstack.Image,Ubuntu,id,myImage      |
      | resourcesAuth | orc,loc,Small_Ubuntu,sauron                                               |
    And I am authenticated with user named "sauron"
    And I pre register orchestrator properties
      | managementUrl | http://cloudifyurl:8099 |
      | numberBackup  | 1                       |
      | managerEmail  | admin@alien.fr          |

  @reset
  Scenario: Creating a new managed service when topology is created from a template should succeed
    And I create an application with name "watchmiddleearth", archive name "watchmiddleearth", description "Use my great eye to find frodo and the ring." and topology template id "org.alien4cloud.nodes.test.SingleUbuntuCompute:1.4.0-SNAPSHOT"
    And I get the deployment topology for the current application
    And I Set a unique location policy to "orc"/"loc" for all nodes
    When I create a service with name "MyService", from the application "watchmiddleearth", environment "Environment"
    Then I should receive a RestResponse with no error
    Given I am authenticated with "ADMIN" role
    When I get the last created service
    Then The SPEL expression "environmentId != null" should return true

  @reset
  Scenario: Creating a new managed service when topology is created including a substituted type should succeed
    And I create a new application with name "watchmiddleearth" and description "Use my great eye to find frodo and the ring." and node templates
      | Compute | org.alien4cloud.nodes.test.SingleUbuntuCompute:1.4.0-SNAPSHOT |
    And I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId | tosca.nodes.Root                                                                  |
    And I successfully save the topology
    And I get the deployment topology for the current application
    And I Set a unique location policy to "orc"/"loc" for all nodes
    When I create a service with name "MyService", from the application "watchmiddleearth", environment "Environment"
    Then I should receive a RestResponse with no error
    Given I am authenticated with "ADMIN" role
    When I get the last created service
    Then The SPEL expression "environmentId != null" should return true
