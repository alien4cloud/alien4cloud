package alien4cloud.it.topology;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.mapping.MappingBuilder;
import org.junit.Assert;

import alien4cloud.dao.ElasticSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.it.Context;
import alien4cloud.it.common.CommonStepDefinitions;
import alien4cloud.it.components.AddCommponentDefinitionSteps;
import alien4cloud.it.utils.JsonTestUtil;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.DeploymentArtifact;
import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedInheritableToscaElement;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.model.components.IndexedToscaElement;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.model.topology.ScalingPolicy;
import alien4cloud.paas.function.FunctionEvaluator;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.topology.AddRelationshipTemplateRequest;
import alien4cloud.rest.topology.NodeTemplateRequest;
import alien4cloud.rest.topology.TopologyDTO;
import alien4cloud.rest.topology.UpdatePropertyRequest;
import alien4cloud.rest.topology.UpdateRelationshipPropertyRequest;
import alien4cloud.rest.topology.task.RequirementToSatify;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.tosca.properties.constraints.ConstraintUtil.ConstraintInformation;
import alien4cloud.utils.MapUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import cucumber.api.DataTable;
import cucumber.api.PendingException;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

@Slf4j
public class TopologyStepDefinitions {
    private final static Map<String, Class<? extends IndexedToscaElement>> WORDS_TO_CLASSES;
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final Client esClient = Context.getEsClientInstance();

    private CommonStepDefinitions commonStepDefinitions = new CommonStepDefinitions();

    static {
        WORDS_TO_CLASSES = Maps.newHashMap();
        WORDS_TO_CLASSES.put("node type", IndexedNodeType.class);
        WORDS_TO_CLASSES.put("relationship type", IndexedRelationshipType.class);
        WORDS_TO_CLASSES.put("node types", IndexedNodeType.class);
    }

    @When("^I retrieve the newly created topology$")
    public void I_retrieve_the_newly_created_topology() throws Throwable {
        // Topology from context
        String topologyId = Context.getInstance().getTopologyId();
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/topologies/" + topologyId));
    }

    @Then("^The RestResponse should contain a topology id$")
    public void The_RestResponse_should_contain_an_id_string() throws Throwable {
        String response = Context.getInstance().getRestResponse();
        assertNotNull(response);
        RestResponse<String> restResponse = JsonUtil.read(response, String.class);
        assertNotNull(restResponse.getData());
        assertFalse(restResponse.getData().isEmpty());
    }

    @When("^I try to retrieve it$")
    public void I_try_to_retrieve_it() throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/topologies/" + topologyId));
    }

    @Then("^The RestResponse should contain a topology$")
    public void The_RestResponse_should_contain_a_topology() throws Throwable {
        String topologyResponseText = Context.getInstance().getRestResponse();
        RestResponse<TopologyDTO> topologyResponse = JsonUtil.read(topologyResponseText, TopologyDTO.class);
        assertNotNull(topologyResponse.getData());
        assertNotNull(topologyResponse.getData().getTopology().getId());
    }

    @Given("^There is a \"([^\"]*)\" with element name \"([^\"]*)\" and archive version \"([^\"]*)\"$")
    public void There_is_a_with_element_name_and_archive_version(String elementType, String elementId, String archiveVersion) throws Throwable {
        String componentId = elementId.concat(":").concat(archiveVersion);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/components/" + componentId));
        IndexedToscaElement idnt = JsonUtil.read(Context.getInstance().takeRestResponse(), WORDS_TO_CLASSES.get(elementType)).getData();
        assertNotNull(idnt);
        assertEquals(componentId, idnt.getId());
    }

    @Given("^There are properties for \"([^\"]*)\" element \"([^\"]*)\" and archive version \"([^\"]*)\"$")
    public void There_are_properties_for_element_and_archive_version(String elementType, String elementId, String archiveVersion, DataTable properties)
            throws Throwable {
        String componentId = elementId.concat(":").concat(archiveVersion);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/components/" + componentId));
        IndexedToscaElement idnt = JsonUtil.read(Context.getInstance().takeRestResponse(), WORDS_TO_CLASSES.get(elementType)).getData();
        assertNotNull(idnt);
        assertEquals(componentId, idnt.getId());
    }

    @When("^I add a node template \"([^\"]*)\" related to the \"([^\"]*)\" node type$")
    public void I_add_a_node_template_related_to_the_node_type(String name, String indexedNodeTypeId) throws Throwable {
        NodeTemplateRequest req = new NodeTemplateRequest(name, indexedNodeTypeId);
        String jSon = jsonMapper.writeValueAsString(req);
        String topologyId = Context.getInstance().getTopologyId();
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/topologies/" + topologyId + "/nodetemplates", jSon));
    }

    @When("^I have added a node template \"([^\"]*)\" related to the \"([^\"]*)\" node type$")
    public void I_have_added_a_node_template_related_to_the_node_type(String name, String indexedNodeTypeId) throws Throwable {
        I_add_a_node_template_related_to_the_node_type(name, indexedNodeTypeId);
        The_RestResponse_should_contain_a_node_type_with_id(indexedNodeTypeId);
    }

    @When("^I have added a node template \"([^\"]*)\" related to the \"([^\"]*)\" node type without check$")
    public void I_have_added_a_node_template_related_to_the_node_type_without_check(String name, String indexedNodeTypeId) throws Throwable {
        I_add_a_node_template_related_to_the_node_type(name, indexedNodeTypeId);
    }

    @When("^I delete a node template \"([^\"]*)\" from the topology$")
    public void I_delete_a_node_template_from_the_topology(String name) throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete("/rest/topologies/" + topologyId + "/nodetemplates/" + name));
    }

    @When("^I update the node template's name from \"([^\"]*)\" to \"([^\"]*)\"$")
    public void I_update_the_node_template_name_from_to(String oldName, String newName) throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().put("/rest/topologies/" + topologyId + "/nodetemplates/" + oldName + "/updateName/" + newName));
    }

    @When("^I add a relationship of type \"([^\"]*)\" defined in archive \"([^\"]*)\" version \"([^\"]*)\" with source \"([^\"]*)\" and target \"([^\"]*)\" for requirement \"([^\"]*)\" of type \"([^\"]*)\" and target capability \"([^\"]*)\"$")
    public void I_add_a_relationship_of_type_with_source_and_target_for_requirement_of_type_and_target_capability(String relType, String archiveName,
            String archiveVersion, String source, String target, String requirementName, String requirementType, String targetCapability) throws Throwable {
        I_add_a_relationship_of_type_with_source_and_target_for_requirement_of_type_and_target_capability(null, relType, archiveName, archiveVersion, source,
                target, requirementName, requirementType, targetCapability);
    }

    @Given("^I have added a relationship \"([^\"]*)\" of type \"([^\"]*)\" defined in archive \"([^\"]*)\" version \"([^\"]*)\" with source \"([^\"]*)\" and target \"([^\"]*)\" for requirement \"([^\"]*)\" of type \"([^\"]*)\" and target capability \"([^\"]*)\"$")
    public void I_have_added_a_relationship_of_type_with_source_and_target_for_requirement_of_type_and_target_capability(String relName, String relType,
            String archiveName, String archiveVersion, String source, String target, String requirementName, String requirementType, String capabilityName)
            throws Throwable {
        I_add_a_relationship_of_type_with_source_and_target_for_requirement_of_type_and_target_capability(relName, relType, archiveName, archiveVersion,
                source, target, requirementName, requirementType, capabilityName);
        commonStepDefinitions.I_should_receive_a_RestResponse_with_no_error();
    }

    @Given("^I add a relationship \"([^\"]*)\" of type \"([^\"]*)\" defined in archive \"([^\"]*)\" version \"([^\"]*)\" with source \"([^\"]*)\" and target \"([^\"]*)\" for requirement \"([^\"]*)\" of type \"([^\"]*)\" and target capability \"([^\"]*)\"$")
    public void I_add_a_relationship_of_type_with_source_and_target_for_requirement_of_type_and_target_capability(String relName, String relType,
            String archiveName, String archiveVersion, String source, String target, String requirementName, String requirementType, String targetCapabilityName)
            throws Throwable {
        RelationshipTemplate relTemp = new RelationshipTemplate();
        relTemp.setType(relType);
        relTemp.setTarget(target);
        relTemp.setRequirementName(requirementName);
        relTemp.setRequirementType(requirementType);
        relTemp.setTargetedCapabilityName(targetCapabilityName);
        String topologyId = Context.getInstance().getTopologyId();
        relName = relName == null || relName.isEmpty() ? getRelationShipName(relType, target) : relName;
        AddRelationshipTemplateRequest addRelationshipTemplateRequest = new AddRelationshipTemplateRequest();
        addRelationshipTemplateRequest.setArchiveName(archiveName);
        addRelationshipTemplateRequest.setArchiveVersion(archiveVersion);
        addRelationshipTemplateRequest.setRelationshipTemplate(relTemp);
        String json = jsonMapper.writeValueAsString(addRelationshipTemplateRequest);
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postJSon("/rest/topologies/" + topologyId + "/nodetemplates/" + source + "/relationships/" + relName, json));
    }

    @When("^I add a relationship of type \"([^\"]*)\" defined in archive \"([^\"]*)\" version \"([^\"]*)\" with source \"([^\"]*)\" and target \"([^\"]*)\"$")
    public void I_add_a_relationship_of_type_with_source_and_target(String relType, String archiveName, String archiveVersion, String source, String target)
            throws Throwable {
        I_add_a_relationship_of_type_with_source_and_target_for_requirement_of_type_and_target_capability(relType, archiveName, archiveVersion, source, target,
                null, null, null);
    }

    private String getRelationShipName(String type, String target) {
        String[] splitted = type.split("\\.");
        String last = splitted[splitted.length - 1];
        return last.trim() + "_" + target.trim();
    }

    @Then("^The RestResponse should not contain a nodetemplate named \"([^\"]*)\"")
    public void The_RestResponse_should_not_contain_a_nodetemplate_named(String key) throws Throwable {
        TopologyDTO topologyDTO = JsonTestUtil.read(Context.getInstance().getRestResponse(), TopologyDTO.class).getData();
        assertNotNull(topologyDTO);
        assertNotNull(topologyDTO.getTopology());
        assertTrue(topologyDTO.getTopology().getNodeTemplates() == null || topologyDTO.getTopology().getNodeTemplates().get(key) == null);
    }

    @Then("The RestResponse should not contain a relationship of type \"([^\"]*)\" with source \"([^\"]*)\" and target \"([^\"]*)\"")
    public void The_RestResponse_should_not_contain_a_relationship_of_type_with_source_and_target(String type, String source, String target) throws Throwable {
        TopologyDTO topologyDTO = JsonTestUtil.read(Context.getInstance().getRestResponse(), TopologyDTO.class).getData();
        assertNotNull(topologyDTO);
        assertNotNull(topologyDTO.getTopology());
        assertNotNull(topologyDTO.getTopology().getNodeTemplates());
        assertNotNull(topologyDTO.getTopology().getNodeTemplates().get(source));
        assertNotNull(topologyDTO.getTopology().getNodeTemplates().get(source).getRelationships() == null
                || topologyDTO.getTopology().getNodeTemplates().get(source).getRelationships().get(getRelationShipName(type, target)) == null);
    }

    @Then("^The RestResponse should contain a nodetemplate named \"([^\"]*)\" and type \"([^\"]*)\"")
    public void The_RestResponse_should_contain_a_nodetemplate_named_and_type(String key, String type) throws Throwable {
        TopologyDTO topologyDTO = JsonTestUtil.read(Context.getInstance().getRestResponse(), TopologyDTO.class).getData();
        assertNotNull(topologyDTO);
        assertNotNull(topologyDTO.getTopology());
        assertNotNull(topologyDTO.getTopology().getNodeTemplates());
        assertEquals(type, topologyDTO.getTopology().getNodeTemplates().get(key).getType());
    }

    @Then("^The RestResponse should contain a node type with \"([^\"]*)\" id$")
    public void The_RestResponse_should_contain_a_node_type_with_id(String expectedId) throws Throwable {
        TopologyDTO topologyDTO = JsonTestUtil.read(Context.getInstance().getRestResponse(), TopologyDTO.class).getData();
        assertNotNull(topologyDTO);
        assertNotNull(topologyDTO.getNodeTypes());
        assertNotNull(topologyDTO.getNodeTypes().get(expectedId.split(":")[0]));
        assertEquals(expectedId, topologyDTO.getNodeTypes().get(expectedId.split(":")[0]).getId());
    }

    @When("^I try to retrieve the created topology$")
    public void I_try_to_retrieve_the_created_topology() throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/topologies/" + Context.getInstance().getTopologyId()));
    }

    @Then("^The topology should contain a nodetemplate named \"([^\"]*)\"$")
    public void The_topology_should_contain_a_nodetemplate_named(String name) throws Throwable {
        String topologyResponseText = Context.getInstance().getRestResponse();
        RestResponse<TopologyDTO> topologyResponse = JsonTestUtil.read(topologyResponseText, TopologyDTO.class);
        assertNotNull(topologyResponse.getData());
        Map<String, NodeTemplate> nodeTemplates = topologyResponse.getData().getTopology().getNodeTemplates();
        assertNotNull(nodeTemplates);
        assertNotNull(nodeTemplates.get(name));
    }

    @When("^I update the node template \"([^\"]*)\"'s property \"([^\"]*)\" to \"([^\"]*)\"$")
    public void I_update_the_node_template_s_property_to(String nodeTempName, String propertyName, String propertyValue) throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();
        UpdatePropertyRequest req = new UpdatePropertyRequest(propertyName, propertyValue);
        String json = jsonMapper.writeValueAsString(req);
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postJSon("/rest/topologies/" + topologyId + "/nodetemplates/" + nodeTempName + "/properties", json));
    }

    @Then("^The topology should contain a nodetemplate named \"([^\"]*)\" with property \"([^\"]*)\" set to \"([^\"]*)\"$")
    public void The_topology_should_contain_a_nodetemplate_named_with_property_set_to(String nodeTemplateName, String propertyName, String propertyValue)
            throws Throwable {
        The_topology_should_contain_a_nodetemplate_named(nodeTemplateName);

        String topologyResponseText = Context.getInstance().getRestResponse();
        NodeTemplate nodeTemp = JsonTestUtil.read(topologyResponseText, TopologyDTO.class).getData().getTopology().getNodeTemplates().get(nodeTemplateName);
        assertNotNull(nodeTemp.getProperties());
        assertNotNull(nodeTemp.getProperties().get(propertyName));
        assertEquals(propertyValue, FunctionEvaluator.getScalarValue(nodeTemp.getProperties().get(propertyName)));
    }

    @Then("^I should have a relationship with type \"([^\"]*)\" from \"([^\"]*)\" to \"([^\"]*)\" in ALIEN$")
    public void I_should_have_a_relationship_with_type_from_to_in_ALIEN(String relType, String source, String target) throws Throwable {
        I_should_have_a_relationship_with_type_from_to_in_ALIEN(null, relType, source, target);
    }

    @Then("^I should have a relationship \"([^\"]*)\" with type \"([^\"]*)\" from \"([^\"]*)\" to \"([^\"]*)\" in ALIEN$")
    public void I_should_have_a_relationship_with_type_from_to_in_ALIEN(String relName, String relType, String source, String target) throws Throwable {

        // I should have a relationship with type
        String topologyJson = Context.getRestClientInstance().get("/rest/topologies/" + Context.getInstance().getTopologyId());
        RestResponse<TopologyDTO> topologyResponse = JsonTestUtil.read(topologyJson, TopologyDTO.class);
        NodeTemplate sourceNode = topologyResponse.getData().getTopology().getNodeTemplates().get(source);
        relName = relName == null || relName.isEmpty() ? getRelationShipName(relType, target) : relName;
        RelationshipTemplate rel = sourceNode.getRelationships().get(relName);
        assertNotNull(rel);
        assertEquals(relType, rel.getType());
        assertEquals(target, rel.getTarget());
        assertNotNull(rel.getRequirementName());
        assertNotNull(rel.getRequirementType());
    }

    @Then("^I should have (\\d+) relationship with source \"([^\"]*)\" and target \"([^\"]*)\" for type \"([^\"]*)\" with requirement \"([^\"]*)\" of type \"([^\"]*)\"$")
    public void I_should_have_relationship_with_source_for_requirement_of_type(int relationshipCount, String source, String target, String relType,
            String requirementName, String requirementType) throws Throwable {
        String topologyJson = Context.getRestClientInstance().get("/rest/topologies/" + Context.getInstance().getTopologyId());
        RestResponse<TopologyDTO> topologyResponse = JsonTestUtil.read(topologyJson, TopologyDTO.class);
        NodeTemplate sourceNode = topologyResponse.getData().getTopology().getNodeTemplates().get(source);
        RelationshipTemplate rel = sourceNode.getRelationships().get(getRelationShipName(relType, target));
        assertNotNull(rel);
        // Only one relationship of this type for the moment : cardinality check soon
        assertEquals(rel.getRequirementName(), requirementName);
        assertEquals(rel.getRequirementType(), requirementType);

    }

    @Then("^I should receive a RestResponse with constraint data name \"([^\"]*)\" and reference \"([^\"]*)\"$")
    public void I_should_receive_a_RestResponse_with_constraint_data_name_and_reference(String name, String reference) throws Throwable {
        RestResponse<?> restResponse = JsonUtil.read(Context.getInstance().getRestResponse());
        Assert.assertNotNull(restResponse.getData());
        ConstraintInformation constraint = JsonUtil.readObject(JsonUtil.toString(restResponse.getData()), ConstraintInformation.class);
        assertEquals(constraint.getName().toString(), name);
        assertEquals(constraint.getReference().toString(), reference);
    }

    @Given("^there are these types with element names and archive version$")
    public void there_are_these_types_with_element_names_and_archive_version(DataTable elements) throws Throwable {
        for (List<String> element : elements.raw()) {
            There_is_a_with_element_name_and_archive_version(element.get(0), element.get(1), element.get(2));
        }
    }

    @When("^I check for the deployable status of the topology$")
    public void I_check_for_the_deployable_status_of_the_topology() throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/topologies/" + topologyId + "/isvalid"));
    }

    @Then("^the topology should be deployable$")
    public void the_topology_should_be_deployable() throws Throwable {
        RestResponse<?> restResponse = JsonUtil.read(Context.getInstance().getRestResponse());
        assertNotNull(restResponse.getData());
        Map<String, Object> dataMap = JsonUtil.toMap(JsonUtil.toString(restResponse.getData()));
        assertTrue(Boolean.valueOf(dataMap.get("valid").toString()));
    }

    @Then("^the topology should not be deployable$")
    public void the_topology_should_not_be_deployable() throws Throwable {
        RestResponse<?> restResponse = JsonUtil.read(Context.getInstance().getRestResponse());
        assertNotNull(restResponse.getData());
        Map<String, Object> dataMap = JsonUtil.toMap(JsonUtil.toString(restResponse.getData()));
        assertFalse(Boolean.valueOf(dataMap.get("valid").toString()));
    }

    @Given("^i create a nodetype \"([^\"]*)\" in an archive name \"([^\"]*)\" version \"([^\"]*)\" with properties$")
    public void i_create_a_nodetype_in_an_archive_name_version_with_properties(String elementId, String arhiveName, String archiveVersion, DataTable properties)
            throws Throwable {

        throw new PendingException();
    }

    @Given("^i create a relationshiptype \"([^\"]*)\" in an archive name \"([^\"]*)\" version \"([^\"]*)\" with properties$")
    public void i_create_a_relationshiptype_in_an_archive_name_version_with_properties(String elementId, String archiveName, String archiveVersion,
            DataTable properties) throws Throwable {
        IndexedRelationshipType relationship = new IndexedRelationshipType();
        relationship.setArchiveName(archiveName);
        relationship.setArchiveVersion(archiveVersion);
        relationship.setElementId(elementId);
        for (List<String> propertyObject : properties.raw()) {
            if (propertyObject.get(0).equals("validSource")) {
                relationship.setValidSources(propertyObject.get(1).split(","));
            } else if (propertyObject.get(0).equals("validTarget")) {
                relationship.setValidTargets(propertyObject.get(1).split(","));
            } else if (propertyObject.get(0).equals("abstract")) {
                relationship.setAbstract(Boolean.valueOf(propertyObject.get(1)));
            }
        }

        esClient.prepareIndex(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, MappingBuilder.indexTypeFromClass(IndexedRelationshipType.class))
                .setSource(JsonUtil.toString(relationship)).setRefresh(true).execute().actionGet();
    }

    @Given("^I create a \"([^\"]*)\" \"([^\"]*)\" in an archive name \"([^\"]*)\" version \"([^\"]*)\"$")
    public void I_create_a_in_an_archive_name_version(String componentType, String elementId, String archiveName, String archiveVersion) throws Throwable {
        IndexedInheritableToscaElement element = new IndexedInheritableToscaElement();
        element.setAbstract(false);
        element.setElementId(elementId);
        element.setArchiveName(archiveName);
        element.setArchiveVersion(archiveVersion);
        Class<?> clazz = null;
        if (componentType.equals("capability") || componentType.equals("capabilities")) {
            clazz = IndexedCapabilityType.class;
        } else {
            throw new PendingException("creation of Type " + componentType + "not supported!");
        }

        esClient.prepareIndex(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, MappingBuilder.indexTypeFromClass(clazz)).setSource(JsonUtil.toString(element))
                .setRefresh(true).execute().actionGet();
    }

    @Given("^I create \"([^\"]*)\" in an archive name \"([^\"]*)\" version \"([^\"]*)\"$")
    public void I_create_in_an_archive_name_version(String componentType, String archiveName, String archiveVersion, List<String> elementIds) throws Throwable {
        for (String elementId : elementIds) {
            I_create_a_in_an_archive_name_version(componentType, elementId, archiveName, archiveVersion);
        }
    }

    @Given("^I add to the csar \"([^\"]*)\" \"([^\"]*)\" the component \"([^\"]*)\"$")
    public void I_add_to_the_csar_the_component(String csarName, String csarVersion, String componentFileName) throws Throwable {
        String csarId = csarName + ":" + csarVersion;
        AddCommponentDefinitionSteps.uploadComponent(csarId, componentFileName);
    }

    @Given("^I add to the csar \"([^\"]*)\" \"([^\"]*)\" the components$")
    public void I_add_to_the_csar_the_components(String csarName, String csarVersion, List<String> componentFileNames) throws Throwable {
        for (String componentFileName : componentFileNames) {
            I_add_to_the_csar_the_component(csarName, csarVersion, componentFileName);
        }
    }

    @Then("^there should not be suggested nodetypes for the \"([^\"]*)\" node template$")
    public void there_should_not_be_suggested_nodetypes_for_the_node_template(String nodeTemplateName) throws Throwable {
        RestResponse<Map> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), Map.class);
        assertNotNull(restResponse.getData());
        String dataString = JsonUtil.toString(restResponse.getData());
        Map<String, Object> validationDTOMap = JsonUtil.toMap(dataString);
        Object tasklist = MapUtil.get(validationDTOMap, "taskList");
        assertNotNull(tasklist);
        assertNull(getSuggestedNodesFor(nodeTemplateName, tasklist));
    }

    private IndexedNodeType[] getSuggestedNodesFor(String nodeTemplateName, Object taskList) throws IOException {
        for (Map<String, Object> task : (List<Map<String, Object>>) taskList) {
            String nodeTemp = (String) MapUtil.get(task, "nodeTemplateName");
            List<Object> suggestedNodeTypes = (List<Object>) MapUtil.get(task, "suggestedNodeTypes");
            if (nodeTemp.equals(nodeTemplateName) && suggestedNodeTypes != null) {
                return JsonUtil.toArray(Context.getInstance().getJsonMapper().writeValueAsString(suggestedNodeTypes), IndexedNodeType.class);
            }
        }
        return null;
    }

    @Then("^the suggested nodes types for the abstracts nodes templates should be:$")
    public void the_suggested_nodes_types_for_the_abstracts_nodes_templates_should_be(DataTable expectedSuggestedElemntIds) throws Throwable {
        RestResponse<Map> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), Map.class);
        assertNotNull(restResponse.getData());
        String dataString = JsonUtil.toString(restResponse.getData());
        Map<String, Object> validationDTOMap = JsonUtil.toMap(dataString);
        Object tasklist = MapUtil.get(validationDTOMap, "taskList");
        assertNotNull(tasklist);
        for (List<String> expected : expectedSuggestedElemntIds.raw()) {
            String[] expectedElementIds = expected.get(1).split(",");
            IndexedNodeType[] indexedNodeTypes = getSuggestedNodesFor(expected.get(0), tasklist);
            assertNotNull(indexedNodeTypes);
            String[] suggestedElementIds = getElementsId(indexedNodeTypes);
            assertNotNull(suggestedElementIds);
            assertEquals(expectedElementIds.length, suggestedElementIds.length);
            Arrays.sort(expectedElementIds);
            Arrays.sort(suggestedElementIds);
            assertArrayEquals(expectedElementIds, suggestedElementIds);
        }
    }

    private static final String ARTIFACT_PATH = "./src/test/resources/data/artifacts/";

    @When("^I update the node template \"([^\"]*)\"'s artifact \"([^\"]*)\" with \"([^\"]*)\"$")
    public void I_update_the_node_template_s_artifact_with(String nodeTemplateName, String artifactId, String artifactName) throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();
        String url = "/rest/topologies/" + topologyId + "/nodetemplates/" + nodeTemplateName + "/artifacts/" + artifactId;
        InputStream artifactStream = Files.newInputStream(Paths.get(ARTIFACT_PATH, artifactName));
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postMultipart(url, "file", artifactStream));
    }

    private static String ARTIFACT_REFERENCE;

    @Then("^the response should contain the artifact reference$")
    public void the_response_should_contain_the_artifact_reference() throws Throwable {
        String artifactReference = JsonUtil.read(Context.getInstance().getRestResponse(), String.class).getData();
        Assert.assertNotNull(artifactReference);
        ARTIFACT_REFERENCE = artifactReference;
    }

    @Then("^The topology should contain a nodetemplate named \"([^\"]*)\" with an artifact \"([^\"]*)\" with the specified UID and name \"([^\"]*)\"$")
    public void The_topology_should_contain_a_nodetemplate_named_with_an_artifact_with_the_specified_UID(String nodeTemplateName, String artifactId,
            String artifactName) throws Throwable {
        The_topology_should_contain_a_nodetemplate_named(nodeTemplateName);

        String topologyResponseText = Context.getInstance().getRestResponse();
        NodeTemplate nodeTemp = JsonUtil.read(topologyResponseText, TopologyDTO.class).getData().getTopology().getNodeTemplates().get(nodeTemplateName);
        Assert.assertNotNull(nodeTemp.getArtifacts());
        Assert.assertFalse(nodeTemp.getArtifacts().isEmpty());
        DeploymentArtifact deploymentArtifact = nodeTemp.getArtifacts().get(artifactId);
        Assert.assertNotNull(deploymentArtifact);
        Assert.assertNotNull(deploymentArtifact.getArtifactType());
        Assert.assertEquals(ARTIFACT_REFERENCE, deploymentArtifact.getArtifactRef());
        Assert.assertEquals(artifactName, deploymentArtifact.getArtifactName());
    }

    @Then("^the node with requirements lowerbound not satisfied should be$")
    public void the_node_with_requirements_lowerbound_not_satisfied_should_be(DataTable expectedRequirementsNames) throws Throwable {
        RestResponse<Map> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), Map.class);
        assertNotNull(restResponse.getData());
        String dataString = JsonUtil.toString(restResponse.getData());
        Map<String, Object> validationDTOMap = JsonUtil.toMap(dataString);
        Object tasklist = MapUtil.get(validationDTOMap, "taskList");
        assertNotNull(tasklist);
        for (List<String> expected : expectedRequirementsNames.raw()) {
            String[] expectedNames = expected.get(1).split(",");
            List<RequirementToSatify> requirementsToSatify = getRequirementsToSatisfy(expected.get(0), tasklist);
            String[] requirementsNames = getRequirementsNames(requirementsToSatify.toArray(new RequirementToSatify[requirementsToSatify.size()]));
            assertNotNull(requirementsNames);
            assertEquals(expectedNames.length, requirementsNames.length);
            Arrays.sort(expectedNames);
            Arrays.sort(requirementsNames);
            assertArrayEquals(expectedNames, requirementsNames);
        }
    }

    private List<RequirementToSatify> getRequirementsToSatisfy(String nodeTemplateName, Object taskList) throws IOException {
        for (Map<String, Object> task : (List<Map<String, Object>>) taskList) {
            String nodeTemp = (String) MapUtil.get(task, "nodeTemplateName");
            List<Object> resToImp = (List<Object>) MapUtil.get(task, "requirementsToImplement");
            if (nodeTemp.equals(nodeTemplateName) && resToImp != null) {
                return JsonUtil.toList(JsonUtil.toString(resToImp), RequirementToSatify.class);
            }
        }
        return null;
    }

    public String[] getElementsId(IndexedNodeType... indexedNodeTypes) {
        String[] toReturn = null;
        for (IndexedNodeType indexedNodeType : indexedNodeTypes) {
            toReturn = ArrayUtils.add(toReturn, indexedNodeType.getElementId());
        }
        return toReturn;
    }

    public String[] getRequirementsNames(RequirementToSatify... requirementsToSatisfy) {
        String[] toReturn = null;
        for (RequirementToSatify requirementToSatisfy : requirementsToSatisfy) {
            toReturn = ArrayUtils.add(toReturn, requirementToSatisfy.getName());
        }

        return toReturn;
    }

    @SuppressWarnings("unchecked")
    private List<String> getRequiredPropertiesNotSet(String nodeTemplateName, Object taskList) throws IOException {
        for (Map<String, Object> task : (List<Map<String, Object>>) taskList) {
            String nodeTemp = (String) MapUtil.get(task, "nodeTemplateName");
            List<Object> resToImp = (List<Object>) MapUtil.get(task, "properties");
            if (nodeTemp.equals(nodeTemplateName) && resToImp != null) {
                return JsonUtil.toList(JsonUtil.toString(resToImp), String.class);
            }
        }
        return null;
    }

    @When("^I add a scaling policy to the node \"([^\"]*)\"$")
    public void I_add_a_scaling_policy_to_the_node(String nodeName) throws Throwable {
        ScalingPolicy policy = new ScalingPolicy(1, 1, 1);
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postJSon("/rest/topologies/" + Context.getInstance().getTopologyId() + "/scalingPolicies/" + nodeName,
                        JsonUtil.toString(policy)));
    }

    @Given("^I have a already added a scaling policy to the node \"([^\"]*)\"$")
    public void I_have_a_already_added_a_scaling_policy_to_the_node(String nodeName) throws Throwable {
        I_add_a_scaling_policy_to_the_node(nodeName);
    }

    @When("^I change the scaling policy of the node \"([^\"]*)\" with max instances to (\\d+), initial instances to (\\d+) and min instances to (\\d+)$")
    public void I_change_the_scaling_of_the_node_with_max_instances_to_initial_instances_to_and_min_instances_to(String nodeName, int maxInstancesValue,
            int initialInstancesValue, int minInstancesValue) throws Throwable {
        ScalingPolicy policy = new ScalingPolicy(minInstancesValue, maxInstancesValue, initialInstancesValue);
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postJSon("/rest/topologies/" + Context.getInstance().getTopologyId() + "/scalingPolicies/" + nodeName,
                        JsonUtil.toString(policy)));
    }

    @Then("^the scaling policy of the node \"([^\"]*)\" should match max instances equals to (\\d+), initial instances equals to (\\d+) and min instances equals to (\\d+)$")
    public void the_scaling_policy_of_the_node_should_match_max_instances_equals_to_initial_instances_equals_to_and_min_instances_equals_to(String nodeName,
            int maxInstances, int initialInstances, int minInstances) throws Throwable {
        I_try_to_retrieve_the_created_topology();
        String topologyResponseText = Context.getInstance().getRestResponse();
        RestResponse<TopologyDTO> topologyResponse = JsonTestUtil.read(topologyResponseText, TopologyDTO.class);
        assertNotNull(topologyResponse.getData());
        Map<String, ScalingPolicy> policies = topologyResponse.getData().getTopology().getScalingPolicies();
        assertTrue(policies != null && !policies.isEmpty());
        ScalingPolicy computePolicy = policies.get(nodeName);
        assertNotNull(computePolicy);
        assertEquals(maxInstances, computePolicy.getMaxInstances());
        assertEquals(minInstances, computePolicy.getMinInstances());
        assertEquals(initialInstances, computePolicy.getInitialInstances());
    }

    @When("^I delete the policy of the node \"([^\"]*)\"$")
    public void I_delete_the_policy(String nodeName) throws Throwable {
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().delete("/rest/topologies/" + Context.getInstance().getTopologyId() + "/scalingPolicies/" + nodeName));
    }

    @Then("^There's no defined scaling policy for the node \"([^\"]*)\"$")
    public void There_s_no_defined_scaling_policy(String nodeName) throws Throwable {
        I_try_to_retrieve_the_created_topology();
        String topologyResponseText = Context.getInstance().getRestResponse();
        RestResponse<TopologyDTO> topologyResponse = JsonTestUtil.read(topologyResponseText, TopologyDTO.class);
        assertNotNull(topologyResponse.getData());
        Map<String, ScalingPolicy> policies = topologyResponse.getData().getTopology().getScalingPolicies();
        if (policies != null) {
            ScalingPolicy computePolicy = policies.get(nodeName);
            assertNull(computePolicy);
        }
    }

    @Then("^the node with required properties not set should be$")
    public void the_node_with_required_properties_not_set_should_be(DataTable expectedRequiredProperties) throws Throwable {
        RestResponse<Map> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), Map.class);
        assertNotNull(restResponse.getData());
        String dataString = JsonUtil.toString(restResponse.getData());
        Map<String, Object> validationDTOMap = JsonUtil.toMap(dataString);
        Object tasklist = MapUtil.get(validationDTOMap, "taskList");
        assertNotNull(tasklist);
        for (List<String> expected : expectedRequiredProperties.raw()) {
            String[] expectedProperties = expected.get(1).split(",");
            List<String> requiredPropertiesNotSet = getRequiredPropertiesNotSet(expected.get(0), tasklist);
            assertNotNull(requiredPropertiesNotSet);
            String[] requiredProperties = requiredPropertiesNotSet.toArray(new String[requiredPropertiesNotSet.size()]);
            assertNotNull(requiredProperties);
            assertEquals(expectedProperties.length, requiredProperties.length);
            Arrays.sort(expectedProperties);
            Arrays.sort(requiredProperties);
            assertArrayEquals(expectedProperties, requiredProperties);
        }
    }

    @When("^I rename the relationship \"([^\"]*)\" into \"([^\"]*)\" from the node template \"([^\"]*)\"$")
    public void I_rename_the_relationship_into_from_the_node_template(String oldRelationshipName, String newRelationshipName, String nodeTemplateName)
            throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();
        NameValuePair nvp = new BasicNameValuePair("newName", newRelationshipName);
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().putUrlEncoded(
                        "/rest/topologies/" + topologyId + "/nodetemplates/" + nodeTemplateName + "/relationships/" + oldRelationshipName + "/updateName",
                        Lists.newArrayList(nvp)));
    }

    @When("^I update the \"([^\"]*)\" property of the relationship \"([^\"]*)\" into \"([^\"]*)\" from the node template \"([^\"]*)\"$")
    public void I_update_the_property_of_the_relationship_into_from_the_node_template(String propertyName, String relationshipName, String newValue,
            String nodeTemplateName) throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();

        UpdateRelationshipPropertyRequest updatePropertyRequest = new UpdateRelationshipPropertyRequest();
        updatePropertyRequest.setPropertyName(propertyName);
        updatePropertyRequest.setPropertyValue(newValue);
        updatePropertyRequest.setRelationshipType("tosca.relationships.HostedOn");
        String json = jsonMapper.writeValueAsString(updatePropertyRequest);
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postJSon(
                        "/rest/topologies/" + topologyId + "/nodetemplates/" + nodeTemplateName + "/relationships/" + relationshipName + "/updateProperty",
                        json));
    }

    @And("^The topology should have as dependencies$")
    public void The_topology_should_have_as_dependencies(DataTable dependencies) throws Throwable {
        Set<CSARDependency> expectedDependencies = Sets.newHashSet();
        for (List<String> row : dependencies.raw()) {
            expectedDependencies.add(new CSARDependency(row.get(0), row.get(1)));
        }
        String topologyResponseText = Context.getInstance().getRestResponse();
        Set<CSARDependency> actualDependencies = JsonTestUtil.read(topologyResponseText, TopologyDTO.class).getData().getTopology().getDependencies();
        Assert.assertEquals(expectedDependencies, actualDependencies);
    }

    @And("^If I search for topology templates I can find one with the name \"([^\"]*)\" and store the related topology as a SPEL context$")
    public void searchForTopologyTemplateByName(String topologyTemplateName) throws Throwable {
        String response = Context.getRestClientInstance().postJSon("/rest/templates/topology/search", "{\"from\":0,\"size\":50}");
        RestResponse<FacetedSearchResult> restResponse = JsonUtil.read(response, FacetedSearchResult.class);
        String topologyId = null;
        for (Object singleResult : restResponse.getData().getData()) {
            Map map = (Map) singleResult;
            if (topologyTemplateName.equals(map.get("name"))) {
                topologyId = map.get("topologyId").toString();
            }
        }
        assertNotNull("A topology template named " + topologyTemplateName + " can not be found", topologyId);
        response = Context.getRestClientInstance().get("/rest/topologies/" + topologyId);
        RestResponse<TopologyDTO> topologyDto = alien4cloud.it.utils.JsonTestUtil.read(response, TopologyDTO.class);
        Context.getInstance().buildEvaluationContext(topologyDto.getData().getTopology());
    }

}
