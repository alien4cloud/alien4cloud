Feature: Flag a node type as the default for a given capability

  Background:
    Given I am authenticated with "COMPONENTS_MANAGER" role
      And I have a node type with id "fastconnect.nodes.SampleNode1" and an archive version "3.0" with capability "fastconnect.capabilities.SampleCapability"
      And I have a node type with id "fastconnect.nodes.SampleNode2" and an archive version "3.0" with capability "fastconnect.capabilities.SampleCapability"

  @reset
  Scenario: Flag a node type as default for the fastconnect.capabilities.SampleCapability capability
    Given I have a component with id "fastconnect.nodes.SampleNode1:3.0"
    When I flag the node type "fastconnect.nodes.SampleNode1:3.0" as default for the "fastconnect.capabilities.SampleCapability" capability
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Get the default node type for fastconnect.capabilities.SampleCapability capability
    Given I have a component with id "fastconnect.nodes.SampleNode1:3.0"
      And I flag the node type "fastconnect.nodes.SampleNode1:3.0" as default for the "fastconnect.capabilities.SampleCapability" capability
    When I search for the default node type for capability "fastconnect.capabilities.SampleCapability"
    Then I should receive a RestResponse with no error
      And the node type id should be "fastconnect.nodes.SampleNode1:3.0"

  @reset
  Scenario: Flag a node type as default for the fastconnect.capabilities.SampleCapability capability to replace other one
    Given I have a component with id "fastconnect.nodes.SampleNode1:3.0"
      And I have a component with id "fastconnect.nodes.SampleNode2:3.0"
      And I flag the node type "fastconnect.nodes.SampleNode1:3.0" as default for the "fastconnect.capabilities.SampleCapability" capability
      And I flag the node type "fastconnect.nodes.SampleNode2:3.0" as default for the "fastconnect.capabilities.SampleCapability" capability
    When I search for the default node type for capability "fastconnect.capabilities.SampleCapability"
    Then I should receive a RestResponse with no error
      And the node type id should be "fastconnect.nodes.SampleNode2:3.0"

  @reset
  Scenario: unflag a node type as default for the fastconnect.capabilities.SampleCapability capability
    Given I have a component with id "fastconnect.nodes.SampleNode1:3.0"
      And I flag the node type "fastconnect.nodes.SampleNode1:3.0" as default for the "fastconnect.capabilities.SampleCapability" capability
    When I unflag the node type "fastconnect.nodes.SampleNode1:3.0" as default for the "fastconnect.capabilities.SampleCapability" capability
      Then I should receive a RestResponse with no error
    When I search for the default node type for capability "fastconnect.capabilities.SampleCapability"
    Then I should receive a RestResponse with no error
      And I should receive a RestResponse with no data
