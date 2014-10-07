Feature: Get cloud configuration

Background:
  Given I am authenticated with "ADMIN" role
  And I upload a plugin
  When I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"

Scenario: Get cloud configuration
  When I get configuration for cloud "Mount doom cloud"
  Then I should receive a RestResponse with no error
    And The cloud configuration should be null
