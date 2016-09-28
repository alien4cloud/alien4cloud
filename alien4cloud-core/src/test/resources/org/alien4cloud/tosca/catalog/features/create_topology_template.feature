Feature: Tosca Catalog: Create topology template

  Background:
    Given I am authenticated with "ADMIN" role
    And I cleanup archives

  Scenario: Creating a topology template should succeed
    When I create an empty topology template "topologyTemplate"
    Then No exception should be thrown
    And The SPEL expression "nodeTemplates" should return "null"
    And The SPEL expression "workspace" should return "ALIEN_GLOBAL_WORKSPACE"
    When I get the archive with name "topologyTemplate" and version "0.1.0-SNAPSHOT"
    Then The csar SPEL expression "name" should return "topologyTemplate"
    Then The csar SPEL expression "workspace" should return "ALIEN_GLOBAL_WORKSPACE"

    ## twoo version of same template
    When I create an empty topology template "topologyTemplate" version "0.2.0-SNAPSHOT"
    Then No exception should be thrown
    And The SPEL expression "nodeTemplates" should return "null"
    And The SPEL expression "workspace" should return "ALIEN_GLOBAL_WORKSPACE"
    When I get the archive with name "topologyTemplate" and version "0.1.0-SNAPSHOT"
    Then The csar SPEL expression "name" should return "topologyTemplate"
    Then The csar SPEL expression "workspace" should return "ALIEN_GLOBAL_WORKSPACE"
    When I get the archive with name "topologyTemplate" and version "0.2.0-SNAPSHOT"
    Then The csar SPEL expression "name" should return "topologyTemplate"
    Then The csar SPEL expression "workspace" should return "ALIEN_GLOBAL_WORKSPACE"

  Scenario: Creating a topology with an existing name and version should fail
    When I create an empty topology template "topologyTemplate"
    And I create an empty topology template "topologyTemplate"
    Then an exception of type "alien4cloud.exception.AlreadyExistException" should be thrown


