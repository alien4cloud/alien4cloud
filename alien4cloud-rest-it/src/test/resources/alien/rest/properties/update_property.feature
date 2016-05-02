Feature: Check a property constraint

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca base types 3.0"

  @reset
  Scenario: I can update a complex property
    Given I am authenticated with "ADMIN" role
  #  And I set value "TT" for "_ALIEN_LIST" tag
  #  Then I should receive a RestResponse with an error code 800
  #  And I set value "A" for "_ALIEN_LIST" tag
  #  Then I should receive a RestResponse with no error
