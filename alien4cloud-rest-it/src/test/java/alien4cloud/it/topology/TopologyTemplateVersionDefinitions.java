package alien4cloud.it.topology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Assert;

import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.it.Context;
import alien4cloud.model.templates.TopologyTemplateVersion;
import alien4cloud.rest.application.model.ApplicationVersionRequest;
import alien4cloud.rest.component.SearchRequest;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class TopologyTemplateVersionDefinitions {

    @When("^I create a new topology template version named \"([^\"]*)\"$")
    public void I_create_a_new_topology_template_version_named(String version) throws Throwable {
        ApplicationVersionRequest request = new ApplicationVersionRequest();
        request.setVersion(version);
        String topologyTemplateId = Context.getInstance().getTopologyTemplate().getId();
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postJSon("/rest/templates/" + topologyTemplateId + "/versions", JsonUtil.toString(request)));
        String versionId = JsonUtil.read(Context.getInstance().getRestResponse(), String.class).getData();
        Context.getInstance().registerTopologyTemplateVersionId(versionId);
    }

    @When("^I create a new topology template version named \"([^\"]*)\" based on the current version$")
    public void I_create_a_new_topology_template_version_named_based_on_the_current_version(String version) throws Throwable {
        ApplicationVersionRequest request = new ApplicationVersionRequest();
        request.setVersion(version);
        request.setTopologyId(Context.getInstance().getTopologyId());
        String topologyTemplateId = Context.getInstance().getTopologyTemplate().getId();
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postJSon("/rest/templates/" + topologyTemplateId + "/versions", JsonUtil.toString(request)));
        String versionId = JsonUtil.read(Context.getInstance().getRestResponse(), String.class).getData();
        Context.getInstance().registerTopologyTemplateVersionId(versionId);
        registerTopologyTemplateVersionTopology();
    }

    @Then("^the topology template named \"([^\"]*)\" should have (\\d+) versions$")
    public void the_topology_template_named_should_have_versions(String templateName, int versionCount) throws Throwable {
        String topologyTemplateId = getTopologyTemplateIdByName(templateName);
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setFrom(0);
        searchRequest.setSize(Integer.MAX_VALUE);
        String restResponse = Context.getRestClientInstance().postJSon("/rest/templates/" + topologyTemplateId + "/versions/search",
                JsonUtil.toString(searchRequest));
        GetMultipleDataResult<?> mdr = JsonUtil.read(restResponse, GetMultipleDataResult.class).getData();
        Assert.assertNotNull(mdr);
        assertEquals(versionCount, mdr.getTotalResults());
    }

    private String getTopologyTemplateIdByName(String templateName) throws Throwable {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setQuery(templateName);
        searchRequest.setFrom(0);
        searchRequest.setSize(Integer.MAX_VALUE);
        String restResponse = Context.getRestClientInstance().postJSon("/rest/templates/topology/search", JsonUtil.toString(searchRequest));
        FacetedSearchResult result = JsonUtil.read(restResponse, FacetedSearchResult.class).getData();
        Assert.assertNotNull(result);
        assertEquals(1, result.getTotalResults());
        Map<?, ?> map = (Map<?, ?>) result.getData()[0];
        Assert.assertTrue(map.containsKey("id"));
        return map.get("id").toString();
    }

    private String getTopologyTemplateVersionId(String topologyTemplateId, String version) throws Throwable {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setQuery(version);
        searchRequest.setFrom(0);
        searchRequest.setSize(Integer.MAX_VALUE);
        String templateVersionJson = Context.getRestClientInstance().postJSon("/rest/templates/" + topologyTemplateId + "/versions/search",
                JsonUtil.toString(searchRequest));
        GetMultipleDataResult<?> result = JsonUtil.read(templateVersionJson, GetMultipleDataResult.class).getData();
        assertNotNull(result);
        assertEquals(1, result.getTotalResults());
        Map<?, ?> ttv = (Map) result.getData()[0];
        assertNotNull(ttv);
        assertTrue(ttv.containsKey("id"));
        return ttv.get("id").toString();
    }

    private void registerTopologyTemplateVersionTopology() throws Throwable {
        String topologyTemplateId = Context.getInstance().getTopologyTemplate().getId();
        String versionId = Context.getInstance().getTopologyTemplateVersionId();
        String restResponse = Context.getRestClientInstance().get("/rest/templates/" + topologyTemplateId + "/versions/" + versionId);
        TopologyTemplateVersion result = JsonUtil.read(restResponse, TopologyTemplateVersion.class, Context.getJsonMapper()).getData();
        assertNotNull(result);
        Context.getInstance().registerTopologyId(result.getTopologyId());
    }

    @When("^I delete the topology template named \"([^\"]*)\" version \"([^\"]*)\"$")
    public void I_delete_the_topology_template_named_version(String templateName, String version) throws Throwable {
        String topologyTemplateId = getTopologyTemplateIdByName(templateName);
        String versionId = getTopologyTemplateVersionId(topologyTemplateId, version);
        String restResponse = Context.getRestClientInstance().delete("/rest/templates/" + topologyTemplateId + "/versions/" + versionId);
        Context.getInstance().registerRestResponse(restResponse);
    }

    @When("^I delete the topology template named \"([^\"]*)\"$")
    public void I_delete_the_topology_template_named(String templateName) throws Throwable {
        String topologyTemplateId = getTopologyTemplateIdByName(templateName);
        String restResponse = Context.getRestClientInstance().delete("/rest/templates/topology/" + topologyTemplateId);
        Context.getInstance().registerRestResponse(restResponse);
    }

}
