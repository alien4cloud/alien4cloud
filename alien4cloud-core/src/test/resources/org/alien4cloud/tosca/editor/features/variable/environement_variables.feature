Feature: Topology editor: Update variables expressions

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology

  Scenario: Updating a variable that doesn't exists yet should succeed
    When I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.variable.UpdateEnvironmentVariableOperation |
      | environmentId | env1                                                                                |
      | name          | toto                                                                                |
      | expression    | toto                                                                                |
    Then No exception should be thrown
    And The dto SPEL expression "archiveContentTree.children[0].children.?[name=='inputs'].size()" should return 1
    And The dto SPEL expression "archiveContentTree.children[0].children.?[name=='inputs'][0].children.?[name=='var_env_env1.yml'].size()" should return 1
    And The dto SPEL expression "archiveContentTree.children[0].children[3].leaf" should return true
    When I load variables for "ENV" / "env1"
    Then The SPEL expression "#this['toto']" should return "toto"

  Scenario: Updating a variable that already exists should succeed
    Given I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.variable.UpdateEnvironmentVariableOperation |
      | environmentId | env1                                                                                |
      | name          | toto                                                                                |
      | expression    | toto                                                                                |
    And I load variables for "ENV" / "env1"
    And The SPEL expression "#this['toto']" should return "toto"
    When I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.variable.UpdateEnvironmentVariableOperation |
      | environmentId | env1                                                                                |
      | name          | toto                                                                                |
      | expression    | toto_updated                                                                        |
    Then No exception should be thrown
    When I load variables for "ENV" / "env1"
    Then The SPEL expression "#this['toto']" should return "toto_updated"

  Scenario: Updating a variable expression to null or empty string should delete the variable
    Given I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.variable.UpdateEnvironmentVariableOperation |
      | environmentId | env1                                                                                |
      | name          | toto                                                                                |
      | expression    | toto                                                                                |
    And I load variables for "ENV" / "env1"
    And The SPEL expression "#this['toto']" should return "toto"
    When I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.variable.UpdateEnvironmentVariableOperation |
      | environmentId | env1                                                                                |
      | name          | toto                                                                                |
    Then No exception should be thrown
    When I load variables for "ENV" / "env1"
    Then The SPEL expression "#this.size()" should return 0

  Scenario: Updating a variable expression after saving the topology should suceed
    Given I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.variable.UpdateEnvironmentVariableOperation |
      | environmentId | env1                                                                                |
      | name          | toto                                                                                |
      | expression    | toto                                                                                |
    And I load variables for "ENV" / "env1"
    And The SPEL expression "#this['toto']" should return "toto"
    And I save the topology
    When I load variables for "ENV" / "env1"
    Then The SPEL expression "#this['toto']" should return "toto"
    When I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.variable.UpdateEnvironmentVariableOperation |
      | environmentId | env1                                                                                |
      | name          | toto                                                                                |
      | expression    | toto_updated                                                                        |
    Then No exception should be thrown
    When I load variables for "ENV" / "env1"
    Then The SPEL expression "#this['toto']" should return "toto_updated"
    And I save the topology
    When I execute the operation
      | type          | org.alien4cloud.tosca.editor.operations.variable.UpdateEnvironmentVariableOperation |
      | environmentId | env1                                                                                |
      | name          | tata                                                                                |
      | expression    | tata                                                                                |
    Then No exception should be thrown
    When I load variables for "ENV" / "env1"
    Then The SPEL expression "#this.size()" should return 2
    Then The SPEL expression "#this['toto']" should return "toto_updated"
    Then The SPEL expression "#this['tata']" should return "tata"

