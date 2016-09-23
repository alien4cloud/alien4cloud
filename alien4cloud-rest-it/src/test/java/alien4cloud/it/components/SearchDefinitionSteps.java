package alien4cloud.it.components;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.alien4cloud.tosca.model.definitions.CapabilityDefinition;
import org.alien4cloud.tosca.model.definitions.RequirementDefinition;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.elasticsearch.client.Client;
import org.elasticsearch.mapping.MappingBuilder;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.common.AlienConstants;
import alien4cloud.dao.ElasticSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.it.Context;
import alien4cloud.rest.component.QueryComponentType;
import alien4cloud.rest.component.SearchRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SearchDefinitionSteps {
    private static final String DEFAULT_ARCHIVE_VERSION = "1.0";
    private static final Map<String, QueryComponentType> QUERY_TYPES;
    private Map<String, String> indexedComponentTypes = Maps.newHashMap();
    List<NodeType> testDataList = new ArrayList<>();
    List<NodeType> notYetSearchedDataList = null;

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
        createAndIndexComponent(count, type, null, 0, null, null);
    }

    @Given("^There is (\\d+) \"([^\"]*)\" with base name \"([^\"]*)\" indexed in ALIEN$")
    public void There_is_indexed_in_ALIEN(int count, String type, String baseName) throws Throwable {
        createAndIndexComponent(count, type, baseName, 0, null, null);
    }

    @When("^I search for \"([^\"]*)\" using query \"([^\"]*)\" from (\\d+) with result size of (\\d+)$")
    public void I_search_for_with_query_from_with_result_size_of(String searchedComponentType, String query, int from, int size) throws Throwable {
        SearchRequest req = new SearchRequest(QUERY_TYPES.get(searchedComponentType), query, from, size, null);
        req.setType(req.getType());

        String jSon = JsonUtil.toString(req);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/v1/components/search", jSon));
    }

    @When("^I search for \"([^\"]*)\" from (\\d+) with result size of (\\d+)$")
    public void I_search_for_from_with_result_size_of(String searchedComponentType, int from, int size) throws Throwable {
        SearchRequest req = new SearchRequest(QUERY_TYPES.get(searchedComponentType), null, from, size, null);
        String jSon = JsonUtil.toString(req);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/v1/components/search", jSon));
    }

    @When("^I make a basic \"([^\"]*)\" search for \"([^\"]*)\" from (\\d+) with result size of (\\d+)$")
    public void I_make_a_basic_search_for_from_with_result_size_of(String query, String searchedComponentType, int from, int size) throws Throwable {
        // BasicSearchRequest req = new BasicSearchRequest(query, from, size);
        SearchRequest req = new SearchRequest(QUERY_TYPES.get(searchedComponentType), query, from, size, null);
        String jSon = JsonUtil.toString(req);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/v1/components/search", jSon));
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
        createAndIndexComponent(count, type, null, countHavingProperty, property, propertyValue);
    }

    @When("^I search for \"([^\"]*)\" from (\\d+) with result size of (\\d+) and filter \"([^\"]*)\" set to \"([^\"]*)\"$")
    public void I_search_for_from_with_result_size_of_and_filter_set_to(String searchedComponentType, int from, int size, String filterName, String filterValue)
            throws Throwable {
        Map<String, String[]> filters = Maps.newHashMap();
        filters.put(filterName, new String[] { filterValue.toLowerCase() });
        SearchRequest req = new SearchRequest(QUERY_TYPES.get(searchedComponentType), null, from, size, filters);
        req.setType(req.getType());

        String jSon = JsonUtil.toString(req);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/v1/components/search", jSon));
    }

    @Then("^The \"([^\"]*)\" in the response should all have the \"([^\"]*)\" \"([^\"]*)\"$")
    public void The_in_the_response_should_all_have_the(String searchedComponentType, String propertyValue, String property) throws Throwable {
        RestResponse<FacetedSearchResult> restResponse = JsonUtil.read(Context.getInstance().takeRestResponse(), FacetedSearchResult.class);
        FacetedSearchResult searchResp = restResponse.getData();

        List<Object> resultData = Lists.newArrayList(searchResp.getData());
        if (searchedComponentType.equalsIgnoreCase("node types")) {
            for (Object object : resultData) {
                NodeType idnt = JsonUtil.readObject(JsonUtil.toString(object), NodeType.class);
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
                RelationshipType idrt = JsonUtil.readObject(JsonUtil.toString(object), RelationshipType.class);
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

    /**
     * tototo
     *
     * @deprecated Do not use this method! pagination is broken with aggregations
     */
    @Deprecated
    // @Given("^I have already made a query to search the (\\d+) first \"([^\"]*)\"$")
    public void I_have_already_made_a_query_to_search_the_first(int size, String searchedComponentType) throws Throwable {
        I_search_for_from_with_result_size_of(searchedComponentType, 0, size);
        notYetSearchedDataList = new ArrayList<>(testDataList);
        RestResponse<FacetedSearchResult> restResponse = JsonUtil.read(Context.getInstance().takeRestResponse(), FacetedSearchResult.class);
        checkPaginatedResult(size, restResponse);
    }

    /**
     * tototo
     *
     * @deprecated Do not use this method! pagination is broken with aggregations
     */
    @Deprecated
    // @Then("^The response should contains (\\d+) other \"([^\"]*)\".$")
    public void The_response_should_contains_other(int expectedSize, String searchedComponentType) throws Throwable {
        RestResponse<FacetedSearchResult> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), FacetedSearchResult.class);
        checkPaginatedResult(expectedSize, restResponse);
    }

    @Then("^The response should contains (\\d+) elements$")
    public void The_response_should_contains(int expectedSize) throws Throwable {
        RestResponse<FacetedSearchResult> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), FacetedSearchResult.class);
        assertEquals(expectedSize, restResponse.getData().getTotalResults());
    }

    private void checkPaginatedResult(int expectedSize, RestResponse<FacetedSearchResult> restResponse) throws IOException {
        List<NodeType> toCheckInDataList = notYetSearchedDataList == null ? new ArrayList<>(testDataList) : notYetSearchedDataList;
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
            NodeType nt = JsonUtil.readObject(JsonUtil.toString(data[i]), NodeType.class);
            assertTrue(toCheckInDataList.contains(nt));
            toCheckInDataList.remove(nt);
        }
    }

    private void createAndIndexComponent(int count, String type, String baseName, int countHavingProperty, String property, String propertyValue)
            throws Exception {
        testDataList.clear();
        Class<?> clazz = QUERY_TYPES.get(type).getIndexedToscaElementClass();
        String typeName = MappingBuilder.indexTypeFromClass(clazz);
        int remaining = countHavingProperty;
        baseName = baseName == null || baseName.isEmpty() ? typeName : baseName;
        for (int i = 0; i < count; i++) {
            AbstractToscaType componentTemplate = (AbstractToscaType) clazz.newInstance();
            String elementId = baseName + "_" + i;
            componentTemplate.setElementId(elementId);
            componentTemplate.setArchiveVersion(DEFAULT_ARCHIVE_VERSION);
            componentTemplate.setWorkspace(AlienConstants.GLOBAL_WORKSPACE_ID);

            if (property != null && remaining > 0) {
                if (type.equalsIgnoreCase("node types")) {
                    switch (property) {
                    case "capability":
                        ((NodeType) componentTemplate).setCapabilities(Lists.newArrayList(new CapabilityDefinition(propertyValue, propertyValue, 1)));
                        break;
                    case "requirement":
                        ((NodeType) componentTemplate).setRequirements((Lists.newArrayList(new RequirementDefinition(propertyValue, propertyValue))));
                        break;
                    case "default capability":
                        ((NodeType) componentTemplate).setDefaultCapabilities((Lists.newArrayList(propertyValue)));
                        break;
                    case "elementId":
                        ((NodeType) componentTemplate).setElementId(propertyValue);
                        break;

                    default:
                        break;
                    }
                } else if (type.equalsIgnoreCase("relationship types")) {
                    ((RelationshipType) componentTemplate).setValidSources(new String[] { propertyValue });
                }
                remaining -= 1;
            }

            String serializeDatum = JsonUtil.toString(componentTemplate);
            log.debug("Saving in ES: " + serializeDatum);
            esClient.prepareIndex(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, typeName).setSource(serializeDatum).setRefresh(true).execute().actionGet();

            if (componentTemplate instanceof NodeType) {
                testDataList.add((NodeType) (componentTemplate));
            }
        }

        indexedComponentTypes.put(type, typeName);
    }

    @When("^I search for all components type from (\\d+) with result size of (\\d+)$")
    public void iSearchForAllComponentsTypeFromWithResultSizeOf(int from, int size) throws Throwable {
        I_search_for_from_with_result_size_of(null, from, size);
    }
}
