Feature: Update an application (image or tags)

  Background:
    Given I am authenticated with "APPLICATIONS_MANAGER" role
    Given There is a "new_application_name_with_tags" application with tags:
      | description     | First beta                      |
      | release_comment | 1st feb in UAT by jhon Doe Team |
      | my_tag          | this is my tag...               |

  @reset
  Scenario: Update image of an existing application
    Given There is a "watchmiddleearth" application
    When i update its image
    Then I should receive a RestResponse with no error
    And the application can be found in ALIEN with its new image

  @reset
  Scenario: Add a new tag
    Given There is a "new_application_name_with_tags" application
    When I add a tag with key "newtag" and value "tag value" to the application
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Delete a tag that exists
    Given There is a "new_application_name_with_tags" application
    And I have an application tag "my_tag"
    When I delete an application tag with key "my_tag"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Delete a tag that do not exists
    Given There is a "new_application_name_with_tags" application
    When I delete an application tag with key "bad_tag"
    Then I should receive a RestResponse with no error

  @reset
  Scenario: Rename an application
    Given There is a "watchmiddleearth" application
    When I set the "name" of this application to "MyNewAppName"
    Then I should receive a RestResponse with no error
    And The application can be found in ALIEN with its "name" set to "MyNewAppName"
    When I search for "MyNewAppName" application
    Then The application update date has changed
    When I set the "description" of this application to "Great app which will succeed"
    Then I should receive a RestResponse with no error
    And The application can be found in ALIEN with its "description" set to "Great app which will succeed"

  @reset
  Scenario: Rename an application with an invalid application name should fail
    Given There is a "watchmiddleearth" application
    When I set the "name" of this application to "new\\\\\application"
    Then I should receive a RestResponse with an error code 619
    And The application can be found in ALIEN with its "name" set to "watchmiddleearth"

  @reset
  Scenario: Rename an application with an existing application name should fail
    Given There is a "watchmiddleearth" application
    When I set the "name" of this application to "new_application_name_with_tags"
    Then I should receive a RestResponse with an error code 502
    And The application can be found in ALIEN with its "name" set to "watchmiddleearth"
