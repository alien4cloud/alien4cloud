package alien4cloud.component.dao;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.mapping.ElasticSearchClient;
import org.elasticsearch.mapping.MappingBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.component.model.Tag;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchFacet;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.IndexingServiceException;
import alien4cloud.tosca.container.model.ToscaElement;
import alien4cloud.tosca.container.model.type.CapabilityDefinition;
import alien4cloud.tosca.container.model.type.RequirementDefinition;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

/**
 *
 * Test class for Search operation on ElasticSearch
 *
 * @author 'Igor Ngouagna'
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context-test.xml")
@Slf4j
public class EsDaoSearchTest {

    private static final String COMPONENT_INDEX = ToscaElement.class.getSimpleName().toLowerCase();
    private final ObjectMapper jsonMapper = new ObjectMapper();

    @Resource
    ElasticSearchClient esclient;
    Client nodeClient;
    @Resource(name = "alien-es-dao")
    IGenericSearchDAO dao;

    List<IndexedNodeType> dataTest = new ArrayList<>();

    IndexedNodeType indexedNodeTypeTest = null;
    IndexedNodeType indexedNodeTypeTest2 = null;
    IndexedNodeType indexedNodeTypeTest3 = null;
    IndexedNodeType indexedNodeTypeTest4 = null;

    private List<Tag> threeTags = Lists.newArrayList(new Tag("icon", "my-icon.png"), new Tag("tag1", "My free tag with my free content (tag-0)"), new Tag(
            "tag2", "Tag2 content"));

    @Before
    public void before() throws JsonProcessingException, InterruptedException {
        nodeClient = esclient.getClient();
        prepareToscaElement();
        saveDataToES(true);
    }

    @Test
    public void simpleSearchTest() throws IndexingServiceException, InterruptedException, IOException {
        // test simple find all search
        GetMultipleDataResult searchResp = dao.find(IndexedNodeType.class, null, 10);
        assertNotNull(searchResp);
        assertEquals(4, searchResp.getTypes().length);
        assertEquals(4, searchResp.getData().length);

        // test simple find with filters
        Map<String, String[]> filters = new HashMap<String, String[]>();
        filters.put("capabilities.type", new String[] { "container" });
        searchResp = dao.find(IndexedNodeType.class, filters, 10);
        assertNotNull(searchResp);
        assertNotNull(searchResp.getTypes());
        assertNotNull(searchResp.getData());
        assertEquals(2, searchResp.getTypes().length);
        assertEquals(2, searchResp.getData().length);
    }

    @Test
    public void searchInTagsTest() throws IndexingServiceException, InterruptedException, IOException {

        // test simple find all search
        GetMultipleDataResult searchResp = dao.find(IndexedNodeType.class, null, 10);
        assertNotNull(searchResp);
        assertEquals(4, searchResp.getTypes().length);
        assertEquals(4, searchResp.getData().length);

        // test simple find with filters
        Map<String, String[]> filters = new HashMap<String, String[]>();
        filters.put("tags", new String[] { "My" });
        searchResp = dao.find(IndexedNodeType.class, filters, 10);
        assertNotNull(searchResp);
        assertNotNull(searchResp.getTypes());
        assertNotNull(searchResp.getData());
        assertEquals(4, searchResp.getTypes().length);
        assertEquals(4, searchResp.getData().length);
    }

    @Test
    public void textBasedSearch() throws IndexingServiceException, JsonParseException, JsonMappingException, IOException, InterruptedException {
        // text search based
        String searchText = "positive";

        GetMultipleDataResult searchResp = dao.search(IndexedNodeType.class, searchText, null, 10);
        assertNotNull(searchResp);
        assertEquals(2, searchResp.getTypes().length);
        assertEquals(2, searchResp.getData().length);
        String[] ids = new String[] { indexedNodeTypeTest.getId(), indexedNodeTypeTest4.getId() };

        for (int i = 0; i < searchResp.getData().length; i++) {
            IndexedNodeType idnt = (IndexedNodeType) searchResp.getData()[i];
            assertElementIn(idnt.getId(), ids);
        }

        // text search based with filters
        Map<String, String[]> filters = new HashMap<String, String[]>();
        filters.put("capabilities.type", new String[] { "container" });
        searchResp = dao.search(IndexedNodeType.class, searchText, filters, 10);
        assertNotNull(searchResp);
        assertNotNull(searchResp.getTypes());
        assertNotNull(searchResp.getData());
        assertEquals(1, searchResp.getTypes().length);
        assertEquals(1, searchResp.getData().length);
        IndexedNodeType idnt = (IndexedNodeType) searchResp.getData()[0];
        assertElementIn(idnt.getElementId(), new String[] { "1" });

        // test nothing found
        searchText = "pacpac";
        searchResp = dao.search(IndexedNodeType.class, searchText, null, 10);
        assertNotNull(searchResp);
        assertNotNull(searchResp.getData());
        assertNotNull(searchResp.getTypes());
        assertEquals(0, searchResp.getData().length);
        assertEquals(0, searchResp.getTypes().length);

    }

    @Test
    public void facetedSearchTest() throws IndexingServiceException, JsonParseException, JsonMappingException, IOException, InterruptedException {
        String searchText = "positive";

        FacetedSearchResult searchResp = dao.facetedSearch(IndexedNodeType.class, searchText, null, 10);
        assertNotNull(searchResp);
        assertEquals(2, searchResp.getTotalResults());
        assertEquals(2, searchResp.getTypes().length);
        assertEquals(2, searchResp.getData().length);
        String[] ids = new String[] { indexedNodeTypeTest.getId(), indexedNodeTypeTest4.getId() };

        for (int i = 0; i < searchResp.getData().length; i++) {
            IndexedNodeType idnt = (IndexedNodeType) searchResp.getData()[i];
            assertElementIn(idnt.getId(), ids);
        }

        // test facets
        Map<String, FacetedSearchFacet[]> mapp = searchResp.getFacets();
        FacetedSearchFacet[] capaFacets = mapp.get("capabilities.type");
        assertNotNull(capaFacets);

        boolean warExist = false;
        long warCount = 0;
        for (int i = 0; i < capaFacets.length; i++) {
            if (capaFacets[i].getFacetValue().equals("war")) {
                warExist = true;
                warCount = capaFacets[i].getCount();
            }
        }

        assertTrue(warExist);
        assertEquals(2, warCount);

        // faceted search with filters
        Map<String, String[]> filters = new HashMap<String, String[]>();
        filters.put("capabilities.type", new String[] { "container" });

        searchResp = dao.facetedSearch(IndexedNodeType.class, searchText, filters, 10);
        assertNotNull(searchResp);

        assertEquals(1, searchResp.getTotalResults());
        assertEquals(1, searchResp.getTypes().length);
        assertEquals(1, searchResp.getData().length);
        IndexedNodeType idnt = (IndexedNodeType) searchResp.getData()[0];
        assertElementIn(idnt.getElementId(), new String[] { "1" });

        // test nothing found
        searchText = "pacpac";
        searchResp = dao.facetedSearch(IndexedNodeType.class, searchText, null, 10);
        assertNotNull(searchResp);
        assertNotNull(searchResp.getData());
        assertNotNull(searchResp.getTypes());
        assertEquals(0, searchResp.getData().length);
        assertEquals(0, searchResp.getTypes().length);

    }

    private void assertElementIn(Object element, Object[] elements) {
        assertTrue(Arrays.asList(elements).contains(element));
    }

    private void prepareToscaElement() {

        Set<CapabilityDefinition> capa = new HashSet<>(Arrays.asList(new CapabilityDefinition("container", "container", 1, 1), new CapabilityDefinition(
                "container1", "container1", 1, 1), new CapabilityDefinition("container2", "container2", 1, 1), new CapabilityDefinition("container3",
                "container3", 1, 1), new CapabilityDefinition("war", "war", 1, 1)));
        Set<RequirementDefinition> req = new HashSet<>(Arrays.asList(new RequirementDefinition("Runtime", "Runtime", null, 1, 1), new RequirementDefinition(
                "server", "server", null, 1, 1), new RequirementDefinition("blob", "blob", null, 1, 1)));
        Set<String> der = new HashSet<>(Arrays.asList("Parent1", "Parent2"));
        indexedNodeTypeTest = createIndexedNodeType("1", "positive", "1.0", "", capa, req, der, new HashSet<String>(), threeTags);
        dataTest.add(indexedNodeTypeTest);

        capa = new HashSet<>(Arrays.asList(new CapabilityDefinition("banana", "banana", 1, 1), new CapabilityDefinition("banana1", "banana1", 1, 1),
                new CapabilityDefinition("container", "container", 1, 1), new CapabilityDefinition("banana3", "banana3", 1, 1), new CapabilityDefinition("zar",
                        "zar", 1, 1)));
        req = new HashSet<>(Arrays.asList(new RequirementDefinition("Pant", "Pant", null, 1, 1), new RequirementDefinition("DBZ", "DBZ", null, 1, 1),
                new RequirementDefinition("Animes", "Animes", null, 1, 1)));
        der = new HashSet<>(Arrays.asList("Songoku", "Kami"));
        indexedNodeTypeTest2 = createIndexedNodeType("2", "pokerFace", "1.0", "", capa, req, der, new HashSet<String>(), threeTags);
        dataTest.add(indexedNodeTypeTest2);

        capa = new HashSet<>(Arrays.asList(new CapabilityDefinition("potatoe", "potatoe", 1, 1), new CapabilityDefinition("potatoe2", "potatoe2", 1, 1),
                new CapabilityDefinition("potatoe3", "potatoe3", 1, 1), new CapabilityDefinition("potatoe4", "potatoe4", 1, 1), new CapabilityDefinition("zor",
                        "zor", 1, 1)));
        req = new HashSet<>(Arrays.asList(new RequirementDefinition("OnePiece", "OnePiece", null, 1, 1), new RequirementDefinition("beelzebub", "beelzebub",
                null, 1, 1), new RequirementDefinition("DBGT", "DBGT", null, 1, 1)));
        der = new HashSet<>(Arrays.asList("Jerome", "Sandrini"));
        indexedNodeTypeTest3 = createIndexedNodeType("3", "nagative", "1.5", "", capa, req, der, new HashSet<String>(), threeTags);
        dataTest.add(indexedNodeTypeTest3);

        capa = new HashSet<>(Arrays.asList(new CapabilityDefinition("yams", "yams", 1, 1), new CapabilityDefinition("yams1", "yams1", 1, 1),
                new CapabilityDefinition("positiveYes", "positiveYes", 1, 1), new CapabilityDefinition("yams3", "yams3", 1, 1), new CapabilityDefinition(
                        "war world", "war world", 1, 1)));
        req = new HashSet<>(Arrays.asList(new RequirementDefinition("Naruto", "Naruto", null, 1, 1), new RequirementDefinition("FT", "FT", null, 1, 1),
                new RequirementDefinition("Bleach", "Bleach", null, 1, 1)));
        der = new HashSet<>(Arrays.asList("Luc", "Boutier"));
        indexedNodeTypeTest4 = createIndexedNodeType("4", "pokerFace", "2.0", "", capa, req, der, new HashSet<String>(), threeTags);
        dataTest.add(indexedNodeTypeTest4);
    }

    private void saveDataToES(boolean refresh) throws JsonProcessingException {
        for (IndexedNodeType datum : dataTest) {
            String json = jsonMapper.writeValueAsString(datum);
            String typeName = MappingBuilder.indexTypeFromClass(datum.getClass());
            nodeClient.prepareIndex(COMPONENT_INDEX, typeName).setSource(json).setRefresh(refresh).execute().actionGet();

            assertDocumentExisit(COMPONENT_INDEX, typeName, datum.getId(), true);
        }
    }

    private void assertDocumentExisit(String indexName, String typeName, String id, boolean expected) {
        GetResponse response = getDocument(indexName, typeName, id);
        assertEquals(expected, response.isExists());
        assertEquals(expected, !response.isSourceEmpty());
    }

    private GetResponse getDocument(String indexName, String typeName, String id) {
        return nodeClient.prepareGet(indexName, typeName, id).execute().actionGet();
    }

    private void clearIndex(String indexName, Class<?> clazz) throws InterruptedException {
        String typeName = MappingBuilder.indexTypeFromClass(clazz);
        log.info("Cleaning ES Index " + COMPONENT_INDEX + " and type " + typeName);
        nodeClient.prepareDeleteByQuery(indexName).setQuery(QueryBuilders.matchAllQuery()).setTypes(typeName).execute().actionGet();
    }

    @After
    public void cleanup() throws InterruptedException {
        clearIndex(COMPONENT_INDEX, IndexedNodeType.class);
    }

    private static IndexedNodeType createIndexedNodeType(String id, String archiveName, String archiveVersion, String description,
            Set<CapabilityDefinition> capabilities, Set<RequirementDefinition> requirements, Set<String> derivedFroms, Set<String> defaultCapabilities,
            List<Tag> tags) {
        IndexedNodeType nodeType = new IndexedNodeType();
        nodeType.setElementId(id);
        nodeType.setArchiveName(archiveName);
        nodeType.setArchiveVersion(archiveVersion);
        nodeType.setCapabilities(capabilities);
        nodeType.setDescription(description);
        nodeType.setDefaultCapabilities(defaultCapabilities);
        nodeType.setRequirements(requirements);
        nodeType.setDerivedFrom(derivedFroms);
        nodeType.setTags(tags);
        return nodeType;
    }
}
