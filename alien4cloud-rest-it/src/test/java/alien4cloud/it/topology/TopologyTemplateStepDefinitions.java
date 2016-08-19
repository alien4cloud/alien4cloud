package alien4cloud.it.topology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.elasticsearch.common.collect.Maps;
import org.junit.Assert;

import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.it.Context;
import alien4cloud.it.Entry;
import alien4cloud.it.common.CommonStepDefinitions;
import alien4cloud.model.templates.TopologyTemplate;
import alien4cloud.model.templates.TopologyTemplateVersion;
import alien4cloud.rest.component.SearchRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.template.CreateTopologyTemplateRequest;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.utils.ReflectionUtil;
import cucumber.api.DataTable;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class TopologyTemplateStepDefinitions {

    private CommonStepDefinitions commonSteps = new CommonStepDefinitions();

    @When("^I create a new topology template with name \"([^\"]*)\" and description \"([^\"]*)\"$")
    public void I_create_a_new_topology_template_with_name_and_description(String topologyTemplateName, String topologyTemplateDesc) throws Throwable {

        CreateTopologyTemplateRequest ttRequest = new CreateTopologyTemplateRequest();
        ttRequest.setDescription(topologyTemplateDesc);
        ttRequest.setName(topologyTemplateName);

        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/v1/templates/topology", JsonUtil.toString(ttRequest)));
        String topologyTemplateId = JsonUtil.read(Context.getInstance().getRestResponse(), String.class).getData();

        // recover the created template to register it
        String templateTopologyJson = Context.getRestClientInstance().get("/rest/v1/templates/topology/" + topologyTemplateId);
        TopologyTemplate template = JsonUtil.read(templateTopologyJson, TopologyTemplate.class).getData();

        if (template != null) {
            Context.getInstance().registerTopologyTemplate(template);
        }
    }

    public static TopologyTemplateVersion getLatestTopologyTemplateVersion(String topologyTemplateId) throws IOException {
        String templateVersionJson = Context.getRestClientInstance().get("/rest/v1/templates/" + topologyTemplateId + "/versions/");
        TopologyTemplateVersion ttv = JsonUtil.read(templateVersionJson, TopologyTemplateVersion.class).getData();
        return ttv;
    }

    @Then("^I can get and register the topology for the last version of the registered topology template$")
    public void I_can_get_the_last_version_for_the_registered_topology_template() throws Throwable {
        TopologyTemplate topologyTemplate = Context.getInstance().getTopologyTemplate();
        TopologyTemplateVersion ttv = getLatestTopologyTemplateVersion(topologyTemplate.getId());
        assertNotNull(ttv);
        assertNotNull(ttv.getTopologyId());
        Context.getInstance().registerTopologyId(ttv.getTopologyId());
    }

    @Given("^I create a new topology template with name \"([^\"]*)\" and description \"([^\"]*)\" and node templates$")
    public void I_create_a_new_topology_template_with_name_and_description_and_node_templates(String topologyTemplateName, String topologyTemplateDesc,
            DataTable nodeTemplates) throws Throwable {

        // create the topology
        I_create_a_new_topology_template_with_name_and_description(topologyTemplateName, topologyTemplateDesc);
        I_can_get_the_last_version_for_the_registered_topology_template();

        EditorStepDefinitions.do_i_get_the_current_topology();

        // add all specified node template to a specific topology (from Application or Topology template)
        for (List<String> row : nodeTemplates.raw()) {
            Map<String, String> operationMap = Maps.newHashMap();
            operationMap.put("type", AddNodeOperation.class.getName());
            operationMap.put("nodeName", row.get(0));
            operationMap.put("indexedNodeTypeId", row.get(1));

            EditorStepDefinitions.do_i_execute_the_operation(operationMap);
        }
        // Save the topology
        EditorStepDefinitions.do_i_save_the_topology();

        assertEquals(EditorStepDefinitions.TOPOLOGY_DTO.getTopology().getNodeTemplates().size(), nodeTemplates.raw().size());
    }

    @Then("^The RestResponse should contain a topology template id$")
    public void The_RestResponse_should_contain_a_topology_template_id() throws Throwable {
        String topologyTemplateId = JsonUtil.read(Context.getInstance().getRestResponse(), String.class).getData();
        assertNotNull(topologyTemplateId);
    }

    @Then("^I can get this newly created topology template$")
    public void I_can_get_this_newly_created_topology_template() throws Throwable {
        TopologyTemplate topologyTemplate = Context.getInstance().getTopologyTemplate();
        assertNotNull(topologyTemplate);
    }

    @Given("^I have created a new topology template with name \"([^\"]*)\" and description \"([^\"]*)\"$")
    public void I_have_created_a_new_topology_template_with_name_and_description(String topologyTemplateName, String topologyTemplateDesc) throws Throwable {
        I_create_a_new_topology_template_with_name_and_description(topologyTemplateName, topologyTemplateDesc);
        commonSteps.I_should_receive_a_RestResponse_with_no_error();
    }

    @When("^I delete the newly created topology template$")
    public void I_delete_the_newly_created_topology_template() throws Throwable {

        TopologyTemplate topologyTemplate = Context.getInstance().getTopologyTemplate();
        assertNotNull(topologyTemplate);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete("/rest/v1/templates/topology/" + topologyTemplate.getId()));
    }

    @When("^I delete the topology template with name \"([^\"]*)\"$")
    public void I_delete_topology_template(String topologyTemplateName) throws Throwable {
        String topologyTemplateId = getTopologyTemplateIdFromName(topologyTemplateName);
        assertNotNull(topologyTemplateId);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete("/rest/v1/templates/topology/" + topologyTemplateId));
    }

    public static String getTopologyTemplateIdFromName(String topologyTemplateName) throws Throwable {
        SearchRequest templateWithNameSearchRequest = new SearchRequest();
        Map<String, String[]> filters = Maps.newHashMap();
        filters.put("name", new String[] { topologyTemplateName });
        templateWithNameSearchRequest.setFilters(filters);
        templateWithNameSearchRequest.setFrom(0);
        templateWithNameSearchRequest.setSize(1);
        String response = Context.getRestClientInstance().postJSon("/rest/v1/templates/topology/search", JsonUtil.toString(templateWithNameSearchRequest));
        RestResponse<FacetedSearchResult> restResponse = JsonUtil.read(response, FacetedSearchResult.class);
        Assert.assertEquals(1, restResponse.getData().getData().length);
        Map<String, Object> singleResult = (Map<String, Object>) restResponse.getData().getData()[0];
        String templateId = (String) singleResult.get("id");
        return templateId;
    }

    @Then("^The related topology shouldn't exist anymore$")
    public void The_related_topology_shouldn_t_exist_anymore() throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/topologies/" + Context.getInstance().getTopologyId()));
        commonSteps.I_should_receive_a_RestResponse_with_an_error_code(504);
    }

    @When("^I update the topology template \"([^\"]*)\" fields:$")
    public void I_update_the_topology_template_fields(String name, List<Entry> fileds) throws Throwable {
        Map<String, String> fieldsMap = Maps.newHashMap();
        for (Entry field : fileds) {
            fieldsMap.put(field.getName(), field.getValue());
        }
        TopologyTemplate topologyTemplate = Context.getInstance().getTopologyTemplate();
        assertNotNull(topologyTemplate);
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().putJSon("/rest/v1/templates/topology/" + topologyTemplate.getId(), JsonUtil.toString(fieldsMap)));
    }

    @And("^The topology template should have its \"([^\"]*)\" set to \"([^\"]*)\"$")
    public void The_topology_template_should_have_its_set_to(String fieldName, String fieldValue) throws Throwable {
        TopologyTemplate topologyTemplate = Context.getInstance().getTopologyTemplate();
        String response = Context.getRestClientInstance().get("/rest/v1/templates/topology/" + topologyTemplate.getId());
        TopologyTemplate topologyTemplateUpdated = JsonUtil.read(response, TopologyTemplate.class).getData();
        assertNotNull(topologyTemplateUpdated);
        assertEquals(fieldValue, ReflectionUtil.getPropertyValue(topologyTemplateUpdated, fieldName).toString());
    }

    @Given("^I expose the template as type \"([^\"]*)\"$")
    public void I_expose_the_template_as_type(String type) throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();
        assertNotNull(topologyId);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("elementId", type));
        Context.getInstance()
                .registerRestResponse(Context.getRestClientInstance().putUrlEncoded("/rest/v1/topologies/" + topologyId + "/substitutions/type", nvps));
    }

    @Given("^I expose the capability \"([^\"]*)\" for the node \"([^\"]*)\"$")
    public void I_expose_the_capability_for_the_node(String capabilityName, String nodeName) throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();
        assertNotNull(topologyId);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("nodeTemplateName", nodeName));
        nvps.add(new BasicNameValuePair("capabilityId", capabilityName));
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().putUrlEncoded("/rest/v1/topologies/" + topologyId + "/substitutions/capabilities/" + capabilityName, nvps));
    }

    @Given("^I rename the exposed capability \"([^\"]*)\" to \"([^\"]*)\"$")
    public void I_rename_the_exposed_capability_to(String capabilityName, String newName) throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();
        assertNotNull(topologyId);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("newCapabilityId", newName));
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postUrlEncoded("/rest/v1/topologies/" + topologyId + "/substitutions/capabilities/" + capabilityName, nvps));
    }

    @Given("^I expose the requirement \"([^\"]*)\" for the node \"([^\"]*)\"$")
    public void I_expose_the_requirement_for_the_node(String requirementName, String nodeName) throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();
        assertNotNull(topologyId);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("nodeTemplateName", nodeName));
        nvps.add(new BasicNameValuePair("requirementId", requirementName));
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().putUrlEncoded("/rest/v1/topologies/" + topologyId + "/substitutions/requirements/" + requirementName, nvps));
    }

    @Given("^I rename the exposed requirement \"([^\"]*)\" to \"([^\"]*)\"$")
    public void I_rename_the_exposed_requirement_to(String requirementName, String newName) throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();
        assertNotNull(topologyId);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("newRequirementId", newName));
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postUrlEncoded("/rest/v1/topologies/" + topologyId + "/substitutions/requirements/" + requirementName, nvps));
    }

}
