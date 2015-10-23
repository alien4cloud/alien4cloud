Feature: CSAR upload rights management

Scenario: Upload CSAR containing types and embeded topology template as COMPONENT_MANAGER should fail
  Given I am authenticated with "COMPONENTS_MANAGER" role
  And I upload the archive "tosca base types 1.0"
  When I upload the archive "topology apache"
  Then I should receive a RestResponse with an error code 102

Scenario: Upload CSAR containing types and embeded topology template as ARCHITECT should fail
  Given I am authenticated with "COMPONENTS_MANAGER" role
  And I upload the archive "tosca base types 1.0"
  When I am authenticated with "ARCHITECT" role
  And I upload the archive "topology apache"
  Then I should receive a RestResponse with an error code 102

Scenario: Upload CSAR containing only types as COMPONENT_MANAGER should success
  Given I am authenticated with "COMPONENTS_MANAGER" role
  When I upload the archive "tosca base types 1.0"
  Then I should receive a RestResponse with no error
  
Scenario: Upload CSAR containing only topology template as ARCHITECT should success
  Given I am authenticated with "COMPONENTS_MANAGER" role
  And I upload the archive "tosca base types 1.0"
  When I am authenticated with "ARCHITECT" role
  And I upload the archive "topology-singlecompute"
  Then I should receive a RestResponse with no error 
    