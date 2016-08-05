Feature: Topology editor: Ensure tests environment

  Scenario: Ensure tests environment
    Given I am authenticated with "ADMIN" role
    And I cleanup archives


    And I upload CSAR from path "../target/it-artifacts/tosca-normative-types-1.0.0-SNAPSHOT.csar"
    And I upload CSAR from path "../target/it-artifacts/tosca-base-types-1.0.csar"
    And I upload CSAR from path "../target/it-artifacts/java-types-1.0.csar"
