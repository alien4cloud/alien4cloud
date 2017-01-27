Feature: Get topology

  @reset
  Scenario: Get a topology
    Given I am authenticated with "APPLICATIONS_MANAGER" role
    And I create an application with name "watchmiddleearth", archive name "watchmiddleearth", description "Use my great eye to find frodo and the ring." and topology template id "null"
    When I try to retrieve it
    Then I should receive a RestResponse with no error
      And The RestResponse should contain a topology

  @reset
  Scenario: Get a topology by a non-existent id should failed
    Given I am authenticated with "APPLICATIONS_MANAGER" role
    And I create an application with name "watchmiddleearth", archive name "watchmiddleearth", description "Use my great eye to find frodo and the ring." and topology template id "null"
    When I get a topology by id "fake-id"
      Then I should receive a RestResponse with an error code 504
