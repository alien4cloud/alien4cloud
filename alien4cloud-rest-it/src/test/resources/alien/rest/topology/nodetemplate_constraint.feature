Feature: Manage Nodetemplates of a topology with constraint

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "constraints"
    And I should receive a RestResponse with no error
    And There is a "node type" with element name "alien.nodes.test.PropertyConstraint" and archive version "1.0"
    And I create a new application with name "watchmiddleearth" and description "Use my great eye to find frodo and the ring."

  @reset
  Scenario: Update a nodetemplate's property with greaterThan constraint violated
    Given I have added a node template "Template1" related to the "alien.nodes.test.PropertyConstraint:1.0" node type
    When I update the node template "Template1"'s property "greater_than_prop" to "A"
    Then I should receive a RestResponse with an error code 800
    And I should receive a RestResponse with constraint data name "greaterThan" and reference "0"

  @reset
  Scenario: Update a nodetemplate's property with greaterOrEqual constraint violated
    Given I have added a node template "TemplateJava" related to the "alien.nodes.test.PropertyConstraint:1.0" node type
    When I update the node template "TemplateJava"'s property "greater_or_equal_prop" to "1.2"
    Then I should receive a RestResponse with an error code 800
    And I should receive a RestResponse with constraint data name "greaterOrEqual" and reference "3"

  @reset
  Scenario: Update a nodetemplate's property with inRange constraint violated
    Given I have added a node template "TemplateCompute" related to the "alien.nodes.test.PropertyConstraint:1.0" node type
    When I update the node template "TemplateCompute"'s property "multiple_version_prop" to "1.2"
    Then I should receive a RestResponse with an error code 800
    And I should receive a RestResponse with constraint data name "inRange" and reference "[1.5, 1.7]"

  @reset
  Scenario: Update a nodetemplate's property with validValues constraint violated
    Given I have added a node template "TemplateCompute" related to the "alien.nodes.test.PropertyConstraint:1.0" node type
    When I update the node template "TemplateCompute"'s property "valid_values_version_prop" to "1.2"
    Then I should receive a RestResponse with an error code 800
    And I should receive a RestResponse with constraint data name "validValues" and reference "[1.5, 1.6, 1.7]"

  @reset
  Scenario: Update a nodetemplate's property with validValues constraint with in range value violated
    Given I have added a node template "TemplateCompute" related to the "alien.nodes.test.PropertyConstraint:1.0" node type
    When I update the node template "TemplateCompute"'s property "valid_values_version_prop" to "1.6"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Update a nodetemplate's property with greaterOrEqual constraint violated
    Given I have added a node template "Template3" related to the "alien.nodes.test.PropertyConstraint:1.0" node type
    When I update the node template "Template3"'s property "greater_or_equal_prop" to "2"
    Then I should receive a RestResponse with an error code 800
    And I should receive a RestResponse with constraint data name "greaterOrEqual" and reference "3"

  @reset
  Scenario: Update a nodetemplate's property with greaterOrEqual constraint without error
    Given I have added a node template "Template3" related to the "alien.nodes.test.PropertyConstraint:1.0" node type
    When I update the node template "Template3"'s property "greater_or_equal_prop" to "3"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Update a nodetemplate's property with equal constraint
    Given I have added a node template "Template4" related to the "alien.nodes.test.PropertyConstraint:1.0" node type
    When I update the node template "Template4"'s property "equal_prop" to "3"
    Then I should receive a RestResponse with an error code 800
    And I should receive a RestResponse with constraint data name "equal" and reference "2"
