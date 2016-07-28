Feature: Test alien4cloud scaling behavior in HA mode
  # Tested case with this Feature:
  #   - alien4cloud scale up: make sure the new scaled instance can be leader and works well
  #   - alien4cloud scale down

  Scenario: Scale up alien4cloud, and fail the previous instances so that the new one could be leader
    When I scale up the alien4cloud compute node by adding 1 instance
     And The node "alien4cloud" should contain 3 instance(s) after at maximum 15 minutes
    When I shutdown the alien process on the alien4cloud leader instance
     And I wait for 10 seconds before continuing the test
    Then alien4cloud should be available
     And I should be able to access the application "ALIEN"
     And the alien4cloud backup instance should be the new leader
    When I shutdown the alien process on the alien4cloud leader instance
     And I wait for 10 seconds before continuing the test
    Then alien4cloud should be available
     And I should be able to access the application "ALIEN"
     And the alien4cloud backup instance should be the new leader

  Scenario: Scale down alien4cloud
    When I scale down the alien4cloud compute node by removing 1 instance
     And I wait for 10 seconds before continuing the test
    Then alien4cloud should be available
     And I should be able to access the application "ALIEN"
