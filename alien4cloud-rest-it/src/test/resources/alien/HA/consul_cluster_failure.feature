Feature: Test consul server process failure
  # Tested case with this Feature:
  #   - one consul server down
  #   - consul cluster up after a moment of unavailability

  Background:
    Given I am authenticated with "ADMIN" role
     And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
     And I upload the archive "samples"
     And I create an application "ALIEN" with a Compute and a JDK nodes
     And I upload a plugin
     And I setup an orchestrator "orchestrator" with a location "test-location"
     And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes
    #When I deploy it


  Scenario: The consul leader server fails, then another consul server fails, and finally the consul cluster is up again
    When I shutdown the consul process on the consul leader instance
     And I wait for 10 seconds before continuing the test
    Then alien4cloud should be available
     And I should be able to access the application "ALIEN"
     And one consul backup instance should be the new leader

    # another consul server failure ==> the consul cluster not available
    When I shutdown the consul process on one of the two remaining running server
     And I wait for 10 seconds before continuing the test
    Then alien4cloud should not be available

    # start all consul servers processes ==> consul cluster available again
    When I restart all unavailable consul servers processes
     And I wait for 30 seconds before continuing the test
    Then alien4cloud should be available
     And I should be able to access the application "ALIEN"
