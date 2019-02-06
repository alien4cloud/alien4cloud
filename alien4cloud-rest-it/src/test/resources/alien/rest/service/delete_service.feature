Feature: Delete service resource

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    When I create a service with name "MyBdService", version "1.0.0", type "tosca.nodes.Database", archive version "1.0.0-SNAPSHOT"
    And I register path "data.id" with class "alien4cloud.model.service.ServiceResource" as "serviceId"

  @reset
  Scenario: Deleting a service should succeed
    When I DELETE "/rest/v1/services/${serviceId}"
    Then I should receive a RestResponse with no error
    When I GET "/rest/v1/services/"
    And I register path "data" with class "alien4cloud.dao.model.GetMultipleDataResult" as "listServiceResponse"
    And I register "listServiceResponse" for SPEL
    Then I should receive a RestResponse with no error
    And The SPEL expression "totalResults" should return 0
    And The SPEL expression "from" should return 0
    And The SPEL expression "to" should return 0

  @reset
  Scenario: Deleting a service when not admin should fail
    Given There are these users in the system
      | sauron |
    And I add a role "APPLICATIONS_MANAGER" to user "sauron"
    And I am authenticated with user named "sauron"
    When I DELETE "/rest/v1/services/${serviceId}"
    Then I should receive a RestResponse with an error code 102
