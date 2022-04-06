Feature: Topology editor: Change version of a csar dependency

  Background:
    Given I am authenticated with "ADMIN" role
    # initialize or reset the types as defined in the initial archive
    And I upload unzipped CSAR from path "src/test/resources/data/csars/dependency_version/dependency1.yml"
    And I upload unzipped CSAR from path "src/test/resources/data/csars/dependency_version/dependency2.yml"
    And I upload unzipped CSAR from path "src/test/resources/data/csars/dependency_version/dependency3.yml"
    And I upload unzipped CSAR from path "src/test/resources/data/csars/dependency_version/dependency4.yml"
    And I upload unzipped CSAR from path "src/test/resources/data/csars/dependency_version/dependency-transitive.yml"
    And I upload unzipped CSAR from path "src/test/resources/data/csars/dependency_version/dependency5.yml"
    And I upload unzipped CSAR from path "src/test/resources/data/csars/dependency_version/dependency-transitive2.yml"
    And I upload unzipped CSAR from path "src/test/resources/data/csars/dependency_version/dependency6.yml"
    # initialize or reset the topology
    And I delete the archive "test-dependencies-change" "0.1.0-SNAPSHOT" if any
    And I upload unzipped CSAR from path "src/test/resources/data/csars/dependency_version/initial-topology.yml"
    And I get the topology related to the CSAR with name "test-dependencies-change" and version "0.1.0-SNAPSHOT"

  Scenario: The new archive removes a node type
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.ChangeDependencyVersionOperation |
      | dependencyName    | test-topo-dependencies-types                                             |
      | dependencyVersion | 0.2-SNAPSHOT                                                             |
    Then an exception of type "alien4cloud.exception.VersionConflictException" should be thrown

  Scenario: The new archive removes a requirement
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.ChangeDependencyVersionOperation |
      | dependencyName    | test-topo-dependencies-types                                             |
      | dependencyVersion | 0.3-SNAPSHOT                                                             |
    Then No exception should be thrown
    And The SPEL expression "dependencies.^[name == 'test-topo-dependencies-types'].version" should return "0.3-SNAPSHOT"
    And The SPEL expression "nodeTemplates.size()" should return 3
    And The SPEL expression "nodeTemplates['TestComponent'].name" should return "TestComponent"
    And The SPEL expression "nodeTemplates['TestComponentSource'].properties['component_version'].value" should return "777"
    And The SPEL expression "nodeTemplates['TestComponentSource'].relationships.size()" should return 1
    And The SPEL expression "nodeTemplates['TestComponentSource'].relationships.values()[0].target" should return "Compute"
    And The SPEL expression "nodeTemplates['TestComponentSource'].requirements['connect']" should return "null"
    And The dto SPEL expression "dependencyConflicts.size()" should return 0

  Scenario: The new archive removes a relationship type and replace it by a new one
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.ChangeDependencyVersionOperation |
      | dependencyName    | test-topo-dependencies-types                                             |
      | dependencyVersion | 0.4-SNAPSHOT                                                             |
    Then an exception of type "alien4cloud.exception.VersionConflictException" should be thrown

  Scenario: The archive has a new dependency
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.ChangeDependencyVersionOperation |
      | dependencyName    | test-topo-dependencies-types                                             |
      | dependencyVersion | 0.5-SNAPSHOT                                                             |
    Then No exception should be thrown
    And The SPEL expression "nodeTemplates.size()" should return 3
    And The SPEL expression "dependencies.^[name == 'test-topo-dependencies-types'].version" should return "0.5-SNAPSHOT"
    And The SPEL expression "dependencies.^[name == 'test-topo-dependencies-trans-types'].version" should return "0.1-SNAPSHOT"
    And The SPEL expression "nodeTemplates.size()" should return 3
    And The SPEL expression "nodeTemplates['TestComponent'].name" should return "TestComponent"
    And The SPEL expression "nodeTemplates['TestComponentSource'].properties['component_version'].value" should return "777"
    And The SPEL expression "nodeTemplates['TestComponentSource'].relationships.size()" should return 2
    And The SPEL expression "nodeTemplates['TestComponentSource'].relationships.values()[0].target" should return "Compute"
    And The SPEL expression "nodeTemplates['TestComponentSource'].relationships['testComponentConnectsToTestComponent'].target" should return "TestComponent"
    And The dto SPEL expression "dependencyConflicts.size()" should return 0

  Scenario: The new archive version has a transitive dependency in a newer version
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.ChangeDependencyVersionOperation |
      | dependencyName    | test-topo-dependencies-types                                             |
      | dependencyVersion | 0.5-SNAPSHOT                                                             |
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.ChangeDependencyVersionOperation |
      | dependencyName    | test-topo-dependencies-types                                             |
      | dependencyVersion | 0.6-SNAPSHOT                                                             |
    Then No exception should be thrown
    And The SPEL expression "nodeTemplates.size()" should return 3
    And The SPEL expression "dependencies.^[name == 'test-topo-dependencies-types'].version" should return "0.6-SNAPSHOT"
    And The SPEL expression "dependencies.^[name == 'test-topo-dependencies-trans-types'].version" should return "0.2-SNAPSHOT"
    And The SPEL expression "nodeTemplates.size()" should return 3
    And The SPEL expression "nodeTemplates['TestComponent'].name" should return "TestComponent"
    And The SPEL expression "nodeTemplates['TestComponentSource'].properties['component_version'].value" should return "777"
    And The SPEL expression "nodeTemplates['TestComponentSource'].properties['my_other_property'].value" should return "some new value"
    And The SPEL expression "nodeTemplates['TestComponentSource'].relationships.size()" should return 2
    And The SPEL expression "nodeTemplates['TestComponentSource'].relationships.values()[0].target" should return "Compute"
    And The SPEL expression "nodeTemplates['TestComponentSource'].relationships['testComponentConnectsToTestComponent'].target" should return "TestComponent"
    And The dto SPEL expression "dependencyConflicts.size()" should return 0


  Scenario: A transitive dependency version is manually changed creating a dependency conflict
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.ChangeDependencyVersionOperation |
      | dependencyName    | test-topo-dependencies-types                                             |
      | dependencyVersion | 0.5-SNAPSHOT                                                             |
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.ChangeDependencyVersionOperation |
      | dependencyName    | test-topo-dependencies-trans-types                                       |
      | dependencyVersion | 0.2-SNAPSHOT                                                             |
    Then No exception should be thrown
    And The SPEL expression "dependencies.^[name == 'test-topo-dependencies-types'].version" should return "0.5-SNAPSHOT"
    And The SPEL expression "dependencies.^[name == 'test-topo-dependencies-trans-types'].version" should return "0.2-SNAPSHOT"
    And The dto SPEL expression "dependencyConflicts.size()" should return 1
    And The dto SPEL expression "dependencyConflicts.^[source == 'test-topo-dependencies-types' ].dependency" should return "test-topo-dependencies-trans-types:0.1-SNAPSHOT"
    And The dto SPEL expression "dependencyConflicts.^[source == 'test-topo-dependencies-types' ].resolvedVersion" should return "0.2-SNAPSHOT"

  Scenario: The topology depends on a dependency and a node with a newer version of it is added then the conflict is resolved manually
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.ChangeDependencyVersionOperation |
      | dependencyName    | test-topo-dependencies-types                                             |
      | dependencyVersion | 0.5-SNAPSHOT                                                             |
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | OtherNode                                                             |
      | indexedNodeTypeId | alien.test.nodes.TestComponentSourceAncestor:0.2-SNAPSHOT             |
    And The dto SPEL expression "dependencyConflicts.size()" should return 1
    And The dto SPEL expression "dependencyConflicts.^[source == 'test-topo-dependencies-types' ].dependency" should return "test-topo-dependencies-trans-types:0.1-SNAPSHOT"
    And The dto SPEL expression "dependencyConflicts.^[source == 'test-topo-dependencies-types' ].resolvedVersion" should return "0.2-SNAPSHOT"
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.ChangeDependencyVersionOperation |
      | dependencyName    | test-topo-dependencies-types                                             |
      | dependencyVersion | 0.5-SNAPSHOT                                                             |
    Then No exception should be thrown
    And The SPEL expression "dependencies.^[name == 'test-topo-dependencies-types'].version" should return "0.5-SNAPSHOT"
    And The SPEL expression "dependencies.^[name == 'test-topo-dependencies-trans-types'].version" should return "0.2-SNAPSHOT"
    And The dto SPEL expression "dependencyConflicts.size()" should return 0