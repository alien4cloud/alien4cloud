Feature: Create cloud

  Background:
    Given I am authenticated with "ADMIN" role

  Scenario: Create a cloud image
    When I create a cloud image with name "Windows 7", architecture "x86_64", type "windows", distribution "Windows" and version "7.0"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain an id string
    And The recently created cloud image should be in available in Alien with name "Windows 7", architecture "x86_64", type "windows", distribution "Windows" and version "7.0"

  Scenario: Create a cloud image with constraint
    When I create a cloud image with name "Windows 7", architecture "x86_64", type "windows", distribution "Windows", version "7.0", min CPUs 2, min memory 2000, min disk 32000
    Then I should receive a RestResponse with no error
    And The RestResponse should contain an id string
    And The recently created cloud image should be in available in Alien with name "Windows 7", architecture "x86_64", type "windows", distribution "Windows", version "7.0", min CPUs 2, min memory 2000, min disk 32000

  Scenario: Create / Update a cloud image with the same name should raise a conflict
    When I create a cloud image with name "Windows 7", architecture "x86_64", type "windows", distribution "Windows" and version "7.0"
    And I create a cloud image with name "Windows 7", architecture "x86_64", type "windows", distribution "Windows" and version "7.1"
    Then I should receive a RestResponse with an error code 502
    When I create a cloud image with name "Windows 8", architecture "x86_64", type "windows", distribution "Windows" and version "8.0"
    And I update the "name" of the recently created cloud image to "Windows 7"
    Then I should receive a RestResponse with an error code 502

  Scenario: Update a cloud image
    When I create a cloud image with name "Windows 7", architecture "x86_64", type "windows", distribution "Windows", version "7.0", min CPUs 2, min memory 2000, min disk 32000
    And I update the "name" of the recently created cloud image to "Windows 7.1"
    Then I should receive a RestResponse with no error
    Then The recently created cloud image should be in available in Alien with name "Windows 7.1", architecture "x86_64", type "windows", distribution "Windows", version "7.0", min CPUs 2, min memory 2000, min disk 32000

  Scenario: Delete a cloud image
    When I create a cloud image with name "Windows 7", architecture "x86_64", type "windows", distribution "Windows", version "7.0", min CPUs 2, min memory 2000, min disk 32000
    And I delete the recently created cloud image
    Then I should receive a RestResponse with no error
    Then The recently created cloud image must not exist any more in Alien

  Scenario: Search for cloud images
    Given I have already created a cloud image with name "Windows 7", architecture "x86_64", type "windows", distribution "Windows" and version "7.0"
    And I have already created a cloud image with name "Ubuntu Trusty", architecture "x86_64", type "linux", distribution "Ubuntu" and version "14.04.1"
    When I search for cloud images without excluding any image
    Then I should receive a 2 cloud images in the search result:
      | Windows 7     | x86_64 | windows | Windows | 7.0     |
      | Ubuntu Trusty | x86_64 | linux   | Ubuntu  | 14.04.1 |
    When I search for cloud images excluding "Windows 7"
    Then I should receive a 1 cloud images in the search result:
      | Ubuntu Trusty | x86_64 | linux | Ubuntu | 14.04.1 |