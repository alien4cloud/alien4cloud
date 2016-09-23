Feature: Plugin configuration

Background:
  Given I am authenticated with "ADMIN" role
    And I have uploaded a plugin

@reset
Scenario: Get an empty plugin configuration
  When I get the plugin configuration
  Then I should receive a RestResponse with no error
  And there should be a configuration object in the response

@reset
Scenario: Set a valid configuration for a plugin
  When I set the plugin configuration with a valid configuration object
  Then I should receive a RestResponse with no error

@reset
Scenario: Set an invalid configuration object for a plugin
  When I set the plugin configuration with an invalid configuration object
  Then I should receive a RestResponse with an error code 352

@reset
Scenario: Get a plugin configuration that is already set
  Given I have set the plugin configuration with a valid configuration object
  When I get the plugin configuration
  Then I should receive a RestResponse with no error
  And there should be a non empty configuration object in the response

@reset
Scenario: Re-use a previous version's configuration: success case
  Given I have set the plugin configuration with a valid configuration object
  When I upload a plugin which "has the same configuration type"
  Then I should receive a RestResponse with no error
    And the new plugin configuration should be the same as for the previous version

  ## Something must have changed in
#@reset
#Scenario: Re-use a previous version's configuration: failure case
#  Given I have set the plugin configuration with a valid configuration object
#  When I upload a plugin which "has a different configuration type"
#  Then I should receive a RestResponse with no error
#    And the new plugin configuration should not be the same as for the previous version
