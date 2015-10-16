Feature: Manage location meta-properties

Background:
  Given I am authenticated with "ADMIN" role
  And I upload the archive "tosca-normative-types-wd06"
  And I upload a plugin
  And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"
  And I enable the orchestrator "Mount doom orchestrator"  
  And I load several configuration tags
  Then I should have 11 configuration tags loaded

 Scenario: I can set properties constraint for the meta-properties of a cloud
  Given I am authenticated with "ADMIN" role
  And I have the tag "CLOUD_META_2" registered for "location"
  And I set the value "TT" for the cloud meta-property "CLOUD_META_1" of the cloud "location"
    Then I should receive a RestResponse with no error
  And I set the value "TT" for the cloud meta-property "CLOUD_META_2" of the cloud "location"
    Then I should receive a RestResponse with an error code 804

 Scenario: Add a tag with an already existing name and target should failed
  Given I am authenticated with "ADMIN" role
  And I have the tag "CLOUD_META_2" registered for "location"
  And I create a new tag with name "CLOUD_META_2" and the target "location"
    Then I should receive a RestResponse with an error code 502
