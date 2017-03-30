package alien4cloud.it.application;

import java.io.IOException;

import org.junit.Assert;

import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.it.Context;
import alien4cloud.it.utils.TestUtils;
import alien4cloud.rest.application.model.ApplicationEnvironmentDTO;
import alien4cloud.rest.application.model.UpdateTopologyVersionForEnvironmentRequest;
import alien4cloud.rest.model.FilteredSearchRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.utils.AlienConstants;
import cucumber.api.java.en.And;
import cucumber.api.java.en.When;

public class ApplicationEnvironmentStepDefinitions {
    public static ApplicationEnvironmentDTO[] ALL_ENVIRONMENTS;
    public static ApplicationEnvironmentDTO CURRENT_ENVIRONMENT_DTO;

    @When("^I get all application environments for application \"([^\"]*)\"$")
    public void getAllApplicationEnvironments(String applicationId) throws Throwable {
        FilteredSearchRequest request = new FilteredSearchRequest();
        request.setFrom(0);
        request.setSize(AlienConstants.MAX_ES_SEARCH_SIZE); // This is actually the maximum search size in a4c (1000 by default)
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postJSon("/rest/v1/applications/" + applicationId + "/environments/search", JsonUtil.toString(request)));

        try {
            RestResponse<GetMultipleDataResult> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), GetMultipleDataResult.class);
            ALL_ENVIRONMENTS = new ApplicationEnvironmentDTO[restResponse.getData().getData().length];
            TestUtils.convert(restResponse.getData(), ALL_ENVIRONMENTS, ApplicationEnvironmentDTO.class);
            CURRENT_ENVIRONMENT_DTO = ALL_ENVIRONMENTS[0];
        } catch (IOException e) {
            // Registration is optional
        }
    }

    @And("^I have (\\d+) environments$")
    public void checkEnvNameAndVersion(int count) {
        Assert.assertNotNull(ALL_ENVIRONMENTS);
        Assert.assertEquals(count, ALL_ENVIRONMENTS.length);
    }

    @And("^Current environment name is \"([^\"]*)\" and version is \"([^\"]*)\"$")
    public void checkEnvNameAndVersion(String environmentName, String environmentVersion) {
        Assert.assertNotNull(CURRENT_ENVIRONMENT_DTO);
        Assert.assertEquals(environmentName, CURRENT_ENVIRONMENT_DTO.getName());
        Assert.assertEquals(environmentVersion, CURRENT_ENVIRONMENT_DTO.getCurrentVersionName());
    }

    @And("^I update the topology version of the application environment named \"([^\"]*)\" to \"([^\"]*)\"$")
    public void iUpdateTheTopologyVersionOfTheApplicationEnvironmentNamedTo(String environmentName, String newTopologyVersion) throws Throwable {
        iUpdateTheTopologyVersionOfTheApplicationEnvironmentNamedToWithInputsFromEnvironment(environmentName, newTopologyVersion, null);
    }

    @And("^I update the topology version of the application environment named \"([^\"]*)\" to \"([^\"]*)\" with inputs from environment \"([^\"]*)\"$")
    public void iUpdateTheTopologyVersionOfTheApplicationEnvironmentNamedToWithInputsFromEnvironment(String environmentName, String newTopologyVersion,
            String environmentToCopyInputs) throws Throwable {
        UpdateTopologyVersionForEnvironmentRequest request = new UpdateTopologyVersionForEnvironmentRequest(newTopologyVersion,
                Context.getInstance().getApplicationEnvironmentId(Context.getInstance().getApplication().getName(), environmentToCopyInputs));
        Context.getInstance()
                .registerRestResponse(
                        Context.getRestClientInstance()
                                .putJSon(
                                        "/rest/v1/applications/" + Context.getInstance().getApplication().getId()
                                                + "/environments/" + Context.getInstance().getApplicationEnvironmentId(
                                                        Context.getInstance().getApplication().getName(), environmentName)
                                                + "/topology-version",
                                        JsonUtil.toString(request)));

    }
}