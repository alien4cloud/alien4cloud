Feature: Topology editor: Recover a topology after csar dependencies updates

  Background:
    Given I am authenticated with "ADMIN" role
    # initialize or reset the types as defined in the initial archive
    And I upload unzipped CSAR from path "src/test/resources/data/csars/topology_recovery/test-topo-recovery-types.yml"
    # initialize or reset the topology
    And I delete the archive "test-recovery-topology" "0.1-SNAPSHOT" if any
    And I upload unzipped CSAR from path "src/test/resources/data/csars/topology_recovery/sample-topology-test-recovery.yml"
    And I get the topology related to the CSAR with name "test-recovery-topology" and version "0.1-SNAPSHOT"

  Scenario: Delete a node type from archive and recover the topology
    Given I upload unzipped CSAR from path "src/test/resources/data/csars/topology_recovery/test-recovery-nodetype-deleted-types.yml"
    ## trying to execute an operation should result into an error
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | CachedTemplateSource                                                  |
      | indexedNodeTypeId | alien.test.nodes.TestComponentSource:0.1-SNAPSHOT                     |
    Then an exception of type "org.alien4cloud.tosca.editor.exception.RecoverTopologyException" should be thrown
    Then The exception SPEL expression "operation.updatedDependencies.size()" should return 1
    And The exception SPEL expression "operation.updatedDependencies.?[#this.name == 'test-topo-recovery-types' and #this.version == '0.1-SNAPSHOT' ].size()" should return 1
    When I recover the topology
    Then No exception should be thrown
    And The SPEL expression "nodeTemplates.size()" should return 2
    And The SPEL expression "nodeTemplates['TestComponent']" should return "null"
    And The SPEL expression "nodeTemplates['TestComponentSource'].relationships.size()" should return 1
    And The SPEL expression "nodeTemplates['TestComponentSource'].relationships.values()[0].target" should return "Compute"
    ##we add this to be sure there is not other recovery operation to be done
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | CachedTemplateSource                                                  |
      | indexedNodeTypeId | alien.test.nodes.TestComponentSource:0.1-SNAPSHOT                     |
    Then No exception should be thrown

  Scenario: Delete a relationship type from archive and recover the topology
    Given I upload unzipped CSAR from path "src/test/resources/data/csars/topology_recovery/test-recovery-reltype-deleted-types.yml"
    ## trying to execute an operation should result into an error
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | CachedTemplateSource                                                  |
      | indexedNodeTypeId | alien.test.nodes.TestComponentSource:0.1-SNAPSHOT                     |
    Then an exception of type "org.alien4cloud.tosca.editor.exception.RecoverTopologyException" should be thrown
    Then The exception SPEL expression "operation.updatedDependencies.size()" should return 1
    And The exception SPEL expression "operation.updatedDependencies.?[#this.name == 'test-topo-recovery-types' and #this.version == '0.1-SNAPSHOT' ].size()" should return 1
    When I recover the topology
    Then No exception should be thrown
    And The SPEL expression "nodeTemplates.size()" should return 3
    And The SPEL expression "nodeTemplates['TestComponentSource'].relationships.size()" should return 7
    And The SPEL expression "nodeTemplates['TestComponentSource'].relationships.values().?[#this.type == 'alien.test.relationships.TestComponentConnectsTo'].size()" should return 0
    ##we add this to be sure there is not other recovery operation to be done
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | CachedTemplateSource                                                  |
      | indexedNodeTypeId | alien.test.nodes.TestComponentSource:0.1-SNAPSHOT                     |
    Then No exception should be thrown

  Scenario: Delete a capability and a requirement from a type archive and recover the topology
    Given I upload unzipped CSAR from path "src/test/resources/data/csars/topology_recovery/test-recovery-capa-requirement-deleted-types.yml"
    ## trying to execute an operation should result into an error
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | CachedTemplateSource                                                  |
      | indexedNodeTypeId | alien.test.nodes.TestComponentSource:0.1-SNAPSHOT                     |
    Then an exception of type "org.alien4cloud.tosca.editor.exception.RecoverTopologyException" should be thrown
    Then The exception SPEL expression "operation.updatedDependencies.size()" should return 1
    And The exception SPEL expression "operation.updatedDependencies.?[#this.name == 'test-topo-recovery-types' and #this.version == '0.1-SNAPSHOT' ].size()" should return 1
    When I recover the topology
    Then No exception should be thrown
    And The SPEL expression "nodeTemplates.size()" should return 3
    And The SPEL expression "nodeTemplates['TestComponentSource'].requirements['req_to_be_deleted']" should return "null"
    And The SPEL expression "nodeTemplates['TestComponent'].capabilities['capa_to_be_deleted']" should return "null"
    And The SPEL expression "nodeTemplates['TestComponentSource'].relationships.size()" should return 6
    And The SPEL expression "nodeTemplates['TestComponentSource'].relationships.values().?[#this.type == 'alien.test.relationships.ReqToBeDeleted'].size()" should return 0
    And The SPEL expression "nodeTemplates['TestComponentSource'].relationships.values().?[#this.type == 'alien.test.relationships.CapaToBeDeleted'].size()" should return 0
    ##we add this to be sure there is not other recovery operation to be done
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | CachedTemplateSource                                                  |
      | indexedNodeTypeId | alien.test.nodes.TestComponentSource:0.1-SNAPSHOT                     |
    Then No exception should be thrown


  Scenario: During edition of the topology, delete a capability and a requirement from a type archive and recover the topology
    Given I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | CachedTemplateSource                                                  |
      | indexedNodeTypeId | alien.test.nodes.TestComponentSource:0.1-SNAPSHOT                     |
    And No exception should be thrown
    And The SPEL expression "nodeTemplates.size()" should return 4
    And I upload unzipped CSAR from path "src/test/resources/data/csars/topology_recovery/test-recovery-capa-requirement-deleted-types.yml"
    ## trying to execute an operation should result into an error
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | CachedTemplateSource                                                  |
      | indexedNodeTypeId | alien.test.nodes.TestComponentSource:0.1-SNAPSHOT                     |
    Then an exception of type "org.alien4cloud.tosca.editor.exception.RecoverTopologyException" should be thrown
    Then The exception SPEL expression "operation.updatedDependencies.size()" should return 1
    And The exception SPEL expression "operation.updatedDependencies.?[#this.name == 'test-topo-recovery-types' and #this.version == '0.1-SNAPSHOT' ].size()" should return 1
    When I recover the topology
    Then No exception should be thrown
    And The SPEL expression "nodeTemplates.size()" should return 4
    And The SPEL expression "nodeTemplates['TestComponentSource'].requirements['req_to_be_deleted']" should return "null"
    And The SPEL expression "nodeTemplates['TestComponent'].capabilities['capa_to_be_deleted']" should return "null"
    And The SPEL expression "nodeTemplates['TestComponentSource'].relationships.size()" should return 6
    And The SPEL expression "nodeTemplates['TestComponentSource'].relationships.values().?[#this.type == 'alien.test.relationships.ReqToBeDeleted'].size()" should return 0
    And The SPEL expression "nodeTemplates['TestComponentSource'].relationships.values().?[#this.type == 'alien.test.relationships.CapaToBeDeleted'].size()" should return 0
    ## check the types in the DTO
    And The dto SPEL expression "nodeTypes['alien.test.nodes.TestComponentSource'].requirements.?[#this.id == 'req_to_be_deleted'].size()" should return 0
    And The dto SPEL expression "nodeTypes['alien.test.nodes.TestComponent'].capabilities.?[#this.id == 'capa_to_be_deleted'].size()" should return 0
    ##we add this to be sure there is not other recovery operation to be done
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | CachedTemplateSource2                                                 |
      | indexedNodeTypeId | alien.test.nodes.TestComponentSource:0.1-SNAPSHOT                     |
    Then No exception should be thrown


  Scenario: Update an archive, and reset the dependent topology
    Given I upload unzipped CSAR from path "src/test/resources/data/csars/topology_recovery/test-recovery-nodetype-deleted-types.yml"
    ## trying to execute an operation should result into an error
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | CachedTemplateSource                                                  |
      | indexedNodeTypeId | alien.test.nodes.TestComponentSource:0.1-SNAPSHOT                     |
    Then an exception of type "org.alien4cloud.tosca.editor.exception.RecoverTopologyException" should be thrown
    Then The exception SPEL expression "operation.updatedDependencies.size()" should return 1
    And The exception SPEL expression "operation.updatedDependencies.?[#this.name == 'test-topo-recovery-types' and #this.version == '0.1-SNAPSHOT' ].size()" should return 1
    When I reset the topology
    Then No exception should be thrown
    And The SPEL expression "nodeTemplates" should return "null"
    ##we add this to be sure there is not other recovery operation to be done
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | CachedTemplateSource                                                  |
      | indexedNodeTypeId | alien.test.nodes.TestComponentSource:0.1-SNAPSHOT                     |
    Then No exception should be thrown
