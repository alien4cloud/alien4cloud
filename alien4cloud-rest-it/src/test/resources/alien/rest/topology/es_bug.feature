Feature: Manage Nodetemplates of a topology with constraint

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload the archive "es schema free bug types"
    Then I should receive a RestResponse with no error
    And I upload the archive "es schema free bug template"
    Then I should receive a RestResponse with no error
    And I create a new application with name "es-schema-free-bug" and description "ES Schema free bug" based on the template with name "es_bug_template"

  @reset
  Scenario: Mix complex and simple nodetemplate's property
    Given I update the node template "idSimple"'s property "id" to "really simple"
    Then I should receive a RestResponse with no error
    Given I update the node template "idComplex"'s complex property "id" to """{"simple": "really simple in a complex one"}"""
    Then I should receive a RestResponse with no error