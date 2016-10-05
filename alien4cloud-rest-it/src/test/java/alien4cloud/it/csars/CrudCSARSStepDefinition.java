package alien4cloud.it.csars;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.alien4cloud.tosca.model.CSARDependency;
import org.junit.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;

import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.it.Context;
import alien4cloud.model.common.Usage;
import alien4cloud.rest.component.SearchRequest;
import alien4cloud.rest.csar.CsarInfoDTO;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class CrudCSARSStepDefinition {

    private String CURRENT_CSAR_NAME;
    private String CURRENT_CSAR_VERSION;
    private final ObjectMapper jsonMapper = new ObjectMapper();

    @Given("^I have CSAR name \"([^\"]*)\" and version \"([^\"]*)\"$")
    public void I_have_CSAR_name_and_version(String csarName, String csarVersion) throws Throwable {
        CURRENT_CSAR_NAME = csarName;
        CURRENT_CSAR_VERSION = csarVersion;
    }

    @When("^I delete a CSAR with id \"([^\"]*)\"$")
    public void I_delete_a_CSAR_with_id(String csarId) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete("/rest/v1/csars/" + csarId));
        RestResponse<?> restResponse = JsonUtil.read(Context.getInstance().getRestResponse());
        Assert.assertNotNull(restResponse);
    }

    @Then("^I should have a CSAR with id \"([^\"]*)\"$")
    public boolean I_have_CSAR_created_with_id(String csarId) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/csars/" + csarId));
        CsarInfoDTO csarInfoDTO = JsonUtil.read(Context.getInstance().takeRestResponse(), CsarInfoDTO.class).getData();
        if (csarInfoDTO == null || csarInfoDTO.getCsar() == null) {
            return false;
        }
        Assert.assertNotNull(csarInfoDTO);
        Assert.assertEquals(csarInfoDTO.getCsar().getId(), csarId);
        return true;
    }

    @Then("^I have no CSAR created with id \"([^\"]*)\"$")
    public void I_have_no_CSAR_created_with_id(String csarId) throws Throwable {
        Assert.assertTrue(!I_have_CSAR_created_with_id(csarId));
    }

    @When("^I add a dependency with name \"([^\"]*)\" version \"([^\"]*)\" to the CSAR with name \"([^\"]*)\" version \"([^\"]*)\"$")
    public void I_add_a_dependency_with_name_version_to_the_CSAR_with_name_version(String dependencyName, String dependencyVersion, String csarName,
            String csarVersion) throws Throwable {
        CSARDependency dependency = new CSARDependency(dependencyName, dependencyVersion);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance()
                .postJSon("/rest/v1/csars/" + csarName + ":" + csarVersion + "-SNAPSHOT" + "/dependencies", JsonUtil.toString(dependency)));
    }

    @Then("^I have the CSAR \"([^\"]*)\" version \"([^\"]*)\" to contain a dependency to \"([^\"]*)\" version \"([^\"]*)\"$")
    public void I_have_the_CSAR_version_to_contain_a_dependency_to_version(String csarName, String csarVersion, String dependencyName, String dependencyVersion)
            throws Throwable {
        String response = Context.getRestClientInstance().get("/rest/v1/csars/" + csarName + ":" + csarVersion + "-SNAPSHOT");
        CsarInfoDTO csar = JsonUtil.read(response, CsarInfoDTO.class).getData();
        Assert.assertTrue(csar.getCsar().getDependencies().contains(new CSARDependency(dependencyName, dependencyVersion)));
    }

    @Then("^I should have a delete csar response with \"([^\"]*)\" related resources$")
    public void I_should_have_a_delete_csar_response_with_related_resources(String resourceCount) throws Throwable {
        RestResponse<?> restResponse = JsonUtil.read(Context.getInstance().getRestResponse());
        Assert.assertNotNull(restResponse);
        List<Usage> resultData = JsonUtil.toList(JsonUtil.toString(restResponse.getData()), Usage.class);
        Assert.assertEquals(Integer.parseInt(resourceCount), resultData.size());
    }

    @Given("^I can find (\\d+) CSAR$")
    public void i_can_find_CSAR(int expectedSize) throws Throwable {
        SearchRequest req = new SearchRequest(null, null, 0, 50, null);
        String jSon = jsonMapper.writeValueAsString(req);
        String response = Context.getRestClientInstance().postJSon("/rest/v1/csars/search", jSon);
        RestResponse<FacetedSearchResult> restResponse = JsonUtil.read(response, FacetedSearchResult.class);
        FacetedSearchResult searchResp = restResponse.getData();
        assertNotNull(searchResp);
        assertNotNull(searchResp.getData());
        assertEquals(expectedSize, searchResp.getData().length);
    }
}
