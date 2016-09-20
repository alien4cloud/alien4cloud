package alien4cloud.it.topology;

import java.io.IOException;
import java.util.Map;

import org.alien4cloud.tosca.model.templates.Topology;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Maps;

import alien4cloud.it.Context;
import alien4cloud.it.common.CommonStepDefinitions;
import alien4cloud.it.utils.TestUtils;
import alien4cloud.rest.application.model.CreateTopologyRequest;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.utils.VersionUtil;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.When;

/**
 */
public class TopologyTemplateStepDefinitions {

    CommonStepDefinitions commonStepDefinitions = new CommonStepDefinitions();

    public static String CURRENT_TOPOLOGY_TEMP_ID;

    @When("^I create a new topology template with name \"([^\"]*)\" and description \"([^\"]*)\"$")
    public void iCreateANewTopologyTemplateWithNameAndDescription(String name, String description) throws Throwable {
        createTopologyTemplate(name, description, VersionUtil.DEFAULT_VERSION_NAME, null);
    }

    @When("^I create a new topology template with name \"([^\"]*)\"$")
    public void iCreateANewTopologyTemplateWithNameAndDescription(String name) throws Throwable {
        createTopologyTemplate(name, null, VersionUtil.DEFAULT_VERSION_NAME, null);
    }

    @Given("^I have created a new topology template with name \"([^\"]*)\" and description \"([^\"]*)\"$")
    public void iHaveCreatedANewTopologyTemplateWithNameAndDescription(String name, String description) throws Throwable {
        iCreateANewTopologyTemplateWithNameAndDescription(name, description);
        commonStepDefinitions.I_should_receive_a_RestResponse_with_no_error();
    }

    @And("^I should be able to retrieve a topology with name \"([^\"]*)\" version \"([^\"]*)\" and store it as a SPEL context$")
    public void i_Should_Be_Able_To_Retrieve_A_Topology_With_Name_Version_And_Store_It_As_A_SPEL_Context(String name, String version) throws Throwable {
        String topologyId = TestUtils.getFullId(name, version);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/catalog/topologies/" + topologyId));
        commonStepDefinitions.I_should_receive_a_RestResponse_with_no_error();

        // register the retrieved topology as SPEL context
        Topology topology = JsonUtil.read(Context.getInstance().getRestResponse(), Topology.class, Context.getJsonMapper()).getData();
        Context.getInstance().buildEvaluationContext(topology);
    }

    @Given("^I create a new topology template with name \"([^\"]*)\" and description \"([^\"]*)\" and node templates$")
    public void iCreateANewTopologyTemplateWithNameAndDescriptionAndNodeTemplates(String name, String description, Map<String, String> nodeTemplates)
            throws Throwable {
        iHaveCreatedANewTopologyTemplateWithNameAndDescription(name, description);

        // add the node templates
        for (Map.Entry<String, String> entry : nodeTemplates.entrySet()) {
            Map<String, String> operationMap = Maps.newHashMap();
            operationMap.put("type", "org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation");
            operationMap.put("nodeName", entry.getKey());
            operationMap.put("indexedNodeTypeId", entry.getValue());
            EditorStepDefinitions.do_i_execute_the_operation(operationMap);
            commonStepDefinitions.I_should_receive_a_RestResponse_with_no_error();
        }
        EditorStepDefinitions.do_i_save_the_topology();
        commonStepDefinitions.I_should_receive_a_RestResponse_with_no_error();
    }

    @When("^I create a new topology template with name \"([^\"]*)\" description \"([^\"]*)\" and version \"([^\"]*)\"$")
    public void iCreateANewTopologyTemplateWithNameDescriptionAndVersion(String name, String description, String version) throws Throwable {
        createTopologyTemplate(name, description, version, null);
    }

    private void createTopologyTemplate(String name, String description, String version, String fromTopologyId) throws IOException {
        CreateTopologyRequest request = new CreateTopologyRequest();
        request.setDescription(description);
        request.setName(name);
        request.setVersion(version);
        request.setFromTopologyId(fromTopologyId);

        Context.getInstance()
                .registerRestResponse(Context.getRestClientInstance().postJSon("/rest/v1/catalog/topologies/template", JsonUtil.toString(request)));
        String topologyId = JsonUtil.read(Context.getInstance().getRestResponse(), String.class).getData();

        if (StringUtils.isNotBlank(topologyId)) {
            Context.getInstance().registerTopologyTemplateId(topologyId);
            CURRENT_TOPOLOGY_TEMP_ID = topologyId;
            Context.getInstance().registerTopologyId(topologyId);
        }
    }

    @When("^I create a new topology template with name \"([^\"]*)\" version \"([^\"]*)\"$")
    public void iCreateANewTopologyTemplateWithNameVersion(String name, String version) throws Throwable {
        createTopologyTemplate(name, null, version, null);
    }

    @When("^I create a new topology with name \"([^\"]*)\" version \"([^\"]*)\" based on the version \"([^\"]*)\"$")
    public void iCreateANewTopologyWithNameVersionBasedOnTheVersion(String name, String version, String fromVersion) throws Throwable {
        createTopologyTemplate(name, null, version, TestUtils.getFullId(name, fromVersion));
    }
}
