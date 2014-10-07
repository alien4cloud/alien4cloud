Feature: tag suggestion

Background:
  Given I am authenticated with "ADMIN" role
    And I have a component with and id "1" and an archive version "3.0" with tags:
      | tagKey          | tagValue              |
      | icon       | 87878877-KKDKEK77     |
      | maturity        | First beta            |
      | release_comment | 1st feb in UAT by jhon Doe Team |
      | version         | 1.3                   |
    And I have a component with and id "2" and an archive version "3.0" with tags:
      | tagKey          | tagValue  |
      | recommended     | false     |
      | mature          | true      |
      | maturation      | hahaha    |
    And I have a component with and id "3" and an archive version "3.0" with tags:
      | tagKey              | tagValue     |
      | recommended_with    | First beta   |
    And There is a "new_application_name_with_tags" application with tags:
    | description       | First beta            |
    | release_comment   | 1st feb in UAT by jhon Doe Team |
    | my_tag            | this is my tag...         |
    | version_two       | v2.5        |


Scenario: suggestion request should return the expected result
  When I ask suggestions for tag "name" with "matur"
  Then I should receive a RestResponse with no error
    And The suggestion response should contains 3 elements

Scenario: suggestion request should be able to search in both application and toscaElements tags
  When I ask suggestions for tag "name" with "vers"
  Then I should receive a RestResponse with no error
    And The suggestion response should contains 2 elements
    And The suggestion response should contains "version_two"
