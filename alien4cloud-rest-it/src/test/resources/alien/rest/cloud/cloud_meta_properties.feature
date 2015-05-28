Feature: Manage cloud meta-properties

Background:
  Given I am authenticated with "ADMIN" role
  And I upload a plugin
  And I create a cloud with name "cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
  And I load several configuration tags
  Then I should have 11 configuration tags loaded

 Scenario: I can set properties constraint for the meta-properties of a cloud
  Given I am authenticated with "ADMIN" role
  And I have the tag "cloud_meta_CLOUD_META_2" registered for "cloud"
  And I set the value "TT" for the cloud meta-property "cloud_meta_CLOUD_META_1" of the cloud "cloud"
    Then I should receive a RestResponse with no error
  And I set the value "TT" for the cloud meta-property "cloud_meta_CLOUD_META_2" of the cloud "cloud"
    Then I should receive a RestResponse with an error code 804

 Scenario: Add a tag with an already existing name and target should failed
  Given I am authenticated with "ADMIN" role
  And I have the tag "cloud_meta_CLOUD_META_2" registered for "cloud"
  And I create a new tag with name "cloud_meta_CLOUD_META_2" and the target "cloud"
    Then I should receive a RestResponse with an error code 502