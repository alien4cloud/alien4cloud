Feature: Get updated dependencies of a topology

  Background:
    Given I am authenticated with "ADMIN" role
    # Archives
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload the local archive "data/csars/topology_recovery/test-topo-recovery-types.yaml"
    And I upload the local archive "data/csars/topology_recovery/sample-topology-test-recovery.yml"
    And I get the topology related to the template with name "test-recovery-topology"
#    And I create a new application with name "watchmiddleearth" and description "Use my great eye to find frodo and the ring."

  @reset
  Scenario: Delete a node type from archive and get the updated dependencies
    Given I upload the local archive "data/csars/topology_recovery_test/test-recovery-nodetype-deleted-types.yml"
  	When I ask for updated dependencies from the registered topology
  	Then The Response should contain the folowwing dependencies
  		|name                    |version|
  		|test-topo-recovery-types|0.1-SNAPSHOT|