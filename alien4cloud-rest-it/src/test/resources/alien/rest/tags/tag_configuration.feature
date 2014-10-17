Feature: Tag configurations

Background:
  Given I am authenticated with "ADMIN" role

Scenario: add tag configuration
  When I add the tag configuration with name "maturity" of type "component"
  Then I should receive a RestResponse with no error
  And The RestResponse should contain valid tag configuration
  And The tag configuration must exist in ALIEN

Scenario: delete tag configuration
  Given I add the tag configuration with name "maturity" of type "component"
  When I delete the tag configuration
  Then I should receive a RestResponse with no error
  And The tag configuration must not exist in ALIEN
