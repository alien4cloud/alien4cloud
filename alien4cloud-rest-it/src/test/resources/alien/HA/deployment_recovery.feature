Feature: Test recovery of a deployment in case of re-election of a new alien4cloud leader
  # Tested case with this Feature:
  #   - deployment recevory
  #   - un-deployment recovery

  Background:
    Given I am authenticated with "ADMIN" role
     And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
     And I upload the archive "samples"
     And I create an application "ALIEN" with a Compute and a JDK nodes
     And I upload a plugin
     And I setup an orchestrator "orchestrator" with a location "test-location"
     And I Set a unique location policy to "Mount doom orchestrator"/"Thark location" for all nodes
     And I deploy it

  Scenario: alien4cloud leader crashes while deployment in progress
    When I shutdown the alien process on the alien4cloud leader instance
     And I wait for 10 seconds before continuing the test
    Then alien4cloud should be available
     And I should be able to access the application "ALIEN"
     And one consul backup instance should be the new leader
     And the deployment of the application "ALIEN" should be in state "DEPLOYMENT_IN_PROGRESS"
    # make sure the deployment terminates well
     And The application's deployment must succeed after 15 minutes

    # undeployment recovery test
    Given I restart all unavailable alien4cloud instance processes
    When I wait for 10 seconds before continuing the test
    Then alien4cloud should be available
     And I should be able to access the application "ALIEN"
    When I trigger undeployment for all environments for application "ALIEN"
    Then I should receive a RestResponse with no error

    When I shutdown the alien process on the alien4cloud leader instance
     And I wait for 10 seconds before continuing the test
    Then alien4cloud should be available
     And I should be able to access the application "ALIEN"
     And one consul backup instance should be the new leader
     And the deployment of the application "ALIEN" should be in state "UNDEPLOYMENT_IN_PROGRESS"
    # make sure the deployment terminates well
     And The application's undeployment must succeed after 15 minutes
