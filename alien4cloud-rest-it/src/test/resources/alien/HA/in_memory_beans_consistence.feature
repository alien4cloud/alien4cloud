Feature: Check the states of a bunch of supposed in-memory beans
  # Tested case with this Feature:
  #   - Audit configuration


  Background:
    Given I am authenticated with "ADMIN" role
#     And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
#     And I upload the archive "samples"
#     And I create an application "ALIEN" with a Compute and a JDK nodes
#     And I upload a plugin
#     And I setup an orchestrator "orchestrator" with a location "test-location"
#     And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes
#     And I have added a node template "Compute" related to the "tosca.nodes.Compute:1.0.0-SNAPSHOT" node type
#     And I add a relationship of type "tosca.relationships.HostedOn" defined in archive "tosca-normative-types" version "1.0.0-SNAPSHOT" with source "customInterface" and target "Compute" for requirement "host" of type "tosca.capabilities.Container" and target capability "compute"
    #When I deploy it


  Scenario: Audit configuration: Fail the alien process on leader instance, and check the audit conf
    Given I disable audit log for following methods:
      | Application | delete |
    When I shutdown the alien process on the alien4cloud leader instance
     And I wait for 10 seconds before continuing the test
    Then alien4cloud should be available
     And I should be able to access the application "ALIEN"
     And the alien4cloud backup instance should be the new leader
    When I get audit log configuration
    Then I should have audit log disabled for:
      | Application | delete |
