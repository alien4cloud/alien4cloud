package alien4cloud.it.application.deployment;

import java.io.IOException;

import org.apache.http.message.BasicNameValuePair;
import org.junit.Assert;

import com.google.common.collect.Lists;

import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.it.application.ApplicationStepDefinitions;
import alien4cloud.it.Context;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.runtime.Execution;
import alien4cloud.rest.utils.JsonUtil;

import cucumber.api.java.en.And;
import cucumber.api.java.en.When;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExecutionStepDefinitions {

    private String EXECUTION_ID = "undefined";

    @When("^I search for executions$")
    public void I_search_for_executions () throws Throwable {
        Thread.sleep(2000); // wait for events
        String restUrl = "/rest/v1/executions/search";
        String response = Context.getRestClientInstance().get(restUrl);
        Context.getInstance().registerRestResponse(response);
    }

    @When("^I search for executions for current deployment$")
    public void I_search_for_executions_for_deployment () throws Throwable {
        Thread.sleep(2000); // wait for events
        String restUrl = "/rest/v1/executions/search";
        String response = Context.getRestClientInstance().getUrlEncoded(restUrl, Lists
                .newArrayList(new BasicNameValuePair("deploymentId", getCurrentDeploymentId())));
        Context.getInstance().registerRestResponse(response);
    }

    @When("^I search for executions for non existing deployment$")
    public void I_search_for_executions_for_non_existing_deployment () throws Throwable {
        Thread.sleep(2000); // wait for events
        String restUrl = "/rest/v1/executions/search";
        String response = Context.getRestClientInstance().getUrlEncoded(restUrl, Lists
                .newArrayList(new BasicNameValuePair("deploymentId", "nonexisting")));
        Context.getInstance().registerRestResponse(response);
    }
    @When("^I search for current execution$")
    public void I_search_for_execution () throws Throwable {
        Thread.sleep(2000); // wait for events
        String restUrl = "/rest/v1/executions/search";
        String response = Context.getRestClientInstance().getUrlEncoded(restUrl, Lists
                .newArrayList(new BasicNameValuePair("query", EXECUTION_ID)));
        Context.getInstance().registerRestResponse(response);
    }

    @When("^I search for non existing execution$")
    public void I_search_for_non_existing_execution () throws Throwable {
        Thread.sleep(2000); // wait for events
        String restUrl = "/rest/v1/executions/search";
        String response = Context.getRestClientInstance().getUrlEncoded(restUrl, Lists
                .newArrayList(new BasicNameValuePair("query", "nonexisting")));
        Context.getInstance().registerRestResponse(response);
    }

    @When("^I get current execution$")
    public void I_get_execution () throws Throwable {
        Thread.sleep(2000); // wait for events
        String restUrl = "/rest/v1/executions/" + EXECUTION_ID;
        String response = Context.getRestClientInstance().get(restUrl);
        Context.getInstance().registerRestResponse(response);
    }

    @When("^I get non existing execution$")
    public void I_get_non_existing_execution () throws Throwable {
        Thread.sleep(2000); // wait for events
        String restUrl = "/rest/v1/executions/nonexisting";
        String response = Context.getRestClientInstance().get(restUrl);
        Context.getInstance().registerRestResponse(response);
    }

    private String getCurrentDeploymentId() throws IOException {
        String applicationName = ApplicationStepDefinitions.CURRENT_APPLICATION.getName();
        Deployment deployment = JsonUtil
                .read(Context.getRestClientInstance()
                        .get("/rest/v1/applications/" + Context.getInstance().getApplicationId(applicationName) + "/environments/"
                                + Context.getInstance().getDefaultApplicationEnvironmentId(applicationName) + "/active-deployment"),
                        Deployment.class)
                .getData();
        return deployment.getId();
    }

    @And("^I should get some executions$")
    public void I_should_get_some_executions() throws Throwable {
       String restResponse = Context.getInstance().getRestResponse();
       FacetedSearchResult searchResult = JsonUtil.read(restResponse, FacetedSearchResult.class).getData();
       Assert.assertNotEquals(searchResult.getTotalResults(), 0L);
       Object jsonData = searchResult.getData()[0];
       Execution exec = JsonUtil.readObject(JsonUtil.toString(jsonData), Execution.class);
       EXECUTION_ID = exec.getId();
    }

    @And("^I should get current deployment executions$")
    public void I_should_get_current_deployment_executions() throws Throwable {
       String restResponse = Context.getInstance().getRestResponse();
       FacetedSearchResult searchResult = JsonUtil.read(restResponse, FacetedSearchResult.class).getData();
       Assert.assertNotEquals(searchResult.getTotalResults(), 0L);
       String depId = getCurrentDeploymentId();
       for (Object jsonData : searchResult.getData()) {
          Execution exec = JsonUtil.readObject(JsonUtil.toString(jsonData), Execution.class);
          Assert.assertEquals(depId, exec.getDeploymentId());
       }
    }

    @And("^I should get no execution$")
    public void I_should_get_no_execution() throws Throwable {
       String restResponse = Context.getInstance().getRestResponse();
       FacetedSearchResult searchResult = JsonUtil.read(restResponse, FacetedSearchResult.class).getData();
       Assert.assertEquals(searchResult.getTotalResults(), 0L);
    }

    @And("^I should find current execution$")
    public void I_should_find_current_execution() throws Throwable {
       String restResponse = Context.getInstance().getRestResponse();
       FacetedSearchResult searchResult = JsonUtil.read(restResponse, FacetedSearchResult.class).getData();
       Assert.assertEquals(searchResult.getTotalResults(), 1L);
       Object jsonData = searchResult.getData()[0];
       Execution exec = JsonUtil.readObject(JsonUtil.toString(jsonData), Execution.class);
       Assert.assertEquals(EXECUTION_ID, exec.getId());
    }

    @And("^I should get current execution$")
    public void I_should_get_current_execution() throws Throwable {
       String restResponse = Context.getInstance().getRestResponse();
       Execution searchResult = JsonUtil.read(restResponse, Execution.class).getData();
       Assert.assertEquals(searchResult.getId(), EXECUTION_ID);
    }
}
