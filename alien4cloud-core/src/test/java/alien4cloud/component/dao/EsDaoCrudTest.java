package alien4cloud.component.dao;

import static org.junit.Assert.*;
import static org.springframework.util.Assert.*;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.ArrayUtils;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.mapping.ElasticSearchClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.component.model.IndexedToscaElement;
import alien4cloud.component.model.Tag;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FetchContext;
import alien4cloud.exception.IndexingServiceException;
import alien4cloud.model.application.Application;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.tosca.container.model.ToscaElement;
import alien4cloud.tosca.container.model.type.CapabilityDefinition;
import alien4cloud.tosca.container.model.type.NodeType;
import alien4cloud.tosca.container.model.type.RequirementDefinition;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context-test.xml")
@Slf4j
public class EsDaoCrudTest {
    private static final String COMPONENT_INDEX = ToscaElement.class.getSimpleName().toLowerCase();
    private static final String APPLICATION_INDEX = Application.class.getSimpleName().toLowerCase();

    private final ObjectMapper jsonMapper = new ObjectMapper();

    private List<Tag> threeTags = Lists.newArrayList(new Tag("node.icon", "my-icon.png"), new Tag("tag1", "My free tag with my free content (tag-0)"), new Tag(
            "tag2", "Tag2 content"));

    @Resource
    private ElasticSearchClient esclient;
    private Client nodeClient;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO dao;

    private IndexedNodeType indexedNodeTypeTest = null;

    @Before
    public void before() {
        nodeClient = esclient.getClient();
        prepareToscaElement();
    }

    @Test
    public void testInitIndexes() throws InterruptedException, ExecutionException, JsonGenerationException, JsonMappingException, IntrospectionException,
            IOException {
        assertIndexExists(COMPONENT_INDEX, true);
        assertIndexExists("toto", false);
        assertTypeExists(COMPONENT_INDEX, IndexedNodeType.class, true);
        assertTypeExists(COMPONENT_INDEX, "tata", false);
    }

    @Test
    public void saveToscaComponentTest() throws IndexingServiceException, IOException {
        dao.save(indexedNodeTypeTest);
        String typeName1 = indexedNodeTypeTest.getClass().getSimpleName().toLowerCase();
        assertDocumentExisit(COMPONENT_INDEX, typeName1, indexedNodeTypeTest.getId(), true);

        GetResponse resp = getDocument(COMPONENT_INDEX, typeName1, indexedNodeTypeTest.getId());
        log.info(resp.getSourceAsString());
        IndexedNodeType nt = jsonMapper.readValue(resp.getSourceAsString(), IndexedNodeType.class);

        assertBeanEqualsToOriginal(nt);
    }

    @Test(expected = IndexingServiceException.class)
    public void indexingNotSupportedToscaComponentTest() throws IndexingServiceException {
        dao.save(new String());
    }

    @Test
    public void findByIdTest() throws IndexingServiceException, JsonProcessingException {
        saveDataToES(indexedNodeTypeTest);

        IndexedNodeType nt = dao.findById(IndexedNodeType.class, indexedNodeTypeTest.getId());
        assertBeanEqualsToOriginal(nt);

        nt = dao.findById(IndexedNodeType.class, indexedNodeTypeTest.getId());
        assertBeanEqualsToOriginal(nt);

        nt = dao.findById(IndexedNodeType.class, "5");
        isNull(nt);

        // findByIds
        List<IndexedNodeType> lnt = dao.findByIds(IndexedNodeType.class, new String[] { indexedNodeTypeTest.getId() });
        List<IndexedNodeType> lnt2 = dao.findByIds(IndexedNodeType.class, new String[] { indexedNodeTypeTest.getId(), "5" });
        isTrue(!lnt.isEmpty());
        isTrue(!lnt2.isEmpty());
        assertEquals(1, lnt.size());
        assertEquals(1, lnt2.size());

        nt = lnt.get(0);
        assertBeanEqualsToOriginal(nt);

        nt = lnt2.get(0);
        assertBeanEqualsToOriginal(nt);

        // findByIdsWithContext
        saveApplications();
        List<Application> apps = dao.findByIdsWithContext(Application.class, FetchContext.DEPLOYMENT, new String[] { "1", "2", "8" });
        log.info("Search: " + JsonUtil.toString(apps));
        assertNotNull(apps);
        assertFalse(apps.isEmpty());
        assertEquals(2, apps.size());
        String[] expectedId = new String[] { "1", "2" };
        String[] ids = null;
        String[] expectedNames = new String[] { "app1", "app2" };
        String[] names = null;

        for (Application application : apps) {
            ids = ArrayUtils.add(ids, application.getId());
            names = ArrayUtils.add(names, application.getName());
            assertNull(application.getDescription());
        }
        Arrays.sort(expectedId);
        Arrays.sort(ids);
        Arrays.sort(expectedNames);
        Arrays.sort(names);

        assertArrayEquals(expectedId, ids);
        assertArrayEquals(expectedNames, names);

    }

    private void saveApplications() {
        Application app = new Application();
        app.setId("1");
        app.setName("app1");
        app.setDescription("this is app1");
        dao.save(app);
        app.setId("2");
        app.setName("app2");
        app.setDescription("this is app2");
        dao.save(app);
        app.setId("3");
        app.setName("app3");
        app.setDescription("this is app3");
        dao.save(app);
    }

    @Test
    public void deleteToscaComponentSuccessfulTest() throws IndexingServiceException, JsonProcessingException {

        String typeName1 = indexedNodeTypeTest.getClass().getSimpleName();

        saveDataToES(indexedNodeTypeTest);
        dao.delete(IndexedNodeType.class, indexedNodeTypeTest.getId());
        assertDocumentExisit(COMPONENT_INDEX, typeName1, indexedNodeTypeTest.getId(), false);

        saveDataToES(indexedNodeTypeTest);
        dao.delete(IndexedNodeType.class, indexedNodeTypeTest.getId());
        assertDocumentExisit(COMPONENT_INDEX, typeName1, indexedNodeTypeTest.getId(), false);

        saveDataToES(indexedNodeTypeTest);
        dao.delete(indexedNodeTypeTest.getClass(), indexedNodeTypeTest.getId());
        assertDocumentExisit(COMPONENT_INDEX, typeName1, indexedNodeTypeTest.getId(), false);
    }

    @Test(expected = IndexingServiceException.class)
    public void unsupportedIndexedDeletionTest() throws JsonProcessingException, IndexingServiceException {
        saveDataToES(indexedNodeTypeTest);
        dao.delete(NodeType.class, "");
    }

    @Test
    public void createAndUpdateToscaElement() {
        // Saving indexednodetype with creationdate
        dao.save(indexedNodeTypeTest);
        IndexedNodeType indexedNodeType = dao.findById(IndexedNodeType.class, indexedNodeTypeTest.getId());
        assertEquals(indexedNodeType.getCreationDate(), indexedNodeTypeTest.getCreationDate());

        // Updating
        List<Tag> updatedTags = Lists.newArrayList();
        updatedTags.add(new Tag("tag2", "UPDATE tag2 value"));
        updatedTags.add(new Tag("NEWTAG", "UPDATE new tag"));
        updateAndSaveIndexedToscaElement(updatedTags);

        indexedNodeType = dao.findById(IndexedNodeType.class, indexedNodeTypeTest.getId());

        assertEquals(indexedNodeType.getCreationDate(), indexedNodeTypeTest.getCreationDate());
        assertTrue(indexedNodeType.getTags().contains(new Tag("NEWTAG", null)));
        assertTrue("LastUpdateDate date should be greater than creationDate date", indexedNodeType.getLastUpdateDate().after(indexedNodeType.getCreationDate()));
    }

    private void updateAndSaveIndexedToscaElement(final List<Tag> tags) {
        Date creationDate = indexedNodeTypeTest.getCreationDate();
        indexedNodeTypeTest.getTags().addAll(tags);
        // Update after creation
        indexedNodeTypeTest.getLastUpdateDate().setTime(creationDate.getTime() + 3600);
        dao.save(indexedNodeTypeTest);
    }

    private void assertIndexExists(String indexName, boolean expected) throws InterruptedException, ExecutionException {
        final ActionFuture<IndicesExistsResponse> indexExistFuture = nodeClient.admin().indices().exists(new IndicesExistsRequest(indexName));
        final IndicesExistsResponse response = indexExistFuture.get();
        assertEquals(expected, response.isExists());
    }

    private void assertTypeExists(String indexName, Class<?> clazz, boolean expected) throws InterruptedException, ExecutionException, JsonGenerationException,
            JsonMappingException, IntrospectionException, IOException {
        assertTypeExists(indexName, clazz.getSimpleName().toLowerCase(), expected);
    }

    private void assertTypeExists(String indexName, String typeToCheck, boolean expected) throws InterruptedException, ExecutionException,
            JsonGenerationException, JsonMappingException, IntrospectionException, IOException {
        final ActionFuture<TypesExistsResponse> typeExistsFuture = nodeClient.admin().indices()
                .typesExists(new TypesExistsRequest(new String[] { indexName }, typeToCheck));
        final TypesExistsResponse response = typeExistsFuture.get();
        assertEquals(expected, response.isExists());
    }

    private void assertDocumentExisit(String indexName, String typeName, String id, boolean expected) {
        GetResponse response = getDocument(indexName, typeName, id);
        assertEquals(expected, response.isExists());
        assertEquals(expected, !response.isSourceEmpty());
    }

    private GetResponse getDocument(String indexName, String typeName, String id) {
        return nodeClient.prepareGet(indexName, typeName, id).execute().actionGet();
    }

    private void prepareToscaElement() {
        Set<CapabilityDefinition> capa = new HashSet<>(Arrays.asList(new CapabilityDefinition("container", "container", 1, 1), new CapabilityDefinition(
                "container1", "container1", 1, 1), new CapabilityDefinition("container2", "container2", 1, 1), new CapabilityDefinition("container3",
                "container3", 1, 1)));
        Set<RequirementDefinition> req = new HashSet<>(Arrays.asList(new RequirementDefinition("Runtime", "Runtime", null, 1, 1), new RequirementDefinition(
                "server", "server", null, 1, 1), new RequirementDefinition("blob", "blob", null, 1, 1)));
        Set<String> der = new HashSet<>(Arrays.asList("Parent1", "Parent2"));
        indexedNodeTypeTest = createIndexedNodeType("1", "positive", "1.0", "", capa, req, der, new HashSet<String>(), threeTags, new Date(), new Date());
    }

    private void saveDataToES(IndexedToscaElement element) throws JsonProcessingException {
        String json = jsonMapper.writeValueAsString(element);
        String typeName = IndexedNodeType.class.getSimpleName().toLowerCase();
        nodeClient.prepareIndex(COMPONENT_INDEX, typeName).setSource(json).setRefresh(true).execute().actionGet();

        assertDocumentExisit(COMPONENT_INDEX, typeName, element.getId(), true);
    }

    private <T> void assertBeanEqualsToOriginal(T bean) {
        if (bean instanceof IndexedNodeType) {
            IndexedNodeType indexedNodeType = (IndexedNodeType) bean;
            assertEquals(indexedNodeTypeTest.getId(), indexedNodeType.getId());
            assertEquals(indexedNodeTypeTest.getArchiveName(), indexedNodeType.getArchiveName());
            assertEquals(0, indexedNodeType.getDefaultCapabilities().size());
            assertEquals(indexedNodeTypeTest.getRequirements().size(), indexedNodeType.getRequirements().size());
            assertEquals(indexedNodeTypeTest.getTags().size(), indexedNodeType.getTags().size());
            assertEquals(indexedNodeTypeTest.getTags(), threeTags);
        }
    }

    private void clearIndex(String indexName, Class<?> clazz) throws InterruptedException {
        String typeName = clazz.getSimpleName().toLowerCase();
        log.info("Cleaning ES Index " + COMPONENT_INDEX + " and type " + typeName);
        nodeClient.prepareDeleteByQuery(indexName).setQuery(QueryBuilders.matchAllQuery()).setTypes(typeName).execute().actionGet();
    }

    private static IndexedNodeType createIndexedNodeType(String id, String archiveName, String archiveVersion, String description,
            Set<CapabilityDefinition> capabilities, Set<RequirementDefinition> requirements, Set<String> derivedFroms, Set<String> defaultCapabilities,
            List<Tag> tags, Date creationDate, Date lastUpdateDate) {
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
        nodeType.setCreationDate(creationDate);
        nodeType.setLastUpdateDate(lastUpdateDate);
        return nodeType;
    }

    @After
    public void cleanup() throws InterruptedException {
        clearIndex(COMPONENT_INDEX, IndexedNodeType.class);
        clearIndex(APPLICATION_INDEX, Application.class);
    }
}
