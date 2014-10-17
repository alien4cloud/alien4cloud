package alien4cloud.component.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

import alien4cloud.component.model.IndexedArtifactType;
import alien4cloud.component.model.IndexedCapabilityType;
import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.component.model.IndexedRelationshipType;
import alien4cloud.component.model.Tag;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.IndexingServiceException;
import alien4cloud.model.application.Application;
import alien4cloud.tosca.container.model.ToscaElement;
import alien4cloud.tosca.container.model.type.CapabilityDefinition;
import alien4cloud.tosca.container.model.type.RequirementDefinition;

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
public class EsDaoSuggestionTest {
    private static final String COMPONENT_INDEX = ToscaElement.class.getSimpleName().toLowerCase();
    private static final String APPLICATION_INDEX = Application.class.getSimpleName().toLowerCase();
    private static final String FETCH_CONTEXT = "tag_suggestion";
    private static final String TAG_NAME_PATH = "tags.name";
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

    private List<Tag> Tags1 = Lists.newArrayList(new Tag("icon", "my-icon.png"), new Tag("potatoe", "patate"), new Tag("potatoe_version", "version de patate"));

    private List<Tag> Tags2 = Lists.newArrayList(new Tag("icon", "my-icon.png"), new Tag("version", "My free tag with my free content (tag-0)"), new Tag(
            "version_1", "Tag2 content"));

    private List<Tag> Tags3 = Lists.newArrayList(new Tag("icon", "my-icon.png"), new Tag("version", "My free tag with my free content (tag-0)"), new Tag(
            "version_1", "Tag2 content"), new Tag("potatoe", "patate"), new Tag("potatoe_version", "de patate"));

    @Before
    public void before() throws JsonProcessingException, InterruptedException {
        nodeClient = esclient.getClient();
        prepareToscaElement();
        saveDataToES(true);
    }

    @Test
    public void simpleSearchTest() throws IndexingServiceException, InterruptedException, IOException {
        String searchText = "ver";
        GetMultipleDataResult searchResp = dao.suggestSearch(new String[] { APPLICATION_INDEX, COMPONENT_INDEX }, new Class<?>[] { Application.class,
                IndexedNodeType.class, IndexedArtifactType.class, IndexedCapabilityType.class, IndexedRelationshipType.class }, TAG_NAME_PATH, searchText,
                FETCH_CONTEXT, 0, 10);
        System.out.println(searchResp.getData().length);
        assertNotNull(searchResp);
        assertNotNull(searchResp.getTypes());
        assertNotNull(searchResp.getData());
        assertEquals(2, searchResp.getTypes().length);
        assertEquals(2, searchResp.getData().length);
        assertElementIn("indexednodetype", searchResp.getTypes());
    }

    private void assertElementIn(Object element, Object[] elements) {
        assertTrue(Arrays.asList(elements).contains(element));
    }

    private void prepareToscaElement() {

        indexedNodeTypeTest = createIndexedNodeType("1", "positive", "1.0", "", null, null, null, null, Tags1);
        dataTest.add(indexedNodeTypeTest);

        indexedNodeTypeTest2 = createIndexedNodeType("2", "pokerFace", "1.0", "", null, null, null, null, Tags2);
        dataTest.add(indexedNodeTypeTest2);

        indexedNodeTypeTest3 = createIndexedNodeType("3", "positive5", "1.5", "", null, null, null, null, Tags3);
        dataTest.add(indexedNodeTypeTest3);

        indexedNodeTypeTest4 = createIndexedNodeType("4", "pakerFace", "2.0", "", null, null, null, null, null);
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
