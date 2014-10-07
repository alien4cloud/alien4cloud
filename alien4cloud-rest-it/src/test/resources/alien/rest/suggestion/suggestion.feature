Feature: generic suggestion

Background:
  Given I am authenticated with "COMPONENTS_MANAGER" role
    And I already had a csar with name "myCsar" and version "1.0"
    And I already had a component "rootNodeType" uploaded
    And I already had a component "javaNodeType" uploaded
    
Scenario: Search suggestion
  When I search for suggestion on index "toscaelement", type "indexednodetype", path "elementId" with text "root"
  Then I should receive a RestResponse with no error
    And The RestResponse should contain 1 element(s):
      | tosca.nodes.Root		|
  
Scenario: Search suggestion with 2 responses
  When I search for suggestion on index "toscaelement", type "indexednodetype", path "elementId" with text "nodes"
  Then I should receive a RestResponse with no error
    And The RestResponse should contain 2 element(s):
      | tosca.nodes.Root		|
      | fastconnect.nodes.Java	|