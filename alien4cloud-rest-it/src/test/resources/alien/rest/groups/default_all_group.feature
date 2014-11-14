Feature: Default internal group representing all users

Background: 
  Given I am authenticated with "ADMIN" role
  And There is a "ALL_USERS" group in the system
#As all indexes are cleaned up before each test, we're obliged to create it
#It is supposed to be created at application bootstrap
  
Scenario: Create a new group with the same name as ALL_USERS internal group
  When I create a new group with name "ALL_USERS" in the system
  Then I should receive a RestResponse with an error code 502

Scenario: Even a user with ADMIN role can't remove this internal group
  When I delete the "ALL_USERS" group 
  Then I should receive a RestResponse with an error code 506 
    And There should be a group "ALL_USERS" in the system
    
Scenario: Getting ALL_USERS group from its name should succeed
  When I get the "ALL_USERS" group 
  Then I should receive a RestResponse with no error

Scenario: Updating ALL_USERS group should fail
  When I update the "ALL_USERS" group's name to "NEW_GROUP" 
  Then I should receive a RestResponse with an error code 506

Scenario: Adding or Removing a user to ALL_USERS group should fail
  When I add the user "sauron" to the group "ALL_USERS"
  Then I should receive a RestResponse with an error code 506
  When I remove the user "gandalf" from the group "ALL_USERS"
  Then I should receive a RestResponse with an error code 506

