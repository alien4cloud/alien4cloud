Feature: Get cloud deployment properties

Background:
  Given I am authenticated with "ADMIN" role
  And I upload a plugin
  When I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider" 

Scenario: Get cloud deployment properties
  When I get deployment properties for cloud "Mount doom cloud"
  Then I should receive a RestResponse with no error
  And The RestResponse should contain the following properties and values
    |      name       |     value     |
    |   managementUrl ||
    |   numberBackup  ||
    |   managerEmail  ||
