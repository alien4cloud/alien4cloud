Feature: CSAR delete

  Background:
    Given I am authenticated with "COMPONENTS_MANAGER" role
    And I upload the archive "tosca base types 1.0"
    And I should receive a RestResponse with no error
    Given I upload the archive "valid-csar-with-test"
    And I should receive a RestResponse with no error

  @reset
  Scenario: Try do delete a CSAR that is a target dependency
    When I delete a CSAR with id "tosca-base-types:1.0"
    Then I should receive a RestResponse with an error code 507
    And I should have a delete csar response with "1" related resources

  @reset
  Scenario: Try do delete a CSAR that is used in a topology
  	Given I am authenticated with "APPLICATIONS_MANAGER" role
    And I create a new application with name "watchmiddleearth" and description "Use my great eye to find frodo and the ring." and node templates
      | Compute | tosca.nodes.Compute:1.0 |
      | Java | fastconnect.nodes.Java:2.0-SNAPSHOT |
    And I execute the operation
      | type                   | org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation |
      | nodeName               | Java                                                                                  |
      | relationshipName       | hostedOnCompute                                                                        |
      | relationshipType       | tosca.relationships.HostedOn                                                          |
      | relationshipVersion    | 1.0                                                                                   |
      | requirementName        | host                                                                                  |
      | target                 | Compute                                                                               |
      | targetedCapabilityName | host                                                                                  |
    And I save the topology
    When I delete a CSAR with id "topology-test:2.0-SNAPSHOT"
    Then I should receive a RestResponse with an error code 507
    And I should have a delete csar response with "1" related resources
    When I delete a CSAR with id "tosca-base-types:1.0"
    Then I should receive a RestResponse with an error code 507
    And I should have a delete csar response with "2" related resources

  @reset
  Scenario: Try do delete a CSAR that is a location dependency
    Given I am authenticated with "ADMIN" role
      And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
      And I upload a plugin
      And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"
      And I enable the orchestrator "Mount doom orchestrator"
      And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    When I delete a CSAR with id "mock-plugin-openstack-types:1.0"
      Then I should receive a RestResponse with an error code 507
      And I should have a delete csar response with "1" related resources

  @reset
  Scenario: Update a snapshot CSAR with less nodes
    Given I am authenticated with "COMPONENTS_MANAGER" role
    Given I upload the archive "valid-csar-with-update1"
    And I should receive a RestResponse with no error
    # in the update2 (3.0-SNAPSHOT), the War is removed
    Given I upload the archive "valid-csar-with-update2"
    And I should receive a RestResponse with no error
    When I search for "node types" using query "war" from 0 with result size of 1000
    Then I should receive a RestResponse with no error
    And The response should contains 1 elements from various types of version "2.0-SNAPSHOT"

  @reset
  Scenario: Delete a CSAR version and ensure that the highest version is well managed
    Given I am authenticated with "COMPONENTS_MANAGER" role
    Given I upload the archive "valid-csar-with-update1"
    And I should receive a RestResponse with no error
    Given I upload the archive "valid-csar-with-update3"
    And I should receive a RestResponse with no error
    When I search for "node types" using query "war" from 0 with result size of 1000
    Then I should receive a RestResponse with no error
    And The response should contains 1 elements from various types of version "4.0-SNAPSHOT" and older versions are
      | 2.0-SNAPSHOT |
      | 3.0-SNAPSHOT |
    When I delete a CSAR with id "topology-test:4.0-SNAPSHOT"
    Then I should receive a RestResponse with no error
    When I search for "node types" using query "war" from 0 with result size of 1000
    Then I should receive a RestResponse with no error
    And The response should contains 1 elements from various types of version "3.0-SNAPSHOT" and older versions are
      | 2.0-SNAPSHOT |

  @reset
  Scenario: Delete a CSAR version and ensure that the older versions are well managed
    Given I am authenticated with "COMPONENTS_MANAGER" role
    Given I upload the archive "valid-csar-with-update1"
    And I should receive a RestResponse with no error
    Given I upload the archive "valid-csar-with-update3"
    And I should receive a RestResponse with no error
    When I search for "node types" using query "war" from 0 with result size of 1000
    Then I should receive a RestResponse with no error
    And The response should contains 1 elements from various types of version "4.0-SNAPSHOT" and older versions are
      | 2.0-SNAPSHOT |
      | 3.0-SNAPSHOT |
    When I delete a CSAR with id "topology-test:3.0-SNAPSHOT"
    Then I should receive a RestResponse with no error
    When I search for "node types" using query "war" from 0 with result size of 1000
    Then I should receive a RestResponse with no error
    And The response should contains 1 elements from various types of version "4.0-SNAPSHOT" and older versions are
      | 2.0-SNAPSHOT |
