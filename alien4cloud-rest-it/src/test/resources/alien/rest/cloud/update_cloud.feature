Feature: Update cloud

Background:
  Given I am authenticated with "ADMIN" role
  And I upload a plugin

Scenario: Update a cloud's name
  Given I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
    When I update cloud name from "Mount doom cloud" to "Mordor cloud"
    Then I should receive a RestResponse with no error
  When I list clouds
  Then I should receive a RestResponse with no error
    And Response should contains a cloud with name "Mordor cloud"

Scenario: Update a cloud's name with same name should not fail (just ignored)
  Given I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
    When I update cloud name from "Mount doom cloud" to "Mount doom cloud"
    Then I should receive a RestResponse with no error
  When I list clouds
  Then I should receive a RestResponse with no error
    And Response should contains a cloud with name "Mount doom cloud"

Scenario: Update a cloud's name with existing name should fail
  When I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
  And I create a cloud with name "Mordor cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
  And I update cloud name from "Mount doom cloud" to "Mordor cloud"
  Then I should receive a RestResponse with an error code 502
  When I list clouds
  Then I should receive a RestResponse with no error
    And Response should contains 2 cloud
    And Response should contains a cloud with name "Mordor cloud"
    And Response should contains a cloud with name "Mount doom cloud"

Scenario: Update a cloud's iaas type
  When I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
  When I update cloud named "Mount doom cloud" iaas type to "OPENSTACK"
  Then I should receive a RestResponse with no error
  When I list clouds
  Then I should receive a RestResponse with no error
    And Response should contains a cloud with name "Mount doom cloud" and iass type "OPENSTACK"

Scenario: Update a cloud's environment type
  When I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
  When I update cloud named "Mount doom cloud" environment type to "DEVELOPMENT"
  Then I should receive a RestResponse with no error
  When I list clouds
  Then I should receive a RestResponse with no error
    And Response should contains a cloud with name "Mount doom cloud" and environment type "DEVELOPMENT"

Scenario: Update the naming policy of deployment
  Given I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
  When I list clouds
  Then I should receive a RestResponse with no error
    And Response should contains a cloud with deploymentNamePattern "environment.name + application.name"
  When I update deployment name pattern of "Mount doom cloud" to "environment.name"
    Then I should receive a RestResponse with no error
  When I list clouds
  Then I should receive a RestResponse with no error
    And Response should contains a cloud with deploymentNamePattern "environment.name"

# Scenario: Update a cloud's authorized groups
#   When I update cloud authorized groups
#       | mordor |
#       | isangard |
#   Then I should receive a RestResponse with no error
#   When I list clouds
#   Then I should receive a RestResponse with no error
#     And Response should contains a cloud with name "Mordor cloud"
#     And Response should contains a cloud with authorized groups
#       | mordor |
#       | isangard |

# Scenario: Update a cloud's authorized users
#   When I update cloud authorized users
#       | sauron |
#       | witch_king_of_angmar |
#   Then I should receive a RestResponse with no error
#   When I list clouds
#   Then I should receive a RestResponse with no error
#     And Response should contains a cloud with name "Mordor cloud"
#     And Response should contains a cloud with authorized users
#       | sauron |
#       | witch_king_of_angmar |
