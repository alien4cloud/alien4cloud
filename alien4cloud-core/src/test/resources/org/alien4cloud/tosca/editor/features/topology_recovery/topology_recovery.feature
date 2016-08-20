Feature: Topology editor: Recover a topology after csar dependencies updates
    ################################################
    ##TODO
    ## Test case recover the topology when it is being edited. Here we should check that the types in the returned DTO (cached types) are well updated
    #################################################

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload unzipped CSAR from path "src/test/resources/data/csars/topology_recovery/test-topo-recovery-types.yml"
    And I upload unzipped CSAR from path "src/test/resources/data/csars/topology_recovery/sample-topology-test-recovery.yml"
    And I get the topology related to the template with name "test-recovery-topology"

  Scenario: Delete a node type from archive and recover the topology
    Given I upload unzipped CSAR from path "src/test/resources/data/csars/topology_recovery/test-recovery-nodetype-deleted-types.yml"
    When I ask for updated dependencies from the registered topology
    Then The SPEL int expression "size()" should return 1
    And The SPEL int expression "?[#this.name == 'test-topo-recovery-types' and #this.version == '0.1-SNAPSHOT' ].size()" should return 1
    When I execute the operation
      | type | org.alien4cloud.tosca.editor.operations.RecoverTopologyOperation |
    Then No exception should be thrown
    And The topology SPEL int expression "nodeTemplates.size()" should return 2
    And The topology SPEL expression "nodeTemplates['TestComponent']" should return "null"
    Then The topology SPEL int expression "nodeTemplates['TestComponentSource'].relationships.size()" should return 1
    Then The topology SPEL expression "nodeTemplates['TestComponentSource'].relationships.values()[0].target" should return "Compute"

  Scenario: Delete a relationship type from archive and recover the topology
    Given I upload unzipped CSAR from path "src/test/resources/data/csars/topology_recovery/test-recovery-reltype-deleted-types.yml"
    When I ask for updated dependencies from the registered topology
    Then The SPEL int expression "size()" should return 1
    And The SPEL int expression "?[#this.name == 'test-topo-recovery-types' and #this.version == '0.1-SNAPSHOT' ].size()" should return 1
    When I execute the operation
      | type | org.alien4cloud.tosca.editor.operations.RecoverTopologyOperation |
    Then No exception should be thrown
    And The topology SPEL int expression "nodeTemplates.size()" should return 3
    Then The topology SPEL int expression "nodeTemplates['TestComponentSource'].relationships.size()" should return 7
    Then The topology SPEL int expression "nodeTemplates['TestComponentSource'].relationships.values().?[#this.type == 'alien.test.relationships.TestComponentConnectsTo'].size()" should return 0

  Scenario: Delete a capability and a requirement from a type archive and recover the topology
    Given I upload unzipped CSAR from path "src/test/resources/data/csars/topology_recovery/test-recovery-capa-requirement-deleted-types.yml"
    When I ask for updated dependencies from the registered topology
    Then The SPEL int expression "size()" should return 1
    And The SPEL int expression "?[#this.name == 'test-topo-recovery-types' and #this.version == '0.1-SNAPSHOT' ].size()" should return 1
    When I execute the operation
      | type | org.alien4cloud.tosca.editor.operations.RecoverTopologyOperation |
    Then No exception should be thrown
    And The topology SPEL int expression "nodeTemplates.size()" should return 3
    And The topology SPEL expression "nodeTemplates['TestComponentSource'].requirements['req_to_be_deleted']" should return "null"
    And The topology SPEL expression "nodeTemplates['TestComponent'].capabilities['capa_to_be_deleted']" should return "null"
    And The topology SPEL int expression "nodeTemplates['TestComponentSource'].relationships.size()" should return 6
    Then The topology SPEL int expression "nodeTemplates['TestComponentSource'].relationships.values().?[#this.type == 'alien.test.relationships.ReqToBeDeleted'].size()" should return 0
    Then The topology SPEL int expression "nodeTemplates['TestComponentSource'].relationships.values().?[#this.type == 'alien.test.relationships.CapaToBeDeleted'].size()" should return 0


  Scenario: During edition of the topology, delete a capability and a requirement from a type archive and recover the topology
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | CachedTemplateSource                                                  |
      | indexedNodeTypeId | alien.test.nodes.TestComponentSource:0.1-SNAPSHOT                     |
    And No exception should be thrown
    And The topology SPEL int expression "nodeTemplates.size()" should return 4
    And I upload unzipped CSAR from path "src/test/resources/data/csars/topology_recovery/test-recovery-capa-requirement-deleted-types.yml"
    When I ask for updated dependencies from the registered topology
    Then The SPEL int expression "size()" should return 1
    And The SPEL int expression "?[#this.name == 'test-topo-recovery-types' and #this.version == '0.1-SNAPSHOT' ].size()" should return 1
    When I execute the operation
      | type | org.alien4cloud.tosca.editor.operations.RecoverTopologyOperation |
    Then No exception should be thrown
    And The topology SPEL int expression "nodeTemplates.size()" should return 4
    And The topology SPEL expression "nodeTemplates['TestComponentSource'].requirements['req_to_be_deleted']" should return "null"
    And The topology SPEL expression "nodeTemplates['TestComponent'].capabilities['capa_to_be_deleted']" should return "null"
    And The topology SPEL int expression "nodeTemplates['TestComponentSource'].relationships.size()" should return 6
    And The topology SPEL int expression "nodeTemplates['TestComponentSource'].relationships.values().?[#this.type == 'alien.test.relationships.ReqToBeDeleted'].size()" should return 0
    And The topology SPEL int expression "nodeTemplates['TestComponentSource'].relationships.values().?[#this.type == 'alien.test.relationships.CapaToBeDeleted'].size()" should return 0
    ## check the types in the DTO
    And The dto SPEL expression "nodeTypes['alien.test.nodes.TestComponentSource'].requirements.?[#this.id == 'req_to_be_deleted'].size()" should return "0"
    And The dto SPEL expression "nodeTypes['alien.test.nodes.TestComponent'].capabilities.?[#this.id == 'capa_to_be_deleted'].size()" should return "0"


  Scenario: Update an archive, and reset the dependent topology
    Given I upload unzipped CSAR from path "src/test/resources/data/csars/topology_recovery/test-recovery-nodetype-deleted-types.yml"
    When I ask for updated dependencies from the registered topology
    Then The SPEL int expression "size()" should return 1
    And The SPEL int expression "?[#this.name == 'test-topo-recovery-types' and #this.version == '0.1-SNAPSHOT' ].size()" should return 1
    When I execute the operation
      | type | org.alien4cloud.tosca.editor.operations.ResetTopologyOperation |
    Then No exception should be thrown
    And The topology SPEL expression "nodeTemplates" should return "null"

