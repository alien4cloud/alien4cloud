package alien4cloud.rest.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import alien4cloud.common.AlienConstants;
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

import alien4cloud.Constants;
import org.alien4cloud.tosca.model.types.NodeType;
import alien4cloud.model.common.Tag;
import alien4cloud.dao.ElasticSearchDAO;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import org.alien4cloud.tosca.model.definitions.CapabilityDefinition;

import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context-search-test.xml")
@Slf4j
public class ComponentTest {

    @Resource
    ElasticSearchClient esclient;
    Client nodeClient;
    @Resource(name = "alien-es-dao")
    IGenericSearchDAO dao;
    @Resource
    ComponentController componentController;

    private static final List<Tag> rootTags;
    private static final Map<String, CapabilityDefinition> capabilities;
    private static final Tag TAG_1, TAG_2, INTERNAL_TAG;
    private NodeType indexedNodeType, tmpIndexedNodeType, indexedNodeType2, indexedNodeType3;

    static {
        rootTags = Lists.newArrayList();
        rootTags.add(new Tag("icon", "/usr/local/root-icon.png"));
        rootTags.add(new Tag("tag1", "Root tag1 value..."));
        rootTags.add(new Tag("tag2", "Root tag2 value..."));

        TAG_1 = new Tag("tag1", "/usr/child-icon.png");
        TAG_2 = new Tag("tag2", "UPDATED - Root tag2 value...");
        INTERNAL_TAG = new Tag("icon", "/usr/child-icon.png");

        capabilities = new HashMap<String, CapabilityDefinition>();
        capabilities.put("wor", new CapabilityDefinition("wor", "wor", 1));
        capabilities.put("jdni", new CapabilityDefinition("jdni", "jdni", 1));
        capabilities.put("container", new CapabilityDefinition("container", "container", 1));
        capabilities.put("feature", new CapabilityDefinition("feature", "feature", 1));
    }

    @Before
    public void before() {
        nodeClient = esclient.getClient();
        prepareNodeTypes();
    }

    @Test
    public void updateComponentTag() {

        // Updating root tags with tagToUpdate2
        UpdateTagRequest updateComponentRequest = new UpdateTagRequest();
        // String key = (String) tagToUpdate2.keySet().toArray()[0];
        updateComponentRequest.setTagKey(TAG_2.getName());
        updateComponentRequest.setTagValue(TAG_2.getValue());

        componentController.upsertTag(indexedNodeType.getId(), updateComponentRequest);
        tmpIndexedNodeType = dao.findById(NodeType.class, indexedNodeType.getId());

        assertEquals("Tags map size should'nt change", tmpIndexedNodeType.getTags().size(), indexedNodeType.getTags().size());
        int index = tmpIndexedNodeType.getTags().indexOf(TAG_2);
        int index2 = indexedNodeType.getTags().indexOf(TAG_2);
        assertNotEquals("tag2 tag value has changed", tmpIndexedNodeType.getTags().get(index).getValue(), indexedNodeType.getTags().get(index2).getValue());
        assertEquals("tag2 tag value should be the same as TAG_2", tmpIndexedNodeType.getTags().get(index).getValue(), TAG_2.getValue());

    }

    @Test
    public void updateComponentTagWithBadComponentId() {

        UpdateTagRequest updateComponentRequest = new UpdateTagRequest();
        // String key = (String) tagToUpdate1.keySet().toArray()[0];
        updateComponentRequest.setTagKey(TAG_2.getName());
        updateComponentRequest.setTagValue(TAG_2.getValue());

        RestResponse<Void> response = componentController.upsertTag("X", updateComponentRequest);

        assertEquals("Should have <" + RestErrorCode.COMPONENT_MISSING_ERROR.getCode() + "> error code returned", response.getError().getCode(),
                RestErrorCode.COMPONENT_MISSING_ERROR.getCode());
        assertNotNull("Error message should'nt be null", response.getError().getMessage());
    }

    @Test
    public void deleteComponentTag() {

        RestResponse<Void> response = null;

        // Remove tagToDelete1
        response = componentController.deleteTag(indexedNodeType.getId(), TAG_1.getName());
        tmpIndexedNodeType = dao.findById(NodeType.class, indexedNodeType.getId());

        assertTrue("Tag <" + TAG_1 + "> does not exist anymore", !tmpIndexedNodeType.getTags().contains(TAG_1));
        assertSame("Tag map size from initial IndexedNodeType decreased", tmpIndexedNodeType.getTags().size(), rootTags.size() - 1);
        assertNull("Delete tag operation response has no error object", response.getError());

        // Remove tagToDelete2
        response = componentController.deleteTag(indexedNodeType.getId(), TAG_2.getName());
        tmpIndexedNodeType = dao.findById(NodeType.class, indexedNodeType.getId());

        assertTrue("Tag <" + TAG_2 + "> does not exist anymore", !tmpIndexedNodeType.getTags().contains(TAG_2));

        // Remove internal tag "icon"
        response = componentController.deleteTag(indexedNodeType.getId(), INTERNAL_TAG.getName());
        assertNotNull("Tag <" + INTERNAL_TAG + "> is internal and cannot be removed", response.getError());
        assertEquals("Should have <" + RestErrorCode.COMPONENT_INTERNALTAG_ERROR.getCode() + "> error code returned", response.getError().getCode(),
                RestErrorCode.COMPONENT_INTERNALTAG_ERROR.getCode());

    }

    @Test
    public void recommendForCapabilityWhenAlreadyRecommendedTest() {
        RestResponse<NodeType> response = null;

        RecommendationRequest recRequest = new RecommendationRequest();
        recRequest.setComponentId(indexedNodeType.getId());
        recRequest.setCapability("jdni");

        response = componentController.recommendComponentForCapability(recRequest);
        assertNull(response.getError());
        assertNotNull(response.getData());
        assertTrue(response.getData().getDefaultCapabilities().contains("jdni"));

        Map<String, String[]> filters = new HashMap<>();
        filters.put(Constants.DEFAULT_CAPABILITY_FIELD_NAME, new String[] { "jdni" });
        GetMultipleDataResult result = dao.find(NodeType.class, filters, 1);
        NodeType component;
        if (result == null || result.getData() == null || result.getData().length == 0) {
            component = null;
        } else {
            component = (NodeType) result.getData()[0];
        }

        assertNotNull(component);
        assertNotNull(component.getDefaultCapabilities());
        assertTrue(component.getId().equals(recRequest.getComponentId()));
        assertTrue(component.getDefaultCapabilities().contains("jdni"));
    }

    @Test
    public void recommendForCapabilityTest() {
        RestResponse<NodeType> response = null;

        RecommendationRequest recRequest = new RecommendationRequest();
        recRequest.setComponentId(indexedNodeType.getId());
        recRequest.setCapability("wor");

        response = componentController.recommendComponentForCapability(recRequest);
        assertNull(response.getError());

        NodeType component = dao.findById(NodeType.class, recRequest.getComponentId());

        assertNotNull(component.getDefaultCapabilities());
        assertEquals(1, component.getDefaultCapabilities().size());
        assertTrue(" component of Id " + component.getId() + " should contains " + "wor", component.getDefaultCapabilities().contains("wor"));
    }

    private void prepareNodeTypes() {

        indexedNodeType = new NodeType();
        indexedNodeType.setElementId("1");
        indexedNodeType.setArchiveName("tosca.nodes.Root");
        indexedNodeType.setArchiveVersion("3.0");
        indexedNodeType.setWorkspace(AlienConstants.GLOBAL_WORKSPACE_ID);
        indexedNodeType.setDerivedFrom(null);
        indexedNodeType.setDescription("Root description...");
        indexedNodeType.setTags(rootTags);
        dao.save(indexedNodeType);

        indexedNodeType2 = new NodeType();
        indexedNodeType2.setElementId("2");
        indexedNodeType2.setArchiveName("tosca.nodes.Root");
        indexedNodeType2.setArchiveVersion("3.0");
        indexedNodeType2.setWorkspace(AlienConstants.GLOBAL_WORKSPACE_ID);
        indexedNodeType2.setDerivedFrom(null);
        indexedNodeType2.setDescription("Root description...");
        indexedNodeType2.setTags(rootTags);
        indexedNodeType2.setCapabilities(new ArrayList<>(capabilities.values()));
        indexedNodeType2.setDefaultCapabilities(new ArrayList<String>());
        indexedNodeType2.getDefaultCapabilities().add("jdni");
        dao.save(indexedNodeType2);

        indexedNodeType3 = new NodeType();
        indexedNodeType3.setElementId("3");
        indexedNodeType3.setArchiveName("tosca.nodes.Root");
        indexedNodeType3.setArchiveVersion("3.0");
        indexedNodeType3.setWorkspace(AlienConstants.GLOBAL_WORKSPACE_ID);
        indexedNodeType3.setDerivedFrom(null);
        indexedNodeType3.setDescription("Root description...");
        indexedNodeType3.setTags(rootTags);
        indexedNodeType3.setCapabilities(new ArrayList<>(capabilities.values()));
        indexedNodeType3.setDefaultCapabilities(new ArrayList<String>());
        indexedNodeType3.getDefaultCapabilities().add("container");
        dao.save(indexedNodeType3);
    }

    private void clearIndex(String indexName, Class<?> clazz) throws InterruptedException {
        String typeName = MappingBuilder.indexTypeFromClass(clazz);
        log.info("Cleaning ES Index " + ElasticSearchDAO.TOSCA_ELEMENT_INDEX + " and type " + typeName);
        nodeClient.prepareDeleteByQuery(indexName).setQuery(QueryBuilders.matchAllQuery()).setTypes(typeName).execute().actionGet();
    }

    @After
    public void cleanup() throws InterruptedException {
        clearIndex(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, NodeType.class);
    }
}