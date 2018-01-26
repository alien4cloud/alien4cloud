package alien4cloud.it.runtime;

import alien4cloud.deployment.model.SecretProviderConfigurationAndCredentials;
import alien4cloud.it.Context;
import alien4cloud.it.application.ApplicationStepDefinitions;
import alien4cloud.it.utils.DataTableUtils;
import alien4cloud.model.secret.SecretProviderConfiguration;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.DataTable;
import cucumber.api.java.en.When;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WorkflowStepDefinition {

    @When("^I launch the workflow \"([^\"]*)\" in the environment view after the deployment using the secret provider \"([^\"]*)\" with the following secret credentials:$")
    public void iLaunchTheWorkflowInTheEnvironmentViewAfterTheDeploymentWithTheFollowingSecretCredentials(String workflowName, String pluginName, DataTable table) throws Throwable {
        String path = "/rest/v1/applications/" + ApplicationStepDefinitions.CURRENT_APPLICATION.getId() + "/environments/"
                + Context.getInstance().getDefaultApplicationEnvironmentId(ApplicationStepDefinitions.CURRENT_APPLICATION.getName()) + "/workflows/"
                + workflowName;
        SecretProviderConfigurationAndCredentials secretProviderConfigurationAndCredentials = new SecretProviderConfigurationAndCredentials();
        SecretProviderConfiguration secretProviderConfiguration = new SecretProviderConfiguration();
        secretProviderConfiguration.setPluginName(pluginName);
        secretProviderConfigurationAndCredentials.setSecretProviderConfiguration(secretProviderConfiguration);
        secretProviderConfigurationAndCredentials.setCredentials(DataTableUtils.dataTableToMap(table));
        String result = Context.getRestClientInstance().postJSon(path, JsonUtil.toString(secretProviderConfigurationAndCredentials));
        Context.getInstance().registerRestResponse(result);
    }
}
