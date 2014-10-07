Feature: Update cloud configuration

Background:
  Given I am authenticated with "ADMIN" role
  And I upload a plugin
  When I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"

Scenario: Update cloud configuration
  When I update configuration for cloud "Mount doom cloud"
  Then I should receive a RestResponse with no error
  When I get configuration for cloud "Mount doom cloud"
  Then I should receive a RestResponse with no error
    And The cloud configuration should not be null

Scenario: Update cloud configuration using invalid json configuration should fail
  When I update configuration for cloud "Mount doom cloud" with wrong configuration
  Then I should receive a RestResponse with an error code 351
  When I get configuration for cloud "Mount doom cloud"
  Then I should receive a RestResponse with no error
    And The cloud configuration should be null
