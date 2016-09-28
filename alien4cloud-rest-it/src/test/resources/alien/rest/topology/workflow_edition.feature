# in these scenari, we will create a topology template, add nodes and edit the workflow
Feature: Workflow edition

  Background:
    Given I am authenticated with "ADMIN" role
	  And I upload the archive "tosca-normative-types-1.0.0-SNAPSHOT"
      And I upload the archive "samples apache"
      And I upload the archive "custom-interface-mock-types"
      And I upload the archive "samples php"
    Given I create a new topology template with name "net.sample.AP" and description "A Linux PHP stack to test workflows"
      And The RestResponse should contain a topology template id
      And I can get and register the topology for the last version of the registered topology template
    Given I add a node template "MyCompute" related to the "tosca.nodes.Compute:1.0.0-SNAPSHOT" node type

  @reset
  Scenario: rename a standard workflow
    Given I edit the workflow named "install"
     When I rename the workflow to "my_install_wf"
     Then I should receive a RestResponse with an error code 500

  @reset
  Scenario: delete a standard workflow
    Given I edit the workflow named "install"
     When I remove the workflow
     Then I should receive a RestResponse with an error code 500

  # we can not remove a step related to a delegate wf (abstract substituable native node)
  @reset
  Scenario: remove a delegate step should fail
    Given I edit the workflow named "install"
     When I remove the workflow step named "MyCompute_install"
     Then I should receive a RestResponse with an error code 850

  # we add a hosedOn stuff, the steps must follow a defined order
  @reset
  Scenario: Add a node hostedOn compute
    Given I add a node template "MySoftware" related to the "alien4cloud.tests.nodes.CustomInterface:1.1.0-SNAPSHOT" node type
     When I have added a relationship "hostedOnCompute" of type "tosca.relationships.HostedOn" defined in archive "tosca-normative-types" version "1.0.0-SNAPSHOT" with source "MySoftware" and target "MyCompute" for requirement "host" of type "tosca.capabilities.Container" and target capability "host"
     Then I should receive a RestResponse with no error
     When I should be able to retrieve a topology with name "net.sample.AP" version "0.1.0-SNAPSHOT" and store it as a SPEL context
      # we have 2 workflows : install and uninstall
     Then The SPEL int expression "workflows.size()" should return 2
      # we have 1 host
      And The SPEL int expression "workflows['install'].hosts.size()" should return 1
      And The SPEL expression "workflows['install'].hosts[0]" should return "MyCompute"
     When I try to retrieve the created topology
      # . > MyCompute_install
     Then The workflow step "MyCompute_install" has no predecessors
      # MyCompute_install > MySoftware_initial
      And The workflow step "MyCompute_install" is followed by: "MySoftware_initial"
      And The workflow step "MySoftware_initial" is preceded by: "MyCompute_install"
      # MySoftware_initial > MySoftware_creating
      And The workflow step "MySoftware_initial" is followed by: "MySoftware_creating"
      And The workflow step "MySoftware_creating" is preceded by: "MySoftware_initial"
      # MySoftware_creating > create_MySoftware
      And The workflow step "MySoftware_creating" is followed by: "create_MySoftware"
      And The workflow step "create_MySoftware" is preceded by: "MySoftware_creating"
      # create_MySoftware > MySoftware_created
      And The workflow step "create_MySoftware" is followed by: "MySoftware_created"
      And The workflow step "MySoftware_created" is preceded by: "create_MySoftware"
      # MySoftware_created > MySoftware_configuring
      And The workflow step "MySoftware_created" is followed by: "MySoftware_configuring"
      And The workflow step "MySoftware_configuring" is preceded by: "MySoftware_created"
      # MySoftware_configuring > configure_MySoftware
      And The workflow step "MySoftware_configuring" is followed by: "configure_MySoftware"
      And The workflow step "configure_MySoftware" is preceded by: "MySoftware_configuring"
      # configure_MySoftware > MySoftware_configured
      And The workflow step "configure_MySoftware" is followed by: "MySoftware_configured"
      And The workflow step "MySoftware_configured" is preceded by: "configure_MySoftware"
      # MySoftware_configured > MySoftware_starting
      And The workflow step "MySoftware_configured" is followed by: "MySoftware_starting"
      And The workflow step "MySoftware_starting" is preceded by: "MySoftware_configured"
      # MySoftware_starting > start_MySoftware
      And The workflow step "MySoftware_starting" is followed by: "start_MySoftware"
      And The workflow step "start_MySoftware" is preceded by: "MySoftware_starting"
      # start_MySoftware > MySoftware_started
      And The workflow step "start_MySoftware" is followed by: "MySoftware_started"
      And The workflow step "MySoftware_started" is preceded by: "start_MySoftware"
      # MySoftware_started > .
      And The workflow step "MySoftware_started" has no followers

  # rename a step and ensure that the wf still being consistent
  @reset
  Scenario: Rename steps successfully
    Given I add a node template "MySoftware" related to the "alien4cloud.tests.nodes.CustomInterface:1.1.0-SNAPSHOT" node type
      And I have added a relationship "hostedOnCompute" of type "tosca.relationships.HostedOn" defined in archive "tosca-normative-types" version "1.0.0-SNAPSHOT" with source "MySoftware" and target "MyCompute" for requirement "host" of type "tosca.capabilities.Container" and target capability "host"
      And I edit the workflow named "install"
      And I rename the workflow step named "create_MySoftware" to "create_MySoftware_operation"
     Then I should receive a RestResponse with no error
     When I try to retrieve the created topology
      # MySoftware_creating > create_MySoftware_operation
     Then The workflow step "MySoftware_creating" is followed by: "create_MySoftware_operation"
      And The workflow step "create_MySoftware_operation" is preceded by: "MySoftware_creating"
      # create_MySoftware_operation > MySoftware_created
      And The workflow step "create_MySoftware_operation" is followed by: "MySoftware_created"
      And The workflow step "MySoftware_created" is preceded by: "create_MySoftware_operation"

  # rename a step and ensure that the new name is not already used
  @reset
  Scenario: Rename steps with name unicity checking
    Given I add a node template "MySoftware" related to the "alien4cloud.tests.nodes.CustomInterface:1.1.0-SNAPSHOT" node type
      And I have added a relationship "hostedOnCompute" of type "tosca.relationships.HostedOn" defined in archive "tosca-normative-types" version "1.0.0-SNAPSHOT" with source "MySoftware" and target "MyCompute" for requirement "host" of type "tosca.capabilities.Container" and target capability "host"
      And I edit the workflow named "install"
     When I rename the workflow step named "MySoftware_created" to "MySoftware_creating"
     Then I should receive a RestResponse with an error code 502

  # remove a step and check that the wf still being consistent
  @reset
  Scenario: Remove a step
    Given I add a node template "MySoftware" related to the "alien4cloud.tests.nodes.CustomInterface:1.1.0-SNAPSHOT" node type
      And I have added a relationship "hostedOnCompute" of type "tosca.relationships.HostedOn" defined in archive "tosca-normative-types" version "1.0.0-SNAPSHOT" with source "MySoftware" and target "MyCompute" for requirement "host" of type "tosca.capabilities.Container" and target capability "host"
      And I edit the workflow named "install"
    Given I remove the workflow step named "configure_MySoftware"
     Then I should receive a RestResponse with no error
     When I try to retrieve the created topology
        # MySoftware_configuring > MySoftware_configured
     Then The workflow step "MySoftware_configuring" is followed by: "MySoftware_configured"
      And The workflow step "MySoftware_configured" is preceded by: "MySoftware_configuring"

  # append a custom command operation call after a given step
  @reset
  Scenario: Add a custom command call
    Given I add a node template "MySoftware" related to the "alien4cloud.tests.nodes.CustomInterface:1.1.0-SNAPSHOT" node type
      And I have added a relationship "hostedOnCompute" of type "tosca.relationships.HostedOn" defined in archive "tosca-normative-types" version "1.0.0-SNAPSHOT" with source "MySoftware" and target "MyCompute" for requirement "host" of type "tosca.capabilities.Container" and target capability "host"
      And I edit the workflow named "install"
     When I append a call operation "mock.success" activity for node "MySoftware" after the step "configure_MySoftware"
     When I try to retrieve the created topology
      # configure_MySoftware > success_MySoftware
     Then The workflow step "configure_MySoftware" is followed by: "success_MySoftware"
      And The workflow step "success_MySoftware" is preceded by: "configure_MySoftware"
      # success_MySoftware > MySoftware_configured
      And The workflow step "success_MySoftware" is followed by: "MySoftware_configured"
      And The workflow step "MySoftware_configured" is preceded by: "success_MySoftware"

  # append a custom command operation call after a given step, then swap them
  @reset
  Scenario: swap steps
    Given I add a node template "MySoftware" related to the "alien4cloud.tests.nodes.CustomInterface:1.1.0-SNAPSHOT" node type
      And I have added a relationship "hostedOnCompute" of type "tosca.relationships.HostedOn" defined in archive "tosca-normative-types" version "1.0.0-SNAPSHOT" with source "MySoftware" and target "MyCompute" for requirement "host" of type "tosca.capabilities.Container" and target capability "host"
      And I edit the workflow named "install"
     When I append a call operation "mock.success" activity for node "MySoftware" after the step "configure_MySoftware"
      And I swap the workflow step "configure_MySoftware" with "success_MySoftware"
     When I try to retrieve the created topology
      # MySoftware_configuring > success_MySoftware
      And The workflow step "MySoftware_configuring" is followed by: "success_MySoftware"
      And The workflow step "success_MySoftware" is preceded by: "MySoftware_configuring"
      # success_MySoftware > configure_MySoftware
      And The workflow step "success_MySoftware" is followed by: "configure_MySoftware"
      And The workflow step "configure_MySoftware" is preceded by: "success_MySoftware"
      # configure_MySoftware > MySoftware_configured
      And The workflow step "configure_MySoftware" is followed by: "MySoftware_configured"
      And The workflow step "MySoftware_configured" is preceded by: "configure_MySoftware"

  # rename a node and check that the workflow still being consistent
  @reset
  Scenario: rename node
    Given I add a node template "MySoftware" related to the "alien4cloud.tests.nodes.CustomInterface:1.1.0-SNAPSHOT" node type
      And I have added a relationship "hostedOnCompute" of type "tosca.relationships.HostedOn" defined in archive "tosca-normative-types" version "1.0.0-SNAPSHOT" with source "MySoftware" and target "MyCompute" for requirement "host" of type "tosca.capabilities.Container" and target capability "host"
     When I update the node template's name from "MySoftware" to "MyStuff"
     Then I should receive a RestResponse with no error
     When I update the node template's name from "MyCompute" to "MyMachine"
     Then I should receive a RestResponse with no error
     When I should be able to retrieve a topology with name "net.sample.AP" version "0.1.0-SNAPSHOT" and store it as a SPEL context
      # we have 2 workflows : install and uninstall
     Then The SPEL int expression "workflows.size()" should return 2
      # we have 1 host
      And The SPEL int expression "workflows['install'].hosts.size()" should return 1
      And The SPEL expression "workflows['install'].hosts[0]" should return "MyMachine"
      And The SPEL expression "workflows['install'].steps['MyCompute_install'].nodeId" should return "MyMachine"
      And The SPEL expression "workflows['install'].steps['MyCompute_install'].hostId" should return "MyMachine"
      And The SPEL expression "workflows['install'].steps['MySoftware_initial'].nodeId" should return "MyStuff"
      And The SPEL expression "workflows['install'].steps['MySoftware_initial'].hostId" should return "MyMachine"
      And The SPEL expression "workflows['install'].steps['MySoftware_creating'].nodeId" should return "MyStuff"
      And The SPEL expression "workflows['install'].steps['MySoftware_creating'].hostId" should return "MyMachine"
      And The SPEL expression "workflows['install'].steps['create_MySoftware'].nodeId" should return "MyStuff"
      And The SPEL expression "workflows['install'].steps['create_MySoftware'].hostId" should return "MyMachine"
      And The SPEL expression "workflows['install'].steps['MySoftware_created'].nodeId" should return "MyStuff"
      And The SPEL expression "workflows['install'].steps['MySoftware_created'].hostId" should return "MyMachine"
      And The SPEL expression "workflows['install'].steps['MySoftware_configuring'].nodeId" should return "MyStuff"
      And The SPEL expression "workflows['install'].steps['MySoftware_configuring'].hostId" should return "MyMachine"
      And The SPEL expression "workflows['install'].steps['configure_MySoftware'].nodeId" should return "MyStuff"
      And The SPEL expression "workflows['install'].steps['configure_MySoftware'].hostId" should return "MyMachine"
      And The SPEL expression "workflows['install'].steps['MySoftware_configured'].nodeId" should return "MyStuff"
      And The SPEL expression "workflows['install'].steps['MySoftware_configured'].hostId" should return "MyMachine"
      And The SPEL expression "workflows['install'].steps['MySoftware_starting'].nodeId" should return "MyStuff"
      And The SPEL expression "workflows['install'].steps['MySoftware_starting'].hostId" should return "MyMachine"
      And The SPEL expression "workflows['install'].steps['start_MySoftware'].nodeId" should return "MyStuff"
      And The SPEL expression "workflows['install'].steps['start_MySoftware'].hostId" should return "MyMachine"
      And The SPEL expression "workflows['install'].steps['MySoftware_started'].nodeId" should return "MyStuff"
      And The SPEL expression "workflows['install'].steps['MySoftware_started'].hostId" should return "MyMachine"

  # connect a given step to 2 others
  @reset
  Scenario: connect step to
    Given I add a node template "MySoftware" related to the "alien4cloud.tests.nodes.CustomInterface:1.1.0-SNAPSHOT" node type
      And I have added a relationship "hostedOnCompute" of type "tosca.relationships.HostedOn" defined in archive "tosca-normative-types" version "1.0.0-SNAPSHOT" with source "MySoftware" and target "MyCompute" for requirement "host" of type "tosca.capabilities.Container" and target capability "host"
      And I edit the workflow named "install"
     When I connect the workflow step "create_MySoftware" to: "configure_MySoftware", "start_MySoftware"
     Then I should receive a RestResponse with no error
     When I try to retrieve the created topology
      # MyCompute_install > MySoftware_initial
     Then The workflow step "MyCompute_install" is followed by: "MySoftware_initial"
      And The workflow step "MySoftware_initial" is preceded by: "MyCompute_install"
      # MySoftware_initial > MySoftware_creating
      And The workflow step "MySoftware_initial" is followed by: "MySoftware_creating"
      And The workflow step "MySoftware_creating" is preceded by: "MySoftware_initial"
      # MySoftware_creating > create_MySoftware
      And The workflow step "MySoftware_creating" is followed by: "create_MySoftware"
      And The workflow step "create_MySoftware" is preceded by: "MySoftware_creating"
      # create_MySoftware > MySoftware_created, configure_MySoftware, start_MySoftware
      And The workflow step "create_MySoftware" is followed by: "MySoftware_created", "configure_MySoftware", "start_MySoftware"
      # MySoftware_created > MySoftware_configuring
      And The workflow step "MySoftware_created" is followed by: "MySoftware_configuring"
      And The workflow step "MySoftware_configuring" is preceded by: "MySoftware_created"
      # MySoftware_configuring > configure_MySoftware
      And The workflow step "MySoftware_configuring" is followed by: "configure_MySoftware"
      # MySoftware_configuring, create_MySoftware > configure_MySoftware
      And The workflow step "configure_MySoftware" is preceded by: "create_MySoftware", "MySoftware_configuring"
      # configure_MySoftware > MySoftware_configured
      And The workflow step "configure_MySoftware" is followed by: "MySoftware_configured"
      And The workflow step "MySoftware_configured" is preceded by: "configure_MySoftware"
      # MySoftware_configured > MySoftware_starting
      And The workflow step "MySoftware_configured" is followed by: "MySoftware_starting"
      And The workflow step "MySoftware_starting" is preceded by: "MySoftware_configured"
      # MySoftware_starting > start_MySoftware
      And The workflow step "MySoftware_starting" is followed by: "start_MySoftware"
      # create_MySoftware, MySoftware_starting > start_MySoftware
      And The workflow step "start_MySoftware" is preceded by: "create_MySoftware", "MySoftware_starting"
      # start_MySoftware > MySoftware_started
      And The workflow step "start_MySoftware" is followed by: "MySoftware_started"
      And The workflow step "MySoftware_started" is preceded by: "start_MySoftware"

  # connect a given step from 2 others
  @reset
  Scenario: connect step from
    Given I add a node template "MySoftware" related to the "alien4cloud.tests.nodes.CustomInterface:1.1.0-SNAPSHOT" node type
      And I have added a relationship "hostedOnCompute" of type "tosca.relationships.HostedOn" defined in archive "tosca-normative-types" version "1.0.0-SNAPSHOT" with source "MySoftware" and target "MyCompute" for requirement "host" of type "tosca.capabilities.Container" and target capability "host"
      And I edit the workflow named "install"
     When I connect the workflow step "start_MySoftware" from: "configure_MySoftware", "create_MySoftware"
     Then I should receive a RestResponse with no error
     When I try to retrieve the created topology
      # MyCompute_install > MySoftware_initial
     Then The workflow step "MyCompute_install" is followed by: "MySoftware_initial"
      And The workflow step "MySoftware_initial" is preceded by: "MyCompute_install"
      # MySoftware_initial > MySoftware_creating
      And The workflow step "MySoftware_initial" is followed by: "MySoftware_creating"
      And The workflow step "MySoftware_creating" is preceded by: "MySoftware_initial"
      # MySoftware_creating > create_MySoftware
      And The workflow step "MySoftware_creating" is followed by: "create_MySoftware"
      And The workflow step "create_MySoftware" is preceded by: "MySoftware_creating"
      # create_MySoftware > MySoftware_created, start_MySoftware
      And The workflow step "create_MySoftware" is followed by: "MySoftware_created", "start_MySoftware"
      And The workflow step "MySoftware_created" is preceded by: "create_MySoftware"
      # MySoftware_created > MySoftware_configuring
      And The workflow step "MySoftware_created" is followed by: "MySoftware_configuring"
      And The workflow step "MySoftware_configuring" is preceded by: "MySoftware_created"
      # MySoftware_configuring > configure_MySoftware
      And The workflow step "MySoftware_configuring" is followed by: "configure_MySoftware"
      And The workflow step "configure_MySoftware" is preceded by: "MySoftware_configuring"
      # configure_MySoftware > MySoftware_configured, start_MySoftware
      And The workflow step "configure_MySoftware" is followed by: "MySoftware_configured", "start_MySoftware"
      And The workflow step "MySoftware_configured" is preceded by: "configure_MySoftware"
      # MySoftware_configured > MySoftware_starting
      And The workflow step "MySoftware_configured" is followed by: "MySoftware_starting"
      And The workflow step "MySoftware_starting" is preceded by: "MySoftware_configured"
      # MySoftware_starting > start_MySoftware
      And The workflow step "MySoftware_starting" is followed by: "start_MySoftware"
      # create_MySoftware, configure_MySoftware, MySoftware_starting > start_MySoftware
      And The workflow step "start_MySoftware" is preceded by: "create_MySoftware", "configure_MySoftware", "MySoftware_starting"
      # start_MySoftware > MySoftware_started
      And The workflow step "start_MySoftware" is followed by: "MySoftware_started"
      And The workflow step "MySoftware_started" is preceded by: "start_MySoftware"

  @reset
  Scenario: diconnect steps, check the workflow, then add an operation and finally reinit workflow
    Given I add a node template "MySoftware" related to the "alien4cloud.tests.nodes.CustomInterface:1.1.0-SNAPSHOT" node type
      And I have added a relationship "hostedOnCompute" of type "tosca.relationships.HostedOn" defined in archive "tosca-normative-types" version "1.0.0-SNAPSHOT" with source "MySoftware" and target "MyCompute" for requirement "host" of type "tosca.capabilities.Container" and target capability "host"
      And I edit the workflow named "install"
     When I disconnect the workflow step from "configure_MySoftware" to "MySoftware_configured"
     Then I should receive a RestResponse with no error
     When I try to retrieve the created topology
     Then The workflow step "configure_MySoftware" has no followers
      And The workflow step "MySoftware_configured" has no predecessors
      And The workflow step "MySoftware_started" has no followers
      And The workflow step "MyCompute_install" has no predecessors
    Given I append a call operation "mock.success" activity for node "MySoftware" after the step "configure_MySoftware"
     Then I should receive a RestResponse with no error
     When I reset the workflow
     Then I should receive a RestResponse with no error
      # the workflow should finally be as in scenario 'Add a node hostedOn compute'
     When I should be able to retrieve a topology with name "net.sample.AP" version "0.1.0-SNAPSHOT" and store it as a SPEL context
      And The SPEL int expression "workflows['install'].steps.size()" should return 11
     When I try to retrieve the created topology
      # . > MyCompute_install
     Then The workflow step "MyCompute_install" has no predecessors
      # MyCompute_install > MySoftware_initial
      And The workflow step "MyCompute_install" is followed by: "MySoftware_initial"
      And The workflow step "MySoftware_initial" is preceded by: "MyCompute_install"
      # MySoftware_initial > MySoftware_creating
      And The workflow step "MySoftware_initial" is followed by: "MySoftware_creating"
      And The workflow step "MySoftware_creating" is preceded by: "MySoftware_initial"
      # MySoftware_creating > create_MySoftware
      And The workflow step "MySoftware_creating" is followed by: "create_MySoftware"
      And The workflow step "create_MySoftware" is preceded by: "MySoftware_creating"
      # create_MySoftware > MySoftware_created
      And The workflow step "create_MySoftware" is followed by: "MySoftware_created"
      And The workflow step "MySoftware_created" is preceded by: "create_MySoftware"
      # MySoftware_created > MySoftware_configuring
      And The workflow step "MySoftware_created" is followed by: "MySoftware_configuring"
      And The workflow step "MySoftware_configuring" is preceded by: "MySoftware_created"
      # MySoftware_configuring > configure_MySoftware
      And The workflow step "MySoftware_configuring" is followed by: "configure_MySoftware"
      And The workflow step "configure_MySoftware" is preceded by: "MySoftware_configuring"
      # configure_MySoftware > MySoftware_configured
      And The workflow step "configure_MySoftware" is followed by: "MySoftware_configured"
      And The workflow step "MySoftware_configured" is preceded by: "configure_MySoftware"
      # MySoftware_configured > MySoftware_starting
      And The workflow step "MySoftware_configured" is followed by: "MySoftware_starting"
      And The workflow step "MySoftware_starting" is preceded by: "MySoftware_configured"
      # MySoftware_starting > start_MySoftware
      And The workflow step "MySoftware_starting" is followed by: "start_MySoftware"
      And The workflow step "start_MySoftware" is preceded by: "MySoftware_starting"
      # start_MySoftware > MySoftware_started
      And The workflow step "start_MySoftware" is followed by: "MySoftware_started"
      And The workflow step "MySoftware_started" is preceded by: "start_MySoftware"
      # MySoftware_started > .
      And The workflow step "MySoftware_started" has no followers

  @reset
  Scenario: create a custom workflow
    Given I add a node template "MySoftware" related to the "alien4cloud.tests.nodes.CustomInterface:1.1.0-SNAPSHOT" node type
      And I have added a relationship "hostedOnCompute" of type "tosca.relationships.HostedOn" defined in archive "tosca-normative-types" version "1.0.0-SNAPSHOT" with source "MySoftware" and target "MyCompute" for requirement "host" of type "tosca.capabilities.Container" and target capability "host"
     When I create a new custom workflow
     Then I should receive a RestResponse with no error
      And the workflow should exist in the topology and I start editing it

  @reset
  Scenario: rename a custom workflow
    Given I add a node template "MySoftware" related to the "alien4cloud.tests.nodes.CustomInterface:1.1.0-SNAPSHOT" node type
      And I have added a relationship "hostedOnCompute" of type "tosca.relationships.HostedOn" defined in archive "tosca-normative-types" version "1.0.0-SNAPSHOT" with source "MySoftware" and target "MyCompute" for requirement "host" of type "tosca.capabilities.Container" and target capability "host"
     When I create a new custom workflow
     Then I should receive a RestResponse with no error
      And the workflow should exist in the topology and I start editing it
     When I rename the workflow to "myCustomWorkflow"
     Then I should receive a RestResponse with no error
     When I should be able to retrieve a topology with name "net.sample.AP" version "0.1.0-SNAPSHOT" and store it as a SPEL context
      # we have 3 workflows : install, uninstall and myCustomWorkflow
     Then The SPEL int expression "workflows.size()" should return 3
      And The SPEL boolean expression "workflows.containsKey('myCustomWorkflow')" should return true

  @reset
  Scenario: rename a custom workflow with an existing name
    Given I add a node template "MySoftware" related to the "alien4cloud.tests.nodes.CustomInterface:1.1.0-SNAPSHOT" node type
      And I have added a relationship "hostedOnCompute" of type "tosca.relationships.HostedOn" defined in archive "tosca-normative-types" version "1.0.0-SNAPSHOT" with source "MySoftware" and target "MyCompute" for requirement "host" of type "tosca.capabilities.Container" and target capability "host"
     When I create a new custom workflow
     Then I should receive a RestResponse with no error
      And the workflow should exist in the topology and I start editing it
     When I rename the workflow to "install"
     Then I should receive a RestResponse with an error code 502

  @reset
  Scenario: delete a custom workflow
    Given I add a node template "MySoftware" related to the "alien4cloud.tests.nodes.CustomInterface:1.1.0-SNAPSHOT" node type
      And I have added a relationship "hostedOnCompute" of type "tosca.relationships.HostedOn" defined in archive "tosca-normative-types" version "1.0.0-SNAPSHOT" with source "MySoftware" and target "MyCompute" for requirement "host" of type "tosca.capabilities.Container" and target capability "host"
     When I create a new custom workflow
     Then I should receive a RestResponse with no error
      And the workflow should exist in the topology and I start editing it
     When I remove the workflow
     Then I should receive a RestResponse with no error
     When I should be able to retrieve a topology with name "net.sample.AP" version "0.1.0-SNAPSHOT" and store it as a SPEL context
      # we have 2 workflows : install, uninstall
     Then The SPEL int expression "workflows.size()" should return 2
      And The SPEL boolean expression "workflows.containsKey('install')" should return true
      And The SPEL boolean expression "workflows.containsKey('uninstall')" should return true

  @reset
  Scenario: create and edit a custom restart workflow
    Given I add a node template "MySoftware" related to the "alien4cloud.tests.nodes.CustomInterface:1.1.0-SNAPSHOT" node type
      And I have added a relationship "hostedOnCompute" of type "tosca.relationships.HostedOn" defined in archive "tosca-normative-types" version "1.0.0-SNAPSHOT" with source "MySoftware" and target "MyCompute" for requirement "host" of type "tosca.capabilities.Container" and target capability "host"
      And I create a new custom workflow
      And I should receive a RestResponse with no error
      And the workflow should exist in the topology and I start editing it
     When I add a set state "stopping" activity for node "MySoftware"
     Then I should receive a RestResponse with no error
     When I add a call operation "tosca.interfaces.node.lifecycle.Standard.stop" activity for node "MySoftware"
     Then I should receive a RestResponse with no error
     When I connect the workflow step "MySoftware_stopping" to: "stop_MySoftware"
     Then I should receive a RestResponse with no error
     When I append a call operation "tosca.interfaces.node.lifecycle.Standard.start" activity for node "MySoftware" after the step "stop_MySoftware"
     Then I should receive a RestResponse with no error
     When I insert a set state "stopped" activity for node "MySoftware" before the step "start_MySoftware"
     Then I should receive a RestResponse with no error
     When I insert a set state "starting" activity for node "MySoftware" before the step "start_MySoftware"
     Then I should receive a RestResponse with no error
     When I append a set state "started" activity for node "MySoftware" after the step "start_MySoftware"
     Then I should receive a RestResponse with no error
     When I insert a call operation "mock.success" activity for node "MySoftware" before the step "MySoftware_starting"
     Then I should receive a RestResponse with no error
     When I try to retrieve the created topology
      # . > MySoftware_stopping
     Then The workflow step "MySoftware_stopping" has no predecessors
      # MySoftware_stopping > stop_MySoftware
      And The workflow step "MySoftware_stopping" is followed by: "stop_MySoftware"
      And The workflow step "stop_MySoftware" is preceded by: "MySoftware_stopping"
      # stop_MySoftware > MySoftware_stopped
      And The workflow step "stop_MySoftware" is followed by: "MySoftware_stopped"
      And The workflow step "MySoftware_stopped" is preceded by: "stop_MySoftware"
      # MySoftware_stopped > success_MySoftware
      And The workflow step "MySoftware_stopped" is followed by: "success_MySoftware"
      And The workflow step "success_MySoftware" is preceded by: "MySoftware_stopped"
      # success_MySoftware > MySoftware_starting
      And The workflow step "success_MySoftware" is followed by: "MySoftware_starting"
      And The workflow step "MySoftware_starting" is preceded by: "success_MySoftware"
      # MySoftware_starting > start_MySoftware
      And The workflow step "MySoftware_starting" is followed by: "start_MySoftware"
      And The workflow step "start_MySoftware" is preceded by: "MySoftware_starting"
      # start_MySoftware > MySoftware_started
      And The workflow step "start_MySoftware" is followed by: "MySoftware_started"
      And The workflow step "MySoftware_started" is preceded by: "start_MySoftware"
      # MySoftware_started > .
      And The workflow step "MySoftware_started" has no followers

  # by connecting start to create, we generate a cycle
  @reset
  Scenario: generate a cycle
    Given I add a node template "MySoftware" related to the "alien4cloud.tests.nodes.CustomInterface:1.1.0-SNAPSHOT" node type
      And I have added a relationship "hostedOnCompute" of type "tosca.relationships.HostedOn" defined in archive "tosca-normative-types" version "1.0.0-SNAPSHOT" with source "MySoftware" and target "MyCompute" for requirement "host" of type "tosca.capabilities.Container" and target capability "host"
      And I edit the workflow named "install"
     When I connect the workflow step "start_MySoftware" to: "create_MySoftware"
     Then I should receive a RestResponse with no error
     When I should be able to retrieve a topology with name "net.sample.AP" version "0.1.0-SNAPSHOT" and store it as a SPEL context
      # we have 1 wf error
     Then The SPEL int expression "workflows['install'].errors.size()" should return 1
