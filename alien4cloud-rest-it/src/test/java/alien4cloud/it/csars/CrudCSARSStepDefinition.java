package alien4cloud.it.csars;

import lombok.extern.slf4j.Slf4j;

import org.junit.Assert;

import alien4cloud.it.Context;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.rest.csar.CreateCsarRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.tosca.container.model.CSARDependency;
import alien4cloud.tosca.model.Csar;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

@Slf4j
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
        String response = JsonUtil.read(Context.getInstance().getRestResponse(), String.class).getData();
        Assert.assertNull(response);
    }

    @Then("^I have CSAR created with id \"([^\"]*)\"$")
    public boolean I_have_CSAR_created_with_id(String csarId) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/csars/" + csarId));
        Csar csar = JsonUtil.read(Context.getInstance().takeRestResponse(), Csar.class).getData();
        if (csar == null) {
            return false;
        }
        Assert.assertNotNull(csar);
        Assert.assertEquals(csar.getId(), csarId);
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
        Csar csar = JsonUtil.read(response, Csar.class).getData();
        Assert.assertTrue(csar.getDependencies().contains(new CSARDependency(dependencyName, dependencyVersion)));
    }

    @When("^I run the test for this snapshot CSAR on cloud \"([^\"]*)\"$")
    public void I_run_the_test_for_this_snapshot_CSAR_on_cloud(String cloudName) throws Throwable {
        String cloudId = Context.getInstance().getCloudId(cloudName);
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().get("/rest/csars/" + CURRENT_CSAR_NAME + "/version/" + CURRENT_CSAR_VERSION + "/cloudid/" + cloudId));
        RestResponse<?> response = JsonUtil.read(Context.getInstance().getRestResponse());
        if (response.getData() != null) {
            Context.getInstance().registerTopologyDeploymentId(response.getData().toString());
        }
    }

    @And("^I should not have active deployment for this CSAR$")
    public void I_should_not_have_active_deployment_for_this_CSAR() throws Throwable {
        RestResponse<Deployment> dep = JsonUtil.read(Context.getRestClientInstance().get(
                "/rest/csars/" + CURRENT_CSAR_NAME + ":" + CURRENT_CSAR_VERSION + "/active-deployment"), Deployment.class);
        Assert.assertNull(dep.getData());
    }

    @And("^I should have active deployment for this CSAR$")
    public void I_should_have_active_deployment_for_this_CSAR() throws Throwable {
        RestResponse<Deployment> dep = JsonUtil.read(Context.getRestClientInstance().get(
                "/rest/csars/" + CURRENT_CSAR_NAME + ":" + CURRENT_CSAR_VERSION + "/active-deployment"), Deployment.class);
        Assert.assertNotNull(dep.getData());
        Assert.assertNotNull(dep.getData().getId());
        Assert.assertNotNull(dep.getData().getCloudId());
        Assert.assertEquals(CURRENT_CSAR_NAME + ":" + CURRENT_CSAR_VERSION, dep.getData().getSourceId());
        Assert.assertEquals(CURRENT_CSAR_NAME, dep.getData().getSourceName());
    }
}
