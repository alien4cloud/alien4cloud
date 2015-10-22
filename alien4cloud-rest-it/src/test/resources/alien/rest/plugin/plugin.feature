Feature: Plugin management

Background:
  Given I am authenticated with "ADMIN" role

Scenario: Upload a plugin
  When I upload a plugin
  Then I should receive a RestResponse with no error

Scenario: Re-Upload an exisitng plugin should fail
  Given I have uploaded a plugin
  When I upload a plugin
  Then I should receive a RestResponse with an error code 502

Scenario: Search for plugins
  Given I upload a plugin
  When I search for plugins
  Then I should receive a RestResponse with no error
    And The plugin response should contains 1 plugin

Scenario: Enable a plugin
  Given I upload a plugin
  When I enable the plugin
  Then I should receive a RestResponse with no error

Scenario: Disable a plugin
  Given I upload a plugin
    And I enable the plugin
  When I disable the plugin
  Then I should receive a RestResponse with no error

#Scenario: Disable a plugin when the plugin is used by a linker
#  Given I upload a plugin
#    And I enable the plugin
#    And I use the plugin
#  When I disable the plugin
#  Then I should receive a RestResponse with an error code 350
#  Then I should receive a RestResponse with a non-empty list of plugin usages.

Scenario: Remove a plugin
  Given I upload a plugin
    And I enable the plugin
  When I remove the plugin
  Then I should receive a RestResponse with no error

#Scenario: Remove a plugin when the plugin is used by a linker
#  Given I upload a plugin
#    And I enable the plugin
#    And I use the plugin
#  When I remove the plugin
#  Then I should receive a RestResponse with an error code 350
#  Then I should receive a RestResponse with a non-empty list of plugin usages.