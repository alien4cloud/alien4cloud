package alien4cloud.it.service;

import static alien4cloud.it.Context.getRestClientInstance;
import static alien4cloud.it.utils.TestUtils.nullAsString;
import static alien4cloud.it.utils.TestUtils.nullable;

import org.apache.commons.lang.StringUtils;

import alien4cloud.it.Context;
import alien4cloud.it.common.CommonStepDefinitions;
import alien4cloud.rest.service.model.CreateManagedServiceResourceRequest;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.java.en.When;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ManagedServiceStepDefinitions {

    @When("^I (successfully\\s)?create a service with name \"([^\"]*)\", from the application \"([^\"]*)\", environment \"([^\"]*)\"$")
    public void iCreateAServiceWithNameFromTheApplicationEnvironement(String successfully, String serviceName, String applicationName, String environmentName)
            throws Throwable {
        String applicationId = Context.getInstance().getApplicationId(applicationName);
        String environmentId = Context.getInstance().getApplicationEnvironmentId(applicationName, environmentName);
        CreateManagedServiceResourceRequest request = new CreateManagedServiceResourceRequest(nullable(serviceName));
        Context.getInstance().registerRestResponse(getRestClientInstance()
                .postJSon(String.format("/rest/applications/%s/environments/%s/services", applicationId, environmentId), JsonUtil.toString(request)));

        CommonStepDefinitions.validateIfNeeded(StringUtils.isNotBlank(successfully));

        try {
            ServiceStepDefinitions.LAST_CREATED_ID = JsonUtil.read(Context.getInstance().getRestResponse(), String.class).getData();
            Context.getInstance().registerService(ServiceStepDefinitions.LAST_CREATED_ID, serviceName);
        } catch (Throwable t) {
        }
    }

    @When("^I get service related to the application \"([^\"]*)\", environment \"([^\"]*)\"$")
    public void iGetServiceRelatedToTheApplicationEnvironement(String applicationName, String environmentName) throws Throwable {
        String applicationId = Context.getInstance().getApplicationId(applicationName);
        String environmentId = Context.getInstance().getApplicationEnvironmentId(applicationName, environmentName);
        Context.getInstance().registerRestResponse(
                getRestClientInstance().get(String.format("/rest/applications/%s/environments/%s/services", applicationId, nullAsString(environmentId))));

        ServiceStepDefinitions.registerServiceResultForSPEL();
    }
}
