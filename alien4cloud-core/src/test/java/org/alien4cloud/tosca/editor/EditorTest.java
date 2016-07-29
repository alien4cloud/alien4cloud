package org.alien4cloud.tosca.editor;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(format = "pretty", tags = { "~@Ignore" }, features = {
        // "src/test/resources/org/alien4cloud/tosca/editor/features/"
        // "src/test/resources/org/alien4cloud/tosca/editor/features/add_node_operation.feature"
        // "src/test/resources/org/alien4cloud/tosca/editor/features/add_relationship.feature"
        // "src/test/resources/org/alien4cloud/tosca/editor/features/delete_node_operation.feature"
        // "src/test/resources/org/alien4cloud/tosca/editor/features/rename_node_operation.feature"
         "src/test/resources/org/alien4cloud/tosca/editor/features/update_node_property_value.feature"
})
public class EditorTest {

}
