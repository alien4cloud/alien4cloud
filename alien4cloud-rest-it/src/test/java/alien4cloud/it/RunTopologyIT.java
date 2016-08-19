package alien4cloud.it;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.Ignore;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(features = {
        //
        // "classpath:alien/rest/topology"
        // "classpath:alien/rest/topology/get_topology.feature"
        // "classpath:alien/rest/topology/out_properties.feature"
        // "classpath:alien/rest/topology/inputs_properties.feature"
        // "classpath:alien/rest/topology/mixing_component_versions.feature"
        // "classpath:alien/rest/topology/nodetemplate_constraint.feature"
        // "classpath:alien/rest/topology/nodetemplate.feature"
        // "classpath:alien/rest/topology/nodetemplate_target_filter.feature"
        // "classpath:alien/rest/topology/relationships.feature"
        // "classpath:alien/rest/topology/replace_node_template.feature"
        // "classpath:alien/rest/topology/roles_on_topologies.feature"
        // "classpath:alien/rest/topology/scaling.feature"
        // "classpath:alien/rest/topology/validate_topology.feature"
        // "classpath:alien/rest/topology/validate_topology_with_meta_properties.feature"
        "classpath:alien/rest/topology/topology_composition.feature"
        // "classpath:alien/rest/topology/workflow_edition.feature"
        //
}, format = { "pretty", "html:target/cucumber/topology", "json:target/cucumber/cucumber-topology.json" })
// @Ignore
public class RunTopologyIT {
}
