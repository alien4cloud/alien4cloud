Feature: Manage Nodetemplates of a topology

Background:
  Given I am authenticated with "ADMIN" role
  And I have a CSAR folder that is "containing base types constraints"
  And I upload it
  And I have a CSAR folder that is "containing java types"
  And I upload it
  And There is a "node type" with element name "tosca.nodes.Compute" and archive version "1.0"
  And There is a "node type" with element name "fastconnect.nodes.Java" and archive version "1.0"
  And I create a new application with name "watchmiddleearth" and description "Use my great eye to find frodo and the ring."

Scenario: Update a nodetemplate's property disk_size with greaterThan constraint
  Given I have added a node template "Template1" related to the "tosca.nodes.Compute:1.0" node type
  When I update the node template "Template1"'s property "disk_size" to "A"
  Then I should receive a RestResponse with an error code 800
  	And I should receive a RestResponse with constraint data name "greaterThan" and reference "0"

Scenario: Update a nodetemplate's property version with greaterOrEqual constraint
  Given I have added a node template "TemplateJava" related to the "fastconnect.nodes.Java:1.0" node type
  When I update the node template "TemplateJava"'s property "version" to "1.2"
  Then I should receive a RestResponse with an error code 800
  	And I should receive a RestResponse with constraint data name "greaterOrEqual" and reference "1.5"

Scenario: Update a nodetemplate's property version with inRange constraint
  Given I have added a node template "TemplateCompute" related to the "tosca.nodes.Compute:1.0" node type
  When I update the node template "TemplateCompute"'s property "target_jvm_version" to "1.2"
  Then I should receive a RestResponse with an error code 800
  	And I should receive a RestResponse with constraint data name "inRange" and reference "[1.5, 1.7]"

Scenario: Update a nodetemplate's property version with validValues constraint
  Given I have added a node template "TemplateCompute" related to the "tosca.nodes.Compute:1.0" node type
  When I update the node template "TemplateCompute"'s property "target_jvm" to "1.2"
  Then I should receive a RestResponse with an error code 800
  	And I should receive a RestResponse with constraint data name "validValues" and reference "[1.5, 1.6, 1.7]"

Scenario: Update a nodetemplate's property version with validValues constraint with in range value
  Given I have added a node template "TemplateCompute" related to the "tosca.nodes.Compute:1.0" node type
  When I update the node template "TemplateCompute"'s property "target_jvm" to "1.6"
  Then I should receive a RestResponse with no error

Scenario: Update a nodetemplate's property num_cpus with greaterOrEqual constraint
  Given I have added a node template "Template3" related to the "tosca.nodes.Compute:1.0" node type
  When I update the node template "Template3"'s property "num_cpus" to "2"
  Then I should receive a RestResponse with an error code 800
  	And I should receive a RestResponse with constraint data name "greaterOrEqual" and reference "3"

Scenario: Update a nodetemplate's property num_cpus with greaterOrEqual constraint without error
  Given I have added a node template "Template3" related to the "tosca.nodes.Compute:1.0" node type
  When I update the node template "Template3"'s property "num_cpus" to "3"
  Then I should receive a RestResponse with no error

Scenario: Update a nodetemplate's property instance_max_count with equal constraint
  Given I have added a node template "Template4" related to the "tosca.nodes.Compute:1.0" node type
  When I update the node template "Template4"'s property "instance_max_count" to "3"
  Then I should receive a RestResponse with an error code 800
    And I should receive a RestResponse with constraint data name "equal" and reference "2"
