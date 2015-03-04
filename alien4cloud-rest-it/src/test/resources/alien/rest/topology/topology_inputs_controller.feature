Feature: Topology inputs controller

  Scenario: Define a property as input
    When I define the property "os_arch" of the node "Compute" as input property
    Then I should receive a RestResponse with no error
    And The topology should have the property "os_arch" of the node "Compute" defined as input property
    
  Scenario: Remove an input property
    Given I define the property "os_arch" of the node "Compute" as input property
    When I define the property "os_arch" of the node "Compute" as non input property
    Then I should receive a RestResponse with no error
    And The topology should not have the property "os_arch" of the node "Compute" defined as input property
    
  Scenario: Define an non existing attribute as input
    When I define the attribute "public_ip_address" of the node "Compute" as input attribute
    Then I should receive a RestResponse with an error code 802
    When I define the attribute "private_ip_address" of the node "Compute" as input attribute
    Then I should receive a RestResponse with an error code 802
    
  Scenario: Associate the property of a node template to an input of the topology
    When I define the property "os_distribution" of the node "Compute" as input property
    Then I should receive a RestResponse with no error
      And The topology should have the property "os_distribution" of the node "Compute" defined as input property
    Then I associate the property "os_version" of a node template to the input "os_distribution"
      And I should receive a RestResponse with no error
    
  Scenario: Associate the property of a relationship template to an input of the topology
    When I define the property "os_distribution" of the node "Compute" as input property
    Then I should receive a RestResponse with no error
      And The topology should have the property "os_distribution" of the node "Compute" defined as input property
    Then I associate the property "os_version" of a relationship to the input "os_distribution"
      And I should receive a RestResponse with no error
 