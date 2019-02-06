Feature: Update service resource

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    When I create a service with name "MyBdService", version "1.0.0", type "tosca.nodes.Database", archive version "1.0.0-SNAPSHOT"
    And I register path "data.id" with class "alien4cloud.model.service.ServiceResource" as "serviceId"

  @reset
  Scenario: Updating a service with a valid request should succeed
    When I PUT "services/update_valid.json" to "/rest/v1/services/${serviceId}"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Updating a service with no name should fail
    When I PUT "services/update_missing_name.json" to "/rest/v1/services/${serviceId}"
    Then I should receive a RestResponse with an error code 501

  @reset
  Scenario: Updating a service twice with same name and version should not fail
    When I PUT "services/update_valid.json" to "/rest/v1/services/${serviceId}"
    Then I should receive a RestResponse with no error
    When I PUT "services/update_valid.json" to "/rest/v1/services/${serviceId}"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Updating a service with an existing name and version should fail
    When I create a service with name "new name", version "1.0.0-SNAPSHOT", type "tosca.nodes.Database", archive version "1.0.0-SNAPSHOT"
    When I PUT "services/update_valid.json" to "/rest/v1/services/${serviceId}"
    Then I should receive a RestResponse with an error code 502

  @reset
  Scenario: Updating a service with no version should fail
    When I PUT "services/update_missing_version.json" to "/rest/v1/services/${serviceId}"
    Then I should receive a RestResponse with an error code 501

  @reset
  Scenario: Updating a service with an invalid version should fail
    When I PUT "services/update_invalid_version.json" to "/rest/v1/services/${serviceId}"
    Then I should receive a RestResponse with an error code 605

  @reset
  Scenario: Updating a service with no node instance should fail
    When I PUT "services/update_missing_nodeinstance.json" to "/rest/v1/services/${serviceId}"
    Then I should receive a RestResponse with an error code 501

  @reset
  Scenario: Updating a service with a request with no node type should fail
    When I PUT "services/update_missing_node_type.json" to "/rest/v1/services/${serviceId}"
    Then I should receive a RestResponse with an error code 501

  @reset
  Scenario: Updating a service with a request with no node type version should fail
    When I PUT "services/update_missing_node_type_version.json" to "/rest/v1/services/${serviceId}"
    Then I should receive a RestResponse with an error code 501

  @reset
  Scenario: Updating a service when not admin should fail
    Given There are these users in the system
      | sauron |
    And I add a role "APPLICATIONS_MANAGER" to user "sauron"
    And I am authenticated with user named "sauron"
    When I PUT "services/update_valid.json" to "/rest/v1/services/${serviceId}"
    Then I should receive a RestResponse with an error code 102
