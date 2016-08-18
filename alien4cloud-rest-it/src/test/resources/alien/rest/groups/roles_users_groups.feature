Feature: Creating a new group

  Background:
    Given I am authenticated with "ADMIN" role
    And There is a "lordOfRing" group in the system
    And There is a "sauron" user in the system
    And There is a "gandalf" user in the system
  @reset
  Scenario: Adding a role to a group should succeed
    When I add the role "COMPONENTS_BROWSER" to the group "lordOfRing"
    Then I should receive a RestResponse with no error
    And the group "lordOfRing" should have the following roles
      | COMPONENTS_BROWSER |

  @reset
  Scenario: Adding a wrong role to a group should fail
    When I add the role "COMPONENTS_BROWSERRRR" to the group "lordOfRing"
    Then I should receive a RestResponse with an error code 504

  @reset
  Scenario: Removing a role from a group should succeed
    Given I have added to the group "lordOfRing" roles
      | COMPONENTS_BROWSER |
      | COMPONENTS_MANAGER |
    When I remove the role "COMPONENTS_BROWSER" from the group "lordOfRing"
    Then I should receive a RestResponse with no error
    And the group "lordOfRing" should have the following roles
      | COMPONENTS_MANAGER |

  @reset
  Scenario: Adding a user to a group should succeed
    When I add the user "sauron" to the group "lordOfRing"
    Then I should receive a RestResponse with no error
    And the group "lordOfRing" should have the following users
      | sauron |

  @reset
  Scenario: Removing a user from a group should succeed
    Given I have added to the group "lordOfRing" users
      | sauron  |
      | gandalf |
    When I remove the user "gandalf" from the group "lordOfRing"
    Then I should receive a RestResponse with no error
    And the group "lordOfRing" should have the following users
      | sauron |

  @reset
  Scenario: Add a user to a group, remove the user and removing the group should succeed
    Given I have added to the group "lordOfRing" users
      | sauron  |
    When I delete the user "sauron"
      Then I should receive a RestResponse with no error
    When I delete the "lordOfRing" group
      Then I should receive a RestResponse with no error

  @reset
  Scenario: Adding a role to a group should succeed and update all the related users grouproles
    Given I have added to the group "lordOfRing" users
      | sauron |
    When I add the role "COMPONENTS_BROWSER" to the group "lordOfRing"
    Then I should receive a RestResponse with no error
    And the group "lordOfRing" should have the following roles
      | COMPONENTS_BROWSER |
    And the user "sauron" should have the following group roles
      | COMPONENTS_BROWSER |

  @reset
  Scenario: Adding a user to a group should succeed and update the related user's grouproles
    Given I have added to the group "lordOfRing" roles
      | COMPONENTS_BROWSER |
      | COMPONENTS_MANAGER |
    When I add the user "sauron" to the group "lordOfRing"
    Then I should receive a RestResponse with no error
    And the group "lordOfRing" should have the following users
      | sauron |
    And the user "sauron" should have the following group
      | lordOfRing |
    And the user "sauron" should have the following group roles
      | COMPONENTS_BROWSER |
      | COMPONENTS_MANAGER |

  @reset
  Scenario: removing a role from a group should succeed and update all the related users grouproles
    Given I have added to the group "lordOfRing" users
      | sauron |
    And I have added to the group "lordOfRing" roles
      | COMPONENTS_BROWSER |
      | COMPONENTS_MANAGER |
    When I remove the role "COMPONENTS_BROWSER" from the group "lordOfRing"
    Then I should receive a RestResponse with no error
    And the group "lordOfRing" should have the following roles
      | COMPONENTS_MANAGER |
    And the user "sauron" should have the following group roles
      | COMPONENTS_MANAGER |

  @reset
  Scenario: Removing a user from a group should succeed and update the related user's grouproles
    Given I have added to the group "lordOfRing" users
      | sauron |
    And I have added to the group "lordOfRing" roles
      | COMPONENTS_BROWSER |
      | COMPONENTS_MANAGER |
    When I remove the user "sauron" from the group "lordOfRing"
    Then I should receive a RestResponse with no error
    And the group "lordOfRing" should not have any users
    And the user "sauron" should not have any group
    And the user "sauron" should not have any group roles

  @reset
  Scenario: Managing a user in two different groups
    Given There is a "theHobbit" group in the system
    And I have added to the group "theHobbit" roles
      | COMPONENTS_MANAGER |
    And I have added to the group "lordOfRing" roles
      | COMPONENTS_BROWSER |
    And I have added to the group "lordOfRing" users
      | gandalf |
    #Adding gandalf to a second group
    When I add the user "gandalf" to the group "theHobbit"
    Then I should receive a RestResponse with no error
    And the group "theHobbit" should have the following users
      | gandalf |
    And the group "lordOfRing" should have the following users
      | gandalf |
    And the user "gandalf" should have the following group
      | lordOfRing |
      | theHobbit  |
    And the user "gandalf" should have the following group roles
      | COMPONENTS_BROWSER |
      | COMPONENTS_MANAGER |
    #Removing gandalf from a group
    When I remove the user "gandalf" from the group "lordOfRing"
    Then I should receive a RestResponse with no error
    And the group "lordOfRing" should not have any users
    And the user "gandalf" should have the following group
      | theHobbit |
    And the user "gandalf" should have the following group roles
      | COMPONENTS_MANAGER |
