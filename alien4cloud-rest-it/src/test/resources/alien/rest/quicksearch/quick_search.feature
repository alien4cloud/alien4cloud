Feature: Quick Search

  Background:
    Given I am authenticated with "ADMIN" role
    And There is 10 "node types" indexed in ALIEN
    And There is 10 "relationship types" indexed in ALIEN
    And I create a new application with name "newApplication" and description "My brand new application."

  @reset
  Scenario: quick search should return the expected number of result
    Given There is 15 "node types" indexed in ALIEN with 15 of them having "appli-node-type" in the "elementId"
    When I quickly search for "appli" from 0 with result size of 10
    Then I should receive a RestResponse with no error
      And The quickSearch response should contains 11 elements

  @reset
  Scenario: quick search should be able to return the both application and node type
    Given There is 15 "node types" indexed in ALIEN with 6 of them having "newApplication-node-type" in the "elementId"
      And I create a new application with name "newApplication-second" and description "My second brand new application."
    When I quickly search for "newAppli" from 0 with result size of 10
    Then I should receive a RestResponse with no error
      And The quickSearch response should contains 6 "node types"
      And The quickSearch response should contains 2 "applications"

  @reset
  Scenario: quick search should be able to return only what is authorized for a component browser
    Given I am authenticated with "ADMIN" role
      And There is 15 "node types" indexed in ALIEN with 6 of them having "newApplication-node-type" in the "elementId"
      And I create a new application with name "newApplication-second" and description "My second brand new application."
    When I authenticate with "COMPONENTS_BROWSER" role
      And I quickly search for "newAppli" from 0 with result size of 10
    Then I should receive a RestResponse with no error
      And The quickSearch response should contains 6 "node types"

  @reset
  Scenario: quick search should be able to return only what user has access (any application roles)
    Given I am authenticated with "APPLICATIONS_MANAGER" role
      And I create a new application with name "newApplication-second" and description "My second brand new application."
    When I authenticate with "ADMIN" role
      And I quickly search for "newAppli" from 0 with result size of 10
    Then I should receive a RestResponse with no error
      And The quickSearch response should contains 2 "applications"
    When I authenticate with "APPLICATIONS_MANAGER" role
      And I quickly search for "newAppli" from 0 with result size of 10
    Then I should receive a RestResponse with no error
      And The quickSearch response should contains 1 "applications"

  @reset
  Scenario: quick search should be able to find applications or components regardless the query word
    Given I am authenticated with "APPLICATIONS_MANAGER" role
      And I create a new application with name "my.application.NewNode" and description "My application with node keyword type."
    When I authenticate with "ADMIN" role
      And I quickly search for "application" from 0 with result size of 10
    Then I should receive a RestResponse with no error
      And The quickSearch response should contains 2 "applications"
    When I quickly search for "node" from 0 with result size of 10
    	Then I should receive a RestResponse with no error
    	And The quickSearch response should contains 1 "applications"
    	# 'node' is found in the application name
    	And The quickSearch response should contains 10 "node types"
    When I authenticate with "APPLICATIONS_MANAGER" role
      And I quickly search for "newnode" from 0 with result size of 10
    Then I should receive a RestResponse with no error
      And The quickSearch response should contains 1 "applications"
