Feature: Delete a component tag

  Background:
    Given I am authenticated with "COMPONENTS_MANAGER" role
    And I have a component with and id "1" and an archive version "3.0" with tags:
  	| tagKey 			| tagValue 						  |
    	| icon			| 87878877-KKDKEK77 			  |
    	| maturity			| First beta 					  |
    	| release_comment	| 1st feb in UAT by jhon Doe Team |

  @reset
  Scenario: Delete a tag that exists
    Given I have a component with id "1:3.0"
    And I have a tag "maturity"
    When I delete a tag with key "maturity"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Delete a internal tag
    Given I have a component with id "1:3.0"
    And I have a tag "icon"
    When I delete a tag with key "icon"
    Then I should receive a RestResponse with an error code 701

  @reset
  Scenario: Delete a tag for a non existing component
    Given I have a fake component with a bad id "1:3.X"
    When I delete a tag with key "maturity"
    Then I should receive a RestResponse with an error code 700
