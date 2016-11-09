Feature: Update / upsert  a component tag

  Background:
    Given I am authenticated with "COMPONENTS_MANAGER" role
    And I have a component with and id "1" and an archive version "3.0" with tags:
  	| tagKey 			| tagValue 						  |
    	| icon			| 87878877-KKDKEK77 			  |
    	| maturity			| First beta 					  |
    	| release_comment	| 1st feb in UAT by jhon Doe Team |

  @reset
  Scenario: Update a non existing tag (insert)
    Given I have a component with id "1:3.0"
    When I update a tag with key "newtag" and value "tagValue"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Update an existing tag
    Given I have a component with id "1:3.0"
    And I have a tag "maturity"
    When I update a tag with key "maturity" and value "Beta test 2"
    Then I should receive a RestResponse with no error
      And I should have tag "maturity" with value "Beta test 2"

  @reset
  Scenario: Update an internal tag 'icon'
    Given I have a component with id "1:3.0"
    And I have a tag "icon"
    When I update a tag with key "icon" and value "/new/icon.png"
    Then I should receive a RestResponse with an error code 500

  @reset
  Scenario: Update a tag for a non existing component
    Given I have a fake component with a bad id "1:3.X"
    When I update a tag with key "icon" and value "/new/icon.png"
    Then I should receive a RestResponse with an error code 700
