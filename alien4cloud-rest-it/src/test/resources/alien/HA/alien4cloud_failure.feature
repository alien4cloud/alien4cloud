Feature: Test alien4cloud failure (process and machine crash)
  # Tested case with this Feature:
  #   - alien process death on the alien4cloud leader
  #   - alien process death on the alien4cloud backup
  #   - alien4cloud leader instance crash
  #   - alien4cloud backup instance crash


  Background:
    Given I am authenticated with "ADMIN" role
     And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
     And I upload the archive "samples"
     And I create an application "ALIEN" with a Compute and a JDK nodes
     And I upload a plugin
     And I setup an orchestrator "orchestrator" with a location "test-location"
     And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes
    #When I deploy it


  Scenario: The alien process fails on the leader instance
    When I shutdown the alien process on the alien4cloud leader instance
     And I wait for 10 seconds before continuing the test
    Then alien4cloud should be available
     And I should be able to access the application "ALIEN"
     And the alien4cloud backup instance should be the new leader

  Scenario: The alien process fails on the backup instance
    When I shutdown the consul agent on the alien4cloud backup instance
     And I wait for 10 seconds before continuing the test
    Then alien4cloud should be available
     And I should be able to access the application "ALIEN"
     And the alien4cloud leader instance should still be the leader

  Scenario: The alien4cloud leader instance crashes
    When I delete the alien4cloud primary instance
     And I wait for 10 seconds before continuing the test
    Then alien4cloud should be available
     And I should be able to access the application "ALIEN"
     And the alien4cloud leader instance should still be the leader

  Scenario: The alien4cloud backup instance crashes
    When I delete the alien4cloud backup instance
     And I wait for 10 seconds before continuing the test
    Then alien4cloud should be available
     And I should be able to access the application "ALIEN"
     And the alien4cloud leader instance should still be the leader
