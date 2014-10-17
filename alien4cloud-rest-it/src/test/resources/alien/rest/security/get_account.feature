Feature: Accessing user account details

Scenario: Getting a user from it's username should succeed.
  Given I authenticate with username "admin" and password "admin"
  When I get the "admin" user
  Then I should receive a RestResponse with no error

#Scenario: Getting a user from an empty username should fail.
#  Given I authenticate with username "admin" and password "admin"
#  When I get the "" user
#  Then I should receive a RestResponse with an error code 501

Scenario: Mass getting for users from a list of usernames 
  Given I authenticate with username "admin" and password "admin"
    And There is a "sauron" user in the system
    And There is a "bilbo" user in the system
    And There is a "golum" user in the system
  When I find users with usernames
    | sauron    |
    | bilbo     |
  Then I should receive a RestResponse with no error
    And the find RestResponse should have the users with usersnames
      | sauron    |
      | bilbo     |
      
Scenario: Mass getting for users from an empty list of usernames 
  Given I authenticate with username "admin" and password "admin"
    And There is a "sauron" user in the system
    And There is a "bilbo" user in the system
  When I find users with an empty usernames list
  Then I should receive a RestResponse with an error code 501
