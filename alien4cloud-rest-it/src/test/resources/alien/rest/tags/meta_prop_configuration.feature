Feature: Assign meta tags to Application, Component, Cloud

Background:
  Given I am authenticated with "ADMIN" role
  And I load several configuration tags
  Then I should have 8 configuration tags loaded

Scenario: I have assigned a meta tag to an application
  When I have the tag "_ALIEN_LIST" registered for "application"
  Then I should receive a RestResponse with no error

Scenario: I have assigned a meta tag to a cloud
  When I have the tag "CLOUD.METAS.CLOUD_META_1" registered for "cloud"
  Then I should receive a RestResponse with no error

Scenario: I have assigned a meta tag to a component
  When I have the tag "COMPONENT_JAVA" registered for "component"
  Then I should receive a RestResponse with no error