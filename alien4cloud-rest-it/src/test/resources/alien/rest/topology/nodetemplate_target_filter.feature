Feature: Manage target filter

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca base types 1.0"
    And I should receive a RestResponse with no error
    And I upload the archive "sample java types 1.0"
    And I should receive a RestResponse with no error
    And There is a "node type" with element name "tosca.nodes.Compute" and archive version "1.0"
    And There is a "node type" with element name "fastconnect.nodes.JavaChef" and archive version "1.0"
    And There is a "node type" with element name "fastconnect.nodes.War" and archive version "1.0"

  @reset
  Scenario: Add a nodetemplate based on a node type id
#    Then I should receive a RestResponse with no error
