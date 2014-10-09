Feature: Default internal group create "ALL"

Background: 
  Given I am authenticated with "ADMIN" role 
  
Scenario: Create a new group with the same name "ALL" 
  Then I should receive a RestResponse with no error