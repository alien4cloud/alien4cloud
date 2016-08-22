Feature: Creating a new application based on a topology template

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca base types 1.0"
    And I should receive a RestResponse with no error
    And I upload the archive "sample java types 1.0"
    And I should receive a RestResponse with no error
    And There is a "node type" with element name "tosca.nodes.Compute" and archive version "1.0"
    And There is a "node type" with element name "fastconnect.nodes.Java" and archive version "1.0"
    And There is a "mairon" user in the system
    And I add a role "ARCHITECT" to user "mairon"
    And There is a "sauron" user in the system
    And I add a role "APPLICATIONS_MANAGER" to user "sauron"

  @reset
  Scenario: Create a topology template with JAVA and COMPUTE node template
    Given I am authenticated with user named "mairon"
    And I create a new topology template with name "topology_template_java" and description "My topology template description1" and node templates
      | NodeTemplateCompute | tosca.nodes.Compute:1.0    |
      | NodeTemplateJava    | fastconnect.nodes.Java:1.0 |
    Then I should receive a RestResponse with no error
    Given I am authenticated with user named "sauron"
    And I create a new application with name "watchmiddleearth" and description "Use my great eye to find frodo and the ring." based on this created template
    And The created application topology is the same as the one in the base topology template
