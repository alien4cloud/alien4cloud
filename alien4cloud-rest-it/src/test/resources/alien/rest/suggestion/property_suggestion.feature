Feature: suggestion on a value backed by a property definition

  Background:
    Given I am authenticated with "ADMIN" role
    And I initialize default suggestions entry
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"

  Scenario: Get all suggestions
    When I get all suggestions for property "architecture" of "capability" "tosca.capabilities.OperatingSystem"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain 2 element(s):
      | x86_32 |
      | x86_64 |
    When I get all suggestions for property "type" of "capability" "tosca.capabilities.OperatingSystem"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain 4 element(s):
      | linux   |
      | aix     |
      | mac     |
      | windows |
    When I get all suggestions for property "distribution" of "capability" "tosca.capabilities.OperatingSystem"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain 11 element(s):
      | linux               |
      | debian              |
      | fedora              |
      | rhel                |
      | ubuntu              |
      | centos              |
      | gentoo              |
      | windows server 2003 |
      | windows server 2008 |
      | windows server 2012 |
      | windows server 2016 |

  Scenario: Get suggestions which match
    When I get suggestions for text "x86_32" for property "architecture" of "capability" "tosca.capabilities.OperatingSystem"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain 2 element(s) in this order:
      | x86_32 |
      | x86_64 |
    When I get suggestions for text "x86_64" for property "architecture" of "capability" "tosca.capabilities.OperatingSystem"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain 2 element(s) in this order:
      | x86_64 |
      | x86_32 |
    When I get suggestions for text "Kubuntu" for property "distribution" of "capability" "tosca.capabilities.OperatingSystem"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain 2 element(s) in this order:
      | ubuntu |
      | gentoo |
    When I get suggestions for text "w" for property "type" of "capability" "tosca.capabilities.OperatingSystem"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain 1 element(s) in this order:
      | windows |

  Scenario: Add suggestion
    When I add suggestion "kubuntu" for property "distribution" of "capability" "tosca.capabilities.OperatingSystem"
    Then I should receive a RestResponse with no error
    When I get suggestions for text "Kubuntu" for property "distribution" of "capability" "tosca.capabilities.OperatingSystem"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain 2 element(s) in this order:
      | kubuntu |
      | ubuntu  |
