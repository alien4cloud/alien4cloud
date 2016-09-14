package alien4cloud.it.topology;

import alien4cloud.it.Context;
import alien4cloud.it.common.CommonStepDefinitions;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.topology.TopologyDTO;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.junit.Assert;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertFalse;

public class TopologyRecoveryStepDefinitions {

    private CommonStepDefinitions commonSteps = new CommonStepDefinitions();

    @When("^I trigger the recovery of the topology$")
    public void I_trigger_the_recovery_of_the_topology() throws Throwable {
        RestResponse<?> response = JsonUtil.read(Context.getInstance().getRestResponse());
        List<CSARDependency> dependencies = JsonUtil.toList(JsonUtil.toString(response.getData()), CSARDependency.class);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance()
                .putJSon("/rest/v1/topologies/" + Context.getInstance().getTopologyId() + "/recover", JsonUtil.toString(dependencies)));
    }

    @When("^I reset the topology$")
    public void I_reset_the_topology() throws Throwable {
        Context.getInstance()
                .registerRestResponse(Context.getRestClientInstance().put("/rest/v1/topologies/" + Context.getInstance().getTopologyId() + "/reset"));
    }

    @Then("^the topology dto should contain (\\d+) nodetemplates$")
    public void the_topology_dto_should_contain_nodetemplates(int count) throws Throwable {
        TopologyDTO dto = JsonUtil.read(Context.getInstance().getRestResponse(), TopologyDTO.class, Context.getJsonMapper()).getData();
        Assert.assertEquals(count, dto.getTopology().getNodeTemplates().size());
    }

    @Then("^the topology dto should contain an emty topology$")
    public void the_topology_dto_should_contain_an_emty_topology() throws Throwable {
        TopologyDTO dto = JsonUtil.read(Context.getInstance().getRestResponse(), TopologyDTO.class, Context.getJsonMapper()).getData();
        Assert.assertTrue(dto.getTopology().isEmpty());
    }

    @Then("^the node \"([^\"]*)\" in the topology dto should have (\\d+) relationshipTemplates$")
    public void the_node_in_the_topology_dto_should_have_relationshiptemplates(String nodeName, int count) throws Throwable {
        TopologyDTO dto = JsonUtil.read(Context.getInstance().getRestResponse(), TopologyDTO.class, Context.getJsonMapper()).getData();
        NodeTemplate template = dto.getTopology().getNodeTemplates().get(nodeName);
        Assert.assertNotNull(template);
        Assert.assertEquals(count, template.getRelationships().size());
    }

    @Then("^there should not be the relationship \"([^\"]*)\" in \"([^\"]*)\" node template in the topology dto$")
    public void there_should_not_be_the_relationship_in_node_template_in_the_topology_dto(String relName, String nodeName) throws IOException {
        TopologyDTO dto = JsonUtil.read(Context.getInstance().getRestResponse(), TopologyDTO.class, Context.getJsonMapper()).getData();
        NodeTemplate template = dto.getTopology().getNodeTemplates().get(nodeName);
        Assert.assertNotNull(template);
        assertFalse(template.getRelationships().containsKey(relName));
    }

    @Then("^the node \"([^\"]*)\" in the topology dto should not have the capability \"([^\"]*)\"$")
    public void I_node_in_the_topology_dto_should_not_have_the_capability(String nodeName, String capabilityName) throws IOException {
        TopologyDTO dto = JsonUtil.read(Context.getInstance().getRestResponse(), TopologyDTO.class, Context.getJsonMapper()).getData();
        NodeTemplate template = dto.getTopology().getNodeTemplates().get(nodeName);
        Assert.assertNotNull(template);
        assertFalse(template.getCapabilities().containsKey(capabilityName));
    }

    @Then("^the node \"([^\"]*)\" in the topology dto should not have the requirement \"([^\"]*)\"$")
    public void I_node_in_the_topology_dto_should_not_have_the_requirement(String nodeName, String requirementName) throws IOException {
        TopologyDTO dto = JsonUtil.read(Context.getInstance().getRestResponse(), TopologyDTO.class, Context.getJsonMapper()).getData();
        NodeTemplate template = dto.getTopology().getNodeTemplates().get(nodeName);
        Assert.assertNotNull(template);
        assertFalse(template.getRequirements().containsKey(requirementName));
    }
}
