package alien4cloud.it.orchestrators;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;

import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.it.Context;
import alien4cloud.it.common.CommonStepDefinitions;
import alien4cloud.model.common.Usage;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.OrchestratorState;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.orchestrator.UpdateOrchestratorRequest;
import alien4cloud.rest.orchestrator.model.CreateOrchestratorRequest;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class OrchestratorsDefinitionsSteps {

    @When("^I (successfully\\s)?create an orchestrator named \"([^\"]*)\" and plugin id \"([^\"]*)\" and bean name \"([^\"]*)\"$")
    public void I_create_an_orchestrator_named_and_plugin_id_and_bean_name(String successfully, String name, String pluginId, String pluginBean)
            throws Throwable {
        CreateOrchestratorRequest orchestrator = new CreateOrchestratorRequest();
        orchestrator.setName(name);
        orchestrator.setPluginId(pluginId);
        orchestrator.setPluginBean(pluginBean);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/v1/orchestrators", JsonUtil.toString(orchestrator)));

        CommonStepDefinitions.validateIfNeeded(StringUtils.isNotBlank(successfully));

        RestResponse<String> idResponse = JsonUtil.read(Context.getInstance().getRestResponse(), String.class);
        Context.getInstance().registerOrchestrator(idResponse.getData(), name);
    }

    @When("^I (successfully\\s)?create an orchestrator named \"([^\"]*)\" and plugin name \"([^\"]*)\" and bean name \"([^\"]*)\"$")
    public void I_create_an_orchestrator_named_and_plugin_name_and_bean_name(String successfully, String name, String pluginName, String pluginBean)
            throws Throwable {
        I_create_an_orchestrator_named_and_plugin_id_and_bean_name(successfully, name, pluginName, pluginBean);
    }

    @When("^I list orchestrators$")
    public void I_list_orchestrators() throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/orchestrators"));
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
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete("/rest/v1/orchestrators/" + orchestratorId));
    }

    @Given("^I (successfully\\s)?enable the orchestrator \"([^\"]*)\"$")
    public void I_enable_the_orchestrator(String successfully, String orchestratorName) throws Throwable {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        String restResponse = Context.getRestClientInstance().postJSon("/rest/v1/orchestrators/" + orchestratorId + "/instance", "{}");
        Context.getInstance().registerRestResponse(restResponse);

        CommonStepDefinitions.validateIfNeeded(StringUtils.isNotBlank(successfully));
    }

    @Deprecated
    @When("^I disable \"([^\"]*)\"$")
    public void I_disable(String orchestratorName) throws Throwable {
        I_disable_the_orchestrator(orchestratorName);
    }

    @When("^I disable the orchestrator \"([^\"]*)\"$")
    public void I_disable_the_orchestrator(String orchestratorName) throws Throwable {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete("/rest/v1/orchestrators/" + orchestratorId + "/instance"));
    }

    @When("^I disable all orchestrators$")
    public void I_disable_all_orchestrators() throws Throwable {
        for (String orchestratorId : Context.getInstance().getOrchestratorIds()) {
            Context.getRestClientInstance().delete("/rest/v1/orchestrators/" + orchestratorId + "/instance");
        }
    }

    @When("^I update orchestrator name from \"([^\"]*)\" to \"([^\"]*)\"$")
    public void I_update_orchestrator_name_from_to(String oldName, String newName) throws Throwable {
        String orchestratorId = Context.getInstance().getOrchestratorId(oldName);
        UpdateOrchestratorRequest updateOrchestratorRequest = new UpdateOrchestratorRequest();
        updateOrchestratorRequest.setName(newName);
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().putJSon("/rest/v1/orchestrators/" + orchestratorId, JsonUtil.toString(updateOrchestratorRequest)));
    }

    @When("^I get the orchestrator named \"([^\"]*)\"$")
    public void I_get_the_orchestrator_named(String orchestratorName) throws Throwable {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/orchestrators/" + orchestratorId));

        // build eval context if possible
        String restResponse = Context.getInstance().getRestResponse();
        RestResponse<Orchestrator> response = JsonUtil.read(restResponse, Orchestrator.class, Context.getJsonMapper());
        if (response.getError() == null) {
            Context.getInstance().buildEvaluationContext(response.getData());
        }
    }

    @Then("^Response should contains the orchestrator with name \"([^\"]*)\" and state enabled \"([^\"]*)\"$")
    public void Response_should_contains_the_orchestrator_with_name_and_state_enabled(String orchestratorName, String isStateEnabled) throws Throwable {
        RestResponse<Orchestrator> orchestratorResponse = JsonUtil.read(Context.getInstance().getRestResponse(), Orchestrator.class);
        Assert.assertNotNull(orchestratorResponse.getData());
        Assert.assertEquals(orchestratorName, orchestratorResponse.getData().getName());
        Assert.assertEquals(Boolean.valueOf(isStateEnabled), orchestratorResponse.getData().getState() == OrchestratorState.CONNECTED);
    }

    @Then("^I should receive a RestResponse with a non-empty list of usages$")
    public void I_should_receive_a_RestResponse_with_a_non_empty_list_of_usages() throws Throwable {
        RestResponse<?> response = JsonUtil.read(Context.getInstance().getRestResponse(), Context.getJsonMapper());
        Assert.assertNotNull(response.getData());
        List<Usage> usages = JsonUtil.toList(JsonUtil.toString(response.getData()), Usage.class, Context.getJsonMapper());
        Assert.assertTrue(CollectionUtils.isNotEmpty(usages));
    }

}
