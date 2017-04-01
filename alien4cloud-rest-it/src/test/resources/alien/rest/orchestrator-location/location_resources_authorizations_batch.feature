Feature: Manage location resources authorizations in batch mode

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
    When I create a resource of type "org.alien4cloud.nodes.mock.Compute" named "Medium1" related to the location "Mount doom orchestrator"/"middle_earth"
    When I create a resource of type "org.alien4cloud.nodes.mock.Compute" named "Medium2" related to the location "Mount doom orchestrator"/"middle_earth"
    Then I should receive a RestResponse with no error


  @reset
  Scenario: Add / Remove rights to a user on location resources
    Given I successfully grant access to the resource type "LOCATION_RESOURCE" named "Medium1" to the user "frodon"
    Given I grant access to the resources type "LOCATION_RESOURCE" to the user "sam"
      | Medium1 |
      | Medium2 |
    Then I should receive a RestResponse with no error
    When I get the authorised users for the resource type "LOCATION_RESOURCE" named "Medium1"
    Then I should have following list of users:
      | frodon |
      | sam    |
    And I get the authorised users for the resource type "LOCATION_RESOURCE" named "Medium2"
    Then I should have following list of users:
      | sam |
    # sam should have been granted permissions on the location
    And I get the authorised users for the resource type "LOCATION" named "middle_earth"
    Then I should have following list of users:
      | frodon |
      | sam    |
    When I revoke access to the resources type "LOCATION_RESOURCE" from the user "sam"
      | Medium1 |
      | Medium2 |
    Then I should receive a RestResponse with no error
    When I get the authorised users for the resource type "LOCATION_RESOURCE" named "Medium1"
    Then I should have following list of users:
      | frodon |
    When I get the authorised users for the resource type "LOCATION_RESOURCE" named "Medium2"
    Then I should not have any authorized users
    Given I successfully revoke access to the resources type "LOCATION_RESOURCE" from the user "frodon"
      | Medium1 |
    When I get the authorised users for the resource type "LOCATION_RESOURCE" named "Medium1"
    Then I should not have any authorized users
    When I get the authorised users for the resource type "LOCATION_RESOURCE" named "Medium2"
    Then I should not have any authorized users


  @reset
  Scenario: Add / Remove rights to a group on location resources
    Given I successfully grant access to the resource type "LOCATION_RESOURCE" named "Medium1" to the group "lordOfRing"
    When I grant access to the resources type "LOCATION_RESOURCE" to the group "hobbits"
      | Medium1 |
      | Medium2 |
    Then I should receive a RestResponse with no error
    When I get the authorised groups for the resource type "LOCATION_RESOURCE" named "Medium1"
    Then I should have following list of groups:
      | lordOfRing |
      | hobbits    |
    And I get the authorised groups for the resource type "LOCATION_RESOURCE" named "Medium2"
    Then I should have following list of groups:
      | hobbits |
    # hobbits should have been granted permissions on the location
    And I get the authorised groups for the resource type "LOCATION" named "middle_earth"
    Then I should have following list of groups:
      | lordOfRing |
      | hobbits    |
    When I revoke access to the resources type "LOCATION_RESOURCE" from the group "lordOfRing"
      | Medium1 |
      | Medium2 |
    Then I should receive a RestResponse with no error
    When I get the authorised groups for the resource type "LOCATION_RESOURCE" named "Medium1"
    Then I should have following list of groups:
      | hobbits |
    When I get the authorised groups for the resource type "LOCATION_RESOURCE" named "Medium2"
    Then I should have following list of groups:
      | hobbits |
    Given I successfully revoke access to the resources type "LOCATION_RESOURCE" from the user "frodon"
      | Medium1 |
    When I get the authorised users for the resource type "LOCATION_RESOURCE" named "Medium1"
    Then I should not have any authorized groups
    When I get the authorised groups for the resource type "LOCATION_RESOURCE" named "Medium2"
    Then I should have following list of groups:
      | hobbits |

  @reset
  Scenario: Add / Remove rights to a application on location resources
    And I create an application with name "ALIEN", archive name "ALIEN", description "" and topology template id "null"
    When I create an application environment of type "DEVELOPMENT" with name "DEV-ALIEN" and description "" for the newly created application
    When I create an application environment of type "INTEGRATION_TESTS" with name "TST-ALIEN" and description "" for the newly created application
    When I create an application environment of type "PRODUCTION" with name "PRD-ALIEN" and description "" for the newly created application
    And I create an application with name "SDE", archive name "SDE", description "" and topology template id "null"
    And I create an application with name "SMAUG", archive name "SMAUG", description "" and topology template id "null"
    Given I grant access to the resource type "LOCATION" named "middle_earth" to the application "SMAUG"
    Given I grant access to the resource type "LOCATION_RESOURCE" named "Medium1" to the application "SMAUG"

    When I grant access to the resources type "LOCATION_RESOURCE" to the application "SDE"
      | Medium1 |
      | Medium2 |
    Then I should receive a RestResponse with no error
    When I get the authorised applications for the resource type "LOCATION_RESOURCE" named "Medium1"
    Then I should have following list of applications:
      | SMAUG |
      | SDE   |
    When I get the authorised applications for the resource type "LOCATION_RESOURCE" named "Medium2"
    Then I should have following list of applications:
      | SDE |
        # SDE should have been granted permissions on the location
    And I get the authorised applications for the resource type "LOCATION" named "middle_earth"
    Then I should have following list of applications:
      | SMAUG |
      | SDE   |
    Given I successfully revoke access to the resources type "LOCATION_RESOURCE" from the application "SDE"
      | Medium1 |
      | Medium2 |
    When I get the authorised applications for the resource type "LOCATION_RESOURCE" named "Medium1"
    Then I should have following list of applications:
      | SMAUG |

  #Environments
    When I grant access to the resources type "LOCATION_RESOURCE" to the environment "DEV-ALIEN" of the application "ALIEN"
      | Medium1 |
      | Medium2 |
    Then I should receive a RestResponse with no error
    When I get the authorised applications for the resource type "LOCATION_RESOURCE" named "Medium1"
    Then I should have following list of environments:
      | DEV-ALIEN |
    When I get the authorised applications for the resource type "LOCATION_RESOURCE" named "Medium2"
    Then I should have following list of environments:
      | DEV-ALIEN |

    Given I grant access to the resources type "LOCATION_RESOURCE" to the environment "PRD-ALIEN" of the application "ALIEN"
      | Medium1 |
      | Medium2 |
    When I get the authorised applications for the resource type "LOCATION_RESOURCE" named "Medium1"
    Then I should have following list of environments:
      | DEV-ALIEN |
      | PRD-ALIEN |
    And I should have following list of applications:
      | ALIEN |
      | SMAUG |
    When I get the authorised applications for the resource type "LOCATION_RESOURCE" named "Medium2"
    Then I should have following list of environments:
      | DEV-ALIEN |
      | PRD-ALIEN |
    And I should have following list of applications:
      | ALIEN |

    Given I successfully revoke access to the resources type "LOCATION_RESOURCE" from the environment "DEV-ALIEN" of the application "ALIEN"
      | Medium1 |
    When I get the authorised applications for the resource type "LOCATION_RESOURCE" named "Medium1"
    Then I should have following list of environments:
      | PRD-ALIEN |
    And I should have following list of applications:
      | ALIEN |
      | SMAUG |
    When I get the authorised applications for the resource type "LOCATION_RESOURCE" named "Medium2"
    Then I should have following list of environments:
      | DEV-ALIEN |
      | PRD-ALIEN |
    And I should have following list of applications:
      | ALIEN |

    Given I successfully revoke access to the resources type "LOCATION_RESOURCE" from the environment "PRD-ALIEN" of the application "ALIEN"
      | Medium1 |
      | Medium2 |
    When I get the authorised applications for the resource type "LOCATION_RESOURCE" named "Medium1"
    Then I should not have any authorized environments
    Then I should have following list of applications:
      | SMAUG |
    When I get the authorised applications for the resource type "LOCATION_RESOURCE" named "Medium2"
    Then I should have following list of environments:
      | DEV-ALIEN |
    And I should have following list of applications:
      | ALIEN |

    Given I successfully revoke access to the resources type "LOCATION_RESOURCE" from the application "SMAUG"
      | Medium1 |
      | Medium2 |
    When I get the authorised applications for the resource type "LOCATION_RESOURCE" named "Medium1"
    Then I should not have any authorized applications
    Then I should not have any authorized environments
    When I get the authorised applications for the resource type "LOCATION_RESOURCE" named "Medium2"
    Then I should have following list of environments:
      | DEV-ALIEN |
    And I should have following list of applications:
      | ALIEN |
    When I successfully revoke access to the resources type "LOCATION_RESOURCE" from the application "ALIEN"
      | Medium2 |
    When I get the authorised applications for the resource type "LOCATION_RESOURCE" named "Medium2"
    Then I should have following list of environments:
      | DEV-ALIEN |
    And I should have following list of applications:
      | ALIEN |
