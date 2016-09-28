package alien4cloud.component.dao;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.get.GetResponse;
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
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.IndexingServiceException;
import alien4cloud.model.common.Tag;
import org.alien4cloud.tosca.model.definitions.CapabilityDefinition;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.definitions.RequirementDefinition;

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
public class EsDaoSearchTest extends AbstractDAOTest {
    private final ObjectMapper jsonMapper = new ObjectMapper();

    @Resource(name = "alien-es-dao")
    IGenericSearchDAO dao;

    List<NodeType> dataTest = new ArrayList<>();

    NodeType indexedNodeTypeTest = null;
    NodeType indexedNodeTypeTest2 = null;
    NodeType indexedNodeTypeTest3 = null;
    NodeType indexedNodeTypeTest4 = null;

    private List<Tag> threeTags = Lists.newArrayList(new Tag("icon", "my-icon.png"), new Tag("tag1", "My free tag with my free content (tag-0)"), new Tag(
            "tag2", "Tag2 content"));

    @Before
    public void before() throws Exception {
        super.before();
        prepareToscaElement();
        saveDataToES(true);
    }

    @Test
    public void simpleSearchTest() throws IndexingServiceException, InterruptedException, IOException {
        // test simple find all search
        GetMultipleDataResult searchResp = dao.find(NodeType.class, null, 10);
        assertNotNull(searchResp);
        assertEquals(4, searchResp.getTypes().length);
        assertEquals(4, searchResp.getData().length);

        // test simple find with filters
        Map<String, String[]> filters = new HashMap<String, String[]>();
        filters.put("capabilities.type", new String[] { "container" });
        searchResp = dao.find(NodeType.class, filters, 10);
        assertNotNull(searchResp);
        assertNotNull(searchResp.getTypes());
        assertNotNull(searchResp.getData());
        assertEquals(2, searchResp.getTypes().length);
        assertEquals(2, searchResp.getData().length);
    }

    @Test
    public void searchInTagsTest() throws IndexingServiceException, InterruptedException, IOException {

        // test simple find all search
        GetMultipleDataResult searchResp = dao.find(NodeType.class, null, 10);
        assertNotNull(searchResp);
        assertEquals(4, searchResp.getTypes().length);
        assertEquals(4, searchResp.getData().length);

        // test simple find with filters
        Map<String, String[]> filters = new HashMap<String, String[]>();
        filters.put("tags", new String[] { "My" });
        searchResp = dao.find(NodeType.class, filters, 10);
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

        GetMultipleDataResult searchResp = dao.search(NodeType.class, searchText, null, 10);
        assertNotNull(searchResp);
        assertEquals(2, searchResp.getTypes().length);
        assertEquals(2, searchResp.getData().length);
        String[] ids = new String[] { indexedNodeTypeTest.getId(), indexedNodeTypeTest4.getId() };

        for (int i = 0; i < searchResp.getData().length; i++) {
            NodeType idnt = (NodeType) searchResp.getData()[i];
            assertElementIn(idnt.getId(), ids);
        }

        // text search based with filters
        Map<String, String[]> filters = new HashMap<String, String[]>();
        filters.put("capabilities.type", new String[] { "container" });
        searchResp = dao.search(NodeType.class, searchText, filters, 10);
        assertNotNull(searchResp);
        assertNotNull(searchResp.getTypes());
        assertNotNull(searchResp.getData());
        assertEquals(1, searchResp.getTypes().length);
        assertEquals(1, searchResp.getData().length);
        NodeType idnt = (NodeType) searchResp.getData()[0];
        assertElementIn(idnt.getElementId(), new String[] { "1" });

        // test nothing found
        searchText = "pacpac";
        searchResp = dao.search(NodeType.class, searchText, null, 10);
        assertNotNull(searchResp);
        assertNotNull(searchResp.getData());
        assertNotNull(searchResp.getTypes());
        assertEquals(0, searchResp.getData().length);
        assertEquals(0, searchResp.getTypes().length);

    }

    @Test
    public void facetedSearchTest() throws IndexingServiceException, JsonParseException, JsonMappingException, IOException, InterruptedException {
        String searchText = "positive";

        FacetedSearchResult searchResp = dao.facetedSearch(NodeType.class, searchText, null, 10);
        assertNotNull(searchResp);
        assertEquals(2, searchResp.getTotalResults());
        assertEquals(2, searchResp.getTypes().length);
        assertEquals(2, searchResp.getData().length);
        String[] ids = new String[] { indexedNodeTypeTest.getId(), indexedNodeTypeTest4.getId() };

        for (int i = 0; i < searchResp.getData().length; i++) {
            NodeType idnt = (NodeType) searchResp.getData()[i];
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

        searchResp = dao.facetedSearch(NodeType.class, searchText, filters, 10);
        assertNotNull(searchResp);

        assertEquals(1, searchResp.getTotalResults());
        assertEquals(1, searchResp.getTypes().length);
        assertEquals(1, searchResp.getData().length);
        NodeType idnt = (NodeType) searchResp.getData()[0];
        assertElementIn(idnt.getElementId(), new String[] { "1" });

        // test nothing found
        searchText = "pacpac";
        searchResp = dao.facetedSearch(NodeType.class, searchText, null, 10);
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

        List<CapabilityDefinition> capa = Arrays.asList(new CapabilityDefinition("container", "container", 1), new CapabilityDefinition("container1",
                "container1", 1), new CapabilityDefinition("container2", "container2", 1), new CapabilityDefinition("container3", "container3", 1),
                new CapabilityDefinition("war", "war", 1));
        List<RequirementDefinition> req = Arrays.asList(new RequirementDefinition("Runtime", "Runtime"), new RequirementDefinition("server", "server"),
                new RequirementDefinition("blob", "blob"));
        List<String> der = Arrays.asList("Parent1", "Parent2");
        indexedNodeTypeTest = TestModelUtil.createIndexedNodeType("1", "positive", "1.0", "", capa, req, der, new ArrayList<String>(), threeTags, new Date(),
                new Date());
        dataTest.add(indexedNodeTypeTest);

        capa = Arrays.asList(new CapabilityDefinition("banana", "banana", 1), new CapabilityDefinition("banana1", "banana1", 1), new CapabilityDefinition(
                "container", "container", 1), new CapabilityDefinition("banana3", "banana3", 1), new CapabilityDefinition("zar", "zar", 1));
        req = Arrays.asList(new RequirementDefinition("Pant", "Pant"), new RequirementDefinition("DBZ", "DBZ"), new RequirementDefinition("Animes", "Animes"));
        der = Arrays.asList("Songoku", "Kami");
        indexedNodeTypeTest2 = TestModelUtil.createIndexedNodeType("2", "pokerFace", "1.0", "", capa, req, der, new ArrayList<String>(), threeTags, new Date(),
                new Date());
        dataTest.add(indexedNodeTypeTest2);

        capa = Arrays.asList(new CapabilityDefinition("potatoe", "potatoe", 1), new CapabilityDefinition("potatoe2", "potatoe2", 1), new CapabilityDefinition(
                "potatoe3", "potatoe3", 1), new CapabilityDefinition("potatoe4", "potatoe4", 1), new CapabilityDefinition("zor", "zor", 1));
        req = Arrays.asList(new RequirementDefinition("OnePiece", "OnePiece"), new RequirementDefinition("beelzebub", "beelzebub"), new RequirementDefinition(
                "DBGT", "DBGT"));
        der = Arrays.asList("Jerome", "Sandrini");
        indexedNodeTypeTest3 = TestModelUtil.createIndexedNodeType("3", "nagative", "1.5", "", capa, req, der, new ArrayList<String>(), threeTags, new Date(),
                new Date());
        dataTest.add(indexedNodeTypeTest3);

        capa = Arrays.asList(new CapabilityDefinition("yams", "yams", 1), new CapabilityDefinition("yams1", "yams1", 1), new CapabilityDefinition(
                "positiveYes", "positiveYes", 1), new CapabilityDefinition("yams3", "yams3", 1), new CapabilityDefinition("war world", "war world", 1));
        req = Arrays
                .asList(new RequirementDefinition("Naruto", "Naruto"), new RequirementDefinition("FT", "FT"), new RequirementDefinition("Bleach", "Bleach"));
        der = Arrays.asList("Luc", "Boutier");
        indexedNodeTypeTest4 = TestModelUtil.createIndexedNodeType("4", "pokerFace", "2.0", "", capa, req, der, new ArrayList<String>(), threeTags, new Date(),
                new Date());
        dataTest.add(indexedNodeTypeTest4);
    }

    private void saveDataToES(boolean refresh) throws JsonProcessingException {
        for (NodeType datum : dataTest) {
            String json = jsonMapper.writeValueAsString(datum);
            String typeName = MappingBuilder.indexTypeFromClass(datum.getClass());
            nodeClient.prepareIndex(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, typeName).setSource(json).setRefresh(refresh).execute().actionGet();

            assertDocumentExisit(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, typeName, datum.getId(), true);
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