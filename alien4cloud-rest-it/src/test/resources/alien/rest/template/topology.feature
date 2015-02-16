Feature: Create topology template

  Background:
    Given I am authenticated with "ARCHITECT" role

  Scenario: Create an empty topology template with no error
    When I create a new topology template with name "topology_template_name1" and description "My topology template description1"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain a topology template id
    And I can get this newly created topology template

  Scenario: Create an empty topology as APPLICATION_MANAGER should raise a permission error
    Given I am authenticated with "APPLICATIONS_MANAGER" role
    When I create a new topology template with name "topology_templater_name2" and description "My topology template description2"
    Then I should receive a RestResponse with an error code 102

  Scenario: Create an empty topology with an existing name should raise duplication error
    Given I have created a new topology template with name "topology_template_name1" and description "My topology template description1"
    When I create a new topology template with name "topology_template_name1" and description "My topology template description1"
    Then I should receive a RestResponse with an error code 502

  Scenario: Delete an existing topology template
    Given I have created a new topology template with name "topology_template_name1" and description "My topology template description1"
    When I delete the newly created topology template
    Then I should receive a RestResponse with no error
    And The related topology shouldn't exist anymore

  Scenario: Update a topology template
    Given I have created a new topology template with name "topology_template_name1" and description "My topology template description1"
    When I update the topology template "topology_template_name1" fields:
      | name        | value                             |
      | name        | topology_templater_renamed        |
      | description | great topology which will succeed |
    Then I should receive a RestResponse with no error
    And The topology template should have its "name" set to "topology_templater_renamed"
    And The topology template should have its "description" set to "great topology which will succeed"

  Scenario: Update a topology template with bad request: failure
    Given I have created a new topology template with name "topology_template_name2" and description "My topology template description2"
    And I have created a new topology template with name "topology_template_name1" and description "My topology template description1"
    When I update the topology template "topology_template_name1" fields:
      | name | value                   |
      | name | topology_template_name2 |
    Then I should receive a RestResponse with an error code 502
    And The topology template should have its "name" set to "topology_template_name1"
