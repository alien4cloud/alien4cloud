package alien4cloud.it.topology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Map;

import org.elasticsearch.common.collect.Maps;
import org.junit.Assert;

import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.it.Context;
import alien4cloud.it.Entry;
import alien4cloud.it.common.CommonStepDefinitions;
import alien4cloud.it.utils.JsonTestUtil;
import alien4cloud.model.templates.TopologyTemplate;
import alien4cloud.rest.component.SearchRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.template.CreateTopologyTemplateRequest;
import alien4cloud.rest.topology.NodeTemplateRequest;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.topology.TopologyDTO;
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

        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/templates/topology", JsonUtil.toString(ttRequest)));
        String topologyTemplateId = JsonUtil.read(Context.getInstance().getRestResponse(), String.class).getData();
        // assertNotNull(topologyTemplateId);

        // recover the created template to register it
        String templateTopologyJson = Context.getRestClientInstance().get("/rest/templates/topology/" + topologyTemplateId);
        TopologyTemplate template = JsonUtil.read(templateTopologyJson, TopologyTemplate.class).getData();
        // assertNotNull(template);

        if (template != null) {
            Context.getInstance().registerTopologyTemplate(template);
            Context.getInstance().registerTopologyId(template.getTopologyId());
        }
    }

    @Given("^I create a new topology template with name \"([^\"]*)\" and description \"([^\"]*)\" and node templates$")
    public void I_create_a_new_topology_template_with_name_and_description_and_node_templates(String topologyTemplateName, String topologyTemplateDesc,
            DataTable nodeTemplates) throws Throwable {

        // create the topology
        I_create_a_new_topology_template_with_name_and_description(topologyTemplateName, topologyTemplateDesc);

        // add all specified nodetemplate to a specific topology (from Application or Topology template)
        for (List<String> row : nodeTemplates.raw()) {
            NodeTemplateRequest req = new NodeTemplateRequest(row.get(0), row.get(1));
            String nodeTypeJson = Context.getInstance().getJsonMapper().writeValueAsString(req);
            String topologyId = Context.getInstance().getTopologyId();
            Context.getInstance().registerRestResponse(
                    Context.getRestClientInstance().postJSon("/rest/topologies/" + topologyId + "/nodetemplates", nodeTypeJson));
        }

        // Created topology should have a node template count == count(nodeTemplates)
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/topologies/" + Context.getInstance().getTopologyId()));
        TopologyDTO topologyTemplateBase = JsonTestUtil.read(Context.getInstance().getRestResponse(), TopologyDTO.class).getData();

        assertEquals(topologyTemplateBase.getTopology().getNodeTemplates().size(), nodeTemplates.raw().size());

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
        assertNotNull(topologyTemplate.getTopologyId());
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
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete("/rest/templates/topology/" + topologyTemplate.getId()));
    }

    @When("^I delete the topology template with name \"([^\"]*)\"$")
    public void I_delete_topology_template(String topologyTemplateName) throws Throwable {
        String topologyTemplateId = getTopologyTemplateIdFromName(topologyTemplateName);
        assertNotNull(topologyTemplateId);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete("/rest/templates/topology/" + topologyTemplateId));
    }

    public static String getTopologyTemplateIdFromName(String topologyTemplateName) throws Throwable {
        SearchRequest templateWithNameSearchRequest = new SearchRequest();
        Map<String, String[]> filters = Maps.newHashMap();
        filters.put("name", new String[] { topologyTemplateName });
        templateWithNameSearchRequest.setFilters(filters);
        templateWithNameSearchRequest.setFrom(0);
        templateWithNameSearchRequest.setSize(1);
        String response = Context.getRestClientInstance().postJSon("/rest/templates/topology/search", JsonUtil.toString(templateWithNameSearchRequest));
        RestResponse<FacetedSearchResult> restResponse = JsonUtil.read(response, FacetedSearchResult.class);
        Assert.assertEquals(1, restResponse.getData().getData().length);
        Map<String, Object> singleResult = (Map<String, Object>) restResponse.getData().getData()[0];
        String templateId = (String) singleResult.get("id");
        return templateId;
    }

    @Then("^The related topology shouldn't exist anymore$")
    public void The_related_topology_shouldn_t_exist_anymore() throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/topologies/" + Context.getInstance().getTopologyId()));
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
                Context.getRestClientInstance().putJSon("/rest/templates/topology/" + topologyTemplate.getId(), JsonUtil.toString(fieldsMap)));
    }

    @And("^The topology template should have its \"([^\"]*)\" set to \"([^\"]*)\"$")
    public void The_topology_template_should_have_its_set_to(String fieldName, String fieldValue) throws Throwable {
        TopologyTemplate topologyTemplate = Context.getInstance().getTopologyTemplate();
        String response = Context.getRestClientInstance().get("/rest/templates/topology/" + topologyTemplate.getId());
        TopologyTemplate topologyTemplateUpdated = JsonUtil.read(response, TopologyTemplate.class).getData();
        assertNotNull(topologyTemplateUpdated);
        assertEquals(fieldValue, ReflectionUtil.getPropertyValue(topologyTemplateUpdated, fieldName).toString());
    }
}
