package alien4cloud.it.topology;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.apache.http.NameValuePair;
import org.junit.Assert;

import alien4cloud.it.Context;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.topology.TopologyDTO;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class OutputPropertiesStepDefinitions {

    private String getPropertyUrl(String propertyName, String nodeName, String propertyType) {
        return "/rest/v1/topologies/" + Context.getInstance().getTopologyId() + "/nodetemplates/" + nodeName + "/property/" + propertyName + "/" + propertyType;
    }

    private String getCapabilityPropertyUrl(String propertyName, String capabilityId, String nodeName) {
        return "/rest/v1/topologies/" + Context.getInstance().getTopologyId() + "/nodetemplates/" + nodeName + "/capability/" + capabilityId + "/property/"
                + propertyName + "/isOutput";
    }

    private String getAttributesUrl(String attributeName, String nodeName) {
        return "/rest/v1/topologies/" + Context.getInstance().getTopologyId() + "/nodetemplates/" + nodeName + "/attributes/" + attributeName + "/output";
    }

    @When("^I define the property \"([^\"]*)\" of the node \"([^\"]*)\" as output property$")
    public void I_define_the_property_of_the_node_as_output_property(String propertyName, String nodeName) throws Throwable {
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postUrlEncoded(getPropertyUrl(propertyName, nodeName, "isOutput"), new ArrayList<NameValuePair>()));
    }

    @Then("^The topology should have the property \"([^\"]*)\" of the node \"([^\"]*)\" defined as output property$")
    public void The_topology_should_have_the_property_of_the_node_defined_as_output_property(String propertyName, String nodeName) throws Throwable {
        TopologyDTO topologyDTO = getTopologyDTO();
        Map<String, Set<String>> outputProperties = topologyDTO.getTopology().getOutputProperties();
        Assert.assertNotNull(outputProperties);
        Set<String> outputPropertiesOfNode = outputProperties.get(nodeName);
        Assert.assertNotNull(outputPropertiesOfNode);
        Assert.assertTrue(outputPropertiesOfNode.contains(propertyName));
    }

    @Then("^The topology should have the capability property \"([^\"]*)\" of the capability \"([^\"]*)\" for the node \"([^\"]*)\" defined as output property$")
    public void The_topology_should_have_the_capability_property_of_the_capability_for_the_node_defined_as_output_property(String propertyName,
            String capabilityId, String nodeName) throws Throwable {
        TopologyDTO topologyDTO = getTopologyDTO();
        Map<String, Map<String, Set<String>>> outputCapabilityProperties = topologyDTO.getTopology().getOutputCapabilityProperties();
        Assert.assertNotNull(outputCapabilityProperties);
        Map<String, Set<String>> outputPropertiesOfNode = outputCapabilityProperties.get(nodeName);
        Assert.assertNotNull(outputPropertiesOfNode);
        Set<String> outputPropertiesOfCapability = outputPropertiesOfNode.get(capabilityId);
        Assert.assertNotNull(outputPropertiesOfCapability);
        Assert.assertTrue(outputPropertiesOfCapability.contains(propertyName));
    }

    @Then("^The topology should not have the capability property \"([^\"]*)\" of the capability \"([^\"]*)\" for the node \"([^\"]*)\" defined as output property$")
    public void The_topology_should_not_have_the_capability_property_of_the_capability_for_the_node_defined_as_output_property(String propertyName,
            String capabilityId, String nodeName) throws Throwable {
        TopologyDTO topologyDTO = getTopologyDTO();
        Map<String, Map<String, Set<String>>> outputCapabilityProperties = topologyDTO.getTopology().getOutputCapabilityProperties();
        Assert.assertNotNull(outputCapabilityProperties);
        Map<String, Set<String>> outputPropertiesOfNode = outputCapabilityProperties.get(nodeName);
        Assert.assertNotNull(outputPropertiesOfNode);
        Set<String> outputPropertiesOfCapability = outputPropertiesOfNode.get(capabilityId);
        Assert.assertNotNull(outputPropertiesOfCapability);
        Assert.assertFalse(outputPropertiesOfCapability.contains(propertyName));
    }

    @When("^I define the property \"([^\"]*)\" of the node \"([^\"]*)\" as non output property$")
    public void I_define_the_property_of_the_node_as_non_output_property(String propertyName, String nodeName) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete(getPropertyUrl(propertyName, nodeName, "isOutput")));
    }

    @Then("^The topology should not have the property \"([^\"]*)\" of the node \"([^\"]*)\" defined as output property$")
    public void The_topology_should_not_have_the_property_of_the_node_defined_as_output_property(String propertyName, String nodeName) throws Throwable {
        TopologyDTO topologyDTO = getTopologyDTO();
        Map<String, Set<String>> outputProperties = topologyDTO.getTopology().getOutputProperties();
        if (outputProperties != null) {
            Set<String> outputPropertiesOfNode = outputProperties.get(nodeName);
            if (outputPropertiesOfNode != null) {
                Assert.assertFalse(outputPropertiesOfNode.contains(propertyName));
            }
        }
    }

    @When("^I define the attribute \"([^\"]*)\" of the node \"([^\"]*)\" as output attribute$")
    public void I_define_the_attribute_of_the_node_as_output_attribute(String attributeName, String nodeName) throws Throwable {
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postUrlEncoded(getAttributesUrl(attributeName, nodeName), new ArrayList<NameValuePair>()));
    }

    @Then("^The topology should have the attribute \"([^\"]*)\" of the node \"([^\"]*)\" defined as output attribute$")
    public void The_topology_should_have_the_attribute_of_the_node_defined_as_output_attribute(String attributeName, String nodeName) throws Throwable {
        TopologyDTO topologyDTO = getTopologyDTO();
        Map<String, Set<String>> outputAttributes = topologyDTO.getTopology().getOutputAttributes();
        Assert.assertNotNull(outputAttributes);
        Set<String> outputAttributesOfNode = outputAttributes.get(nodeName);
        Assert.assertNotNull(outputAttributesOfNode);
        Assert.assertTrue(outputAttributesOfNode.contains(attributeName));
    }

    @When("^I remove the attribute \"([^\"]*)\" of the node \"([^\"]*)\" from the output attributes$")
    public void I_remove_the_attribute_of_the_node_from_the_output_attributes(String attributeName, String nodeName) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete(getAttributesUrl(attributeName, nodeName)));
    }

    @Then("^The topology should not have the attribute \"([^\"]*)\" of the node \"([^\"]*)\" defined as output attribute$")
    public void The_topology_should_not_have_the_attribute_of_the_node_defined_as_output_attribute(String attributeName, String nodeName) throws Throwable {
        TopologyDTO topologyDTO = getTopologyDTO();
        Map<String, Set<String>> outputAttributes = topologyDTO.getTopology().getOutputAttributes();
        if (outputAttributes != null) {
            Set<String> outputAttributesOfNode = outputAttributes.get(nodeName);
            if (outputAttributesOfNode != null) {
                Assert.assertFalse(outputAttributesOfNode.contains(attributeName));
            }
        }
    }

    @When("^I define the property \"([^\"]*)\" of the capability \"([^\"]*)\" of the node \"([^\"]*)\" as output property$")
    public void I_define_the_property_of_the_capability_of_the_node_as_output_property(String propertyName, String capabilityId, String nodeName)
            throws Throwable {
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postUrlEncoded(getCapabilityPropertyUrl(propertyName, capabilityId, nodeName), new ArrayList<NameValuePair>()));
    }

    @When("^I define the property \"([^\"]*)\" of the capability \"([^\"]*)\" of the node \"([^\"]*)\" as non output property$")
    public void I_define_the_property_of_the_capability_of_the_node_as_non_output_property(String propertyName, String capabilityId, String nodeName)
            throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete(getCapabilityPropertyUrl(propertyName, capabilityId, nodeName)));
    }

    private TopologyDTO getTopologyDTO() throws IOException {
        TopologyDTO topologyDTO = JsonUtil.read(Context.getRestClientInstance().get("/rest/v1/topologies/" + Context.getInstance().getTopologyId()),
                TopologyDTO.class, Context.getJsonMapper()).getData();
        return topologyDTO;
    }
}
