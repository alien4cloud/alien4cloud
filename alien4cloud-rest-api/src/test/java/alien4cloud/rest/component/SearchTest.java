package alien4cloud.rest.component;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

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
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.rest.component.ComponentController;
import alien4cloud.rest.component.QueryComponentType;
import alien4cloud.rest.component.SearchRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.tosca.container.model.ToscaElement;
import alien4cloud.tosca.container.model.type.CapabilityDefinition;
import alien4cloud.tosca.container.model.type.RequirementDefinition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context-search-test.xml")
@Slf4j
public class SearchTest {

    private static final String COMPONENT_INDEX = ToscaElement.class.getSimpleName().toLowerCase();
    private final ObjectMapper jsonMapper = new ObjectMapper();

    @Resource
    ElasticSearchClient esclient;
    Client nodeClient;
    @Resource(name = "alien-es-dao")
    IGenericSearchDAO dao;

    @Resource
    ComponentController componentController;

    List<IndexedNodeType> dataTest = new ArrayList<>();

    IndexedNodeType indexedNodeTypeTest = null;
    IndexedNodeType indexedNodeTypeTest2 = null;
    IndexedNodeType indexedNodeTypeTest3 = null;
    IndexedNodeType indexedNodeTypeTest4 = null;
    private static final int NUMBER_ELEMENT = 10;

    @Before
    public void before() throws JsonProcessingException, InterruptedException {
        nodeClient = esclient.getClient();
        prepareToscaElement();
        saveDataToES(true);
    }

    @Test
    public void searchPostTest() {
        String query = "positive";
        RestResponse<FacetedSearchResult> response;
        SearchRequest req;
        FacetedSearchResult data;
        String[] ids;
        // without filters
        req = new SearchRequest(QueryComponentType.NODE_TYPE, query, 0, NUMBER_ELEMENT, null);
        response = componentController.search(req, true);
        assertNotNull(response);
        assertNotNull(response.getData());
        assertNull(response.getError());
        data = response.getData();
        assertEquals(2, data.getTotalResults());
        assertEquals(2, data.getTypes().length);
        assertEquals(2, data.getData().length);
        ids = new String[] { indexedNodeTypeTest.getId(), indexedNodeTypeTest4.getId() };

        for (int i = 0; i < data.getData().length; i++) {
            IndexedNodeType idnt = (IndexedNodeType) data.getData()[i];
            assertElementIn(idnt.getId(), ids);
        }

        // filter based test
        Map<String, String[]> filters = new HashMap<String, String[]>();
        filters.put("capabilities.type", new String[] { "container", "banana" });
        req = new SearchRequest(QueryComponentType.NODE_TYPE, query, 0, NUMBER_ELEMENT, filters);
        response = componentController.search(req, true);
        assertNotNull(response);
        assertNotNull(response.getData());
        assertNull(response.getError());
        data = response.getData();
        assertEquals(1, data.getTotalResults());
        assertEquals(1, data.getTypes().length);
        assertEquals(1, data.getData().length);
        IndexedNodeType idnt = (IndexedNodeType) data.getData()[0];
        assertElementIn(idnt.getId(), new String[] { indexedNodeTypeTest.getId() });

        // test nothing found
        query = "pacpac";
        req = new SearchRequest(QueryComponentType.NODE_TYPE, query, 0, NUMBER_ELEMENT, null);
        response = componentController.search(req, true);
        assertNotNull(response);
        assertNotNull(response.getData());
        assertNull(response.getError());
        data = response.getData();
        assertNotNull(data.getTypes());
        assertEquals(0, data.getTotalResults());
        assertEquals(0, data.getData().length);
        assertEquals(0, data.getTypes().length);
    }

    private void prepareToscaElement() {
        Set<CapabilityDefinition> capa = new HashSet<CapabilityDefinition>(Arrays.asList(new CapabilityDefinition("container", "container", 1, 1),
                new CapabilityDefinition("container1", "container1", 1, 1), new CapabilityDefinition("container2", "container2", 1, 1),
                new CapabilityDefinition("container3", "container3", 1, 1), new CapabilityDefinition("war", "war", 1, 1)));
        Set<RequirementDefinition> req = new HashSet<RequirementDefinition>(Arrays.asList(new RequirementDefinition("Runtime", "Runtime", null, 1, 1),
                new RequirementDefinition("server", "server", null, 1, 1), new RequirementDefinition("blob", "blob", null, 1, 1)));

        Set<String> der = new HashSet<String>(Arrays.asList("Parent1", "Parent2"));
        indexedNodeTypeTest = createIndexedNodeType("1", "positive", "1.0", "", capa, req, der, new HashSet<String>());
        indexedNodeTypeTest.setId("1");
        dataTest.add(indexedNodeTypeTest);

        capa = new HashSet<>(Arrays.asList(new CapabilityDefinition("banana", "banana", 1, 1), new CapabilityDefinition("banana1", "banana1", 1, 1),
                new CapabilityDefinition("container", "container", 1, 1), new CapabilityDefinition("banana3", "banana3", 1, 1), new CapabilityDefinition("zar",
                        "zar", 1, 1)));
        req = new HashSet<>(Arrays.asList(new RequirementDefinition("Pant", "Pant", null, 1, 1), new RequirementDefinition("DBZ", "DBZ", null, 1, 1),
                new RequirementDefinition("Animes", "Animes", null, 1, 1)));
        der = new HashSet<String>(Arrays.asList("Songoku", "Kami"));
        indexedNodeTypeTest2 = createIndexedNodeType("2", "pokerFace", "1.0", "", capa, req, der, new HashSet<String>());
        dataTest.add(indexedNodeTypeTest2);

        capa = new HashSet<>(Arrays.asList(new CapabilityDefinition("potatoe", "potatoe", 1, 1), new CapabilityDefinition("potatoe2", "potatoe2", 1, 1),
                new CapabilityDefinition("potatoe3", "potatoe3", 1, 1), new CapabilityDefinition("potatoe4", "potatoe4", 1, 1), new CapabilityDefinition("zor",
                        "zor", 1, 1)));
        req = new HashSet<>(Arrays.asList(new RequirementDefinition("OnePiece", "OnePiece", null, 1, 1), new RequirementDefinition("beelzebub", "beelzebub",
                null, 1, 1), new RequirementDefinition("DBGT", "DBGT", null, 1, 1)));
        der = new HashSet<String>(Arrays.asList("Jerome", "Sandrini"));
        indexedNodeTypeTest3 = createIndexedNodeType("3", "nagative", "1.5", "", capa, req, der, new HashSet<String>());
        dataTest.add(indexedNodeTypeTest3);

        capa = new HashSet<>(Arrays.asList(new CapabilityDefinition("yams", "yams", 1, 1), new CapabilityDefinition("yams1", "yams1", 1, 1),
                new CapabilityDefinition("positiveYes", "positiveYes", 1, 1), new CapabilityDefinition("yams3", "yams3", 1, 1), new CapabilityDefinition(
                        "war world", "war world", 1, 1)));
        req = new HashSet<>(Arrays.asList(new RequirementDefinition("Naruto", "Naruto", null, 1, 1), new RequirementDefinition("FT", "FT", null, 1, 1),
                new RequirementDefinition("Bleach", "Bleach", null, 1, 1)));
        der = new HashSet<String>(Arrays.asList("Luc", "Boutier"));
        indexedNodeTypeTest4 = createIndexedNodeType("4", "pokerFace", "2.0", "", capa, req, der, new HashSet<String>());
        dataTest.add(indexedNodeTypeTest4);
    }

    private void saveDataToES(boolean refresh) throws JsonProcessingException {
        for (IndexedNodeType datum : dataTest) {
            String json = jsonMapper.writeValueAsString(datum);
            String typeName = MappingBuilder.indexTypeFromClass(datum.getClass());
            nodeClient.prepareIndex(COMPONENT_INDEX, typeName).setSource(json).setRefresh(refresh).execute().actionGet();
        }
    }

    private void clearIndex(String indexName, Class<?> clazz) throws InterruptedException {
        String typeName = clazz.getSimpleName();
        log.info("Cleaning ES Index " + COMPONENT_INDEX + " and type " + typeName);
        nodeClient.prepareDeleteByQuery(indexName).setQuery(QueryBuilders.matchAllQuery()).setTypes(typeName).execute().actionGet();
    }

    private void assertElementIn(String element, String[] elements) {
        assertTrue(Arrays.asList(elements).contains(element));
    }

    @After
    public void cleanup() throws InterruptedException {
        clearIndex(COMPONENT_INDEX, IndexedNodeType.class);
    }

    private static IndexedNodeType createIndexedNodeType(String id, String archiveName, String archiveVersion, String description,
            Set<CapabilityDefinition> capabilities, Set<RequirementDefinition> requirements, Set<String> derivedFroms, Set<String> defaultCapabilities) {
        IndexedNodeType nodeType = new IndexedNodeType();
        nodeType.setElementId(id);
        nodeType.setArchiveName(archiveName);
        nodeType.setArchiveVersion(archiveVersion);
        nodeType.setCapabilities(capabilities);
        nodeType.setDescription(description);
        nodeType.setDefaultCapabilities(defaultCapabilities);
        nodeType.setRequirements(requirements);
        nodeType.setDerivedFrom(derivedFroms);
        return nodeType;
    }
}
