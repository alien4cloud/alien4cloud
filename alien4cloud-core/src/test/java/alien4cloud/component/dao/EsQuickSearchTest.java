package alien4cloud.component.dao;

import static org.junit.Assert.*;

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
import org.elasticsearch.mapping.MappingBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.dao.ElasticSearchDAO;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.IndexingServiceException;
import alien4cloud.model.application.Application;
import org.alien4cloud.tosca.model.definitions.CapabilityDefinition;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.definitions.RequirementDefinition;

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
public class EsQuickSearchTest extends AbstractDAOTest {

    private static final String APPLICATION_INDEX = Application.class.getSimpleName().toLowerCase();
    private final ObjectMapper jsonMapper = new ObjectMapper();

    @Resource(name = "alien-es-dao")
    IGenericSearchDAO alienDAO;

    List<NodeType> componentDataTest = new ArrayList<>();
    List<Application> applcationDataTest = new ArrayList<>();

    NodeType indexedNodeTypeTest = null;
    NodeType indexedNodeTypeTest2 = null;

    @Before
    public void before() throws Exception {
        super.before();
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
                NodeType.class, Application.class }, searchText, null, null, 0, 10);
        assertNotNull(searchResp);
        assertNotNull(searchResp.getTypes());
        assertNotNull(searchResp.getData());
        assertEquals(2, searchResp.getTypes().length);
        assertEquals(2, searchResp.getData().length);
        assertElementIn("nodetype", searchResp.getTypes());
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
        for (NodeType datum : componentDataTest) {
            String json = jsonMapper.writeValueAsString(datum);
            String typeName = MappingBuilder.indexTypeFromClass(datum.getClass());
            nodeClient.prepareIndex(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, typeName).setSource(json).setRefresh(refresh).execute().actionGet();

            assertDocumentExisit(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, typeName, datum.getId(), true);
            refresh();
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
}
