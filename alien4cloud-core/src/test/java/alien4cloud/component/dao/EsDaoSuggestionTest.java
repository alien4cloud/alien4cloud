package alien4cloud.component.dao;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
import alien4cloud.model.common.Tag;
import org.alien4cloud.tosca.model.types.ArtifactType;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

/**
 * Test class for suggestion operation on ElasticSearch
 * 
 * @author 'Igor Ngouagna'
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context-test.xml")
@Slf4j
public class EsDaoSuggestionTest extends AbstractDAOTest {
    private static final String APPLICATION_INDEX = Application.class.getSimpleName().toLowerCase();
    private static final String FETCH_CONTEXT = "tag_suggestion";
    private static final String TAG_NAME_PATH = "tags.name";
    private final ObjectMapper jsonMapper = new ObjectMapper();

    @Resource(name = "alien-es-dao")
    IGenericSearchDAO dao;

    List<NodeType> dataTest = new ArrayList<>();

    NodeType indexedNodeTypeTest = null;
    NodeType indexedNodeTypeTest2 = null;
    NodeType indexedNodeTypeTest3 = null;
    NodeType indexedNodeTypeTest4 = null;

    private List<Tag> Tags1 = Lists.newArrayList(new Tag("icon", "my-icon.png"), new Tag("potatoe", "patate"), new Tag("potatoe_version", "version de patate"));

    private List<Tag> Tags2 = Lists.newArrayList(new Tag("icon", "my-icon.png"), new Tag("version", "My free tag with my free content (tag-0)"), new Tag(
            "version_1", "Tag2 content"));

    private List<Tag> Tags3 = Lists.newArrayList(new Tag("icon", "my-icon.png"), new Tag("version", "My free tag with my free content (tag-0)"), new Tag(
            "version_1", "Tag2 content"), new Tag("potatoe", "patate"), new Tag("potatoe_version", "de patate"));

    @Before
    public void before() throws Exception {
        super.before();
        prepareToscaElement();
        saveDataToES(true);
    }

    @Test
    public void simpleSearchTest() throws IndexingServiceException, InterruptedException, IOException {
        String searchText = "ver";
        GetMultipleDataResult searchResp = dao.suggestSearch(new String[] { APPLICATION_INDEX, ElasticSearchDAO.TOSCA_ELEMENT_INDEX }, new Class<?>[] {
                Application.class, NodeType.class, ArtifactType.class, CapabilityType.class, RelationshipType.class },
                TAG_NAME_PATH, searchText, FETCH_CONTEXT, 0, 10);
        System.out.println(searchResp.getData().length);
        assertNotNull(searchResp);
        assertNotNull(searchResp.getTypes());
        assertNotNull(searchResp.getData());
        assertEquals(2, searchResp.getTypes().length);
        assertEquals(2, searchResp.getData().length);
        assertElementIn("nodetype", searchResp.getTypes());
    }

    private void assertElementIn(Object element, Object[] elements) {
        assertTrue(Arrays.asList(elements).contains(element));
    }

    private void prepareToscaElement() {
        indexedNodeTypeTest = TestModelUtil.createIndexedNodeType("1", "positive", "1.0", "", null, null, null, null, Tags1, new Date(), new Date());
        dataTest.add(indexedNodeTypeTest);

        indexedNodeTypeTest2 = TestModelUtil.createIndexedNodeType("2", "pokerFace", "1.0", "", null, null, null, null, Tags2, new Date(), new Date());
        dataTest.add(indexedNodeTypeTest2);

        indexedNodeTypeTest3 = TestModelUtil.createIndexedNodeType("3", "positive5", "1.5", "", null, null, null, null, Tags3, new Date(), new Date());
        dataTest.add(indexedNodeTypeTest3);

        indexedNodeTypeTest4 = TestModelUtil.createIndexedNodeType("4", "pakerFace", "2.0", "", null, null, null, null, null, new Date(), new Date());
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
