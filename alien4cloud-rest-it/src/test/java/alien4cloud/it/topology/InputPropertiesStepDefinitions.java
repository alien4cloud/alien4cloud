package alien4cloud.it.topology;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Assert;

import alien4cloud.it.Context;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.rest.topology.TopologyDTO;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class InputPropertiesStepDefinitions {

    @When("^I define the property \"([^\"]*)\" of the node \"([^\"]*)\" as input property$")
    public void I_define_the_property_of_the_node_as_input_property(String inputId, String nodeName) throws Throwable {
        String fullUrl = String.format("/rest/topologies/%s/inputs/%s", Context.getInstance().getTopologyId(), inputId);
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType("string");
        String json = JsonUtil.toString(propertyDefinition);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon(fullUrl, json));
    }

    @When("^I remove the input property \"([^\"]*)\"$")
    public void I_remove_the_input_property(String inputId) throws Throwable {
        String fullUrl = String.format("/rest/topologies/%s/inputs/%s", Context.getInstance().getTopologyId(), inputId);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete(fullUrl));
    }

    @When("^I unset the property \"([^\"]*)\" of the node \"([^\"]*)\" as input property$")
    public void I_unset_the_property_of_the_node_as_input_property(String propertyId, String nodeName) throws Throwable {
        String url = String.format("/rest/topologies/%s/nodetemplates/%s/property/%s/input", Context.getInstance().getTopologyId(), nodeName, propertyId);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete(url));
    }

    @When("^I define the property \"([^\"]*)\" of the node \"([^\"]*)\" as input int property$")
    public void I_define_the_property_of_the_node_as_input_int_property(String inputId, String nodeName) throws Throwable {
        String fullUrl = String.format("/rest/topologies/%s/inputs/%s", Context.getInstance().getTopologyId(), inputId);
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType("integer");
        String json = JsonUtil.toString(propertyDefinition);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon(fullUrl, json));
    }

    @When("^I rename the property \"([^\"]*)\" to \"([^\"]*)\"$")
    public void I_rename_the_property_to(String oldInputId, String newInputId) throws Throwable {
        String url = String.format("/rest/topologies/%s/inputs/%s", Context.getInstance().getTopologyId(), oldInputId);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("newInputId", newInputId));
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().putUrlEncoded(url, nvps));
    }

    @Then("^The topology should have the property \"([^\"]*)\" defined as input property$")
    public void The_topology_should_have_the_property_of_the_node_defined_as_input_property(String inputId) throws Throwable {
        TopologyDTO topologyDTO = JsonUtil.read(Context.getRestClientInstance().get("/rest/topologies/" + Context.getInstance().getTopologyId()),
                TopologyDTO.class).getData();
        Map<String, PropertyDefinition> inputProperties = topologyDTO.getTopology().getInputs();
        Assert.assertNotNull(inputProperties);
        PropertyDefinition inputPropertieDefinition = inputProperties.get(inputId);
        Assert.assertNotNull(inputPropertieDefinition);
    }

    @Then("^The topology should not have the property \"([^\"]*)\" defined as input property$")
    public void The_topology_should_not_have_the_property_defined_as_input_property(String inputId) throws Throwable {
        TopologyDTO topologyDTO = JsonUtil.read(Context.getRestClientInstance().get("/rest/topologies/" + Context.getInstance().getTopologyId()),
                TopologyDTO.class).getData();
        Map<String, PropertyDefinition> inputProperties = topologyDTO.getTopology().getInputs();
        Assert.assertFalse(inputProperties.containsKey(inputId));
    }

    @Then("^I associate the property \"([^\"]*)\" of a node template \"([^\"]*)\" to the input \"([^\"]*)\"$")
    public void I_associate_the_property_of_a_node_template_to_the_input(String property, String nodeTemplateName, String inputId) throws Throwable {
        String fullUrl = String.format("/rest/topologies/%s/nodetemplates/%s/property/%s/input", Context.getInstance().getTopologyId(), nodeTemplateName,
                property);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("inputId", inputId));
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postUrlEncoded(fullUrl, nvps));
    }

    @Then("^I set the property \"([^\"]*)\" of a relationship \"([^\"]*)\" for the node template \"([^\"]*)\" to the input \"([^\"]*)\"$")
    public void I_set_the_property_of_a_relationship_for_the_node_template_to_the_input(String property, String relationshipTemplateId,
            String nodeTemplateName, String inputId)
            throws Throwable {
        String fullUrl = String.format("/rest/topologies/%s/nodetemplates/%s/relationship/%s/property/%s/input", Context.getInstance().getTopologyId(),
                nodeTemplateName, relationshipTemplateId, property);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("inputId", inputId));
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postUrlEncoded(fullUrl, nvps));
    }

    @Then("^I unset the property \"([^\"]*)\" of a relationship \"([^\"]*)\" for the node template \"([^\"]*)\"$")
    public void I_unset_the_property_of_a_relationship_for_the_node_template_to_the_input(String property, String relationshipTemplateId,
            String nodeTemplateName) throws Throwable {
        String fullUrl = String.format("/rest/topologies/%s/nodetemplates/%s/relationship/%s/property/%s/input", Context.getInstance().getTopologyId(),
                nodeTemplateName, relationshipTemplateId, property);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete(fullUrl));
    }

    @When("^I ask for the input candidate for the node template \"([^\"]*)\" and property \"([^\"]*)\"$")
    public void I_ask_for_the_input_candidate_for_the_node_template_and_property(String nodeTemplateId, String property) throws Throwable {
        String url = String.format("/rest/topologies/%s/nodetemplates/%s/property/%s/inputcandidats", Context.getInstance().getTopologyId(),
                nodeTemplateId, property);
        String restResult = Context.getRestClientInstance().get(url);
        List<String> candidates = JsonUtil.read(restResult, List.class).getData();
        Context.getInstance().buildEvaluationContext(candidates);
    }

    @When("^I ask for the input candidate for the node template \"([^\"]*)\" and property \"([^\"]*)\" of relationship \"([^\"]*)\"$")
    public void I_ask_for_the_input_candidate_for_the_node_template_and_property_of_relationship(String nodeTemplateId, String property, String relationshipId)
            throws Throwable {
        String url = String.format("/rest/topologies/%s/nodetemplates/%s/relationship/%s/property/%s/inputcandidats", Context.getInstance().getTopologyId(),
                nodeTemplateId, relationshipId, property);
        String restResult = Context.getRestClientInstance().get(url);
        List<String> candidates = JsonUtil.read(restResult, List.class).getData();
        Context.getInstance().buildEvaluationContext(candidates);
    }

    @When("^I ask for the input candidate for the node template \"([^\"]*)\" and property \"([^\"]*)\" of capability \"([^\"]*)\"$")
    public void I_ask_for_the_input_candidate_for_the_node_template_and_property_of_capability(String nodeTemplateId, String property, String capabilityId)
            throws Throwable {
        String url = String.format("/rest/topologies/%s/nodetemplates/%s/capability/%s/property/%s/inputcandidats", Context.getInstance().getTopologyId(),
                nodeTemplateId, capabilityId, property);
        String restResult = Context.getRestClientInstance().get(url);
        List<String> candidates = JsonUtil.read(restResult, List.class).getData();
        Context.getInstance().buildEvaluationContext(candidates);
    }

    @When("^I set the property \"([^\"]*)\" of capability \"([^\"]*)\" the node \"([^\"]*)\" as input property name \"([^\"]*)\"$")
    public void I_define_the_property_of_capability_the_node_as_input_property(String propertyId, String capabilityId, String nodeTemplateId, String inputId)
            throws Throwable {
        String url = String.format("/rest/topologies/%s/nodetemplates/%s/capability/%s/property/%s/input", Context.getInstance().getTopologyId(),
                nodeTemplateId, capabilityId, propertyId);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("inputId", inputId));
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postUrlEncoded(url, nvps));
    }

    @When("^I unset the property \"([^\"]*)\" of capability \"([^\"]*)\" the node \"([^\"]*)\" as input property$")
    public void I_unset_the_property_of_capability_the_node_as_input_property(String propertyId, String capabilityId, String nodeTemplateId)
            throws Throwable {
        String url = String.format("/rest/topologies/%s/nodetemplates/%s/capability/%s/property/%s/input", Context.getInstance().getTopologyId(),
                nodeTemplateId, capabilityId, propertyId);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete(url));
    }
}
