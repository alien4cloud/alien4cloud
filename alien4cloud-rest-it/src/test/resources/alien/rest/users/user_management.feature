Feature: User's management

Background: 
  Given I am authenticated with "ADMIN" role
    
Scenario:  Delete the last admin user should failed
  Given I am authenticated with "ADMIN" role
  When I delete the user "admin"
    Then I should receive a RestResponse with an error code 390
   