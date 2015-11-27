package alien4cloud.it.orchestrators;

import java.util.Map;

import alien4cloud.it.Context;
import alien4cloud.model.orchestrators.OrchestratorConfiguration;
import alien4cloud.paas.exception.NotSupportedException;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;

import com.google.common.collect.Maps;

import cucumber.api.java.en.And;
import cucumber.api.java.en.When;

public class OrchestratorsConfigurationDefinitionsSteps {

    @And("^I update cloudify (\\d+) manager's url to the OpenStack's jenkins management server for orchestrator with name \"([^\"]*)\"$")
    public void I_update_cloudify_manager_s_url_to_the_OpenStack_s_jenkins_management_server_for_cloud_with_name(int cloudifyVersion, String cloudName)
            throws Throwable {
        switch (cloudifyVersion) {
            case 3:
                I_update_cloudify_manager_s_url_to_with_login_and_password_for_cloud_with_name(3, Context.getInstance().getCloudify3ManagerUrl(), Context
                                .getInstance().getAppProperty("openstack.cfy3.manager_user"), Context.getInstance().getAppProperty("openstack.cfy3.manager_password"),
                        cloudName);
                break;
            default:
                throw new NotSupportedException("Version " + cloudifyVersion + " of provider cloudify is not supported");
        }
    }

    @When("^I get configuration for orchestrator \"([^\"]*)\"$")
    public void I_get_configuration_for_orchestrator(String orchestratorName) throws Throwable {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/orchestrators/" + orchestratorId + "/configuration"));
        RestResponse<OrchestratorConfiguration> orchestratorConfigurationResponse = JsonUtil.read(Context.getInstance().getRestResponse(), OrchestratorConfiguration.class);
        Map<String, Object> configuration = (Map<String, Object>)orchestratorConfigurationResponse.getData().getConfiguration();
        Context.getInstance().setOrchestratorConfiguration(configuration);
    }


//
//    @When("^I update configuration for orchestrator \"([^\"]*)\"$")
//    public void I_update_configuration_for_orchestrator(String orchestratorName) throws Throwable {
//        String orchestratorId = Context.getInstance().getorchestratorId(orchestratorName);
//
//        ProviderConfig config = new ProviderConfig();
//        config.setFirstArgument("firstArgument");
//        config.setSecondArgument("secondArgument");
//
//        Context.getInstance().registerRestResponse(
//                Context.getRestClientInstance().putJSon("/rest/orchestrators/" + orchestratorId + "/configuration", JsonUtil.toString(config)));
//    }

    @And("^I update cloudify (\\d+) manager's url to \"([^\"]*)\" with login \"([^\"]*)\" and password \"([^\"]*)\" for orchestrator with name \"([^\"]*)\"$")
    public void I_update_cloudify_manager_s_url_to_with_login_and_password_for_cloud_with_name(int cloudifyVersion, String cloudifyUrl, String login, String password, String orchestratorName)  throws Throwable {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        Map<String, Object> config = Context.getInstance().getOrchestratorConfiguration();
        switch (cloudifyVersion) {
            case 3:
                config.put("url", cloudifyUrl);
                break;
            default:
                throw new IllegalArgumentException("Cloudify version not supported " + cloudifyVersion);
        }
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().putJSon("/rest/orchestrators/" + orchestratorId + "/configuration", JsonUtil.toString(config)));

    }

//    @When("^I update configuration for orchestrator \"([^\"]*)\" with wrong configuration$")
//    public void I_update_configuration_for_orchestrator_with_wrong_configuration(String orchestratorName) throws Throwable {
//        String orchestratorId = Context.getInstance().getCloudId(orchestratorName);
//        Context.getInstance()
//                .registerRestResponse(Context.getRestClientInstance().putJSon("/rest/orchestrators/" + orchestratorId + "/configuration", JsonUtil.toString("")));
//    }
//
//    @Then("^The orchestrator configuration should be null$")
//    public void The_orchestrator_configuration_should_be_null() throws Throwable {
//        RestResponse<ProviderConfig> response = JsonUtil.read(Context.getInstance().getRestResponse(), ProviderConfig.class);
//        assertNull(response.getData());
//    }
//
//    @Then("^The orchestrator configuration should not be null$")
//    public void The_orchestrator_configuration_should_not_be_null() throws Throwable {
//        RestResponse<ProviderConfig> response = JsonUtil.read(Context.getInstance().getRestResponse(), ProviderConfig.class);
//        assertNotNull(response.getData());
//    }


}
