package alien4cloud.it.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.client.Client;
import org.elasticsearch.mapping.MappingBuilder;

import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.component.model.IndexedRelationshipType;
import alien4cloud.component.model.IndexedToscaElement;
import alien4cloud.dao.ElasticSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.it.Context;
import alien4cloud.rest.component.QueryComponentType;
import alien4cloud.rest.component.SearchRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.tosca.model.CapabilityDefinition;
import alien4cloud.tosca.model.RequirementDefinition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

@Slf4j
public class SearchDefinitionSteps {
    private static final String DEFAULT_ARCHIVE_VERSION = "1.0";
    private static final Map<String, QueryComponentType> QUERY_TYPES;
    private Map<String, String> indexedComponentTypes = Maps.newHashMap();
    private final ObjectMapper jsonMapper = new ObjectMapper();
    List<IndexedNodeType> testDataList = new ArrayList<>();
    List<IndexedNodeType> notYetSearchedDataList = null;

    private final Client esClient = Context.getEsClientInstance();

    static {
        QUERY_TYPES = Maps.newHashMap();
        QUERY_TYPES.put("node types", QueryComponentType.NODE_TYPE);
        QUERY_TYPES.put("capability types", QueryComponentType.CAPABILITY_TYPE);
        QUERY_TYPES.put("relationship types", QueryComponentType.RELATIONSHIP_TYPE);
        QUERY_TYPES.put("artifact types", QueryComponentType.ARTIFACT_TYPE);
    }

    @Given("^There is (\\d+) \"([^\"]*)\" indexed in ALIEN$")
    public void There_is_indexed_in_ALIEN(int count, String type) throws Throwable {
        createAndIndexComponent(count, type, 0, null, null);
    }

    @When("^I search for \"([^\"]*)\" from (\\d+) with result size of (\\d+)$")
    public void I_search_for_from_with_result_size_of(String searchedComponentType, int from, int size) throws Throwable {
        SearchRequest req = new SearchRequest(QUERY_TYPES.get(searchedComponentType), null, from, size, null);
        req.setType(req.getType());

        String jSon = jsonMapper.writeValueAsString(req);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/components/search", jSon));
    }

    @Then("^The response should contains (\\d+) elements from various types.$")
    public void The_response_should_contains_elements_from_various_types(int expectedSize) throws Throwable {
        RestResponse<FacetedSearchResult> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), FacetedSearchResult.class);
        FacetedSearchResult searchResp = restResponse.getData();
        assertNotNull(searchResp);
        assertNotNull(searchResp.getTypes());
        assertNotNull(searchResp.getData());
        assertEquals(expectedSize, searchResp.getTypes().length);
        assertEquals(expectedSize, searchResp.getData().length);

        // check various types in result
        ArrayList<String> resultTypes = new ArrayList<>(Arrays.asList(searchResp.getTypes()));
        for (String indexedType : indexedComponentTypes.values()) {
            assertTrue(" Result Types should contains  " + indexedType + ".", resultTypes.contains(indexedType));
        }
    }

    @Then("^The response should contains (\\d+) \"([^\"]*)\".$")
    public void The_response_should_contains_(int expectedSize, String searchedComponentType) throws Throwable {
        RestResponse<FacetedSearchResult> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), FacetedSearchResult.class);
        FacetedSearchResult searchResp = restResponse.getData();
        assertNotNull(searchResp);
        assertNotNull(searchResp.getTypes());
        assertNotNull(searchResp.getData());
        assertTrue(searchResp.getTypes().length >= expectedSize);
        assertTrue(searchResp.getData().length >= expectedSize);

        // check result types
        List<String> resultTypes = Lists.newArrayList(searchResp.getTypes());
        int count = 0;
        for (String type : resultTypes) {
            count = type.equals(indexedComponentTypes.get(searchedComponentType)) ? count += 1 : count;
        }
        assertEquals("There should be " + expectedSize + " " + searchedComponentType + " in result.", expectedSize, count);
    }

    @Given("^There is (\\d+) \"([^\"]*)\" indexed in ALIEN with (\\d+) of them having a \"([^\"]*)\" \"([^\"]*)\"$")
    public void There_is_indexed_in_ALIEN_with_of_them_having_a(int count, String type, int countHavingProperty, String propertyValue, String property)
            throws Throwable {
        createAndIndexComponent(count, type, countHavingProperty, property, propertyValue);
    }

    @When("^I search for \"([^\"]*)\" from (\\d+) with result size of (\\d+) and filter \"([^\"]*)\" set to \"([^\"]*)\"$")
    public void I_search_for_from_with_result_size_of_and_filter_set_to(String searchedComponentType, int from, int size, String filterName, String filterValue)
            throws Throwable {
        Map<String, String[]> filters = Maps.newHashMap();
        filters.put(filterName, new String[] { filterValue.toLowerCase() });
        SearchRequest req = new SearchRequest(QUERY_TYPES.get(searchedComponentType), null, from, size, filters);
        req.setType(req.getType());

        String jSon = jsonMapper.writeValueAsString(req);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/components/search", jSon));
    }

    @Then("^The \"([^\"]*)\" in the response should all have the \"([^\"]*)\" \"([^\"]*)\"$")
    public void The_in_the_response_should_all_have_the(String searchedComponentType, String propertyValue, String property) throws Throwable {
        RestResponse<FacetedSearchResult> restResponse = JsonUtil.read(Context.getInstance().takeRestResponse(), FacetedSearchResult.class);
        FacetedSearchResult searchResp = restResponse.getData();

        List<Object> resultData = Lists.newArrayList(searchResp.getData());
        if (searchedComponentType.equalsIgnoreCase("node types")) {
            for (Object object : resultData) {
                IndexedNodeType idnt = jsonMapper.readValue(jsonMapper.writeValueAsString(object), IndexedNodeType.class);
                switch (property) {
                case "capability":
                    assertNotNull(idnt.getCapabilities());
                    assertTrue(idnt.getCapabilities().contains(new CapabilityDefinition(propertyValue, propertyValue, 1)));
                    break;
                case "requirement":
                    assertNotNull(idnt.getRequirements());
                    assertTrue(idnt.getRequirements().contains(new RequirementDefinition(propertyValue, propertyValue)));
                    break;
                default:
                    break;
                }
            }
        } else if (searchedComponentType.equalsIgnoreCase("relationship types")) {
            for (Object object : resultData) {
                IndexedRelationshipType idrt = jsonMapper.readValue(jsonMapper.writeValueAsString(object), IndexedRelationshipType.class);
                switch (property) {
                case "validSource":
                    assertNotNull(idrt.getValidSources());
                    assertTrue(Arrays.equals(new String[] { propertyValue }, idrt.getValidSources()));
                    break;
                default:
                    break;
                }
            }
        }
    }

    @Given("^I have already made a query to search the (\\d+) first \"([^\"]*)\"$")
    public void I_have_already_made_a_query_to_search_the_first(int size, String searchedComponentType) throws Throwable {
        I_search_for_from_with_result_size_of(searchedComponentType, 0, size);
        notYetSearchedDataList = new ArrayList<>(testDataList);
        RestResponse<FacetedSearchResult> restResponse = JsonUtil.read(Context.getInstance().takeRestResponse(), FacetedSearchResult.class);
        checkPaginatedResult(size, restResponse);
    }

    @Then("^The response should contains (\\d+) other \"([^\"]*)\".$")
    public void The_response_should_contains_other(int expectedSize, String searchedComponentType) throws Throwable {
        RestResponse<FacetedSearchResult> restResponse = JsonUtil.read(Context.getInstance().takeRestResponse(), FacetedSearchResult.class);
        checkPaginatedResult(expectedSize, restResponse);
    }

    private void checkPaginatedResult(int expectedSize, RestResponse<FacetedSearchResult> restResponse) throws IOException {
        List<IndexedNodeType> toCheckInDataList = notYetSearchedDataList == null ? new ArrayList<>(testDataList) : notYetSearchedDataList;
        assertTrue(" The size of data to check (" + toCheckInDataList.size() + ") is less than the expected size (" + expectedSize + ") of search result ",
                toCheckInDataList.size() >= expectedSize);
        FacetedSearchResult searchResp;
        searchResp = restResponse.getData();
        assertNotNull(searchResp);
        assertNotNull(searchResp.getTypes());
        assertNotNull(searchResp.getData());
        assertEquals(expectedSize, searchResp.getTypes().length);
        assertEquals(expectedSize, searchResp.getData().length);

        // testing the pertinence of returned data
        Object[] data = searchResp.getData();
        for (int i = 0; i < data.length; i++) {
            IndexedNodeType nt = jsonMapper.readValue(jsonMapper.writeValueAsString(data[i]), IndexedNodeType.class);
            assertTrue(toCheckInDataList.contains(nt));
            toCheckInDataList.remove(nt);
        }
    }

    private void createAndIndexComponent(int count, String type, int countHavingProperty, String property, String propertyValue) throws Exception {
        testDataList.clear();
        Class<?> clazz = QUERY_TYPES.get(type).getIndexedToscaElementClass();
        String typeName = MappingBuilder.indexTypeFromClass(clazz);
        int remaining = countHavingProperty;
        for (int i = 0; i < count; i++) {
            IndexedToscaElement componentTemplate = (IndexedToscaElement) clazz.newInstance();
            componentTemplate.setElementId(type + "_" + i);
            componentTemplate.setArchiveVersion(DEFAULT_ARCHIVE_VERSION);
            componentTemplate.setHighestVersion(true);
            if (property != null && remaining > 0) {
                if (type.equalsIgnoreCase("node types")) {
                    switch (property) {
                    case "capability":
                        ((IndexedNodeType) componentTemplate).setCapabilities(Lists.newArrayList(new CapabilityDefinition(propertyValue, propertyValue, 1)));
                        break;
                    case "requirement":
                        ((IndexedNodeType) componentTemplate).setRequirements((Lists.newArrayList(new RequirementDefinition(propertyValue, propertyValue))));
                        break;
                    case "default capability":
                        ((IndexedNodeType) componentTemplate).setDefaultCapabilities((Lists.newArrayList(propertyValue)));
                        break;
                    case "elementId":
                        ((IndexedNodeType) componentTemplate).setElementId(propertyValue);
                        break;

                    default:
                        break;
                    }
                } else if (type.equalsIgnoreCase("relationship types")) {
                    ((IndexedRelationshipType) componentTemplate).setValidSources(new String[] { propertyValue });
                }
                remaining -= 1;
            }

            String serializeDatum = jsonMapper.writeValueAsString(componentTemplate);
            log.debug("Saving in ES: " + serializeDatum);
            esClient.prepareIndex(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, typeName).setSource(serializeDatum).setRefresh(true).execute().actionGet();

            if (componentTemplate instanceof IndexedNodeType) {
                testDataList.add((IndexedNodeType) (componentTemplate));
            }
        }

        indexedComponentTypes.put(type, typeName);
    }

}
