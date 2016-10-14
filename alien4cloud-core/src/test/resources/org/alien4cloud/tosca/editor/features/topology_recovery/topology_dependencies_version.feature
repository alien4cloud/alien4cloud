Feature: Topology editor: Recover a topology after csar dependencies updates

  Background:
    Given I am authenticated with "ADMIN" role
    # initialize or reset the types as defined in the initial archive
    And I upload unzipped CSAR from path "src/test/resources/data/csars/dependency_version/dependency1.yml"
    And I upload unzipped CSAR from path "src/test/resources/data/csars/dependency_version/dependency2.yml"
    And I upload unzipped CSAR from path "src/test/resources/data/csars/dependency_version/dependency3.yml"
    And I upload unzipped CSAR from path "src/test/resources/data/csars/dependency_version/dependency4.yml"
    And I upload unzipped CSAR from path "src/test/resources/data/csars/dependency_version/dependency-transitive.yml"
    And I upload unzipped CSAR from path "src/test/resources/data/csars/dependency_version/dependency5.yml"
    # initialize or reset the topology
    And I delete the archive "test-dependencies-change" "0.1.0-SNAPSHOT" if any
    And I upload unzipped CSAR from path "src/test/resources/data/csars/dependency_version/initial-topology.yml"
    And I get the topology related to the CSAR with name "test-dependencies-change" and version "0.1.0-SNAPSHOT"

  Scenario: A node type has been removed
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.ChangeDependencyVersionOperation |
      | dependencyName    | test-topo-dependencies-types                                             |
      | dependencyVersion | 0.2-SNAPSHOT                                                             |
    Then No exception should be thrown
    And The SPEL expression "dependencies.^[name == 'test-topo-dependencies-types'].version" should return "0.2-SNAPSHOT"
    And The SPEL expression "nodeTemplates.size()" should return 2
    And The SPEL expression "nodeTemplates['TestComponent']" should return "null"
    And The SPEL expression "nodeTemplates['TestComponentSource'].properties['component_version'].value" should return "777"
    And The SPEL expression "nodeTemplates['TestComponentSource'].relationships.size()" should return 1
    And The SPEL expression "nodeTemplates['TestComponentSource'].relationships.values()[0].target" should return "Compute"

  Scenario: The requirement has been removed from the source node
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

  Scenario: The relationship type has been removed and replaced by a new one
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.ChangeDependencyVersionOperation |
      | dependencyName    | test-topo-dependencies-types                                             |
      | dependencyVersion | 0.4-SNAPSHOT                                                             |
    Then No exception should be thrown
    And The SPEL expression "dependencies.^[name == 'test-topo-dependencies-types'].version" should return "0.4-SNAPSHOT"
    And The SPEL expression "nodeTemplates.size()" should return 3
    And The SPEL expression "nodeTemplates['TestComponent'].name" should return "TestComponent"
    And The SPEL expression "nodeTemplates['TestComponentSource'].properties['component_version'].value" should return "777"
    And The SPEL expression "nodeTemplates['TestComponentSource'].relationships.size()" should return 1
    And The SPEL expression "nodeTemplates['TestComponentSource'].relationships.values()[0].target" should return "Compute"

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

