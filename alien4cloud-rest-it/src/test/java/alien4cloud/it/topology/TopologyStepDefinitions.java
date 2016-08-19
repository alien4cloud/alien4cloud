package alien4cloud.it.topology;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alien4cloud.test.setup.TestDataRegistry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.mapping.MappingBuilder;
import org.junit.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;

import alien4cloud.dao.ElasticSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.it.Context;
import alien4cloud.it.common.CommonStepDefinitions;
import alien4cloud.it.components.AddCommponentDefinitionSteps;
import alien4cloud.model.components.*;
import alien4cloud.model.templates.TopologyTemplateVersion;
import alien4cloud.model.topology.NodeGroup;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.model.topology.ScalingPolicy;
import alien4cloud.paas.function.FunctionEvaluator;
import alien4cloud.rest.component.SearchRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.topology.TopologyDTO;
import alien4cloud.topology.TopologyUtils;
import alien4cloud.topology.task.RequirementToSatisfy;
import alien4cloud.topology.task.TaskCode;
import alien4cloud.topology.task.TaskLevel;
import alien4cloud.tosca.properties.constraints.ConstraintUtil.ConstraintInformation;
import alien4cloud.utils.FileUtil;
import alien4cloud.utils.MapUtil;
import cucumber.api.DataTable;
import cucumber.api.PendingException;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import lombok.extern.slf4j.Slf4j;

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
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/topologies/" + topologyId));
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
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/topologies/" + topologyId));
    }

    @Then("^I get a topology by id \"([^\"]*)\"$")
    public void I_get_a_topology_by_id(String topologyId) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/topologies/" + topologyId));
    }

    @Then("^The RestResponse should contain a topology$")
    public void The_RestResponse_should_contain_a_topology() throws Throwable {
        String topologyResponseText = Context.getInstance().getRestResponse();
        RestResponse<TopologyDTO> topologyResponse = JsonUtil.read(topologyResponseText, TopologyDTO.class, Context.getJsonMapper());
        assertNotNull(topologyResponse.getData());
        assertNotNull(topologyResponse.getData().getTopology().getId());
    }

    @Given("^There is a \"([^\"]*)\" with element name \"([^\"]*)\" and archive version \"([^\"]*)\"$")
    public void There_is_a_with_element_name_and_archive_version(String elementType, String elementId, String archiveVersion) throws Throwable {
        String componentId = elementId.concat(":").concat(archiveVersion);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/components/" + componentId));
        IndexedToscaElement idnt = JsonUtil.read(Context.getInstance().takeRestResponse(), WORDS_TO_CLASSES.get(elementType), Context.getJsonMapper())
                .getData();
        assertNotNull(idnt);
        assertEquals(componentId, idnt.getId());
    }

    @Given("^There are properties for \"([^\"]*)\" element \"([^\"]*)\" and archive version \"([^\"]*)\"$")
    public void There_are_properties_for_element_and_archive_version(String elementType, String elementId, String archiveVersion, DataTable properties)
            throws Throwable {
        String componentId = elementId.concat(":").concat(archiveVersion);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/components/" + componentId));
        IndexedToscaElement idnt = JsonUtil.read(Context.getInstance().takeRestResponse(), WORDS_TO_CLASSES.get(elementType), Context.getJsonMapper())
                .getData();
        assertNotNull(idnt);
        assertEquals(componentId, idnt.getId());
    }

    private String getRelationShipName(String type, String target) {
        String[] splitted = type.split("\\.");
        String last = splitted[splitted.length - 1];
        return last.trim() + "_" + target.trim();
    }

    @Then("^The RestResponse should not contain a nodetemplate named \"([^\"]*)\"")
    public void The_RestResponse_should_not_contain_a_nodetemplate_named(String key) throws Throwable {
        TopologyDTO topologyDTO = JsonUtil.read(Context.getInstance().getRestResponse(), TopologyDTO.class, Context.getJsonMapper()).getData();
        assertNotNull(topologyDTO);
        assertNotNull(topologyDTO.getTopology());
        assertTrue(topologyDTO.getTopology().getNodeTemplates() == null || topologyDTO.getTopology().getNodeTemplates().get(key) == null);
    }

    @Then("The RestResponse should not contain a relationship of type \"([^\"]*)\" with source \"([^\"]*)\" and target \"([^\"]*)\"")
    public void The_RestResponse_should_not_contain_a_relationship_of_type_with_source_and_target(String type, String source, String target) throws Throwable {
        TopologyDTO topologyDTO = JsonUtil.read(Context.getInstance().getRestResponse(), TopologyDTO.class, Context.getJsonMapper()).getData();
        assertNotNull(topologyDTO);
        assertNotNull(topologyDTO.getTopology());
        assertNotNull(topologyDTO.getTopology().getNodeTemplates());
        assertNotNull(topologyDTO.getTopology().getNodeTemplates().get(source));
        assertNotNull(topologyDTO.getTopology().getNodeTemplates().get(source).getRelationships() == null
                || topologyDTO.getTopology().getNodeTemplates().get(source).getRelationships().get(getRelationShipName(type, target)) == null);
    }

    @Then("^The RestResponse should contain a nodetemplate named \"([^\"]*)\" and type \"([^\"]*)\"")
    public void The_RestResponse_should_contain_a_nodetemplate_named_and_type(String key, String type) throws Throwable {
        TopologyDTO topologyDTO = JsonUtil.read(Context.getInstance().getRestResponse(), TopologyDTO.class, Context.getJsonMapper()).getData();
        assertNotNull(topologyDTO);
        assertNotNull(topologyDTO.getTopology());
        assertNotNull(topologyDTO.getTopology().getNodeTemplates());
        assertEquals(type, topologyDTO.getTopology().getNodeTemplates().get(key).getType());
    }

    @Then("^The RestResponse should contain a node type with \"([^\"]*)\" id$")
    public void The_RestResponse_should_contain_a_node_type_with_id(String expectedId) throws Throwable {
        TopologyDTO topologyDTO = JsonUtil.read(Context.getInstance().getRestResponse(), TopologyDTO.class, Context.getJsonMapper()).getData();
        assertNotNull(topologyDTO);
        assertNotNull(topologyDTO.getNodeTypes());
        assertNotNull(topologyDTO.getNodeTypes().get(expectedId.split(":")[0]));
        assertEquals(expectedId, topologyDTO.getNodeTypes().get(expectedId.split(":")[0]).getId());
    }

    @When("^I try to retrieve the created topology$")
    public void I_try_to_retrieve_the_created_topology() throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/topologies/" + Context.getInstance().getTopologyId()));
    }

    @Then("^The topology should contain a nodetemplate named \"([^\"]*)\"$")
    public void The_topology_should_contain_a_nodetemplate_named(String name) throws Throwable {
        String topologyResponseText = Context.getInstance().getRestResponse();
        RestResponse<TopologyDTO> topologyResponse = JsonUtil.read(topologyResponseText, TopologyDTO.class, Context.getJsonMapper());
        assertNotNull(topologyResponse.getData());
        Map<String, NodeTemplate> nodeTemplates = topologyResponse.getData().getTopology().getNodeTemplates();
        assertNotNull(nodeTemplates);
        assertNotNull(nodeTemplates.get(name));
    }

    @Then("^The topology should contain a nodetemplate named \"([^\"]*)\" with property \"([^\"]*)\" set to \"([^\"]*)\"$")
    public void The_topology_should_contain_a_nodetemplate_named_with_property_set_to(String nodeTemplateName, String propertyName, String propertyValue)
            throws Throwable {
        The_topology_should_contain_a_nodetemplate_named(nodeTemplateName);

        String topologyResponseText = Context.getInstance().getRestResponse();
        NodeTemplate nodeTemp = JsonUtil.read(topologyResponseText, TopologyDTO.class, Context.getJsonMapper()).getData().getTopology().getNodeTemplates()
                .get(nodeTemplateName);
        assertNotNull(nodeTemp.getProperties());
        if (propertyValue != null) {
            assertNotNull(nodeTemp.getProperties().get(propertyName));
        }
        assertEquals(propertyValue, FunctionEvaluator.getScalarValue(nodeTemp.getProperties().get(propertyName)));
    }

    @Then("^The topology should contain a nodetemplate named \"([^\"]*)\" with property \"([^\"]*)\" set to null$")
    public void The_topology_should_contain_a_nodetemplate_named_with_property_set_to_null(String nodeTemplateName, String propertyName) throws Throwable {
        The_topology_should_contain_a_nodetemplate_named_with_property_set_to(nodeTemplateName, propertyName, null);
    }

    @Then("^I should have a relationship with type \"([^\"]*)\" from \"([^\"]*)\" to \"([^\"]*)\" in ALIEN$")
    public void I_should_have_a_relationship_with_type_from_to_in_ALIEN(String relType, String source, String target) throws Throwable {
        I_should_have_a_relationship_with_type_from_to_in_ALIEN(null, relType, source, target);
    }

    @Then("^I should have a relationship \"([^\"]*)\" with type \"([^\"]*)\" from \"([^\"]*)\" to \"([^\"]*)\" in ALIEN$")
    public void I_should_have_a_relationship_with_type_from_to_in_ALIEN(String relName, String relType, String source, String target) throws Throwable {

        // I should have a relationship with type
        String topologyJson = Context.getRestClientInstance().get("/rest/v1/topologies/" + Context.getInstance().getTopologyId());
        RestResponse<TopologyDTO> topologyResponse = JsonUtil.read(topologyJson, TopologyDTO.class, Context.getJsonMapper());
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
        String topologyJson = Context.getRestClientInstance().get("/rest/v1/topologies/" + Context.getInstance().getTopologyId());
        RestResponse<TopologyDTO> topologyResponse = JsonUtil.read(topologyJson, TopologyDTO.class, Context.getJsonMapper());
        NodeTemplate sourceNode = topologyResponse.getData().getTopology().getNodeTemplates().get(source);
        RelationshipTemplate rel = sourceNode.getRelationships().get(getRelationShipName(relType, target));
        assertNotNull(rel);
        // Only one relationship of this type for the moment : cardinality check soon
        assertEquals(rel.getRequirementName(), requirementName);
        assertEquals(rel.getRequirementType(), requirementType);

    }

    @Then("^I should receive a RestResponse with constraint data name \"([^\"]*)\" and reference \"([^\"]*)\"$")
    public void I_should_receive_a_RestResponse_with_constraint_data_name_and_reference(String name, String reference) throws Throwable {
        RestResponse<?> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), Context.getJsonMapper());
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

    @When("^I check for the valid status of the topology$")
    public void I_check_for_the_valid_status_of_the_topology() throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/topologies/" + topologyId + "/isvalid?environmentId="
                + Context.getInstance().getDefaultApplicationEnvironmentId(Context.getInstance().getApplication().getName())));
    }

    @When("^I check for the valid status of the topology on the default environment$")
    public void I_check_for_the_valid_status_of_the_topology_on_the_default_environment() throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/topologies/" + topologyId + "/isvalid?environmentId="
                + Context.getInstance().getDefaultApplicationEnvironmentId(Context.getInstance().getApplication().getName())));
    }

    @Then("^the topology should be valid$")
    public void the_topology_should_be_valid() throws Throwable {
        RestResponse<?> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), Context.getJsonMapper());
        assertNotNull(restResponse.getData());
        Map<String, Object> dataMap = JsonUtil.toMap(JsonUtil.toString(restResponse.getData()));
        assertTrue(Boolean.valueOf(dataMap.get("valid").toString()));
    }

    @Then("^the topology should not be valid$")
    public void the_topology_should_not_be_valid() throws Throwable {
        RestResponse<?> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), Context.getJsonMapper());
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
        RestResponse<Map> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), Map.class, Context.getJsonMapper());
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
                return JsonUtil.toArray(Context.getInstance().getJsonMapper().writeValueAsString(suggestedNodeTypes), IndexedNodeType.class,
                        Context.getJsonMapper());
            }
        }
        return null;
    }

    @Then("^the suggested nodes types for the abstracts nodes templates should be:$")
    public void the_suggested_nodes_types_for_the_abstracts_nodes_templates_should_be(DataTable expectedSuggestedElemntIds) throws Throwable {
        RestResponse<Map> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), Map.class, Context.getJsonMapper());
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

    @When("^I update the application's input artifact \"([^\"]*)\" with \"([^\"]*)\"$")
    public void I_update_the_application_s_input_artifact_with(String artifactId, String artifactName) throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();
        String url = "/rest/v1/topologies/" + topologyId + "/inputArtifacts/" + artifactId + "/upload";
        InputStream artifactStream = Files.newInputStream(Paths.get(ARTIFACT_PATH, artifactName));
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postMultipart(url, artifactName, artifactStream));
    }

    @Then("^The topology should contain a nodetemplate named \"([^\"]*)\" with an artifact \"([^\"]*)\" with the specified UID and name \"([^\"]*)\"$")
    public void The_topology_should_contain_a_nodetemplate_named_with_an_artifact_with_the_specified_UID(String nodeTemplateName, String artifactId,
            String artifactName) throws Throwable {
        The_topology_should_contain_a_nodetemplate_named(nodeTemplateName);

        String topologyResponseText = Context.getInstance().getRestResponse();
        NodeTemplate nodeTemp = JsonUtil.read(topologyResponseText, TopologyDTO.class, Context.getJsonMapper()).getData().getTopology().getNodeTemplates()
                .get(nodeTemplateName);
        Assert.assertNotNull(nodeTemp.getArtifacts());
        Assert.assertFalse(nodeTemp.getArtifacts().isEmpty());
        DeploymentArtifact deploymentArtifact = nodeTemp.getArtifacts().get(artifactId);
        Assert.assertNotNull(deploymentArtifact);
        Assert.assertNotNull(deploymentArtifact.getArtifactType());
        Assert.assertEquals(artifactName, deploymentArtifact.getArtifactName());
    }

    @Then("^the node with requirements lowerbound not satisfied should be$")
    public void the_node_with_requirements_lowerbound_not_satisfied_should_be(DataTable expectedRequirementsNames) throws Throwable {
        RestResponse<Map> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), Map.class, Context.getJsonMapper());
        assertNotNull(restResponse.getData());
        String dataString = JsonUtil.toString(restResponse.getData());
        Map<String, Object> validationDTOMap = JsonUtil.toMap(dataString);
        Object tasklist = MapUtil.get(validationDTOMap, "taskList");
        assertNotNull(tasklist);
        for (List<String> expected : expectedRequirementsNames.raw()) {
            String[] expectedNames = expected.get(1).split(",");
            List<RequirementToSatisfy> requirementsToSatify = getRequirementsToSatisfy(expected.get(0), tasklist);
            String[] requirementsNames = getRequirementsNames(requirementsToSatify.toArray(new RequirementToSatisfy[requirementsToSatify.size()]));
            assertNotNull(requirementsNames);
            assertEquals(expectedNames.length, requirementsNames.length);
            Arrays.sort(expectedNames);
            Arrays.sort(requirementsNames);
            assertArrayEquals(expectedNames, requirementsNames);
        }
    }

    private List<RequirementToSatisfy> getRequirementsToSatisfy(String nodeTemplateName, Object taskList) throws IOException {
        for (Map<String, Object> task : (List<Map<String, Object>>) taskList) {
            String nodeTemp = (String) MapUtil.get(task, "nodeTemplateName");
            List<Object> resToImp = (List<Object>) MapUtil.get(task, "requirementsToImplement");
            if (nodeTemp.equals(nodeTemplateName) && resToImp != null) {
                return JsonUtil.toList(JsonUtil.toString(resToImp), RequirementToSatisfy.class, Context.getJsonMapper());
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

    public String[] getRequirementsNames(RequirementToSatisfy... requirementsToSatisfy) {
        String[] toReturn = null;
        for (RequirementToSatisfy requirementToSatisfy : requirementsToSatisfy) {
            toReturn = ArrayUtils.add(toReturn, requirementToSatisfy.getName());
        }

        return toReturn;
    }

    @SuppressWarnings("unchecked")
    private List<String> getRequiredPropertiesNotSet(String nodeTemplateName, Object taskList) throws IOException {
        for (Map<String, Object> task : (List<Map<String, Object>>) taskList) {
            String nodeTemp = (String) MapUtil.get(task, "nodeTemplateName");
            Map<TaskLevel, List<String>> resToImp = (Map<TaskLevel, List<String>>) MapUtil.get(task, "properties");
            if (nodeTemp.equals(nodeTemplateName) && resToImp != null) {
                return JsonUtil.toList(JsonUtil.toString(resToImp.get(TaskLevel.REQUIRED.toString())), String.class);
            }
        }
        return null;
    }

    @Then("^the scaling policy of the node \"([^\"]*)\" should match max instances equals to (\\d+), initial instances equals to (\\d+) and min instances equals to (\\d+)$")
    public void the_scaling_policy_of_the_node_should_match_max_instances_equals_to_initial_instances_equals_to_and_min_instances_equals_to(String nodeName,
            int maxInstances, int initialInstances, int minInstances) throws Throwable {
        I_try_to_retrieve_the_created_topology();
        String topologyResponseText = Context.getInstance().getRestResponse();
        RestResponse<TopologyDTO> topologyResponse = JsonUtil.read(topologyResponseText, TopologyDTO.class, Context.getJsonMapper());
        assertNotNull(topologyResponse.getData());
        ScalingPolicy computePolicy = TopologyUtils
                .getScalingPolicy(TopologyUtils.getScalableCapability(topologyResponse.getData().getTopology(), nodeName, true));
        assertNotNull(computePolicy);
        assertEquals(maxInstances, computePolicy.getMaxInstances());
        assertEquals(minInstances, computePolicy.getMinInstances());
        assertEquals(initialInstances, computePolicy.getInitialInstances());
    }

    @Then("^There's no defined scaling policy for the node \"([^\"]*)\"$")
    public void There_s_no_defined_scaling_policy(String nodeName) throws Throwable {
        I_try_to_retrieve_the_created_topology();
        String topologyResponseText = Context.getInstance().getRestResponse();
        RestResponse<TopologyDTO> topologyResponse = JsonUtil.read(topologyResponseText, TopologyDTO.class, Context.getJsonMapper());
        assertNotNull(topologyResponse.getData());
        ScalingPolicy computePolicy = TopologyUtils
                .getScalingPolicy(TopologyUtils.getScalableCapability(topologyResponse.getData().getTopology(), nodeName, true));
        assertEquals(ScalingPolicy.NOT_SCALABLE_POLICY, computePolicy);
    }

    @Then("^the node with required properties not set should be$")
    public void the_node_with_required_properties_not_set_should_be(DataTable expectedRequiredProperties) throws Throwable {
        RestResponse<Map> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), Map.class, Context.getJsonMapper());
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

    @And("^The topology should have as dependencies$")
    public void The_topology_should_have_as_dependencies(DataTable dependencies) throws Throwable {
        Set<CSARDependency> expectedDependencies = Sets.newHashSet();
        for (List<String> row : dependencies.raw()) {
            expectedDependencies.add(new CSARDependency(row.get(0), row.get(1)));
        }
        String topologyResponseText = Context.getInstance().getRestResponse();
        Set<CSARDependency> actualDependencies = JsonUtil.read(topologyResponseText, TopologyDTO.class, Context.getJsonMapper()).getData().getTopology()
                .getDependencies();
        Assert.assertEquals(expectedDependencies, actualDependencies);
    }

    @And("^If I search for topology templates I can find one with the name \"([^\"]*)\" and store the related topology as a SPEL context$")
    public void searchForTopologyTemplateByName(String topologyTemplateName) throws Throwable {
        String topologyId = getTopologyIdFromTemplateName(topologyTemplateName, null);
        assertNotNull("A topology template named " + topologyTemplateName + " can not be found", topologyId);
        String response = Context.getRestClientInstance().get("/rest/v1/topologies/" + topologyId);
        RestResponse<TopologyDTO> topologyDto = JsonUtil.read(response, TopologyDTO.class, Context.getJsonMapper());
        Context.getInstance().buildEvaluationContext(topologyDto.getData().getTopology());
    }

    @And("^If I search for topology templates I can find one with the name \"([^\"]*)\" version \"([^\"]*)\" and store the related topology as a SPEL context$")
    public void searchForTopologyTemplateByName(String topologyTemplateName, String topologyTemplateVersion) throws Throwable {
        String topologyId = getTopologyIdFromTemplateName(topologyTemplateName, topologyTemplateVersion);
        assertNotNull("A topology template named " + topologyTemplateName + " can not be found", topologyId);
        Context.getInstance().registerTopologyId(topologyId);
        String response = Context.getRestClientInstance().get("/rest/v1/topologies/" + topologyId);
        RestResponse<TopologyDTO> topologyDto = JsonUtil.read(response, TopologyDTO.class, Context.getJsonMapper());
        Context.getInstance().buildEvaluationContext(topologyDto.getData().getTopology());
    }

    private String getTopologyIdFromTemplateName(String topologyTemplateName, String topologyTemplateVersion) throws Throwable {
        String response = Context.getRestClientInstance().postJSon("/rest/v1/templates/topology/search", "{\"from\":0,\"size\":50}");
        RestResponse<FacetedSearchResult> restResponse = JsonUtil.read(response, FacetedSearchResult.class);
        String topologyTemplateId = null;
        for (Object singleResult : restResponse.getData().getData()) {
            Map map = (Map) singleResult;
            if (topologyTemplateName.equals(map.get("name"))) {
                topologyTemplateId = map.get("id").toString();
            }
        }
        assertNotNull(topologyTemplateId);
        if (topologyTemplateVersion != null) {
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.setQuery(topologyTemplateVersion);
            searchRequest.setFrom(0);
            searchRequest.setSize(Integer.MAX_VALUE);
            String templateVersionJson = Context.getRestClientInstance().postJSon("/rest/v1/templates/" + topologyTemplateId + "/versions/search",
                    JsonUtil.toString(searchRequest));
            GetMultipleDataResult<?> result = JsonUtil.read(templateVersionJson, GetMultipleDataResult.class).getData();
            assertNotNull(result);
            assertEquals(1, result.getTotalResults());
            Map<?, ?> ttv = (Map) result.getData()[0];
            assertNotNull(ttv);
            assertTrue(ttv.containsKey("topologyId"));
            return ttv.get("topologyId").toString();
        } else {
            // get the last known version of topology for this template
            String templateVersionJson = Context.getRestClientInstance().get("/rest/v1/templates/" + topologyTemplateId + "/versions/");
            TopologyTemplateVersion ttv = JsonUtil.read(templateVersionJson, TopologyTemplateVersion.class).getData();
            assertNotNull(ttv);
            assertNotNull(ttv.getTopologyId());
            return ttv.getTopologyId();
        }
    }

    @And("^I export the YAML from topology template named \"([^\"]*)\" and build a test dataset named \"([^\"]*)\"$")
    public void exportTopologyYamlAndAddItInTestData(String topologyTemplateName, String testDataName) throws Throwable {
        exportTopologyYamlAndAddItInTestDataReplacingVersion(topologyTemplateName, testDataName, null, null);
    }

    @Given("^I export the YAML from topology template named \"([^\"]*)\" and build a test dataset named \"([^\"]*)\" changing the version from \"([^\"]*)\" to \"([^\"]*)\"$")
    public void exportTopologyYamlAndAddItInTestDataReplacingVersion(String topologyTemplateName, String testDataName, String oldVersion, String newVersion)
            throws Throwable {
        String topologyId = getTopologyIdFromTemplateName(topologyTemplateName, null);
        assertNotNull("A topology template named " + topologyTemplateName + " can not be found", topologyId);
        String response = Context.getRestClientInstance().get("/rest/v1/topologies/" + topologyId + "/yaml");
        RestResponse<String> restResponse = JsonUtil.read(response, String.class, Context.getJsonMapper());
        String yaml = restResponse.getData();
        if (oldVersion != null && newVersion != null) {
            yaml = Strings.replace(yaml, "template_version: " + oldVersion, "template_version: " + newVersion);
        }
        Path dir = Files.createTempDirectory("yamlTopo");
        Path filePath = Files.createFile(Paths.get(dir.toAbsolutePath() + File.separator + "service-template.yaml"));
        FileUtils.writeStringToFile(filePath.toFile(), yaml);
        Path csarPath = TestDataRegistry.IT_ARTIFACTS_DIR.resolve(testDataName + ".csar");
        FileUtil.zip(dir, csarPath);
        FileUtil.delete(dir);
        TestDataRegistry.TEST_ARTIFACTS.put(testDataName, csarPath);
    }

    @And("^The RestResponse should contain a group named \"([^\"]*)\" whose members are \"([^\"]*)\" and policy is \"([^\"]*)\"$")
    public void The_RestResponse_should_contain_a_group_named_whose_members_are_and_policy_is(String groupName, String members, String policy)
            throws Throwable {
        String topologyResponseText = Context.getInstance().getRestResponse();
        RestResponse<TopologyDTO> topologyResponse = JsonUtil.read(topologyResponseText, TopologyDTO.class, Context.getJsonMapper());
        Assert.assertNotNull(topologyResponse.getData().getTopology().getGroups());
        NodeGroup nodeGroup = topologyResponse.getData().getTopology().getGroups().get(groupName);
        Set<String> expectedMembers = Sets.newHashSet(members.split(","));
        Assert.assertNotNull(nodeGroup);
        Assert.assertEquals(nodeGroup.getMembers(), expectedMembers);
        Assert.assertEquals(nodeGroup.getPolicies().iterator().next().getType(), policy);
        for (String expectedMember : expectedMembers) {
            NodeTemplate nodeTemplate = topologyResponse.getData().getTopology().getNodeTemplates().get(expectedMember);
            Assert.assertNotNull(nodeTemplate);
            Assert.assertTrue(nodeTemplate.getGroups().contains(groupName));
        }
    }

    @Then("^The RestResponse should not contain any group$")
    public void The_RestResponse_should_not_contain_any_group() throws Throwable {
        String topologyResponseText = Context.getInstance().getRestResponse();
        RestResponse<TopologyDTO> topologyResponse = JsonUtil.read(topologyResponseText, TopologyDTO.class, Context.getJsonMapper());
        Map<String, NodeGroup> groups = topologyResponse.getData().getTopology().getGroups();
        Assert.assertTrue(groups == null || groups.isEmpty());
    }

    @And("^The topology should have scalability policy error concerning \"([^\"]*)\"$")
    public void The_topology_should_have_scalability_policy_error_concerning(String scalabilityProperty) throws Throwable {
        RestResponse<Map> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), Map.class, Context.getJsonMapper());
        assertNotNull(restResponse.getData());
        List<Map<String, Object>> taskList = (List<Map<String, Object>>) restResponse.getData().get("taskList");
        assertNotNull(taskList);
        assertFalse(taskList.isEmpty());
        for (Map<String, Object> task : taskList) {
            if (task.get("code").equals(TaskCode.SCALABLE_CAPABILITY_INVALID.toString())) {
                ((List<String>) ((Map<String, Object>) task.get("properties")).get(TaskLevel.ERROR.toString())).contains(scalabilityProperty);
            }
        }
    }

}
