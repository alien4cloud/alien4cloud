package alien4cloud.it.csars;

import java.util.List;

import org.junit.Assert;

import alien4cloud.it.Context;
import alien4cloud.it.utils.JsonTestUtil;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.rest.csar.CreateCsarRequest;
import alien4cloud.rest.csar.CsarInfoDTO;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.topology.CsarRelatedResourceDTO;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class CrudCSARSStepDefinition {

    private String CURRENT_CSAR_NAME;
    private String CURRENT_CSAR_VERSION;

    @Given("^I have CSAR name \"([^\"]*)\" and version \"([^\"]*)\"$")
    public void I_have_CSAR_name_and_version(String csarName, String csarVersion) throws Throwable {
        CURRENT_CSAR_NAME = csarName;
        CURRENT_CSAR_VERSION = csarVersion;
    }

    @When("^I create a CSAR with name \"([^\"]*)\" and version \"([^\"]*)\"$")
    public void I_create_a_CSAR_with_name_and_version(String csarName, String csarVersion) throws Throwable {
        CreateCsarRequest request = new CreateCsarRequest(csarName, csarVersion, null);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/csars/", JsonUtil.toString(request)));
        Context.getInstance().registerCsarId(JsonUtil.read(Context.getInstance().getRestResponse(), String.class).getData());
    }

    @When("^I create a CSAR$")
    public void I_create_a_CSAR() throws Throwable {
        CreateCsarRequest request = new CreateCsarRequest(CURRENT_CSAR_NAME, CURRENT_CSAR_VERSION, null);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/csars/", JsonUtil.toString(request)));
    }

    @When("^I delete a CSAR with id \"([^\"]*)\"$")
    public void I_delete_a_CSAR_with_id(String csarId) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete("/rest/csars/" + csarId));
        RestResponse<?> restResponse = JsonUtil.read(Context.getInstance().getRestResponse());
        Assert.assertNotNull(restResponse);
    }

    @Then("^I have CSAR created with id \"([^\"]*)\"$")
    public boolean I_have_CSAR_created_with_id(String csarId) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/csars/" + csarId));
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
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postJSon("/rest/csars/" + csarName + ":" + csarVersion + "-SNAPSHOT" + "/dependencies",
                        JsonUtil.toString(dependency)));
    }

    @Then("^I have the CSAR \"([^\"]*)\" version \"([^\"]*)\" to contain a dependency to \"([^\"]*)\" version \"([^\"]*)\"$")
    public void I_have_the_CSAR_version_to_contain_a_dependency_to_version(String csarName, String csarVersion, String dependencyName, String dependencyVersion)
            throws Throwable {
        String response = Context.getRestClientInstance().get("/rest/csars/" + csarName + ":" + csarVersion + "-SNAPSHOT");
        CsarInfoDTO csar = JsonUtil.read(response, CsarInfoDTO.class).getData();
        Assert.assertTrue(csar.getCsar().getDependencies().contains(new CSARDependency(dependencyName, dependencyVersion)));
    }

    @When("^I run the test for this snapshot CSAR on cloud \"([^\"]*)\"$")
    public void I_run_the_test_for_this_snapshot_CSAR_on_cloud(String cloudName) throws Throwable {
        String cloudId = Context.getInstance().getCloudId(cloudName);
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().get("/rest/csars/" + CURRENT_CSAR_NAME + "/version/" + CURRENT_CSAR_VERSION + "/cloudid/" + cloudId));
        RestResponse<?> response = JsonTestUtil.read(Context.getInstance().getRestResponse());
        if (response.getData() != null) {
            Context.getInstance().registerTopologyDeploymentId(response.getData().toString());
        }
    }

    @And("^I should not have active deployment for this CSAR$")
    public void I_should_not_have_active_deployment_for_this_CSAR() throws Throwable {
        RestResponse<Deployment> dep = JsonUtil.read(
                Context.getRestClientInstance().get("/rest/csars/" + CURRENT_CSAR_NAME + ":" + CURRENT_CSAR_VERSION + "/active-deployment"), Deployment.class);
        Assert.assertNull(dep.getData());
    }

    @And("^I should have active deployment for this CSAR$")
    public void I_should_have_active_deployment_for_this_CSAR() throws Throwable {
        RestResponse<Deployment> dep = JsonUtil.read(
                Context.getRestClientInstance().get("/rest/csars/" + CURRENT_CSAR_NAME + ":" + CURRENT_CSAR_VERSION + "/active-deployment"), Deployment.class);
        Assert.assertNotNull(dep.getData());
        Assert.assertNotNull(dep.getData().getId());
//        Assert.assertNotNull(dep.getData().getCloudId());
        Assert.fail("Fix test");
        Assert.assertEquals(CURRENT_CSAR_NAME + ":" + CURRENT_CSAR_VERSION, dep.getData().getSourceId());
        Assert.assertEquals(CURRENT_CSAR_NAME, dep.getData().getSourceName());
    }

    @Then("^I should have a delete csar response with \"([^\"]*)\" related resources$")
    public void I_should_have_a_delete_csar_response_with_related_resources(String resourceCount) throws Throwable {
        RestResponse<?> restResponse = JsonUtil.read(Context.getInstance().getRestResponse());
        Assert.assertNotNull(restResponse);
        List<CsarRelatedResourceDTO> resultData = JsonUtil.toList(JsonUtil.toString(restResponse.getData()), CsarRelatedResourceDTO.class);
        Assert.assertEquals(resultData.size(), Integer.parseInt(resourceCount));
    }
}
