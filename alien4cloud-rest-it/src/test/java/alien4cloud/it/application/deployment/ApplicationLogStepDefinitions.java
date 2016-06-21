package alien4cloud.it.application.deployment;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;

import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.it.Context;
import alien4cloud.it.application.ApplicationStepDefinitions;
import alien4cloud.paas.model.PaaSDeploymentLog;
import alien4cloud.rest.application.model.SearchLogRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.SortConfiguration;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.DataTable;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;

public class ApplicationLogStepDefinitions {

    @Then("^I should receive log entries that containing$")
    public void iShouldReceiveLogEntriesThatContaining(DataTable logEntries) throws Throwable {
        RestResponse<FacetedSearchResult> searchResult = JsonUtil.read(Context.getInstance().getRestResponse(), FacetedSearchResult.class);
        Assert.assertNull(searchResult.getError());
        Assert.assertNotNull(searchResult.getData());
        Assert.assertNotNull(searchResult.getData().getData());
        Object[] logsFound = searchResult.getData().getData();
        for (int i = 0; i < logsFound.length; i++) {
            PaaSDeploymentLog logFound = JsonUtil.toObject(logsFound[i], PaaSDeploymentLog.class);
            Assert.assertEquals(logEntries.raw().get(i).get(0), logFound.getType());
            Assert.assertEquals(logEntries.raw().get(i).get(1), logFound.getContent());
        }
    }

    @Given("^I search for log of the current application$")
    public void iSearchForLogOfTheCurrentApplication() throws Throwable {
        search(null, null, null, null);
    }

    private void search(Map<String, String[]> filters, Date fromDate, Date toDate, SortConfiguration sortConfiguration) throws IOException {
        String appId = ApplicationStepDefinitions.CURRENT_APPLICATION.getId();
        SearchLogRequest searchLogRequest = new SearchLogRequest(null, 0, Integer.MAX_VALUE, fromDate, toDate, sortConfiguration, filters);
        String environmentId = Context.getInstance().getApplicationEnvironmentId(ApplicationStepDefinitions.CURRENT_APPLICATION.getName(), "Environment");
        String restUrl = String.format("/rest/v1/applications/%s/environments/%s/logs/search", appId, environmentId);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon(restUrl, JsonUtil.toString(searchLogRequest)));
    }

    private void searchByType(String type, Date fromDate, Date toDate, SortConfiguration sortConfiguration) throws IOException {
        Map<String, String[]> filters = new HashMap<>();
        filters.put("type", new String[]{type});
        search(filters, fromDate, toDate, sortConfiguration);
    }

    @Given("^I search for log of the current application of type \"([^\"]*)\"$")
    public void iSearchForLogOfTheCurrentApplicationOfType(String type) throws Throwable {
        searchByType(type, null, null, null);
    }

    @Given("^I search for log of the current application of type \"([^\"]*)\" and order by \"([^\"]*)\" in \"([^\"]*)\" order$")
    public void iSearchForLogOfTheCurrentApplicationOfTypeAndOrderByInOrder(String type, String orderBy, String sortOrder) throws Throwable {
        SortConfiguration sortConfiguration = new SortConfiguration(orderBy, sortOrder.equals("ascending"));
        searchByType(type, null, null, sortConfiguration);
    }

    @And("^I sleep for (\\d+) seconds so that indices are fully refreshed$")
    public void iSleepForSecondsSoThatIndicesAreFullyRefreshed(int sleepTime) throws Throwable {
        Thread.sleep(sleepTime * 1000L);
    }
}
