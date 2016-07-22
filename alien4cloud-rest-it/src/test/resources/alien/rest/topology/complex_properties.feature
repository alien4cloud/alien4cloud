Feature: Manage Nodetemplates of a topology with constraint

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload the archive "complex properties types"
    Then I should receive a RestResponse with no error
    And I upload the archive "complex properties template"
    Then I should receive a RestResponse with no error
    And I create a new application with name "complex-properties" and description "Complex properties validation" based on the template with name "complex_data_type_template"

  @reset
  Scenario: Validate complex properties feature
    Given I update the node template "complex"'s property "simple_complex" to "real simple"
    Then I should receive a RestResponse with no error
    Given I update the node template "complex"'s property "simple_complex" to "really complicate with a lots of things"
    Then I should receive a RestResponse with an error code 800
    Given I update the node template "complex"'s complex property "complex" to """{"simple": "real simple"}"""
    Then I should receive a RestResponse with no error
    Given I update the node template "complex"'s complex property "complex" to """{"simple": "really complicate with a lots of things"}"""
    Then I should receive a RestResponse with an error code 800
    Given I update the node template "complex"'s complex property "map" to """{"one_key": "real simple"}"""
    Then I should receive a RestResponse with no error
    Given I update the node template "complex"'s complex property "map" to """{"one_key": "really complicate with a lots of things"}"""
    Then I should receive a RestResponse with an error code 800
    Given I update the node template "complex"'s complex property "list" to """["real simple"]"""
    Then I should receive a RestResponse with no error
    Given I update the node template "complex"'s complex property "list" to """["really complicate with a lots of things"]"""
    Then I should receive a RestResponse with an error code 800