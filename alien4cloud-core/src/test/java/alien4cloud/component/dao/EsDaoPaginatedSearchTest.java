package alien4cloud.component.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import alien4cloud.common.AlienConstants;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.mapping.MappingBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.dao.ElasticSearchDAO;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchFacet;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.FetchContext;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.IndexingServiceException;
import org.alien4cloud.tosca.model.definitions.CapabilityDefinition;
import org.alien4cloud.tosca.model.types.NodeType;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context-test.xml")
@Slf4j
public class EsDaoPaginatedSearchTest extends AbstractDAOTest {
    private final ObjectMapper jsonMapper = new ObjectMapper();

    public EsDaoPaginatedSearchTest() {
        super();
        jsonMapper.setVisibility(PropertyAccessor.ALL, Visibility.PUBLIC_ONLY);
    }

    @Resource(name = "alien-es-dao")
    IGenericSearchDAO dao;

    List<NodeType> testDataList = new ArrayList<>();
    List<NodeType> jndiTestDataList = new ArrayList<>();

    @Override
    @Before
    public void before() throws Exception {
        super.before();
        saveDataToES();
    }

    @Test
    public void simpleFindPaginatedTest() throws IOException {

        int maxElement;
        int size;

        // test simple find all search
        maxElement = getCount(QueryBuilders.matchAllQuery());
        size = 11;

        assertTrue(maxElement > 0);
        testSimpleSearchWellPaginated(maxElement, size, null);

        // test simple find with filters
        FilterBuilder filter = FilterBuilders.termFilter("capabilities.type", "jndi");
        QueryBuilder queryBuilder = QueryBuilders.matchAllQuery();
        queryBuilder = QueryBuilders.filteredQuery(queryBuilder, filter);

        maxElement = getCount(queryBuilder);
        size = 4;
        assertTrue(maxElement > 0);
        Map<String, String[]> filters = new HashMap<String, String[]>();
        filters.put("capabilities.type", new String[] { "jndi" });
        testSimpleSearchWellPaginated(maxElement, size, filters);

    }

    @Test
    public void textBasedSearchPaginatedTest() throws IndexingServiceException, IOException, InterruptedException {

        // text search based
        String searchText = "jndi";
        int maxElement = getCount(QueryBuilders.matchPhrasePrefixQuery("_all", searchText).maxExpansions(10));
        int size = 7;
        assertTrue(maxElement > 0);
        testTextBasedSearchWellPaginated(maxElement, size, searchText, null);

        // text search based with filters
        FilterBuilder filter = FilterBuilders.termFilter("capabilities.type", "jndi");
        QueryBuilder queryBuilder = QueryBuilders.matchAllQuery();
        queryBuilder = QueryBuilders.filteredQuery(queryBuilder, filter);
        maxElement = getCount(queryBuilder);
        Map<String, String[]> filters = new HashMap<String, String[]>();
        filters.put("capabilities.type", new String[] { "jndi" });
        assertTrue(maxElement > 0);
        testTextBasedSearchWellPaginated(maxElement, size, searchText, filters);

        // test when nothing found
        searchText = "pacpac";
        maxElement = getCount(QueryBuilders.matchPhrasePrefixQuery("_all", searchText).maxExpansions(10));
        assertEquals(0, maxElement);
        GetMultipleDataResult<NodeType> searchResp = dao.search(NodeType.class, searchText, null, 0, size);
        assertNotNull(searchResp);
        assertNotNull(searchResp.getData());
        assertNotNull(searchResp.getTypes());
        assertEquals(0, searchResp.getData().length);
        assertEquals(0, searchResp.getTypes().length);

    }

    // @Ignore
    @Test
    public void facetedSearchPaginatedTest() throws IndexingServiceException, IOException, InterruptedException {
        String searchText = "jndi";
        int maxElement = getCount(QueryBuilders.matchPhrasePrefixQuery("_all", searchText).maxExpansions(10));
        int size = 7;

        // simple faceted pagination
        assertTrue(maxElement > 0);
        testFacetedSearchWellPaginated(maxElement, size, searchText, null, null);

        // faceted search with filters
        FilterBuilder filter = FilterBuilders.termFilter("capabilities.type", "jndi");
        QueryBuilder queryBuilder = QueryBuilders.matchAllQuery();
        queryBuilder = QueryBuilders.filteredQuery(queryBuilder, filter);
        maxElement = getCount(queryBuilder);

        Map<String, String[]> filters = new HashMap<String, String[]>();
        filters.put("capabilities.type", new String[] { "jndi" });

        assertTrue(maxElement > 0);
        testFacetedSearchWellPaginated(maxElement, size, searchText, filters, FetchContext.SUMMARY);

        // test nothing found
        // test when nothing found
        searchText = "pacpac";
        maxElement = getCount(QueryBuilders.matchPhrasePrefixQuery("_all", searchText).maxExpansions(10));
        assertEquals(0, maxElement);
        GetMultipleDataResult<NodeType> searchResp = dao.facetedSearch(NodeType.class, searchText, null, null, 0, size);
        assertNotNull(searchResp);
        assertNotNull(searchResp.getData());
        assertNotNull(searchResp.getTypes());
        assertEquals(0, searchResp.getData().length);
        assertEquals(0, searchResp.getTypes().length);

    }

    private static boolean filterContainsValue(Map<String, String[]> filters, String filterValue) {
        for (String[] values : filters.values()) {
            for (String value : values) {
                if ((value == null && filterValue == null) || (value != null && value.equals(filterValue))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void testSimpleSearchWellPaginated(int maxElement, int size, Map<String, String[]> filters) throws IOException {
        List<NodeType> expectedDataList = filters != null && filterContainsValue(filters, "jndi") ? new ArrayList<>(jndiTestDataList)
                : new ArrayList<>(testDataList);
        GetMultipleDataResult<NodeType> searchResp;
        int expectedSize;
        for (int from = 0; from < maxElement; from += size) {
            expectedSize = (maxElement - from) > size ? size : maxElement - from;
            searchResp = dao.find(NodeType.class, filters, from, size);
            assertNotNull(searchResp);
            assertNotNull(searchResp.getTypes());
            assertNotNull(searchResp.getData());
            assertEquals(expectedSize, searchResp.getTypes().length);
            assertEquals(expectedSize, searchResp.getData().length);

            // testing the pertinence of returned data
            Object[] data = searchResp.getData();
            for (Object element : data) {
                NodeType nt = jsonMapper.readValue(jsonMapper.writeValueAsString(element), NodeType.class);
                assertTrue(expectedDataList.contains(nt));
                expectedDataList.remove(nt);
            }
        }

        // assert the list is empty at the end.
        assertTrue(expectedDataList.isEmpty());
    }

    private void testFacetedSearchWellPaginated(int maxElement, int size, String searchText, Map<String, String[]> filters, String fetchContext)
            throws IOException {
        List<NodeType> expectedDataList = new ArrayList<>(jndiTestDataList);
        String facetNameToCheck;
        String factetValueToCheck;
        long expectedFacetCount = 0;
        FacetedSearchResult<?> searchResp;
        int expectedSize = 0;
        boolean facetToCheckExist = false;
        for (int from = 0; from < maxElement; from += size) {
            expectedSize = (maxElement - from) > size ? size : maxElement - from;
            searchResp = dao.facetedSearch(NodeType.class, searchText, filters, fetchContext, from, size);
            assertNotNull(searchResp);
            assertNotNull(searchResp.getTypes());
            assertNotNull(searchResp.getData());
            assertEquals(expectedSize, searchResp.getTypes().length);
            assertEquals(expectedSize, searchResp.getData().length);

            // testing the pertinence of returned data
            Object[] data = searchResp.getData();
            for (Object element : data) {
                NodeType nt = jsonMapper.readValue(jsonMapper.writeValueAsString(element), NodeType.class);

                // TODO assert fetch context result.
                assertTrue(expectedDataList.contains(nt));
                expectedDataList.remove(nt);
            }

            // test returned facets
            if (filters != null && filters.containsKey("capabilities.type")) {
                facetNameToCheck = "requirements.type";
                factetValueToCheck = "network";
                expectedFacetCount = 6;
            } else {
                facetNameToCheck = "capabilities.type";
                factetValueToCheck = "war";
                expectedFacetCount = 4;
            }
            assertNotNull(searchResp.getFacets());
            assertTrue(!searchResp.getFacets().isEmpty());
            FacetedSearchFacet[] facets = searchResp.getFacets().get(facetNameToCheck);
            assertNotNull(facets);
            long facetCount = 0;
            for (FacetedSearchFacet facet : facets) {
                if (facet.getFacetValue().equals(factetValueToCheck)) {
                    facetToCheckExist = true;
                    facetCount = facet.getCount();
                }
            }

            assertTrue(facetToCheckExist);
            assertEquals(expectedFacetCount, facetCount);

        }

        // assert the list is empty at the end.
        assertTrue(expectedDataList.isEmpty());

    }

    private void testTextBasedSearchWellPaginated(int maxElement, int size, String searchText, Map<String, String[]> filters) throws IOException {
        List<NodeType> expectedDataList = new ArrayList<>(jndiTestDataList);
        GetMultipleDataResult searchResp;
        int expectedSize;
        for (int from = 0; from < maxElement; from += size) {
            expectedSize = (maxElement - from) > size ? size : maxElement - from;
            searchResp = dao.search(NodeType.class, searchText, filters, from, size);
            assertNotNull(searchResp);
            assertNotNull(searchResp.getTypes());
            assertNotNull(searchResp.getData());
            assertEquals(expectedSize, searchResp.getTypes().length);
            assertEquals(expectedSize, searchResp.getData().length);

            // testing the pertinence of returned data
            Object[] data = searchResp.getData();
            for (Object element : data) {
                NodeType nt = jsonMapper.readValue(jsonMapper.writeValueAsString(element), NodeType.class);
                assertTrue(expectedDataList.contains(nt));
                expectedDataList.remove(nt);
            }
        }

        // assert the list is empty at the end.
        assertTrue(expectedDataList.isEmpty());
    }

    private int getCount(QueryBuilder queryBuilder) {
        return (int) nodeClient.prepareCount(ElasticSearchDAO.TOSCA_ELEMENT_INDEX).setTypes(MappingBuilder.indexTypeFromClass(NodeType.class))
                .setQuery(queryBuilder).execute().actionGet().getCount();
    }

    private void saveDataToES() throws IOException, IndexingServiceException {
        testDataList.clear();

        Path path = Paths.get("src/test/resources/nodetypes-faceted-search-result.json");
        FacetedSearchResult res = jsonMapper.readValue(path.toFile(), FacetedSearchResult.class);
        Object[] data = res.getData();
        for (Object element : data) {
            String serializeDatum = jsonMapper.writeValueAsString(element);
            NodeType indexedNodeType = jsonMapper.readValue(serializeDatum, NodeType.class);
            indexedNodeType.setWorkspace(AlienConstants.GLOBAL_WORKSPACE_ID);
            String typeName = MappingBuilder.indexTypeFromClass(NodeType.class);
            dao.save(indexedNodeType);
            assertDocumentExisit(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, typeName, indexedNodeType.getId(), true);
            testDataList.add(indexedNodeType);
            for (CapabilityDefinition capaDef : indexedNodeType.getCapabilities()) {
                if (capaDef.getType().equals("jndi")) {
                    jndiTestDataList.add(indexedNodeType);
                }
            }
        }
        refresh();
    }

    private void assertDocumentExisit(String indexName, String typeName, String id, boolean expected) {
        GetResponse response = getDocument(indexName, typeName, id);
        assertEquals(expected, response.isExists());
        assertEquals(expected, !response.isSourceEmpty());
    }

    private GetResponse getDocument(String indexName, String typeName, String id) {
        return nodeClient.prepareGet(indexName, typeName, id).execute().actionGet();
    }
}