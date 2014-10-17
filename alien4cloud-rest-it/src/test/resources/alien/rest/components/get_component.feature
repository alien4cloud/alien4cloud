Feature: Get details for component

Background:
  Given I am authenticated with "COMPONENTS_BROWSER" role

Scenario: Query for a component
  Given I have a component with uuid "whatever:3.0"
  When I get the component with uuid "whatever:3.0"
  Then I should receive a RestResponse with no error
    And I should retrieve a component detail with list of it's properties and interfaces.

Scenario: get a component other than a node type
  Given I am authenticated with "COMPONENTS_MANAGER" role
    And I already had a csar with name "myCsar" and version "1.0-SNAPSHOT"
    #And I add to the csar "myCsar" "1.0-SNAPSHOT" the component "rootNodeType"
    And I create "capabilities" in an archive name "myCsar" version "1.0-SNAPSHOT"
      |tosca.capabilities.Container|
      |tosca.capabilities.Feature|
      |fastconnect.capabilities.Runner|
  When I try to get a component with id "tosca.capabilities.Container:1.0-SNAPSHOT" 
  Then I should receive a RestResponse with no error
    And I should have a component with id "tosca.capabilities.Container:1.0-SNAPSHOT"
