Feature: Manage location meta-properties

Background:
  Given I am authenticated with "ADMIN" role
  And I upload the archive "tosca-normative-types-wd06"
  And I upload a plugin
  And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"
  And I enable the orchestrator "Mount doom orchestrator"
  And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
  And I load several configuration tags
  Then I should have 11 configuration tags loaded

 Scenario: I can set properties constraint for the meta-properties of a location
  Given I am authenticated with "ADMIN" role
  And I have the tag "LOCATION_META_2" registered for "location"
  And I set the value "TT" to the location meta-property "LOCATION_META_1" of the location "Thark location" of the orchestrator "Mount doom orchestrator"
    Then I should receive a RestResponse with no error
  And I set the value "TT" to the location meta-property "LOCATION_META_2" of the location "Thark location" of the orchestrator "Mount doom orchestrator"
    Then I should receive a RestResponse with an error code 804

 Scenario: Add a tag with an already existing name and target should failed
  Given I am authenticated with "ADMIN" role
  And I have the tag "LOCATION_META_2" registered for "location"
  And I create a new tag with name "LOCATION_META_2" and the target "location"
    Then I should receive a RestResponse with an error code 502
