Feature: Add a component

Background:
  Given I am authenticated with "COMPONENTS_MANAGER" role
  And I already had a csar with name "myCsar" and version "1.0-SNAPSHOT"
  
Scenario: Add a component
  When I upload the component "rootNodeType"
  Then I should receive a RestResponse with no error