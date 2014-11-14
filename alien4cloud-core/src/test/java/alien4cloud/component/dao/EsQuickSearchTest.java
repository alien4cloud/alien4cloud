package alien4cloud.component.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
import alien4cloud.dao.ElasticSearchDAO;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.IndexingServiceException;
import alien4cloud.model.application.Application;
import alien4cloud.tosca.container.model.type.CapabilityDefinition;
import alien4cloud.tosca.container.model.type.RequirementDefinition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

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
// @Ignore
public class EsQuickSearchTest {

    private static final String APPLICATION_INDEX = Application.class.getSimpleName().toLowerCase();
    private final ObjectMapper jsonMapper = new ObjectMapper();

    @Resource
    ElasticSearchClient esclient;
    Client nodeClient;
    @Resource(name = "alien-es-dao")
    IGenericSearchDAO alienDAO;

    List<IndexedNodeType> componentDataTest = new ArrayList<>();
    List<Application> applcationDataTest = new ArrayList<>();

    IndexedNodeType indexedNodeTypeTest = null;
    IndexedNodeType indexedNodeTypeTest2 = null;
    IndexedNodeType indexedNodeTypeTest3 = null;
    IndexedNodeType indexedNodeTypeTest4 = null;

    @Before
    public void before() throws JsonProcessingException, InterruptedException {
        nodeClient = esclient.getClient();
        prepareToscaElement();
        saveDataToES(true);
        Application app = new Application();
        app.setName("application-1");
        Map<String, Set<String>> userRoles = Maps.newHashMap();
        userRoles.put("Igor", Sets.newHashSet("APPLICATION_MANAGER"));
        app.setUserRoles(userRoles);
        alienDAO.save(app);
        applcationDataTest.add(app);
    }

    @Test
    public void simpleQuickSearchTest() throws IndexingServiceException, InterruptedException, IOException {
        String searchText = "app";
        GetMultipleDataResult searchResp = alienDAO.search(new String[] { APPLICATION_INDEX, ElasticSearchDAO.TOSCA_ELEMENT_INDEX }, new Class<?>[] {
                IndexedNodeType.class, Application.class }, searchText, null, null, 0, 10);
        assertNotNull(searchResp);
        assertNotNull(searchResp.getTypes());
        assertNotNull(searchResp.getData());
        assertEquals(2, searchResp.getTypes().length);
        assertEquals(2, searchResp.getData().length);
        assertElementIn("indexednodetype", searchResp.getTypes());
        assertElementIn("application", searchResp.getTypes());
    }

    private void assertElementIn(Object element, Object[] elements) {
        assertTrue(Arrays.asList(elements).contains(element));
    }

    private void prepareToscaElement() {
        List<CapabilityDefinition> capa = Arrays.asList(new CapabilityDefinition("container", "container", 1), new CapabilityDefinition("container1",
                "container1", 1), new CapabilityDefinition("container2", "container2", 1), new CapabilityDefinition("container3", "container3", 1));
        List<RequirementDefinition> req = Arrays.asList(new RequirementDefinition("Runtime", "Runtime"), new RequirementDefinition("server", "server"),
                new RequirementDefinition("blob", "blob"));
        List<String> der = Arrays.asList("app", "Parent2");
        indexedNodeTypeTest = TestModelUtil.createIndexedNodeType("1", "app-1", "1.0", "", capa, req, der, new ArrayList<String>(), null, new Date(),
                new Date());
        componentDataTest.add(indexedNodeTypeTest);

        capa = Arrays.asList(new CapabilityDefinition("banana", "banana", 1), new CapabilityDefinition("banana1", "banana1", 1), new CapabilityDefinition(
                "container", "container", 1), new CapabilityDefinition("banana3", "banana3", 1), new CapabilityDefinition("zar", "zar", 1));
        req = Arrays.asList(new RequirementDefinition("Pant", "Pant"), new RequirementDefinition("DBZ", "DBZ"), new RequirementDefinition("Animes", "Animes"));
        der = Arrays.asList("Songoku", "Kami");
        indexedNodeTypeTest2 = TestModelUtil.createIndexedNodeType("2", "pokerFace", "1.0", "", capa, req, der, new ArrayList<String>(), null, new Date(),
                new Date());
        componentDataTest.add(indexedNodeTypeTest2);
    }

    private void saveDataToES(boolean refresh) throws JsonProcessingException {
        for (IndexedNodeType datum : componentDataTest) {
            String json = jsonMapper.writeValueAsString(datum);
            String typeName = MappingBuilder.indexTypeFromClass(datum.getClass());
            nodeClient.prepareIndex(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, typeName).setSource(json).setRefresh(refresh).execute().actionGet();

            assertDocumentExisit(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, typeName, datum.getId(), true);
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
        log.info("Cleaning ES Index " + indexName + " and type " + typeName);
        nodeClient.prepareDeleteByQuery(indexName).setQuery(QueryBuilders.matchAllQuery()).setTypes(typeName).execute().actionGet();
    }

    @After
    public void cleanup() throws InterruptedException {
        clearIndex(APPLICATION_INDEX, Application.class);
        clearIndex(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, IndexedNodeType.class);
    }
}
