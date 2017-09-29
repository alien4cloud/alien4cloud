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
        // "src/test/resources/org/alien4cloud/tosca/editor/features/policies/add_policy.feature"
        //
})
public class EditorTest {
}