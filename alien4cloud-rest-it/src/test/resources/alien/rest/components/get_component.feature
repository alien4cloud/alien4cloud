Feature: Get details for component

  Background:
    Given I am authenticated with "COMPONENTS_MANAGER" role
    And I upload the archive "tosca base types 1.0"
    And I am authenticated with "COMPONENTS_BROWSER" role

  @reset
  Scenario: Query for a component
    When I get the component with uuid "tosca.nodes.Compute:1.0"
    Then I should receive a RestResponse with no error
    And I should retrieve a component detail with list of it's properties and interfaces.

  @reset
  Scenario: get a component other than a node type
    When I try to get a component with id "tosca.capabilities.Container:1.0"
    Then I should receive a RestResponse with no error
    And I should have a component with id "tosca.capabilities.Container:1.0"
