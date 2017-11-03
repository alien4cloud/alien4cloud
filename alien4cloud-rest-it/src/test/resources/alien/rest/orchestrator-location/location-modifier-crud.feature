Feature: Location management

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"

  @reset
  Scenario: Create a location modifier
    When I create a location modifier with plugin id "alien4cloud-mock-paas-provider" and bean name "mock-anti-affinity-modifier" and phase "pre-policy-match" to the location "Thark location" of the orchestrator "Mount doom orchestrator"
      Then I should receive a RestResponse with no error
    When I list location modifiers of the location "Thark location" of the orchestrator "Mount doom orchestrator"
      Then I should receive a RestResponse with no error
      And Response should contains 1 location modifier
      And Response should contains a location modifier with plugin id "alien4cloud-mock-paas-provider" and bean name "mock-anti-affinity-modifier" and phase "pre-policy-match"

  @reset
  Scenario: Create a location modifier with an invalid phase should failed
    When I create a location modifier with plugin id "alien4cloud-mock-paas-provider" and bean name "mock-anti-affinity-modifier" and phase "pre-location-match" to the location "Thark location" of the orchestrator "Mount doom orchestrator"
      Then I should receive a RestResponse with an error code 501
    When I create a location modifier with plugin id "alien4cloud-mock-paas-provider" and bean name "mock-anti-affinity-modifier" and phase "shouldFailed" to the location "Thark location" of the orchestrator "Mount doom orchestrator"
      Then I should receive a RestResponse with an error code 501

  @reset
  Scenario: Create a location modifier with an invalid plugin bean should failed
    When I create a location modifier with plugin id "shouldFailed" and bean name "mock-anti-affinity-modifier" and phase "pre-location-match" to the location "Thark location" of the orchestrator "Mount doom orchestrator"
    Then I should receive a RestResponse with an error code 500
    When I create a location modifier with plugin id "alien4cloud-mock-paas-provider" and bean name "shouldFailed" and phase "shouldFailed" to the location "Thark location" of the orchestrator "Mount doom orchestrator"
    Then I should receive a RestResponse with an error code 500

  @reset
  Scenario: Delete a location modifier
    When I create a location modifier with plugin id "alien4cloud-mock-paas-provider" and bean name "mock-anti-affinity-modifier" and phase "pre-policy-match" to the location "Thark location" of the orchestrator "Mount doom orchestrator"
      Then I should receive a RestResponse with no error
    When I delete a location modifier at index 0 on the location "Thark location" to the orchestrator "Mount doom orchestrator"
      Then I should receive a RestResponse with no error
    When I delete a location modifier at index 5 on the location "Thark location" to the orchestrator "Mount doom orchestrator"
      Then I should receive a RestResponse with an error code 504

  @reset
  Scenario: Delete a location modifier that doesnt exist should failed
    When I delete a location modifier at index 5 on the location "Thark location" to the orchestrator "Mount doom orchestrator"
    Then I should receive a RestResponse with an error code 504

# todo: add an other modifier to the mock to test this scenario
#  @reset
#  Scenario: Move a location modifier
#    When I create a location modifier with plugin id "alien4cloud-mock-paas-provider" and bean name "mock-anti-affinity-modifier" and phase "pre-policy-match" to the location "Thark location" of the orchestrator "Mount doom orchestrator"
#    And I create a location modifier with plugin id "alien4cloud-mock-paas-provider" and bean name "mock-anti-affinity-modifier" and phase "pre-policy-match" to the location "Thark location" of the orchestrator "Mount doom orchestrator"
#    And I create a location modifier with plugin id "alien4cloud-mock-paas-provider" and bean name "mock-anti-affinity-modifier" and phase "pre-policy-match" to the location "Thark location" of the orchestrator "Mount doom orchestrator"
#    And I list location modifiers of the location "Thark location" of the orchestrator "Mount doom orchestrator"
#      Then I should receive a RestResponse with no error
#      And Response should contains 3 location modifier
#      And the location at index 0 should have the plugin id "alien4cloud-mock-paas-provider_1"
#      And the location at index 1 should have the plugin id "alien4cloud-mock-paas-provider_2"
#      And the location at index 2 should have the plugin id "alien4cloud-mock-paas-provider_3"
#    When I move a location modifier from index 0 to index 2 for the location "Thark location" of the orchestrator "Mount doom orchestrator"
#      And I list location modifiers of the location "Thark location" of the orchestrator "Mount doom orchestrator"
#      Then I should receive a RestResponse with no error
#      And Response should contains 3 location modifier
#      And the location at index 0 should have the plugin id "alien4cloud-mock-paas-provider_2"
#      And the location at index 1 should have the plugin id "alien4cloud-mock-paas-provider_3"
#      And the location at index 2 should have the plugin id "alien4cloud-mock-paas-provider_1"

  @reset
  Scenario: Move a location modifier with wrong index should failed
    When I create a location modifier with plugin id "alien4cloud-mock-paas-provider" and bean name "mock-anti-affinity-modifier" and phase "pre-policy-match" to the location "Thark location" of the orchestrator "Mount doom orchestrator"
    And I create a location modifier with plugin id "alien4cloud-mock-paas-provider" and bean name "mock-anti-affinity-modifier" and phase "pre-policy-match" to the location "Thark location" of the orchestrator "Mount doom orchestrator"
    When I move a location modifier from index 0 to index 5 for the location "Thark location" of the orchestrator "Mount doom orchestrator"
      Then I should receive a RestResponse with an error code 504
    When I move a location modifier from index 500 to index 1 for the location "Thark location" of the orchestrator "Mount doom orchestrator"
      Then I should receive a RestResponse with an error code 504

  @reset
  Scenario: Location modifier should be order by phase
    When I create a location modifier with plugin id "alien4cloud-mock-paas-provider" and bean name "mock-anti-affinity-modifier" and phase "pre-policy-match" to the location "Thark location" of the orchestrator "Mount doom orchestrator"
    And I create a location modifier with plugin id "alien4cloud-mock-paas-provider" and bean name "mock-anti-affinity-modifier" and phase "pre-policy-match" to the location "Thark location" of the orchestrator "Mount doom orchestrator"
    And I create a location modifier with plugin id "alien4cloud-mock-paas-provider" and bean name "mock-anti-affinity-modifier" and phase "post-node-match" to the location "Thark location" of the orchestrator "Mount doom orchestrator"
    And I create a location modifier with plugin id "alien4cloud-mock-paas-provider" and bean name "mock-anti-affinity-modifier" and phase "pre-policy-match" to the location "Thark location" of the orchestrator "Mount doom orchestrator"
    And I create a location modifier with plugin id "alien4cloud-mock-paas-provider" and bean name "mock-anti-affinity-modifier" and phase "post-location-match" to the location "Thark location" of the orchestrator "Mount doom orchestrator"
    And I list location modifiers of the location "Thark location" of the orchestrator "Mount doom orchestrator"
      Then I should receive a RestResponse with no error
      And Response should contains 5 location modifier
      And the location at index 0 should have the phase "post-location-match"
      And the location at index 4 should have the phase "post-node-match"
