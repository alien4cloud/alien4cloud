Feature: Replace node templates

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload the local archive "data/csars/node_replacement/node_replacement.yaml"
    And I should receive a RestResponse with no error
    And I create an application with name "node_replacement", archive name "watchmiddleearth", description "Node replacement tests" and topology template id "node_replacement:0.1-SNAPSHOT"
    And I should receive a RestResponse with no error

  @reset
  Scenario: asking for possible replacements for an abstract node template
    Given I ask for replacements for the node "JVM_1"
    Then I should receive a RestResponse with no error
    And there should be the followings in replacements nodes types
      |alien.test.nodes.Tomcat|
      |alien.test.nodes.JBoss|
      |alien.test.nodes.OracleJVM|
    # actually all nodes that have a "hostedOn container" requirement matche

  @reset
  Scenario: replacing a node template only as a source
    Given I ask for replacements for the node "ApplicationServer_2"
    Then I should receive a RestResponse with no error
    And the possible replacements nodes types should be
      |alien.test.nodes.Tomcat|
      |alien.test.nodes.JBoss |

  @reset
  Scenario: replacing a node template only as a target
    Given I ask for replacements for the node "JVM_2"
    Then I should receive a RestResponse with no error
    And the possible replacements nodes types should be
      |alien.test.nodes.OracleJVM|

    # Only alien.test.nodes.Tomcat satisfy all requirements from the node Application_3
  @reset
  Scenario: replacing a node template as a source and target
    Given I ask for replacements for the node "ApplicationServer_3"
    Then I should receive a RestResponse with no error
    And the possible replacements nodes types should be
      |alien.test.nodes.Tomcat|
