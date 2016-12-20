Feature: Manage location meta-properties

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I load several configuration tags
    Then I should have 11 configuration tags loaded

  @reset
  Scenario: I can set properties constraint for the meta-properties of a location
    Given I am authenticated with "ADMIN" role
    And I have the tag "LOCATION_META_2" registered for "location"
    And I set the value "TT" to the location meta-property "LOCATION_META_1" of the location "Thark location" of the orchestrator "Mount doom orchestrator"
      Then I should receive a RestResponse with no error
    And I set the value "TT" to the location meta-property "LOCATION_META_2" of the location "Thark location" of the orchestrator "Mount doom orchestrator"
      Then I should receive a RestResponse with an error code 804

  @reset
  Scenario: Add a tag with an already existing name and target should failed
    Given I am authenticated with "ADMIN" role
    And I have the tag "LOCATION_META_2" registered for "location"
    And I create a new tag with name "LOCATION_META_2" and the target "location"
      Then I should receive a RestResponse with an error code 502

  @reset
  Scenario: I can set properties and list their values from location
    Given I am authenticated with "ADMIN" role
    And I have the tag "LOCATION_META_2" registered for "location"
    And I set the value "test1" to the location meta-property "LOCATION_META_1" of the location "Thark location" of the orchestrator "Mount doom orchestrator"
      Then I should receive a RestResponse with no error
    And I set the value "2" to the location meta-property "LOCATION_META_2" of the location "Thark location" of the orchestrator "Mount doom orchestrator"
      Then I should receive a RestResponse with no error
    When I list locations of the orchestrator "Mount doom orchestrator"
      Then I should receive a RestResponse with no error
      And Response should contains 4 meta-property for the location "Thark location"
      And Response should contains a meta-property with value "test1" for "LOCATION_META_1"
      And Response should contains a meta-property with value "2" for "LOCATION_META_2"
