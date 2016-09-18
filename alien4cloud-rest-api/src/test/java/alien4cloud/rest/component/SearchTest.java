package alien4cloud.rest.component;

import static org.junit.Assert.*;

import java.util.*;

import javax.annotation.Resource;

import alien4cloud.common.AlienConstants;
import org.alien4cloud.tosca.model.definitions.CapabilityDefinition;
import org.alien4cloud.tosca.model.definitions.RequirementDefinition;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.alien4cloud.tosca.model.types.NodeType;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import alien4cloud.dao.ElasticSearchDAO;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.rest.model.RestResponse;
import lombok.extern.slf4j.Slf4j;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context-search-test.xml")
@Slf4j
public class SearchTest {
    private final ObjectMapper jsonMapper = new ObjectMapper();

    @Resource
    ElasticSearchClient esclient;
    Client nodeClient;
    @Resource(name = "alien-es-dao")
    IGenericSearchDAO dao;

    @Resource
    ComponentController componentController;

    List<NodeType> dataTest = new ArrayList<>();

    NodeType indexedNodeTypeTest = null;
    NodeType indexedNodeTypeTest2 = null;
    NodeType indexedNodeTypeTest3 = null;
    NodeType indexedNodeTypeTest4 = null;
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
        RestResponse<FacetedSearchResult<? extends AbstractToscaType>> response;
        SearchRequest req;
        FacetedSearchResult data;
        String[] ids;
        // without filters
        req = new SearchRequest(QueryComponentType.NODE_TYPE, query, 0, NUMBER_ELEMENT, null);
        response = componentController.search(req);
        assertNotNull(response);
        assertNotNull(response.getData());
        assertNull(response.getError());
        data = response.getData();
        assertEquals(2, data.getTotalResults());
        assertEquals(2, data.getTypes().length);
        assertEquals(2, data.getData().length);
        ids = new String[] { indexedNodeTypeTest.getId(), indexedNodeTypeTest4.getId() };

        for (int i = 0; i < data.getData().length; i++) {
            NodeType idnt = (NodeType) data.getData()[i];
            assertElementIn(idnt.getId(), ids);
        }

        // filter based test
        Map<String, String[]> filters = new HashMap<String, String[]>();
        filters.put("capabilities.type", new String[] { "container", "banana" });
        req = new SearchRequest(QueryComponentType.NODE_TYPE, query, 0, NUMBER_ELEMENT, filters);
        response = componentController.search(req);
        assertNotNull(response);
        assertNotNull(response.getData());
        assertNull(response.getError());
        data = response.getData();
        assertEquals(1, data.getTotalResults());
        assertEquals(1, data.getTypes().length);
        assertEquals(1, data.getData().length);
        NodeType idnt = (NodeType) data.getData()[0];
        assertElementIn(idnt.getId(), new String[] { indexedNodeTypeTest.getId() });

        // test nothing found
        query = "pacpac";
        req = new SearchRequest(QueryComponentType.NODE_TYPE, query, 0, NUMBER_ELEMENT, null);
        response = componentController.search(req);
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
        List<CapabilityDefinition> capa = Lists.newArrayList(new CapabilityDefinition("container", "container", 1),
                new CapabilityDefinition("container1", "container1", 1), new CapabilityDefinition("container2", "container2", 1),
                new CapabilityDefinition("container3", "container3", 1), new CapabilityDefinition("war", "war", 1));
        List<RequirementDefinition> req = Lists.newArrayList(new RequirementDefinition("Runtime", "Runtime"), new RequirementDefinition("server", "server"),
                new RequirementDefinition("blob", "blob"));

        List<String> der = Lists.newArrayList("Parent1", "Parent2");
        indexedNodeTypeTest = createIndexedNodeType("1", "positive", "1.0", "", capa, req, der, new ArrayList<String>());
        indexedNodeTypeTest.setId("1");
        dataTest.add(indexedNodeTypeTest);

        capa = Lists.newArrayList(new CapabilityDefinition("banana", "banana", 1), new CapabilityDefinition("banana1", "banana1", 1),
                new CapabilityDefinition("container", "container", 1), new CapabilityDefinition("banana3", "banana3", 1),
                new CapabilityDefinition("zar", "zar", 1));
        req = Lists.newArrayList(new RequirementDefinition("Pant", "Pant"), new RequirementDefinition("DBZ", "DBZ"),
                new RequirementDefinition("Animes", "Animes"));
        der = Lists.newArrayList("Songoku", "Kami");
        indexedNodeTypeTest2 = createIndexedNodeType("2", "pokerFace", "1.0", "", capa, req, der, new ArrayList<String>());
        dataTest.add(indexedNodeTypeTest2);

        capa = Lists.newArrayList(new CapabilityDefinition("potatoe", "potatoe", 1), new CapabilityDefinition("potatoe2", "potatoe2", 1),
                new CapabilityDefinition("potatoe3", "potatoe3", 1), new CapabilityDefinition("potatoe4", "potatoe4", 1),
                new CapabilityDefinition("zor", "zor", 1));
        req = Lists.newArrayList(new RequirementDefinition("OnePiece", "OnePiece"), new RequirementDefinition("beelzebub", "beelzebub"),
                new RequirementDefinition("DBGT", "DBGT"));
        der = Lists.newArrayList("Jerome", "Sandrini");
        indexedNodeTypeTest3 = createIndexedNodeType("3", "nagative", "1.5", "", capa, req, der, new ArrayList<String>());
        dataTest.add(indexedNodeTypeTest3);

        capa = Lists.newArrayList(new CapabilityDefinition("yams", "yams", 1), new CapabilityDefinition("yams1", "yams1", 1),
                new CapabilityDefinition("positiveYes", "positiveYes", 1), new CapabilityDefinition("yams3", "yams3", 1),
                new CapabilityDefinition("war world", "war world", 1));
        req = Lists.newArrayList(new RequirementDefinition("Naruto", "Naruto"), new RequirementDefinition("FT", "FT"),
                new RequirementDefinition("Bleach", "Bleach"));
        der = Lists.newArrayList("Luc", "Boutier");
        indexedNodeTypeTest4 = createIndexedNodeType("4", "pokerFace", "2.0", "", capa, req, der, new ArrayList<String>());
        dataTest.add(indexedNodeTypeTest4);
    }

    private void saveDataToES(boolean refresh) throws JsonProcessingException {
        for (NodeType datum : dataTest) {
            String json = jsonMapper.writeValueAsString(datum);
            String typeName = MappingBuilder.indexTypeFromClass(datum.getClass());
            nodeClient.prepareIndex(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, typeName).setSource(json).setRefresh(refresh).execute().actionGet();
        }
    }

    private void clearIndex(String indexName, Class<?> clazz) throws InterruptedException {
        String typeName = clazz.getSimpleName();
        log.info("Cleaning ES Index " + ElasticSearchDAO.TOSCA_ELEMENT_INDEX + " and type " + typeName);
        nodeClient.prepareDeleteByQuery(indexName).setQuery(QueryBuilders.matchAllQuery()).setTypes(typeName).execute().actionGet();
    }

    private void assertElementIn(String element, String[] elements) {
        assertTrue(Arrays.asList(elements).contains(element));
    }

    @After
    public void cleanup() throws InterruptedException {
        clearIndex(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, NodeType.class);
    }

    private static NodeType createIndexedNodeType(String id, String archiveName, String archiveVersion, String description,
            List<CapabilityDefinition> capabilities, List<RequirementDefinition> requirements, List<String> derivedFroms, List<String> defaultCapabilities) {
        NodeType nodeType = new NodeType();
        nodeType.setElementId(id);
        nodeType.setArchiveName(archiveName);
        nodeType.setArchiveVersion(archiveVersion);
        nodeType.setWorkspace(AlienConstants.GLOBAL_WORKSPACE_ID);
        nodeType.setCapabilities(capabilities);
        nodeType.setDescription(description);
        nodeType.setDefaultCapabilities(defaultCapabilities);
        nodeType.setRequirements(requirements);
        nodeType.setDerivedFrom(derivedFroms);
        return nodeType;
    }
}
