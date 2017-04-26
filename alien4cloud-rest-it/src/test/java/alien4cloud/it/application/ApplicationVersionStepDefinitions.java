package alien4cloud.it.application;

import static alien4cloud.it.Context.getRestClientInstance;
import static alien4cloud.it.utils.TestUtils.nullable;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;

import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.it.Context;
import alien4cloud.it.common.CommonStepDefinitions;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.rest.application.model.CreateApplicationVersionRequest;
import alien4cloud.rest.application.model.UpdateApplicationVersionRequest;
import alien4cloud.rest.model.FilteredSearchRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class ApplicationVersionStepDefinitions {
    public static ApplicationVersion CURRENT_VERSION;

    @When("^I get the application version for application \"([^\"]*)\" with id \"([^\"]*)\"$")
    public void getApplicationVersion(String applicationId, String versionId) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/applications/" + applicationId + "/versions/" + versionId));
        // Try to register the application version (this works only if the operations is successful)
        try {
            CURRENT_VERSION = null;
            CURRENT_VERSION = JsonUtil.read(Context.getInstance().getRestResponse(), ApplicationVersion.class).getData();
        } catch (IOException e) {
            // Registration is optional
        }
    }

    @And("^The application version should have an application topology version with version \"([^\"]*)\"$")
    public void checkApplicationTopologyVersion(String topologyVersion) throws Throwable {
        Assert.assertNotNull(CURRENT_VERSION);
        Assert.assertNotNull(CURRENT_VERSION.getTopologyVersions());
        Assert.assertNotNull(CURRENT_VERSION.getTopologyVersions().get(topologyVersion));
    }

    @And("^The application version should not have an application topology version with version \"([^\"]*)\"$")
    public void checkApplicationTopologyVersionNotExists(String topologyVersion) throws Throwable {
        Assert.assertNotNull(CURRENT_VERSION);
        Assert.assertNotNull(CURRENT_VERSION.getTopologyVersions());
        Assert.assertNull(CURRENT_VERSION.getTopologyVersions().get(topologyVersion));
    }

    @When("^I (successfully\\s)?create an application version for application \"([^\"]*)\" with version \"([^\"]*)\", description \"([^\"]*)\", topology template id \"([^\"]*)\" and previous version id \"([^\"]*)\"$")
    public void stepCreateApplicationVersion(String successfully, String applicationId, String version, String description, String topologyTemplateId,
            String fromVersionId) throws Throwable {
        createApplicationVersion(applicationId, nullable(version), nullable(description), nullable(topologyTemplateId), nullable(fromVersionId));
        CommonStepDefinitions.validateIfNeeded(StringUtils.isNotBlank(successfully));
    }

    private void createApplicationVersion(String applicationId, String version, String description, String topologyTemplateId, String fromVersionId)
            throws Throwable {
        CreateApplicationVersionRequest request = new CreateApplicationVersionRequest(version, description, topologyTemplateId, fromVersionId);
        Context.getInstance()
                .registerRestResponse(getRestClientInstance().postJSon("/rest/v1/applications/" + applicationId + "/versions", JsonUtil.toString(request)));
    }

    @When("^I delete an application version for application \"([^\"]*)\" with version id \"([^\"]*)\"$")
    public void deleteApplicationVersion(String applicationId, String versionId) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete("/rest/v1/applications/" + applicationId + "/versions/" + versionId));
    }

    @When("^I search for application versions$")
    public void I_search_for_application_versions() throws Throwable {
        Application app = Context.getInstance().getApplication();
        FilteredSearchRequest request = new FilteredSearchRequest();
        request.setFrom(0);
        request.setSize(10);
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postJSon("/rest/v1/applications/" + app.getId() + "/versions/search", JsonUtil.toString(request)));
    }

    @Then("^I should receive (\\d+) application versions in the search result$")
    public void I_should_receive_application_versions_in_the_search_result(int nbAppVerions) throws Throwable {
        RestResponse<FacetedSearchResult> response = JsonUtil.read(Context.getInstance().getRestResponse(), FacetedSearchResult.class);
        Assert.assertNull(response.getError());
        Assert.assertNotNull(response.getData());
        Assert.assertTrue(response.getData().getTotalResults() == nbAppVerions);
    }

    @When("^I update the application version for application \"([^\"]*)\" version id \"([^\"]*)\" with new version \"([^\"]*)\" and description \"([^\"]*)\"$")
    public void updateApplicationVersionStep(String applicationId, String versionId, String newVersion, String description) throws Throwable {
        updateApplicationVersion(nullable(applicationId), nullable(versionId), nullable(newVersion), nullable(description));
    }

    private void updateApplicationVersion(String applicationId, String versionId, String newVersion, String description) throws Throwable {
        UpdateApplicationVersionRequest request = new UpdateApplicationVersionRequest();
        request.setVersion(newVersion);
        request.setDescription(description);
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().putJSon("/rest/v1/applications/" + applicationId + "/versions/" + versionId, JsonUtil.toString(request)));

    }
}
