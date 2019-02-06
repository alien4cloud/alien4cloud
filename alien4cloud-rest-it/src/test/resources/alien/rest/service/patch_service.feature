Feature: Patch service resource

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    When I create a service with name "MyBdService", version "1.0.0", type "tosca.nodes.Database", archive version "1.0.0-SNAPSHOT"
    And I register path "data.id" with class "alien4cloud.model.service.ServiceResource" as "serviceId"

  @reset
  Scenario: Patching a service name should succeed
    When I PATCH "services/patch_name.json" to "/rest/v1/services/${serviceId}"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Patching a service name with empty value should fail
    When I PATCH "services/patch_name_empty.json" to "/rest/v1/services/${serviceId}"
    Then I should receive a RestResponse with an error code 501

  @reset
  Scenario: Patching a service name with null value should fail
    When I PATCH "services/patch_name_null.json" to "/rest/v1/services/${serviceId}"
    Then I should receive a RestResponse with an error code 501

  @reset
  Scenario: Patching a service name with existing value should fail
    When I create a service with name "Patched name", version "1.0.0", type "tosca.nodes.Database", archive version "1.0.0-SNAPSHOT"
    When I PATCH "services/patch_name.json" to "/rest/v1/services/${serviceId}"
    Then I should receive a RestResponse with an error code 502

  @reset
  Scenario: Patching a service version should succeed
    When I PATCH "services/patch_version.json" to "/rest/v1/services/${serviceId}"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Patching a service version with empty value should fail
    When I PATCH "services/patch_version_empty.json" to "/rest/v1/services/${serviceId}"
    Then I should receive a RestResponse with an error code 501

  @reset
  Scenario: Patching a service version with null value should fail
    When I PATCH "services/patch_version_null.json" to "/rest/v1/services/${serviceId}"
    Then I should receive a RestResponse with an error code 501

  @reset
  Scenario: Patching a service state to started should succeed
    And I PATCH "services/patch_service_properties.json" to "/rest/v1/services/${serviceId}"
    When I PATCH "services/patch_service_attr_state_started.json" to "/rest/v1/services/${serviceId}"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Patching a service state to started with missing required properties should fail
    When I PATCH "services/patch_service_attr_state_started.json" to "/rest/v1/services/${serviceId}"
    Then I should receive a RestResponse with an error code 500

  @reset
  Scenario: Patching a service state to initial when started should succeed
    And I PATCH "services/patch_service_properties.json" to "/rest/v1/services/${serviceId}"
    When I PATCH "services/patch_service_attr_state_started.json" to "/rest/v1/services/${serviceId}"
    When I PATCH "services/patch_service_attr_state_initial.json" to "/rest/v1/services/${serviceId}"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Patching a service name for started service should fail
    And I PATCH "services/patch_service_properties.json" to "/rest/v1/services/${serviceId}"
    When I PATCH "services/patch_service_attr_state_started.json" to "/rest/v1/services/${serviceId}"
    When I PATCH "services/patch_name.json" to "/rest/v1/services/${serviceId}"
    Then I should receive a RestResponse with an error code 509

  @reset
  Scenario: Patching a service version for started service should fail
    And I PATCH "services/patch_service_properties.json" to "/rest/v1/services/${serviceId}"
    When I PATCH "services/patch_service_attr_state_started.json" to "/rest/v1/services/${serviceId}"
    When I PATCH "services/patch_version.json" to "/rest/v1/services/${serviceId}"
    Then I should receive a RestResponse with an error code 509

  @reset
  Scenario: Patching a service when not admin should fail
    Given There are these users in the system
      | sauron |
    And I add a role "APPLICATIONS_MANAGER" to user "sauron"
    And I am authenticated with user named "sauron"
    When I PUT "services/update_valid.json" to "/rest/v1/services/${serviceId}"
    Then I should receive a RestResponse with an error code 102
