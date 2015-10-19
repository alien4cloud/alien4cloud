Feature: Create / Delete operations on application environment

Background:
  Given I am authenticated with "ADMIN" role
  And There are these users in the system
    | frodon |
  And I create a new group with name "hobbits" in the system
  And I create a new application with name "LAMP" and description "LAMP Stack application..." without errors
  
Scenario: Add role to user 
  When I add a role "DEPLOYMENT_MANAGER" to user "frodon" on the resource type "ENVIRONMENT" named "Environment"
  Then I should receive a RestResponse with no error
  When I get the application environment named "Environment"
  And I register the rest response data as SPEL context of type "alien4cloud.model.application.ApplicationEnvironment"
  And The SPEL expression "userRoles['frodon'][0]" should return "DEPLOYMENT_MANAGER"
  
Scenario: Add then remove role to user 
  Given I add a role "DEPLOYMENT_MANAGER" to user "frodon" on the resource type "ENVIRONMENT" named "Environment"
  When I remove a role "DEPLOYMENT_MANAGER" to user "frodon" on the resource type "ENVIRONMENT" named "Environment"
  Then I should receive a RestResponse with no error
  When I get the application environment named "Environment"
  And I register the rest response data as SPEL context of type "alien4cloud.model.application.ApplicationEnvironment"
  And The SPEL boolean expression "userRoles['frodon'] == null" should return true

Scenario: Add role to group 
  When I add a role "DEPLOYMENT_MANAGER" to group "hobbits" on the resource type "ENVIRONMENT" named "Environment"
  Then I should receive a RestResponse with no error
  When I get the application environment named "Environment"
  And I register the rest response data as SPEL context of type "alien4cloud.model.application.ApplicationEnvironment"
  And The SPEL expression "groupRoles.entrySet()[0].value[0]" should return "DEPLOYMENT_MANAGER"
  
Scenario: Add then remove role to group 
  Given I add a role "DEPLOYMENT_MANAGER" to group "hobbits" on the resource type "ENVIRONMENT" named "Environment"
  When I remove a role "DEPLOYMENT_MANAGER" to group "hobbits" on the resource type "ENVIRONMENT" named "Environment"
  Then I should receive a RestResponse with no error
  When I get the application environment named "Environment"
  And I register the rest response data as SPEL context of type "alien4cloud.model.application.ApplicationEnvironment"
  And The SPEL boolean expression "groupRoles == null" should return true
  