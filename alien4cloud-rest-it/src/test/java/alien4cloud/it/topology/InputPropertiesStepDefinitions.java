package alien4cloud.it.topology;

import java.util.ArrayList;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.junit.Assert;

import alien4cloud.it.Context;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.rest.topology.TopologyDTO;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class InputPropertiesStepDefinitions {

    private String getInputUrl(String inputId) {
        return "/rest/topologies-inputs/" + Context.getInstance().getTopologyId() + "/" + inputId;
    }

    @When("^I define the property \"([^\"]*)\" of the node \"([^\"]*)\" as input property$")
    public void I_define_the_property_of_the_node_as_input_property(String inputId, String nodeName) throws Throwable {
        Context.getInstance().registerRestResponse(
Context.getRestClientInstance().putUrlEncoded(getInputUrl(inputId), new ArrayList<NameValuePair>()));
    }

    @Then("^The topology should have the property \"([^\"]*)\" of the node \"([^\"]*)\" defined as input property$")
    public void The_topology_should_have_the_property_of_the_node_defined_as_input_property(String propertyName, String nodeName) throws Throwable {
        TopologyDTO topologyDTO = JsonUtil.read(Context.getRestClientInstance().get("/rest/topologies/" + Context.getInstance().getTopologyId()),
                TopologyDTO.class).getData();
        Map<String, PropertyDefinition> inputProperties = topologyDTO.getTopology().getInputs();
        Assert.assertNotNull(inputProperties);
        PropertyDefinition inputPropertiesOfNode = inputProperties.get(nodeName);
        Assert.assertNotNull(inputPropertiesOfNode);
        Assert.assertTrue(inputPropertiesOfNode.getType().contains(propertyName));
    }

    @When("^I define the property \"([^\"]*)\" of the node \"([^\"]*)\" as non input property$")
    public void I_define_the_property_of_the_node_as_non_input_property(String inputId) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete(getInputUrl(inputId)));
    }

    @Then("^The topology should not have the property \"([^\"]*)\" of the node \"([^\"]*)\" defined as input property$")
    public void The_topology_should_not_have_the_property_of_the_node_defined_as_input_property(String propertyName, String nodeName) throws Throwable {
        TopologyDTO topologyDTO = JsonUtil.read(Context.getRestClientInstance().get("/rest/topologies/" + Context.getInstance().getTopologyId()),
                TopologyDTO.class).getData();
        Map<String, PropertyDefinition> inputProperties = topologyDTO.getTopology().getInputs();
        if (inputProperties != null) {
            PropertyDefinition inputPropertiesOfNode = inputProperties.get(nodeName);
            if (inputPropertiesOfNode != null) {
                Assert.assertFalse(inputPropertiesOfNode.getType().contains(propertyName));
            }
        }
    }
}
