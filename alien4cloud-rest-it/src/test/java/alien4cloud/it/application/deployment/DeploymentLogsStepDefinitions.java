package alien4cloud.it.application.deployment;

import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;

import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.it.application.ApplicationStepDefinitions;
import alien4cloud.it.Context;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.paas.model.PaaSDeploymentLog;
import alien4cloud.rest.application.model.SearchLogRequest;
import alien4cloud.rest.utils.JsonUtil;

import cucumber.api.java.en.And;
import cucumber.api.java.en.When;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeploymentLogsStepDefinitions {

    @When("^I search for deployment logs$")
    public void I_search_for_deployment_logs () throws Throwable {
        String restUrl = "/rest/v1/deployment/logs/search";
        String response = Context.getRestClientInstance().postJSon(restUrl,"{\"from\":0,\"size\":10}");
        Context.getInstance().registerRestResponse(response);
    }

    @When("^I search for deployment logs for current deployment$")
    public void I_search_for_deployment_logs_for_deployment () throws Throwable {
        String restUrl = "/rest/v1/deployment/logs/search";
        String response = Context.getRestClientInstance().postJSon(restUrl, getCurrentDeploymentReq());
        Context.getInstance().registerRestResponse(response);
    }

    @When("^I search for deployment logs for non existing deployment$")
    public void I_search_for_deployment_logs_for_non_existing_deployment () throws Throwable {
        String restUrl = "/rest/v1/deployment/logs/search";
        String response = Context.getRestClientInstance().postJSon(restUrl,getInvalidDeploymentReq());
        Context.getInstance().registerRestResponse(response);
    }

    @When("^I search for deployment logs with time interval$")
    public void I_search_for_deployment_logs_with_time_interval () throws Throwable {
        String restUrl = "/rest/v1/deployment/logs/search";
        String response = Context.getRestClientInstance().postJSon(restUrl,getCurrentTimeReq());
        Context.getInstance().registerRestResponse(response);
    }

    @When("^I search for deployment logs in the future$")
    public void I_search_for_deployment_logs_in_the_future () throws Throwable {
        String restUrl = "/rest/v1/deployment/logs/search";
        String response = Context.getRestClientInstance().postJSon(restUrl,getFutureTimeReq());
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

    private String getCurrentDeploymentReq() throws IOException {
        SearchLogRequest req = new SearchLogRequest();
        Map<String,String[]> filter = new HashMap<String,String[]>();
        filter.put ("deploymentId", new String[]{getCurrentDeploymentId()});
        req.setFilters(filter);
        return JsonUtil.toString(req);
    }

    private String getInvalidDeploymentReq() throws IOException {
        SearchLogRequest req = new SearchLogRequest();
        Map<String,String[]> filter = new HashMap<String,String[]>();
        filter.put ("deploymentId", new String[]{"invalid"});
        req.setFilters(filter);
        return JsonUtil.toString(req);
    }

    private String getCurrentTimeReq() throws IOException {
        SearchLogRequest req = new SearchLogRequest();
        GregorianCalendar now = new GregorianCalendar();
        now.setTime(new Date());
        GregorianCalendar from = new GregorianCalendar();
        from.setTimeInMillis(now.getTimeInMillis() - 60000); // now - 1 mn
        GregorianCalendar to = new GregorianCalendar();
        to.setTimeInMillis(now.getTimeInMillis() + 60000); // now + 1 mn
        req.setFromDate(from.getTime());
        req.setToDate(to.getTime());
        return JsonUtil.toString(req);
    }

    private String getFutureTimeReq() throws IOException {
        SearchLogRequest req = new SearchLogRequest();
        GregorianCalendar now = new GregorianCalendar();
        now.setTime(new Date());
        GregorianCalendar from = new GregorianCalendar();
        from.setTimeInMillis(now.getTimeInMillis() + 600000); // now + 10 mn
        req.setFromDate(from.getTime());
        return JsonUtil.toString(req);
    }

    @And("^I should get some deployment logs$")
    public void I_should_get_some_deployment_logs() throws Throwable {
       String restResponse = Context.getInstance().getRestResponse();
       FacetedSearchResult searchResult = JsonUtil.read(restResponse, FacetedSearchResult.class).getData();
       Assert.assertNotEquals(searchResult.getTotalResults(), 0L);
    }

    @And("^I should get no deployment log$")
    public void I_should_get_no_deployment_log() throws Throwable {
       String restResponse = Context.getInstance().getRestResponse();
       FacetedSearchResult searchResult = JsonUtil.read(restResponse, FacetedSearchResult.class).getData();
       Assert.assertEquals(searchResult.getTotalResults(), 0L);
    }

    @And("^I should get current deployment logs$")
    public void I_should_get_current_deployment_logs() throws Throwable {
       String restResponse = Context.getInstance().getRestResponse();
       FacetedSearchResult searchResult = JsonUtil.read(restResponse, FacetedSearchResult.class).getData();
       Assert.assertNotEquals(searchResult.getTotalResults(), 0L);
       String depId = getCurrentDeploymentId();
       for (Object jsonData : searchResult.getData()) {
          PaaSDeploymentLog log = JsonUtil.readObject(JsonUtil.toString(jsonData), PaaSDeploymentLog.class);
          Assert.assertEquals(depId, log.getDeploymentId());
       }
    }
}
