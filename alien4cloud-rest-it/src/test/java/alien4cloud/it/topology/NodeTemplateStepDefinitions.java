package alien4cloud.it.topology;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.ArrayUtils;

import alien4cloud.it.Context;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.topology.NodeTemplateRequest;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.topology.TopologyDTO;

import com.fasterxml.jackson.databind.ObjectMapper;

import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

@Slf4j
public class NodeTemplateStepDefinitions {

    private TopologyStepDefinitions topoSteps = new TopologyStepDefinitions();
    private ObjectMapper jsoMapper = Context.getInstance().getJsonMapper();

    @When("^I ask for replacements for the node \"([^\"]*)\"$")
    public void I_ask_for_replacements_for_the_node(String nodeTemplateName) throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().get("/rest/topologies/" + topologyId + "/nodetemplates/" + nodeTemplateName + "/replace"));
    }

    @Then("^the possible replacements nodes types should be$")
    public void the_possible_replacements_nodes_types_should_be(List<String> expectedElementIds) throws Throwable {
        IndexedNodeType[] replacements = JsonUtil.read(Context.getInstance().getRestResponse(), IndexedNodeType[].class).getData();
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
        IndexedNodeType[] replacements = JsonUtil.read(Context.getInstance().getRestResponse(), IndexedNodeType[].class).getData();
        assertNotNull(replacements);
        assertEquals(1, replacements.length);
        assertEquals(expectedElementId, replacements[0].getElementId());
    }

    @When("^I replace the node template \"([^\"]*)\" with a node \"([^\"]*)\" related to the \"([^\"]*)\" node type$")
    public void I_replace_the_node_template_with_a_node_related_to_the_node_type(String oldNodetemplateName, String newNodeTemplateName,
            String indexedNodeTypeId) throws Throwable {
        NodeTemplateRequest req = new NodeTemplateRequest(newNodeTemplateName, indexedNodeTypeId);
        String jSon = jsoMapper.writeValueAsString(req);
        String topologyId = Context.getInstance().getTopologyId();
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().putJSon("/rest/topologies/" + topologyId + "/nodetemplates/" + oldNodetemplateName + "/replace", jSon));
    }

    @When("^I delete the relationship \"([^\"]*)\" from the node template \"([^\"]*)\"$")
    public void I_delete_the_relationship_from_the_node_template(String relName, String nodeTempName) throws Throwable {
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().delete(
                        "/rest/topologies/" + Context.getInstance().getTopologyId() + "/nodetemplates/" + nodeTempName + "/relationships/" + relName));
    }

    @Then("^I should not have the relationship \"([^\"]*)\" in \"([^\"]*)\" node template$")
    public void I_should_not_have_a_relationship_in_node_template(String relName, String nodeTempName) throws Throwable {
        String topologyJson = Context.getRestClientInstance().get("/rest/topologies/" + Context.getInstance().getTopologyId());
        RestResponse<TopologyDTO> topologyResponse = JsonUtil.read(topologyJson, TopologyDTO.class, Context.getJsonMapper());
        NodeTemplate sourceNode = topologyResponse.getData().getTopology().getNodeTemplates().get(nodeTempName);
        Map<String, RelationshipTemplate> rels = sourceNode.getRelationships();
        if (rels != null) {
            assertFalse(rels.containsKey(relName));
        } else {
            log.info("No relationship found in I_should_not_have_a_relationship_in_node_template(String relName, String nodeTempName)");
        }
    }

    // @Then("^The RestResponse should contain a node template with a relationship \"([^\"]*)\" of type \"([^\"]*)\" and target \"([^\"]*)\"$")
    // public void The_RestResponse_should_contain_a_node_template_with_a_relationship_of_type_and_target(String expectedRelationshipName,
    // String expectedRelationshipType, String expectedRelationshipTarget) throws Throwable {
    // NodeTemplateDTO restNodeTemplateDTO = JsonUtil.read(Context.getInstance().getRestResponse(), NodeTemplateDTO.class).getData();
    // assertNotNull(restNodeTemplateDTO);
    // assertNotNull(restNodeTemplateDTO.getNodeTemplate());
    // Map<String, RelationshipTemplate> relationships = restNodeTemplateDTO.getNodeTemplate().getRelationships();
    // assertNotNull(relationships);
    // RelationshipTemplate relTemplate = relationships.get(expectedRelationshipName.trim());
    // assertNotNull(relTemplate);
    // assertEquals(expectedRelationshipType.trim(), relTemplate.getType());
    // assertEquals(expectedRelationshipTarget.trim(), relTemplate.getTarget());
    // }

    @Then("^there should be the followings in replacements nodes types$")
    public void there_should_be_the_followings_in_replacements_nodes_types(List<String> expectedElementIds) throws Throwable {
        IndexedNodeType[] replacements = JsonUtil.read(Context.getInstance().getRestResponse(), IndexedNodeType[].class).getData();
        assertNotNull(replacements);
        String[] elementIds = topoSteps.getElementsId(replacements);
        String[] expectedElementIdsArray = expectedElementIds.toArray(new String[expectedElementIds.size()]);
        for (String expected : expectedElementIdsArray) {
            assertTrue(ArrayUtils.contains(elementIds, expected));
        }
    }

}
