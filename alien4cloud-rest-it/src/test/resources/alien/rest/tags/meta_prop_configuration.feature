Feature: Insert meta tags for Application, Component, Cloud and CRUD operations

Background:
  Given I am authenticated with "ADMIN" role
  And I load several configuration tags
  Then I should have 11 configuration tags loaded

Scenario: I have assigned a meta tag to an application
  When I have the tag "_ALIEN_LIST" registered for "application"
  Then I should receive a RestResponse with no error

Scenario: I have assigned a meta tag to a cloud
  When I have the tag "CLOUD_META_1" registered for "cloud"
  Then I should receive a RestResponse with no error

Scenario: I have assigned a meta tag to a component
  When I have the tag "COMPONENT_JAVA" registered for "component"
  Then I should receive a RestResponse with no error

Scenario: Delete a configuration tag
  Given I have the tag "_ALIEN_PASSWORD_MIN4" registered for "application"
  And I have the tag "CLOUD_META_1" registered for "cloud"
  When I delete the tag configuration "_ALIEN_PASSWORD_MIN4"
  Then I should receive a RestResponse with no error
  And The tag configuration "_ALIEN_PASSWORD_MIN4" must not exist in ALIEN
  When I delete the tag configuration "CLOUD_META_1"
  Then I should receive a RestResponse with no error
  And The tag configuration "CLOUD_META_1" must not exist in ALIEN
