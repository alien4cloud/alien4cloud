package alien4cloud.it.runtime;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.alien4cloud.tosca.normative.constants.ToscaFunctionConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import com.google.common.collect.Lists;
import org.junit.Assert;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;

import alien4cloud.deployment.model.SecretProviderConfigurationAndCredentials;
import alien4cloud.it.Context;
import alien4cloud.it.application.ApplicationStepDefinitions;
import alien4cloud.it.common.CommonStepDefinitions;
import alien4cloud.it.topology.TopologyStepDefinitions;
import alien4cloud.it.utils.DataTableUtils;
import alien4cloud.it.utils.PropertyUtils;
import alien4cloud.model.secret.SecretProviderConfiguration;
import alien4cloud.paas.model.OperationExecRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.DataTable;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeStepDefinitions {

    private TopologyStepDefinitions topoSteps = new TopologyStepDefinitions();
    private ApplicationStepDefinitions appSteps = new ApplicationStepDefinitions();
    private CommonStepDefinitions commonSteps = new CommonStepDefinitions();

    @When("^I trigger on the node template \"([^\"]*)\" the custom command \"([^\"]*)\" of the interface \"([^\"]*)\" for application \"([^\"]*)\"$")
    public void I_trigger_on_the_node_template_the_custom_command_of_the_interface_on_the_cloud(String nodeTemplateName, String commandName,
            String interfaceName, String appName) throws Throwable {
        I_trigger_on_the_node_template_the_custom_command_of_the_interface_for_application_with_parameters(nodeTemplateName, commandName, interfaceName,
                appName, null);
    }

    @When("^I ask the runtime topology of the application \"([^\"]*)\" on the location \"([^\"]*)\" of \"([^\"]*)\"$")
    public void I_ask_the_runtime_topology_of_the_application_on_the_cloud(String applicationName, String locationName, String orchestratorName)
            throws Throwable {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        NameValuePair nvp = new BasicNameValuePair("locationId", Context.getInstance().getLocationId(orchestratorId, locationName));
        String applicationId = Context.getInstance().getApplication().getId();
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().getUrlEncoded(
                "/rest/v1/runtime/" + applicationId + "/environment/" + Context.getInstance().getDefaultApplicationEnvironmentId(applicationName) + "/topology",
                Lists.newArrayList(nvp)));

    }

    @Then("^The operation response should contain the result \"([^\"]*)\" for instance \"([^\"]*)\"$")
    public void The_operation_response_should_contain_the_result_for_instance(String expectedResponse, String instanceId) throws Throwable {
        RestResponse<?> restResponse = JsonUtil.read(Context.getInstance().getRestResponse());
        Map<String, String> executionResults = JsonUtil.toMap(JsonUtil.toString(restResponse.getData()), String.class, String.class);
        Assert.assertNotNull(executionResults.get(instanceId));
        Assert.assertTrue(executionResults.get(instanceId).contains(expectedResponse));
    }

    /**
     * Attention: Should use the json format if the property value is a function when filling the parameters.
     * 
     * @param nodeTemplateName
     * @param commandName
     * @param interfaceName
     * @param appName
     * @param secretProviderPluginName
     * @param secretCredentials
     * @param operationParameters
     * @throws Throwable
     */
    @When("^I trigger on the node template \"([^\"]*)\" the custom command \"([^\"]*)\" of the interface \"([^\"]*)\" for application \"([^\"]*)\" using the secret provider \"([^\"]*)\" and the secret credentials \"([^\"]*)\" with parameters:$")
    public void iTriggerOnTheNodeTemplateTheCustomCommandOfTheInterfaceForApplicationUsingTheSecretProviderAndTheSecretCredentialsWithParameters(
            String nodeTemplateName, String commandName, String interfaceName, String appName, String secretProviderPluginName, String secretCredentials,
            DataTable operationParameters) throws Throwable {
        OperationExecRequest commandRequest = new OperationExecRequest();
        commandRequest.setNodeTemplateName(nodeTemplateName);
        commandRequest.setInterfaceName(interfaceName);
        commandRequest.setOperationName(commandName);
        commandRequest.setApplicationEnvironmentId(Context.getInstance().getDefaultApplicationEnvironmentId(appName));
        if (StringUtils.isNotBlank(secretProviderPluginName) && StringUtils.isNotBlank(secretCredentials)) {
            commandRequest.setSecretProviderPluginName(secretProviderPluginName);
            Map<String, String> credentials = Splitter.on(",").withKeyValueSeparator(":").split(secretCredentials.replaceAll("\\s+", ""));
            commandRequest.setSecretProviderCredentials(credentials);
        }
        if (operationParameters != null) {
            Map<String, Object> parameters = Maps.newHashMap();
            for (List<String> operationParameter : operationParameters.raw()) {
                // check if the property is a function
                if (PropertyUtils.isFunction(ToscaFunctionConstants.GET_SECRET, operationParameter.get(1))) {
                    parameters.put(operationParameter.get(0), PropertyUtils.toFunctionValue(operationParameter.get(1)));
                } else {
                    parameters.put(operationParameter.get(0), operationParameter.get(1));
                }
            }
            commandRequest.setParameters(parameters);
        }
        String jSon = JsonUtil.toString(commandRequest);
        String restResponse = Context.getRestClientInstance().postJSon("/rest/v1/runtime/" + Context.getInstance().getApplication().getId() + "/operations/",
                jSon);
        Context.getInstance().registerRestResponse(restResponse);
    }

    @When("^I trigger on the node template \"([^\"]*)\" the custom command \"([^\"]*)\" of the interface \"([^\"]*)\" for application \"([^\"]*)\" with parameters:$")
    public void I_trigger_on_the_node_template_the_custom_command_of_the_interface_for_application_with_parameters(String nodeTemplateName, String commandName,
            String interfaceName, String appName, DataTable operationParameters) throws Throwable {
        iTriggerOnTheNodeTemplateTheCustomCommandOfTheInterfaceForApplicationUsingTheSecretProviderAndTheSecretCredentialsWithParameters(nodeTemplateName,
                commandName, interfaceName, appName, null, null, operationParameters);
    }

    private String scale(String nodeName, int instancesToScale, SecretProviderConfigurationAndCredentials secretProviderConfigurationAndCredentials)
            throws IOException {
        String path = "/rest/v1/applications/" + ApplicationStepDefinitions.CURRENT_APPLICATION.getId() + "/environments/"
                + Context.getInstance().getDefaultApplicationEnvironmentId(ApplicationStepDefinitions.CURRENT_APPLICATION.getName()) + "/scale/" + nodeName
                + "?instances=" + instancesToScale;
        if (secretProviderConfigurationAndCredentials == null) {
            return Context.getRestClientInstance().post(path);
        }
        return Context.getRestClientInstance().postJSon(path, JsonUtil.toString(secretProviderConfigurationAndCredentials));
    }

    @When("^I scale up the node \"([^\"]*)\" by adding (\\d+) instance\\(s\\)$")
    public void I_scale_up_the_node_by_adding_instance_s(String nodeName, int instancesToAdd) throws Throwable {
        log.info("Scale up the node " + nodeName + " by " + instancesToAdd);
        Context.getInstance().registerRestResponse(scale(nodeName, instancesToAdd, null));
        log.info("Finished scaling up the node " + nodeName + " by " + instancesToAdd);
    }

    @When("^I scale up the node \"([^\"]*)\" by adding (\\d+) instance\\(s\\) with the following credentials defined by the secret provider plugin \"([^\"]*)\"$")
    public void iScaleUpTheNodeByAddingInstanceSWithTheFollowingCredentialsDefinedByTheSecretProviderPlugin(String nodeName, int instancesToAdd,
            String pluginName, DataTable table) throws Throwable {
        log.info("Scale up the node " + nodeName + " by " + instancesToAdd);
        SecretProviderConfigurationAndCredentials secretProviderConfigurationAndCredentials = new SecretProviderConfigurationAndCredentials();
        secretProviderConfigurationAndCredentials.setCredentials(DataTableUtils.dataTableToMap(table));
        SecretProviderConfiguration secretProviderConfiguration = new SecretProviderConfiguration();
        secretProviderConfiguration.setPluginName(pluginName);
        secretProviderConfigurationAndCredentials.setSecretProviderConfiguration(secretProviderConfiguration);
        Context.getInstance().registerRestResponse(scale(nodeName, instancesToAdd, secretProviderConfigurationAndCredentials));
        log.info("Finished scaling up the node " + nodeName + " by " + instancesToAdd);
    }

    @When("^I scale down the node \"([^\"]*)\" by removing (\\d+) instance\\(s\\)$")
    public void I_scale_down_the_node_by_removing_instance_s(String nodeName, int instancesToRemove) throws Throwable {
        log.info("Scale down the node " + nodeName + " by " + instancesToRemove);
        Context.getInstance().registerRestResponse(scale(nodeName, -1 * instancesToRemove, null));
        log.info("Finished scaling down the node " + nodeName + " by " + instancesToRemove);
    }

}
