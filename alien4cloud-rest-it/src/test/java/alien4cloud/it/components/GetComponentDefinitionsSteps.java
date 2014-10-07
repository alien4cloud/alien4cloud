package alien4cloud.it.components;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.elasticsearch.client.Client;
import org.elasticsearch.mapping.MappingBuilder;

import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.dao.ElasticSearchDAO;
import alien4cloud.exception.IndexingServiceException;
import alien4cloud.it.Context;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.tosca.container.model.ToscaElement;
import alien4cloud.utils.MapUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class GetComponentDefinitionsSteps {

    private static final String COMPONENT_INDEX = ToscaElement.class.getSimpleName().toLowerCase();
    private final ObjectMapper jsonMapper = new ElasticSearchDAO.ElasticSearchMapper();
    private final Client esClient = Context.getEsClientInstance();

    @Given("^I have a component with uuid \"([^\"]*)\"$")
    public void I_have_a_component_with_uuid(String componentId) throws Throwable {
        saveDataToES(componentId, true);
    }

    @When("^I get the component with uuid \"([^\"]*)\"$")
    public void I_get_the_component_with_uuid(String componentId) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/components/" + componentId));
    }

    @Then("^I should retrieve a component detail with list of it's properties and interfaces.$")
    public void I_should_retrieve_a_component_detail_with_list_of_it_s_properties_and_interfaces() throws Throwable {
        IndexedNodeType idnt = JsonUtil.read(Context.getInstance().takeRestResponse(), IndexedNodeType.class).getData();
        assertNotNull(idnt);
        assertNotNull(idnt.getProperties());
        assertTrue(!idnt.getProperties().isEmpty());
        assertTrue(!idnt.getProperties().values().isEmpty());
        assertNotNull(idnt.getInterfaces());
    }

    @When("^I try to get a component with id \"([^\"]*)\"$")
    public void I_try_to_get_a_component_with_id(String componentId) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/components/" + componentId));
    }

    @Then("^I should have a component with id \"([^\"]*)\"$")
    public void I_should_have_a_component_with_id(String componentId) throws Throwable {
        RestResponse<Map> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), Map.class);
        assertNotNull(restResponse.getData());
        String id = (String) MapUtil.get(restResponse.getData(), "id");
        assertEquals(componentId, id);
    }

    private void saveDataToES(String componentId, boolean refresh) throws IOException, IndexingServiceException {
        String samplePathString = "src/test/resources/data/components/indexed_nodetypes.json";
        Path path = Paths.get(samplePathString);
        List<Object> tempList = jsonMapper.readValue(path.toFile(), ArrayList.class);
        List<IndexedNodeType> idntList = new ArrayList<>();
        for (Object ob : tempList) {
            idntList.add(jsonMapper.readValue(jsonMapper.writeValueAsString(ob), IndexedNodeType.class));
        }
        String typeName = MappingBuilder.indexTypeFromClass(IndexedNodeType.class);
        if (componentId != null && !componentId.trim().isEmpty()) {
            for (IndexedNodeType indexedNodeType : idntList) {
                if (indexedNodeType.getId().equalsIgnoreCase(componentId)) {
                    String serializeDatum = jsonMapper.writeValueAsString(indexedNodeType);
                    esClient.prepareIndex(COMPONENT_INDEX, typeName).setSource(serializeDatum).setRefresh(refresh).execute().actionGet();
                    return;
                }
            }
            fail("No component with id <" + componentId + "> found in the sample file <" + samplePathString + ">.");
        } else {
            for (IndexedNodeType indexedNodeType : idntList) {
                String serializeDatum = jsonMapper.writeValueAsString(indexedNodeType);
                esClient.prepareIndex(COMPONENT_INDEX, typeName).setSource(serializeDatum).setRefresh(refresh).execute().actionGet();
            }
        }
    }

}
