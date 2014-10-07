package alien4cloud.it.components;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.elasticsearch.client.Client;
import org.elasticsearch.mapping.MappingBuilder;

import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.exception.IndexingServiceException;
import alien4cloud.it.Context;
import alien4cloud.rest.component.RecommendationRequest;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.tosca.container.model.ToscaElement;
import alien4cloud.tosca.container.model.type.CapabilityDefinition;

import com.fasterxml.jackson.databind.ObjectMapper;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class DefineAsDefaultForCapabilityDefinitionsSteps {

    private static final String COMPONENT_INDEX = ToscaElement.class.getSimpleName().toLowerCase();
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final Client esClient = Context.getEsClientInstance();

    @Given("^I have a node type with id \"([^\"]*)\" and an archive version \"([^\"]*)\" with capability \"([^\"]*)\"$")
    public void I_have_a_node_type_with_id_and_an_archive_version_with_capability(String elementId, String archiveVersion, String capability) throws Throwable {
        Set<CapabilityDefinition> capabilities = new HashSet<>();
        capabilities.add(new CapabilityDefinition(capability, capability, 1, 1));
        createOneIndexNodeType(elementId, archiveVersion, capabilities, true);
    }

    @Given("^I flag the node type \"([^\"]*)\" as default for the \"([^\"]*)\" capability$")
    // @When("^I flag the node type \"([^\"]*)\" as default for the \"([^\"]*)\" capability$")
    public void I_flag_the_node_type_as_default_for_the_capability(String componentId, String capability) throws Throwable {
        RecommendationRequest recRequest = new RecommendationRequest(componentId, capability);
        String jSon = jsonMapper.writeValueAsString(recRequest);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/components/recommendation", jSon));
    }

    @When("^I search for the default node type for capability \"([^\"]*)\"$")
    public void I_search_for_the_default_node_type_for_capability(String capability) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/components/recommendation/" + capability));
    }

    @Then("^the node type id should be \"([^\"]*)\"$")
    public void the_node_type_id_should_be(String componentId) throws Throwable {
        IndexedNodeType idnt = JsonUtil.read(Context.getInstance().takeRestResponse(), IndexedNodeType.class).getData();
        assertEquals(componentId, idnt.getId());
    }

    @When("^I unflag the node type \"([^\"]*)\" as default for the \"([^\"]*)\" capability$")
    public void I_unflag_the_node_type_as_default_for_the_capability(String componentId, String capability) throws Throwable {
        RecommendationRequest recRequest = new RecommendationRequest(componentId, capability);
        String jSon = jsonMapper.writeValueAsString(recRequest);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/components/unflag", jSon));
    }

    /**
     * Load components IndexedNodeType from a file to use it in tests
     * 
     * @param componentId
     * @param refresh
     * @throws IOException
     * @throws IndexingServiceException
     */
    private void createOneIndexNodeType(String elementId, String archiveVersion, Set<CapabilityDefinition> capabilities, boolean refresh) throws IOException,
            IndexingServiceException {

        IndexedNodeType indexedNodeType = new IndexedNodeType();
        indexedNodeType.setElementId(elementId);
        indexedNodeType.setArchiveVersion(archiveVersion);
        indexedNodeType.setCapabilities(capabilities);

        String typeName = MappingBuilder.indexTypeFromClass(IndexedNodeType.class);
        String serializeDatum = jsonMapper.writeValueAsString(indexedNodeType);
        esClient.prepareIndex(COMPONENT_INDEX, typeName).setSource(serializeDatum).setRefresh(refresh).execute().actionGet();
    }
}
