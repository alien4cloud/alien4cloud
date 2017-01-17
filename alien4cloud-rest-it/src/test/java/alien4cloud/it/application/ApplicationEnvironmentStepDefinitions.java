package alien4cloud.it.application;

import java.io.IOException;

import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.it.Context;
import alien4cloud.it.utils.TestUtils;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.rest.application.model.ApplicationEnvironmentDTO;
import alien4cloud.rest.component.SearchRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.utils.AlienConstants;
import cucumber.api.java.en.And;
import cucumber.api.java.en.When;
import org.junit.Assert;

import static alien4cloud.it.Context.getRestClientInstance;

public class ApplicationEnvironmentStepDefinitions {
    public static ApplicationEnvironmentDTO[] ALL_ENVIRONMENTS;
    public static ApplicationEnvironmentDTO CURRENT_ENVIRONMENT_DTO;

    @When("^I get all application environments for application \"([^\"]*)\"$")
    public void getAllApplicationEnvironments(String applicationId) throws Throwable {
        SearchRequest request = new SearchRequest();
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
}