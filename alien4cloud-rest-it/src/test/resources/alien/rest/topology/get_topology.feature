Feature: Get topology

Scenario: Get a topology
  Given I am authenticated with "APPLICATIONS_MANAGER" role
  And I create a new application with name "watchmiddleearth" and description "Use my great eye to find frodo and the ring."
  When I try to retrieve it
  Then I should receive a RestResponse with no error
    And The RestResponse should contain a topology
