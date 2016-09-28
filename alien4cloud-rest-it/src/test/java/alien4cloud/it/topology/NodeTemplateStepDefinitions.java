package alien4cloud.it.topology;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import alien4cloud.it.Context;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.topology.TopologyDTO;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NodeTemplateStepDefinitions {

    private TopologyStepDefinitions topoSteps = new TopologyStepDefinitions();
    private ObjectMapper jsoMapper = Context.getInstance().getJsonMapper();

    @When("^I ask for replacements for the node \"([^\"]*)\"$")
    public void I_ask_for_replacements_for_the_node(String nodeTemplateName) throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().get("/rest/v1/topologies/" + topologyId + "/nodetemplates/" + nodeTemplateName + "/replace"));
    }

    @Then("^the possible replacements nodes types should be$")
    public void the_possible_replacements_nodes_types_should_be(List<String> expectedElementIds) throws Throwable {
        NodeType[] replacements = JsonUtil.read(Context.getInstance().getRestResponse(), NodeType[].class).getData();
        assertNotNull(replacements);
        String[] elementIds = topoSteps.getElementsId(replacements);
        assertEquals(expectedElementIds.size(), elementIds.length);
        String[] expectedArrayElementIds = expectedElementIds.toArray(new String[expectedElementIds.size()]);
        Arrays.sort(expectedArrayElementIds);
        Arrays.sort(elementIds);
        assertArrayEquals(expectedArrayElementIds, elementIds);
    }

    @Then("^the possible replacements nodes types should be \"([^\"]*)\"$")
    public void the_possible_replacements_nodes_types_should_be(String expectedElementId) throws Throwable {
        NodeType[] replacements = JsonUtil.read(Context.getInstance().getRestResponse(), NodeType[].class).getData();
        assertNotNull(replacements);
        assertEquals(1, replacements.length);
        assertEquals(expectedElementId, replacements[0].getElementId());
    }

    @Then("^I should not have the relationship \"([^\"]*)\" in \"([^\"]*)\" node template$")
    public void I_should_not_have_a_relationship_in_node_template(String relName, String nodeTempName) throws Throwable {
        String topologyJson = Context.getRestClientInstance().get("/rest/v1/topologies/" + Context.getInstance().getTopologyId());
        RestResponse<TopologyDTO> topologyResponse = JsonUtil.read(topologyJson, TopologyDTO.class, Context.getJsonMapper());
        NodeTemplate sourceNode = topologyResponse.getData().getTopology().getNodeTemplates().get(nodeTempName);
        Map<String, RelationshipTemplate> rels = sourceNode.getRelationships();
        if (rels != null) {
            assertFalse(rels.containsKey(relName));
        } else {
            log.info("No relationship found in I_should_not_have_a_relationship_in_node_template(String relName, String nodeTempName)");
        }
    }

    @Then("^there should be the followings in replacements nodes types$")
    public void there_should_be_the_followings_in_replacements_nodes_types(List<String> expectedElementIds) throws Throwable {
        NodeType[] replacements = JsonUtil.read(Context.getInstance().getRestResponse(), NodeType[].class).getData();
        assertNotNull(replacements);
        String[] elementIds = topoSteps.getElementsId(replacements);
        String[] expectedElementIdsArray = expectedElementIds.toArray(new String[expectedElementIds.size()]);
        for (String expected : expectedElementIdsArray) {
            assertTrue(ArrayUtils.contains(elementIds, expected));
        }
    }
}
