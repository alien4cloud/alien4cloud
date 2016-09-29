package alien4cloud.it.topology;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation;
import org.alien4cloud.tosca.editor.operations.inputs.RenameInputOperation;
import org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodeCapabilityPropertyAsInputOperation;
import org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodePropertyAsInputOperation;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.apache.commons.collections4.MapUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Assert;

import com.fasterxml.jackson.databind.JavaType;

import alien4cloud.it.Context;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.topology.TopologyDTO;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class InputPropertiesStepDefinitions {

    @When("^I define the property \"([^\"]*)\" of the node \"([^\"]*)\" as input property$")
    public void I_define_the_property_of_the_node_as_input_property(String inputId, String nodeName) throws Throwable {
        // get the topologyTDO to have the real type of the propertyDefinition
        PropertyDefinition propertyDefinition = getPropertyDefinition(nodeName, inputId);
        AddInputOperation addInputOperation = new AddInputOperation();
        addInputOperation.setPropertyDefinition(propertyDefinition);
        addInputOperation.setInputName(inputId);
        EditorStepDefinitions.do_i_execute_the_operation(addInputOperation);

        SetNodePropertyAsInputOperation setNodePropertyAsInputOperation = new SetNodePropertyAsInputOperation();
        setNodePropertyAsInputOperation.setInputName(inputId);
        setNodePropertyAsInputOperation.setPropertyName(inputId);
        setNodePropertyAsInputOperation.setNodeName(nodeName);
        EditorStepDefinitions.do_i_execute_the_operation(setNodePropertyAsInputOperation);
        EditorStepDefinitions.do_i_save_the_topology();
    }

    private PropertyDefinition getPropertyDefinition(String nodeName, String propertyName) throws Throwable {
        PropertyDefinition propDef = null;
        String url = String.format("/rest/v1/topologies/%s", Context.getInstance().getTopologyId());
        String response = Context.getRestClientInstance().get(url);
        TopologyDTO topologyDTO = JsonUtil.read(response, TopologyDTO.class, Context.getJsonMapper()).getData();
        NodeTemplate template = MapUtils.getObject(topologyDTO.getTopology().getNodeTemplates(), nodeName);
        if (template != null) {
            NodeType nodeType = MapUtils.getObject(topologyDTO.getNodeTypes(), template.getType());
            if (nodeType != null) {
                propDef = MapUtils.getObject(nodeType.getProperties(), propertyName);
            }
        }
        if (propDef == null) {
            throw new NullPointerException(
                    "The property definition is required for node " + nodeName + " and property " + propertyName + ", please check your cucumber scenario.");
        }
        return propDef;
    }

    @Given("^I define the capability \"(.*?)\" property \"(.*?)\" of the node \"(.*?)\" as input property$")
    public void i_define_the_capability_property_of_the_node_of_typeId_as_input_property(String capabilityName, String propertyName, String nodeName)
            throws Throwable {
        String url = String.format("/rest/v1/topologies/%s", Context.getInstance().getTopologyId());
        String response = Context.getRestClientInstance().get(url);
        TopologyDTO topologyDTO = JsonUtil.read(response, TopologyDTO.class, Context.getJsonMapper()).getData();
        NodeTemplate template = MapUtils.getObject(topologyDTO.getTopology().getNodeTemplates(), nodeName);
        Capability capability = template.getCapabilities().get(capabilityName);

        CapabilityType capabilityType = topologyDTO.getCapabilityTypes().get(capability.getType());
        PropertyDefinition propertyDefinition = capabilityType.getProperties().get(propertyName);

        String fullUrl = String.format("/rest/v1/topologies/%s/inputs/%s", Context.getInstance().getTopologyId(), propertyName);
        String json = JsonUtil.toString(propertyDefinition);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon(fullUrl, json));
    }

    @When("^I define the property \"([^\"]*)\" of the node \"([^\"]*)\" of typeId \"([^\"]*)\" as input property$")
    public void I_define_the_property_of_the_node_as_of_typeId_as_input_property(String inputId, String nodeName, String typeId) throws Throwable {
        // get the component to use the right property definition
        String componentResponse = Context.getRestClientInstance().get("/rest/v1/components/" + typeId);
        RestResponse<NodeType> componentResult = JsonUtil.read(componentResponse, NodeType.class, Context.getJsonMapper());
        PropertyDefinition propertyDefinition = componentResult.getData().getProperties().get(inputId);
        String fullUrl = String.format("/rest/v1/topologies/%s/inputs/%s", Context.getInstance().getTopologyId(), inputId);
        String json = JsonUtil.toString(propertyDefinition);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon(fullUrl, json));
    }

    @When("^I remove the input property \"([^\"]*)\"$")
    public void I_remove_the_input_property(String inputId) throws Throwable {
        String fullUrl = String.format("/rest/v1/topologies/%s/inputs/%s", Context.getInstance().getTopologyId(), inputId);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete(fullUrl));
    }

    @When("^I unset the property \"([^\"]*)\" of the node \"([^\"]*)\" as input property$")
    public void I_unset_the_property_of_the_node_as_input_property(String propertyId, String nodeName) throws Throwable {
        String url = String.format("/rest/v1/topologies/%s/nodetemplates/%s/property/%s/input", Context.getInstance().getTopologyId(), nodeName, propertyId);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete(url));
    }

    @When("^I rename the input \"([^\"]*)\" to \"([^\"]*)\"$")
    public void I_rename_the_input_to(String oldInputId, String newInputId) throws Throwable {
        RenameInputOperation renameInputOperation = new RenameInputOperation();
        renameInputOperation.setInputName(oldInputId);
        renameInputOperation.setNewInputName(newInputId);
        EditorStepDefinitions.do_i_execute_the_operation(renameInputOperation);
        EditorStepDefinitions.do_i_save_the_topology();
    }

    @Then("^The topology should have the property \"([^\"]*)\" defined as input property$")
    public void The_topology_should_have_the_property_of_the_node_defined_as_input_property(String inputId) throws Throwable {
        String response = Context.getRestClientInstance().get("/rest/v1/topologies/" + Context.getInstance().getTopologyId());
        JavaType restResponseType = Context.getJsonMapper().getTypeFactory().constructParametricType(RestResponse.class, TopologyDTO.class);
        TopologyDTO topologyDTO = ((RestResponse<TopologyDTO>) Context.getJsonMapper().readValue(response, restResponseType)).getData();

        Map<String, PropertyDefinition> inputProperties = topologyDTO.getTopology().getInputs();
        Assert.assertNotNull(inputProperties);
        PropertyDefinition inputPropertieDefinition = inputProperties.get(inputId);
        Assert.assertNotNull(inputPropertieDefinition);
    }

    @Then("^The topology should not have the property \"([^\"]*)\" defined as input property$")
    public void The_topology_should_not_have_the_property_defined_as_input_property(String inputId) throws Throwable {
        TopologyDTO topologyDTO = JsonUtil.read(Context.getRestClientInstance().get("/rest/v1/topologies/" + Context.getInstance().getTopologyId()),
                TopologyDTO.class, Context.getJsonMapper()).getData();
        Map<String, PropertyDefinition> inputProperties = topologyDTO.getTopology().getInputs();
        Assert.assertFalse(inputProperties.containsKey(inputId));
    }

    @Then("^I associate the property \"([^\"]*)\" of a node template \"([^\"]*)\" to the input \"([^\"]*)\"$")
    public void I_associate_the_property_of_a_node_template_to_the_input(String property, String nodeTemplateName, String inputId) throws Throwable {
        SetNodePropertyAsInputOperation setNodePropertyAsInputOperation = new SetNodePropertyAsInputOperation();
        setNodePropertyAsInputOperation.setInputName(inputId);
        setNodePropertyAsInputOperation.setPropertyName(property);
        setNodePropertyAsInputOperation.setNodeName(nodeTemplateName);
        EditorStepDefinitions.do_i_execute_the_operation(setNodePropertyAsInputOperation);
        EditorStepDefinitions.do_i_save_the_topology();
    }

    @Then("^I set the property \"([^\"]*)\" of a relationship \"([^\"]*)\" for the node template \"([^\"]*)\" to the input \"([^\"]*)\"$")
    public void I_set_the_property_of_a_relationship_for_the_node_template_to_the_input(String property, String relationshipTemplateId, String nodeTemplateName,
            String inputId) throws Throwable {
        String fullUrl = String.format("/rest/v1/topologies/%s/nodetemplates/%s/relationship/%s/property/%s/input", Context.getInstance().getTopologyId(),
                nodeTemplateName, relationshipTemplateId, property);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("inputId", inputId));
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postUrlEncoded(fullUrl, nvps));
    }

    @Then("^I unset the property \"([^\"]*)\" of a relationship \"([^\"]*)\" for the node template \"([^\"]*)\"$")
    public void I_unset_the_property_of_a_relationship_for_the_node_template_to_the_input(String property, String relationshipTemplateId,
            String nodeTemplateName) throws Throwable {
        String fullUrl = String.format("/rest/v1/topologies/%s/nodetemplates/%s/relationship/%s/property/%s/input", Context.getInstance().getTopologyId(),
                nodeTemplateName, relationshipTemplateId, property);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete(fullUrl));
    }

    @When("^I ask for the input candidate for the node template \"([^\"]*)\" and property \"([^\"]*)\"$")
    public void I_ask_for_the_input_candidate_for_the_node_template_and_property(String nodeTemplateId, String property) throws Throwable {
        String url = String.format("/rest/v1/topologies/%s/nodetemplates/%s/property/%s/inputcandidats", Context.getInstance().getTopologyId(), nodeTemplateId,
                property);
        String restResult = Context.getRestClientInstance().get(url);
        List<String> candidates = JsonUtil.read(restResult, List.class).getData();
        Context.getInstance().buildEvaluationContext(candidates);
    }

    @When("^I ask for the input candidate for the node template \"([^\"]*)\" and property \"([^\"]*)\" of relationship \"([^\"]*)\"$")
    public void I_ask_for_the_input_candidate_for_the_node_template_and_property_of_relationship(String nodeTemplateId, String property, String relationshipId)
            throws Throwable {
        String url = String.format("/rest/v1/topologies/%s/nodetemplates/%s/relationship/%s/property/%s/inputcandidats", Context.getInstance().getTopologyId(),
                nodeTemplateId, relationshipId, property);
        String restResult = Context.getRestClientInstance().get(url);
        List<String> candidates = JsonUtil.read(restResult, List.class).getData();
        Context.getInstance().buildEvaluationContext(candidates);
    }

    @When("^I ask for the input candidate for the node template \"([^\"]*)\" and property \"([^\"]*)\" of capability \"([^\"]*)\"$")
    public void I_ask_for_the_input_candidate_for_the_node_template_and_property_of_capability(String nodeTemplateId, String property, String capabilityId)
            throws Throwable {
        String url = String.format("/rest/v1/topologies/%s/nodetemplates/%s/capability/%s/property/%s/inputcandidats", Context.getInstance().getTopologyId(),
                nodeTemplateId, capabilityId, property);
        String restResult = Context.getRestClientInstance().get(url);
        List<String> candidates = JsonUtil.read(restResult, List.class).getData();
        Context.getInstance().buildEvaluationContext(candidates);
    }

    @When("^I set the property \"([^\"]*)\" of capability \"([^\"]*)\" the node \"([^\"]*)\" as input property name \"([^\"]*)\"$")
    public void I_define_the_property_of_capability_the_node_as_input_property(String propertyId, String capabilityId, String nodeTemplateId, String inputId)
            throws Throwable {
        SetNodeCapabilityPropertyAsInputOperation operation = new SetNodeCapabilityPropertyAsInputOperation();
        operation.setNodeName(nodeTemplateId);
        operation.setPropertyName(propertyId);
        operation.setCapabilityName(capabilityId);
        operation.setInputName(inputId);
        EditorStepDefinitions.do_i_execute_the_operation(operation);
        EditorStepDefinitions.do_i_save_the_topology();
    }

    @When("^I unset the property \"([^\"]*)\" of capability \"([^\"]*)\" the node \"([^\"]*)\" as input property$")
    public void I_unset_the_property_of_capability_the_node_as_input_property(String propertyId, String capabilityId, String nodeTemplateId) throws Throwable {
        String url = String.format("/rest/v1/topologies/%s/nodetemplates/%s/capability/%s/property/%s/input", Context.getInstance().getTopologyId(),
                nodeTemplateId, capabilityId, propertyId);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete(url));
    }
}
