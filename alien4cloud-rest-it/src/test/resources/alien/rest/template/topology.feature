Feature: Create topology template

  Background:
    Given I am authenticated with "ARCHITECT" role

  @reset
  Scenario: Create an empty topology template with no error
    When I create a new topology template with name "topology_template_name1" and description "My topology template description1"
    Then I should receive a RestResponse with no error
    And I should receive a RestResponse with a non empty string data

  @reset
  Scenario: Create an empty topology as APPLICATION_MANAGER should raise a permission error
    Given I am authenticated with "APPLICATIONS_MANAGER" role
    When I create a new topology template with name "topology_templater_name2" and description "My topology template description2"
    Then I should receive a RestResponse with an error code 102

  @reset
  Scenario: Create an empty topology template with an invalid name should fail
    When I create a new topology template with name "topology_template///name1" and description "My topology template description1"
    Then I should receive a RestResponse with an error code 500

  @reset
  Scenario: Delete an existing topology template
    Given I have created a new topology template with name "topology_template_name1" and description "My topology template description1"
    When I delete a CSAR with id "topology_template_name1:0.1.0-SNAPSHOT"
    Then I should receive a RestResponse with no error
    And The registered topology should not exist
