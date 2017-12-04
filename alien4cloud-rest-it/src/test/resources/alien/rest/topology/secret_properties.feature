Feature: Topology secrets controller

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
    And I upload the archive "php-type:1.4.0-SNAPSHOT"
    And I upload the archive "mysql-type:1.4.0-SNAPSHOT"
    And I upload the archive "apache-type:1.4.0-SNAPSHOT"
    And I upload the archive "wordpress-type:1.4.0-SNAPSHOT"
    And I upload the local archive "data/csars/wordpress/wordpress.yaml"
    And I should receive a RestResponse with no error
    And I create an application with name "secret_properties_test", archive name "secret_properties_test", description "Secret properties tests" and topology template id "Wordpress:0.1.0-SNAPSHOT"
    And I should receive a RestResponse with no error


  @reset
  Scenario: Define a property as secret and then unset a property which is a secret
    When I define the property "db_password" of the node "mysql" as secret with a secret path "kv/pwd" and I save the topology
    Then I should receive a RestResponse with no error
    And The topology should have the property "db_password" of a node "mysql" defined as a secret with a secret path "kv/pwd"
    When I unset the property "db_password" of the node "mysql" back to normal value
    Then I should receive a RestResponse with no error
    And The topology should contain a nodetemplate named "mysql" with property "db_password" set to null

  @reset
  Scenario: Define a property as secret which is null and should receive an error
    When I define the property "db_password" of the node "mysql" as secret with a secret path ""
    Then I should receive a RestResponse with an error code 500

  @reset
  Scenario: Define a capability as secret and then unset a capability which is a secret
    When I define the property "port" of capability "data_endpoint" of the node "apache" as secret with a secret path "kv/port" and I save the topology
    Then I should receive a RestResponse with no error
    And The topology should have the property "port" of capability "data_endpoint" of a node "apache" defined as a secret with a secret path "kv/port"
    When I unset the property "port" of capability "data_endpoint" of the node "apache" back to normal value
    Then I should receive a RestResponse with no error
    And The topology should contain a nodetemplate named "apache" with property "port" of capability "data_endpoint" set to null

  @reset
  Scenario: Define a relationship as secret and then unset a relationship which is a secret
    When I define the property "credential" of relationship "wordpressConnectToMysqlMysql" from the node "wordpress" as secret with a secret path "kv/credential" and I save the topology
    Then I should receive a RestResponse with no error
    And The topology should have the property "credential" of relationship "wordpressConnectToMysqlMysql" from the node "wordpress" defined as a secret with a secret path "kv/credential"
    When I unset the property "credential" of relationship "wordpressConnectToMysqlMysql" from the node "wordpress" back to normal value
    Then I should receive a RestResponse with no error
    And I should have a relationship "wordpressConnectToMysqlMysql" with type "org.alien4cloud.relationships.WordpressConnectToMysql" from "wordpress" to "mysql" in ALIEN

  @reset
  Scenario: Define a capability as secret which is null and should receive an error
    When I define the property "port" of capability "database_endpoint" of the node "Database" as secret with a secret path ""
    Then I should receive a RestResponse with an error code 500

  @reset
  Scenario: We can not set the property "component_version" as a secret
    When I define the property "component_version" of the node "SoftwareComponent" as secret with a secret path "kv/version"
    Then I should receive a RestResponse with an error code 500

  @reset
  Scenario: We can not set the capability "scalable" as a secret
    When I define the property "min_instances" of capability "scalable" of the node "Compute" as secret with a secret path "kv/scalable"
    Then I should receive a RestResponse with an error code 500