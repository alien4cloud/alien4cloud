Feature: Authentication

@reset
Scenario: Retrieving the ALIEN's roles list
  When I retrieve the ALIEN's roles list
  Then I should receive a RestResponse with no error

@reset
Scenario: Authenticate to alien with exiting account
  Given I am logged out
  When I authenticate with username "admin" and password "admin"
  Then I should receive a RestResponse with no error

@reset
Scenario: Authenticate to alien with wrong password should fail
  Given I am logged out
  When I authenticate with username "admin" and password "wrongpassword"
  Then I should receive a RestResponse with an error code 101

@reset
Scenario: Accessing a service with no account should fail
  Given I am logged out
  When I create a new user with username "sauron" and password "thering" in the system
  Then I should receive a RestResponse with an error code 100

@reset
Scenario: Accessing a service with no account should fail
  Given I am authenticated with "COMPONENTS_BROWSER" role
  When I create a new user with username "sauron" and password "thering" in the system
  Then I should receive a RestResponse with an error code 102

@reset
Scenario: Authenticate to alien using internal user should succeed.
  Given I am authenticated with "ADMIN" role
  And There is no "sauron" user in the system
  And I create a new user with username "sauron" and password "thering" in the system
  And I am logged out
  When I authenticate with username "sauron" and password "thering"
  Then I should receive a RestResponse with no error

@reset
Scenario: Authenticate to alien using internal user should fail if wrong password.
  Given I am authenticated with "ADMIN" role
  And There is no "sauron" user in the system
  And I create a new user with username "sauron" and password "thering" in the system
  And I am logged out
  When I authenticate with username "sauron" and password "idontwantthering"
  Then I should receive a RestResponse with an error code 101
