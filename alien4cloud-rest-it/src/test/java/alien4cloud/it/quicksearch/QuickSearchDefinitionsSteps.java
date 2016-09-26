package alien4cloud.it.quicksearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.elasticsearch.client.Client;
import org.elasticsearch.mapping.MappingBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.common.AlienConstants;
import alien4cloud.dao.ElasticSearchDAO;
import alien4cloud.dao.ElasticSearchMapper;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.it.Context;
import alien4cloud.rest.component.QueryComponentType;
import alien4cloud.rest.model.BasicSearchRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QuickSearchDefinitionsSteps {
    private final ObjectMapper jsonMapper = ElasticSearchMapper.getInstance();

    private static final String DEFAULT_ARCHIVE_VERSION = "1.0";
    private static Map<String, String> indexedTypes = Maps.newHashMap();
    List<NodeType> testDataList = new ArrayList<>();
    List<NodeType> notYetSearchedDataList = null;
    private static final Map<String, QueryComponentType> QUERY_TYPES;

    private final Client esClient = Context.getEsClientInstance();

    static {
        QUERY_TYPES = Maps.newHashMap();
        QUERY_TYPES.put("node types", QueryComponentType.NODE_TYPE);
        QUERY_TYPES.put("capability types", QueryComponentType.CAPABILITY_TYPE);
        QUERY_TYPES.put("relationship types", QueryComponentType.RELATIONSHIP_TYPE);
        QUERY_TYPES.put("artifact types", QueryComponentType.ARTIFACT_TYPE);
        indexedTypes.put("applications", "application");
        indexedTypes.put("node types", "nodetype");
    }

    @When("^I quickly search for \"([^\"]*)\" from (\\d+) with result size of (\\d+)$")
    public void I_quickly_search_for_from_with_result_size_of(String searchText, int from, int expectedSize) throws Throwable {
        BasicSearchRequest req = new BasicSearchRequest(searchText, from, expectedSize);

        String jSon = jsonMapper.writeValueAsString(req);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/v1/quicksearch", jSon));
    }

    @Given("^There is (\\d+) \"([^\"]*)\" indexed in ALIEN with (\\d+) of them having \"([^\"]*)\" in the \"([^\"]*)\"$")
    public void There_is_indexed_in_ALIEN_with_of_them_having_in_the(int count, String type, int countHavingProperty, String propertyValue, String property)
            throws Throwable {
        createAndIndexComponent(count, type, countHavingProperty, property, propertyValue);
    }

    @Then("^The quickSearch response should contains (\\d+) elements$")
    public void The_quickSearch_response_should_contains_elements(int expectedSize) throws Throwable {
        RestResponse<GetMultipleDataResult> restResponse = JsonUtil.read(Context.getInstance().takeRestResponse(), GetMultipleDataResult.class);
        GetMultipleDataResult searchResp = restResponse.getData();

        assertNotNull(searchResp);
        assertNotNull(searchResp.getTypes());
        assertNotNull(searchResp.getData());
        assertEquals(expectedSize, searchResp.getTypes().length);
        assertEquals(expectedSize, searchResp.getData().length);
    }

    @Then("^The quickSearch response should contains (\\d+) \"([^\"]*)\"$")
    public void The_quickSearch_response_should_contains(int expectedSize, String searchedType) throws Throwable {
        RestResponse<GetMultipleDataResult> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), GetMultipleDataResult.class);
        GetMultipleDataResult searchResp = restResponse.getData();

        assertNotNull(searchResp);
        assertNotNull(searchResp.getTypes());
        assertNotNull(searchResp.getData());
        assertTrue(searchResp.getTypes().length >= expectedSize);
        assertTrue(searchResp.getData().length >= expectedSize);

        // check result types
        List<String> resultTypes = Lists.newArrayList(searchResp.getTypes());
        String esType = indexedTypes.get(searchedType);
        int count = Collections.frequency(Lists.newArrayList(searchResp.getTypes()), esType);
        assertEquals("There should be " + expectedSize + " " + searchedType + " in the quicksearch result.", expectedSize, count);
    }

    private void createAndIndexComponent(int count, String type, int countHavingProperty, String property, String propertyValue) throws Exception {
        testDataList.clear();
        Class<?> clazz = QUERY_TYPES.get(type).getIndexedToscaElementClass();
        String typeName = MappingBuilder.indexTypeFromClass(clazz);
        int remaining = countHavingProperty;
        for (int i = 0; i < count; i++) {
            AbstractToscaType componentTemplate = (AbstractToscaType) clazz.newInstance();
            componentTemplate.setElementId(type + "_" + i);
            componentTemplate.setArchiveVersion(DEFAULT_ARCHIVE_VERSION);
            componentTemplate.setWorkspace(AlienConstants.GLOBAL_WORKSPACE_ID);

            if (property != null && remaining > 0) {
                if (type.equalsIgnoreCase("node types")) {
                    switch (property) {
                    case "elementId":
                        ((NodeType) componentTemplate).setElementId(propertyValue + "_" + remaining);
                        break;

                    default:
                        break;
                    }
                } else if (type.equalsIgnoreCase("relationship types")) {
                    ((RelationshipType) componentTemplate).setValidSources(new String[] { propertyValue });
                }
                remaining -= 1;
            }

            String serializeDatum = jsonMapper.writeValueAsString(componentTemplate);
            log.debug("Saving in ES: " + serializeDatum);
            esClient.prepareIndex(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, typeName).setSource(serializeDatum).setRefresh(true).execute().actionGet();

            if (componentTemplate instanceof NodeType) {
                testDataList.add((NodeType) (componentTemplate));
            }
        }
        indexedTypes.put(type, typeName);
    }
}