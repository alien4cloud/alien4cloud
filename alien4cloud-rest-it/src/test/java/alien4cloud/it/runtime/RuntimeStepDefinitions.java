package alien4cloud.it.runtime;

import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.elasticsearch.common.collect.Lists;
import org.junit.Assert;

import alien4cloud.it.Context;
import alien4cloud.it.application.ApplicationStepDefinitions;
import alien4cloud.it.common.CommonStepDefinitions;
import alien4cloud.it.topology.TopologyStepDefinitions;
import alien4cloud.paas.model.OperationExecRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class RuntimeStepDefinitions {

    private TopologyStepDefinitions topoSteps = new TopologyStepDefinitions();
    private ApplicationStepDefinitions appSteps = new ApplicationStepDefinitions();
    private CommonStepDefinitions commonSteps = new CommonStepDefinitions();

    @Given("^I have an application \"([^\"]*)\" with a topology containing a nodeTemplate \"([^\"]*)\" related to \"([^\"]*)\" for \"([^\"]*)\"$")
    public void I_have_an_application_with_a_topology_containing_a_nodeTemplate_related_to(String applicationName, String nodeTemplateName, String nodeTypeId)
            throws Throwable {
        appSteps.I_have_an_application_with_name(applicationName);
        topoSteps.I_have_added_a_node_template_related_to_the_node_type(nodeTemplateName, nodeTypeId);
    }

    @When("^I trigger on the node template \"([^\"]*)\" the custom command \"([^\"]*)\" of the interface \"([^\"]*)\" on the cloud \"([^\"]*)\"$")
    public void I_trigger_on_the_node_template_the_custom_command_of_the_interface_on_the_cloud(String nodeTemplateName, String commandName,
            String interfaceName, String cloudName, String appName) throws Throwable {
        OperationExecRequest commandRequest = new OperationExecRequest();
        commandRequest.setNodeTemplateName(nodeTemplateName);
        commandRequest.setInterfaceName(interfaceName);
        commandRequest.setOperationName(commandName);
        commandRequest.setApplicationEnvironmentId(Context.getInstance().getDefaultApplicationEnvironmentId(appName));
        String jSon = JsonUtil.toString(commandRequest);
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postJSon("/rest/runtime/" + Context.getInstance().getApplication().getId() + "/operations/", jSon));

    }

    @Given("^I have deleted a node template \"([^\"]*)\" from the topology$")
    public void I_have_deleted_a_node_template_from_the_topology(String nodeTemplateName) throws Throwable {
        topoSteps.I_delete_a_node_template_from_the_topology(nodeTemplateName);
        commonSteps.I_should_receive_a_RestResponse_with_no_error();
        topoSteps.The_RestResponse_should_not_contain_a_nodetemplate_named(nodeTemplateName);
    }

    @When("^I ask the runtime topology of the application \"([^\"]*)\" on the cloud \"([^\"]*)\"$")
    public void I_ask_the_runtime_topology_of_the_application_on_the_cloud(String applicationName, String cloudName) throws Throwable {
        Context context = Context.getInstance();
        NameValuePair nvp = new BasicNameValuePair("cloudId", Context.getInstance().getCloudId(cloudName));
        String applicationId = Context.getInstance().getApplication().getId();
        context.registerRestResponse(Context.getRestClientInstance().getUrlEncoded("/rest/runtime/" + applicationId + "/topology", Lists.newArrayList(nvp)));

    }

    @Then("^The operation response should contain the result \"([^\"]*)\" for instance \"([^\"]*)\"$")
    public void The_operation_response_should_contain_the_result_for_instance(String expectedResponse, String instanceId) throws Throwable {
        RestResponse<?> restResponse = JsonUtil.read(Context.getInstance().getRestResponse());
        Map<String, String> executionResults = JsonUtil.toMap(JsonUtil.toString(restResponse.getData()), String.class, String.class);
        Assert.assertNotNull(executionResults.get(instanceId));
        Assert.assertEquals(expectedResponse, executionResults.get(instanceId));
    }
}
