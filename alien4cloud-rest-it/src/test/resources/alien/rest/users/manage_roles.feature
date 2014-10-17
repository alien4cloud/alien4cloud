Feature: Manage user's roles 

Background: 

  Given I am authenticated with "ADMIN" role
    And There is a "sauron" user in the system

Scenario:  adding roles to a user
  Given I am authenticated with "ADMIN" role
  When I add a role "COMPONENTS_BROWSER" to user "sauron"
  Then I should receive a RestResponse with no error
  When I get the "sauron" user
  Then I should receive a RestResponse with no error
    And The response should contain a user "sauron" having "COMPONENTS_BROWSER" role
    
Scenario:  removing a role from a user
  Given I am authenticated with "ADMIN" role
    And there is a user "sauron" with the "COMPONENTS_BROWSER" role
    And there is a user "sauron" with the "APPLICATIONS_MANAGER" role
  When I remove a role "COMPONENTS_BROWSER" to user "sauron"
  Then I should receive a RestResponse with no error
  When I get the "sauron" user
  Then I should receive a RestResponse with no error
    And The response should contain a user "sauron" not having "COMPONENTS_BROWSER" role
    And The response should contain a user "sauron" having "APPLICATIONS_MANAGER" role