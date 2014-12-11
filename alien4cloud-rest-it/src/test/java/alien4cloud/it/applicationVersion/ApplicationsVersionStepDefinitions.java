package alien4cloud.it.applicationVersion;

import java.util.UUID;

import org.junit.Assert;

import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.it.Context;
import alien4cloud.model.application.Application;
import alien4cloud.rest.application.UpdateApplicationVersionRequest;
import alien4cloud.rest.component.SearchRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class ApplicationsVersionStepDefinitions {
    
    @Given("^I create an application version with version \"([^\"]*)\"$")
    public void I_create_an_application_version_with_version(String version) throws Throwable {
        Application app = Context.getInstance().getApplication();
        UpdateApplicationVersionRequest request = new UpdateApplicationVersionRequest();
        request.setApplicationId(app.getId());
        request.setVersion(version);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/applications/" + app.getId() + "/versions" , JsonUtil.toString(request)));
        String currentApplicationVersionId = JsonUtil.read(Context.getInstance().getRestResponse(), String.class).getData();
        Context.getInstance().registerApplicationVersionId(version, currentApplicationVersionId);
    }
    
    @Given("^I delete an application version with name \"([^\"]*)\"$")
    public void I_delete_an_application_version_with_name(String versionName) throws Throwable {
        Application app = Context.getInstance().getApplication();
        String currentApplicationVersionId = Context.getInstance().getApplicationVersionId(versionName);
        if (currentApplicationVersionId == null) {
            currentApplicationVersionId = UUID.randomUUID().toString();
        }
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().delete("/rest/applications/" + app.getId() + "/versions/" + currentApplicationVersionId));
    }

    @When("^I search for application versions$")
    public void I_search_for_application_versions() throws Throwable {
        Application app = Context.getInstance().getApplication();
        SearchRequest request = new SearchRequest();
        request.setFrom(0);
        request.setSize(10);
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postJSon("/rest/applications/" + app.getId() + "/versions/search", JsonUtil.toString(request)));
    }

    @Then("^I should receive (\\d+) application versions in the search result$")
    public void I_should_receive_application_versions_in_the_search_result(int nbAppVerions) throws Throwable {
        RestResponse<FacetedSearchResult> response = JsonUtil.read(Context.getInstance().getRestResponse(), FacetedSearchResult.class);
        Assert.assertNull(response.getError());
        Assert.assertNotNull(response.getData());
        Assert.assertTrue(response.getData().getTotalResults() == nbAppVerions);
    }

    @Given("^I update an application version with version \"([^\"]*)\" to \"([^\"]*)\"$")
    public void I_update_an_application_version_with_version(String oldNameVersion, String newNameVersion) throws Throwable {
        Application app = Context.getInstance().getApplication();
        String currentApplicationVersionId = Context.getInstance().getApplicationVersionId(oldNameVersion);
        UpdateApplicationVersionRequest request = new UpdateApplicationVersionRequest();
        request.setApplicationId(app.getId());
        request.setVersion(newNameVersion);
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().putJSon("/rest/applications/" + app.getId() + "/versions/" + currentApplicationVersionId,
                        JsonUtil.toString(request)));
    }
}
