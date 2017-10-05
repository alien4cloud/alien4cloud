package org.alien4cloud.tosca.editor;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(format = "pretty", tags = { "~@Ignore" }, features = {
        //
        "src/test/resources/org/alien4cloud/tosca/editor/"
        // "src/test/resources/org/alien4cloud/tosca/editor/features/"
        // "src/test/resources/org/alien4cloud/tosca/editor/features/_initialize_archives.feature",
        // "src/test/resources/org/alien4cloud/tosca/editor/features/node/add_node.feature",
        // "src/test/resources/org/alien4cloud/tosca/editor/features/node/delete_node.feature",
        // "src/test/resources/org/alien4cloud/tosca/editor/features/node/reset_node_deployment_artifact.feature",
        // "src/test/resources/org/alien4cloud/tosca/editor/features/topology_recovery/topology_recovery.feature"
        // "src/test/resources/org/alien4cloud/tosca/editor/features/policies/update_policy_property_value.feature"
        // "src/test/resources/org/alien4cloud/tosca/editor/features/policies/update_policy_targets.feature"
        //
})
public class EditorTest {
}