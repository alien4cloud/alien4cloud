Feature: Get cloud details

Background:
  Given I am authenticated with "ADMIN" role
  And I upload a plugin
  When I create a cloud with name "mock-paas-cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider" 
  And I update cloud named "mock-paas-cloud" iaas type to "OPENSTACK"
  And I update cloud named "mock-paas-cloud" environment type to "DEVELOPMENT"
  And There are these users in the system
    | frodon |
    | bilbo  |
  And I add a role "CLOUD_DEPLOYER" to user "bilbo" on the resource type "CLOUD" named "mock-paas-cloud"

Scenario: Get cloud details by name without cloud role should fail
  Given I am authenticated with user named "frodon"
  When I get the cloud by name "mock-paas-cloud"
  Then I should receive a RestResponse with an error code 102

Scenario: Get cloud details by name with role on it should success
  Given I am authenticated with user named "bilbo"
  When I get the cloud by name "mock-paas-cloud"
  Then I should receive a RestResponse with no error
  And I should receive a cloud with name "mock-paas-cloud"

Scenario: Get cloud details by id without cloud role should fail
  Given I am authenticated with user named "frodon"
  When I get the cloud "mock-paas-cloud"
  Then I should receive a RestResponse with an error code 102

Scenario: Get cloud details by id with cloud role should success
  Given I am authenticated with user named "bilbo"
  When I get the cloud "mock-paas-cloud"
  Then I should receive a RestResponse with no error
  And The Response should contains cloud with name "mock-paas-cloud" and iass type "OPENSTACK" and environment type "DEVELOPMENT"