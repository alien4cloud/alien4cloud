Feature: Get cloud details

Background:
  Given I am authenticated with "ADMIN" role
  And I upload a plugin
  When I create a cloud with name "Mock Cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider" 
  And I update cloud named "Mock Cloud" iaas type to "OPENSTACK"
  And I update cloud named "Mock Cloud" environment type to "DEVELOPMENT"

Scenario: Get cloud by name
  When I get the cloud "Mock Cloud"
  Then I should receive a RestResponse with no error
  And I should receive a cloud with name "Mock Cloud"

Scenario: Get cloud details
  When I get the cloud "Mock Cloud"
  Then I should receive a RestResponse with no error
  And the Response should contains cloud with name "Mock Cloud" and iass type "OPENSTACK" and environment type "DEVELOPMENT"
