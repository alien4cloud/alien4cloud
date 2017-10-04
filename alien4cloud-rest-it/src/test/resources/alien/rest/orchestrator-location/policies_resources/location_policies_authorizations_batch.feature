Feature: Manage location policies resources authorizations in batch mode

  Background:
    Given I am authenticated with "ADMIN" role
    And There are these users in the system
      | frodon  |
      | sam     |
      | gandalf |
    And There are groups in the system
      | lordOfRing |
      | hobbits    |
      | elves      |
    And I add the user "frodon" to the group "lordOfRing"
    And I add the user "sam" to the group "hobbits"
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "middle_earth" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    Given I grant access to the resource type "LOCATION" named "middle_earth" to the user "frodon"
    Given I grant access to the resource type "LOCATION" named "middle_earth" to the group "lordOfRing"
    When I create a policy resource of type "org.alien4cloud.policies.mock.MinimalPolicyType" named "MinimalPolicyType1" related to the location "Mount doom orchestrator"/"middle_earth"
    When I create a policy resource of type "org.alien4cloud.policies.mock.MinimalPolicyType" named "MinimalPolicyType2" related to the location "Mount doom orchestrator"/"middle_earth"
    Then I should receive a RestResponse with no error


  @reset
  Scenario: Add / Remove rights to a user on location policies resources
    Given I successfully grant access to the resource type "LOCATION_POLICY" named "MinimalPolicyType1" to the user "frodon"
    Given I grant access to the resources type "LOCATION_POLICY" to the user "sam"
      | MinimalPolicyType1 |
      | MinimalPolicyType2 |
    Then I should receive a RestResponse with no error
    When I get the authorised users for the resource type "LOCATION_POLICY" named "MinimalPolicyType1"
    Then I should have following list of users:
      | frodon |
      | sam    |
    And I get the authorised users for the resource type "LOCATION_POLICY" named "MinimalPolicyType2"
    Then I should have following list of users:
      | sam |
    # sam should have been granted permissions on the location
    And I get the authorised users for the resource type "LOCATION" named "middle_earth"
    Then I should have following list of users:
      | frodon |
      | sam    |
    When I revoke access to the resources type "LOCATION_POLICY" from the user "sam"
      | MinimalPolicyType1 |
      | MinimalPolicyType2 |
    Then I should receive a RestResponse with no error
    When I get the authorised users for the resource type "LOCATION_POLICY" named "MinimalPolicyType1"
    Then I should have following list of users:
      | frodon |
    When I get the authorised users for the resource type "LOCATION_POLICY" named "MinimalPolicyType2"
    Then I should not have any authorized users
    Given I successfully revoke access to the resources type "LOCATION_POLICY" from the user "frodon"
      | MinimalPolicyType1 |
    When I get the authorised users for the resource type "LOCATION_POLICY" named "MinimalPolicyType1"
    Then I should not have any authorized users
    When I get the authorised users for the resource type "LOCATION_POLICY" named "MinimalPolicyType2"
    Then I should not have any authorized users


  @reset
  Scenario: Add / Remove rights to a group on location policies resources
    Given I successfully grant access to the resource type "LOCATION_POLICY" named "MinimalPolicyType1" to the group "lordOfRing"
    When I grant access to the resources type "LOCATION_POLICY" to the group "hobbits"
      | MinimalPolicyType1 |
      | MinimalPolicyType2 |
    Then I should receive a RestResponse with no error
    When I get the authorised groups for the resource type "LOCATION_POLICY" named "MinimalPolicyType1"
    Then I should have following list of groups:
      | lordOfRing |
      | hobbits    |
    And I get the authorised groups for the resource type "LOCATION_POLICY" named "MinimalPolicyType2"
    Then I should have following list of groups:
      | hobbits |
    # hobbits should have been granted permissions on the location
    And I get the authorised groups for the resource type "LOCATION" named "middle_earth"
    Then I should have following list of groups:
      | lordOfRing |
      | hobbits    |
    When I revoke access to the resources type "LOCATION_POLICY" from the group "lordOfRing"
      | MinimalPolicyType1 |
      | MinimalPolicyType2 |
    Then I should receive a RestResponse with no error
    When I get the authorised groups for the resource type "LOCATION_POLICY" named "MinimalPolicyType1"
    Then I should have following list of groups:
      | hobbits |
    When I get the authorised groups for the resource type "LOCATION_POLICY" named "MinimalPolicyType2"
    Then I should have following list of groups:
      | hobbits |
    Given I successfully revoke access to the resources type "LOCATION_POLICY" from the user "frodon"
      | MinimalPolicyType1 |
    When I get the authorised users for the resource type "LOCATION_POLICY" named "MinimalPolicyType1"
    Then I should not have any authorized groups
    When I get the authorised groups for the resource type "LOCATION_POLICY" named "MinimalPolicyType2"
    Then I should have following list of groups:
      | hobbits |

  @reset
  Scenario: Add / Remove rights to a application on location policies resources
    And I create an application with name "ALIEN", archive name "ALIEN", description "" and topology template id "null"
    When I create an application environment of type "DEVELOPMENT" with name "DEV-ALIEN" and description "" for the newly created application
    When I create an application environment of type "INTEGRATION_TESTS" with name "TST-ALIEN" and description "" for the newly created application
    When I create an application environment of type "PRODUCTION" with name "PRD-ALIEN" and description "" for the newly created application
    And I create an application with name "SDE", archive name "SDE", description "" and topology template id "null"
    And I create an application with name "SMAUG", archive name "SMAUG", description "" and topology template id "null"
    Given I grant access to the resource type "LOCATION" named "middle_earth" to the application "SMAUG"
    Given I grant access to the resource type "LOCATION_POLICY" named "MinimalPolicyType1" to the application "SMAUG"

    When I grant access to the resources type "LOCATION_POLICY" to the application "SDE"
      | MinimalPolicyType1 |
      | MinimalPolicyType2 |
    Then I should receive a RestResponse with no error
    When I get the authorised applications for the resource type "LOCATION_POLICY" named "MinimalPolicyType1"
    Then I should have following list of applications:
      | SMAUG |
      | SDE   |
    When I get the authorised applications for the resource type "LOCATION_POLICY" named "MinimalPolicyType2"
    Then I should have following list of applications:
      | SDE |
        # SDE should have been granted permissions on the location
    And I get the authorised applications for the resource type "LOCATION" named "middle_earth"
    Then I should have following list of applications:
      | SMAUG |
      | SDE   |
    Given I successfully revoke access to the resources type "LOCATION_POLICY" from the application "SDE"
      | MinimalPolicyType1 |
      | MinimalPolicyType2 |
    When I get the authorised applications for the resource type "LOCATION_POLICY" named "MinimalPolicyType1"
    Then I should have following list of applications:
      | SMAUG |

  #Environments
    When I grant access to the resources type "LOCATION_POLICY" to the environment "DEV-ALIEN" of the application "ALIEN"
      | MinimalPolicyType1 |
      | MinimalPolicyType2 |
    Then I should receive a RestResponse with no error
    When I get the authorised applications for the resource type "LOCATION_POLICY" named "MinimalPolicyType1"
    Then I should have following list of environments:
      | DEV-ALIEN |
    When I get the authorised applications for the resource type "LOCATION_POLICY" named "MinimalPolicyType2"
    Then I should have following list of environments:
      | DEV-ALIEN |

    Given I grant access to the resources type "LOCATION_POLICY" to the environment "PRD-ALIEN" of the application "ALIEN"
      | MinimalPolicyType1 |
      | MinimalPolicyType2 |
    When I get the authorised applications for the resource type "LOCATION_POLICY" named "MinimalPolicyType1"
    Then I should have following list of environments:
      | DEV-ALIEN |
      | PRD-ALIEN |
    And I should have following list of applications:
      | ALIEN |
      | SMAUG |
    When I get the authorised applications for the resource type "LOCATION_POLICY" named "MinimalPolicyType2"
    Then I should have following list of environments:
      | DEV-ALIEN |
      | PRD-ALIEN |
    And I should have following list of applications:
      | ALIEN |

    Given I successfully revoke access to the resources type "LOCATION_POLICY" from the environment "DEV-ALIEN" of the application "ALIEN"
      | MinimalPolicyType1 |
    When I get the authorised applications for the resource type "LOCATION_POLICY" named "MinimalPolicyType1"
    Then I should have following list of environments:
      | PRD-ALIEN |
    And I should have following list of applications:
      | ALIEN |
      | SMAUG |
    When I get the authorised applications for the resource type "LOCATION_POLICY" named "MinimalPolicyType2"
    Then I should have following list of environments:
      | DEV-ALIEN |
      | PRD-ALIEN |
    And I should have following list of applications:
      | ALIEN |

    Given I successfully revoke access to the resources type "LOCATION_POLICY" from the environment "PRD-ALIEN" of the application "ALIEN"
      | MinimalPolicyType1 |
      | MinimalPolicyType2 |
    When I get the authorised applications for the resource type "LOCATION_POLICY" named "MinimalPolicyType1"
    Then I should not have any authorized environments
    Then I should have following list of applications:
      | SMAUG |
    When I get the authorised applications for the resource type "LOCATION_POLICY" named "MinimalPolicyType2"
    Then I should have following list of environments:
      | DEV-ALIEN |
    And I should have following list of applications:
      | ALIEN |

    Given I successfully revoke access to the resources type "LOCATION_POLICY" from the application "SMAUG"
      | MinimalPolicyType1 |
      | MinimalPolicyType2 |
    When I get the authorised applications for the resource type "LOCATION_POLICY" named "MinimalPolicyType1"
    Then I should not have any authorized applications
    Then I should not have any authorized environments
    When I get the authorised applications for the resource type "LOCATION_POLICY" named "MinimalPolicyType2"
    Then I should have following list of environments:
      | DEV-ALIEN |
    And I should have following list of applications:
      | ALIEN |
    When I successfully revoke access to the resources type "LOCATION_POLICY" from the application "ALIEN"
      | MinimalPolicyType2 |
    When I get the authorised applications for the resource type "LOCATION_POLICY" named "MinimalPolicyType2"
    Then I should have following list of environments:
      | DEV-ALIEN |
    And I should have following list of applications:
      | ALIEN |
