Feature: Delete application

  @reset
  Scenario: deleting an application
    Given I am authenticated with "APPLICATIONS_MANAGER" role
    And I have applications with names and descriptions
      | watchmiddleearth | Use my great eye to find frodo and the ring. |
    When I delete the application "watchmiddleearth"
    Then I should receive a RestResponse with no error
    And I should receive a RestResponse with a boolean data "true"
    And the application should not be found

  @reset
  Scenario: deleting an application that has a deployed environment should fail
    Given I am authenticated with "ADMIN" role
    And I setup alien with
      | user          | sauron,APPLICATIONS_MANAGER                                          |
      | archives      | tosca-normative-types-1.0.0-SNAPSHOT                                 |
      | orchestrators | orc                                                                  |
      | location      | orc,loc,OpenStack,sauron                                             |
      | resource      | orc,loc,org.alien4cloud.nodes.mock.openstack.Flavor,Small,id,small   |
      | resource      | orc,loc,org.alien4cloud.nodes.mock.openstack.Image,Ubuntu,id,myImage |
      | resourcesAuth | orc,loc,Small_Ubuntu,sauron                                          |
    And I am authenticated with user named "sauron"
    And I pre register orchestrator properties
      | managementUrl | http://cloudifyurl:8099 |
      | numberBackup  | 1                       |
      | managerEmail  | admin@alien.fr          |
    And I create a new application with name "watchmiddleearth" and description "Use my great eye to find frodo and the ring." and node templates
      | Compute | tosca.nodes.Compute:1.0.0-SNAPSHOT |
    And I get the deployment topology for the current application
    And I Set a unique location policy to "orc"/"loc" for all nodes
    And I deploy it
    When I delete the application "watchmiddleearth"
    Then I should receive a RestResponse with an error code 607

  @reset
  Scenario: deleting an application that has an environment exposed as a service should fail
    Given I am authenticated with "ADMIN" role
    And I setup alien with
      | user          | sauron,APPLICATIONS_MANAGER                                          |
      | archives      | tosca-normative-types-1.0.0-SNAPSHOT                                 |
      | orchestrators | orc                                                                  |
      | location      | orc,loc,OpenStack,sauron                                             |
      | resource      | orc,loc,org.alien4cloud.nodes.mock.openstack.Flavor,Small,id,small   |
      | resource      | orc,loc,org.alien4cloud.nodes.mock.openstack.Image,Ubuntu,id,myImage |
      | resourcesAuth | orc,loc,Small_Ubuntu,sauron                                          |
    And I am authenticated with user named "sauron"
    And I pre register orchestrator properties
      | managementUrl | http://cloudifyurl:8099 |
      | numberBackup  | 1                       |
      | managerEmail  | admin@alien.fr          |
    And I create a new application with name "watchmiddleearth" and description "Use my great eye to find frodo and the ring." and node templates
      | Compute | tosca.nodes.Compute:1.0.0-SNAPSHOT |
    And I get the deployment topology for the current application
    And I Set a unique location policy to "orc"/"loc" for all nodes
    And I execute the operation
      | type      | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | elementId | tosca.nodes.Root                                                                  |
    And I successfully save the topology
    And I create a service with name "MyService", from the application "watchmiddleearth", environment "Environment"
    When I delete the application "watchmiddleearth"
    Then I should receive a RestResponse with an error code 507
