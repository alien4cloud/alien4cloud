package alien4cloud.it.orchestrators;

import java.io.IOException;

import org.junit.Assert;

import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.it.Context;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.orchestrator.model.CreateOrchestratorRequest;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class OrchestratorsDefinitionsSteps {

    @When("^I create an orchestrator named \"([^\"]*)\" and plugin id \"([^\"]*)\" and bean name \"([^\"]*)\"$")
    public void I_create_an_orchestrator_named_and_plugin_id_and_bean_name(String name, String pluginId, String pluginBean) throws Throwable {
        CreateOrchestratorRequest orchestrator = new CreateOrchestratorRequest();
        orchestrator.setName(name);
        orchestrator.setPluginId(pluginId);
        orchestrator.setPluginBean(pluginBean);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/orchestrators", JsonUtil.toString(orchestrator)));
        RestResponse<String> idResponse = JsonUtil.read(Context.getInstance().getRestResponse(), String.class);
        Context.getInstance().registerOrchestrator(idResponse.getData(), name);
    }

    @When("^I list orchestrators$")
    public void I_list_orchestrators() throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/orchestrators"));
    }

    @Then("^Response should contains (\\d+) orchestrator$")
    public void Response_should_contains_orchestrator(int count) throws Throwable {
        RestResponse<GetMultipleDataResult> response = JsonUtil.read(Context.getInstance().getRestResponse(), GetMultipleDataResult.class);
        Assert.assertEquals(count, response.getData().getTotalResults());
    }

    @Then("^Response should contains an orchestrator with name \"([^\"]*)\"$")
    public void Response_should_contains_an_orchestrator_with_name(String name) throws Throwable {
        RestResponse<GetMultipleDataResult> response = JsonUtil.read(Context.getInstance().getRestResponse(), GetMultipleDataResult.class);
        boolean contains = false;
        for (Object cloudAsMap : response.getData().getData()) {
            Orchestrator orchestrator = JsonUtil.readObject(JsonUtil.toString(cloudAsMap), Orchestrator.class);
            if (name.equals(orchestrator.getName())) {
                contains = true;
            }
        }
        Assert.assertTrue(contains);
    }

    @When("^I delete an orchestrator with name \"([^\"]*)\"$")
    public void I_delete_an_orchestrator_with_name(String orchestratorName) throws Throwable {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete("/rest/orchestrators/" + orchestratorId));
    }

    @Given("^I enable the orchestrator \"([^\"]*)\"$")
    public void I_enable_the_orchestrator(String orchestratorName) throws IOException {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/orchestrators/" + orchestratorId + "/instance", "{}"));
    }

    @When("^I disable \"([^\"]*)\"$")
    public void I_disable(String orchestratorName) throws Throwable {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete("/rest/orchestrators/" + orchestratorId + "/instance"));
    }

    @When("^I update orchestrator name from \"([^\"]*)\" to \"([^\"]*)\"$")
    public void I_update_orchestrator_name_from_to(String oldName, String newName) throws Throwable {
        String orchestratorId = Context.getInstance().getOrchestratorId(oldName);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().putJSon("/rest/orchestrators/" + orchestratorId, newName));
    }

}
