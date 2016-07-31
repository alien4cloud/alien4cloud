package org.alien4cloud.tosca.editor;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(format = "pretty", tags = { "~@Ignore" }, features = {
        //
        "src/test/resources/org/alien4cloud/tosca/editor/features/"
        // "src/test/resources/org/alien4cloud/tosca/editor/features/_initialize_archives.feature",
        // "src/test/resources/org/alien4cloud/tosca/editor/features/add_group_member.feature",
        // "src/test/resources/org/alien4cloud/tosca/editor/features/add_input.feature"
        // "src/test/resources/org/alien4cloud/tosca/editor/features/add_node.feature"
        // "src/test/resources/org/alien4cloud/tosca/editor/features/add_relationship.feature"
        // "src/test/resources/org/alien4cloud/tosca/editor/features/delete_group.feature"
        // "src/test/resources/org/alien4cloud/tosca/editor/features/delete_input.feature"
        // "src/test/resources/org/alien4cloud/tosca/editor/features/delete_node.feature"
        // "src/test/resources/org/alien4cloud/tosca/editor/features/delete_relationship.feature"
        // "src/test/resources/org/alien4cloud/tosca/editor/features/remove_group_member.feature",
        // "src/test/resources/org/alien4cloud/tosca/editor/features/rename_group.feature"
        // "src/test/resources/org/alien4cloud/tosca/editor/features/rename_input.feature"
        // "src/test/resources/org/alien4cloud/tosca/editor/features/rename_node.feature"
        // "src/test/resources/org/alien4cloud/tosca/editor/features/set_node_property_as_input.feature"
        // "src/test/resources/org/alien4cloud/tosca/editor/features/update_node_property_value.feature"
})
public class EditorTest {
}
