package alien4cloud.it.components;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.alien4cloud.tosca.model.types.NodeType;
import org.elasticsearch.client.Client;
import org.elasticsearch.mapping.MappingBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import alien4cloud.common.AlienConstants;
import alien4cloud.dao.ElasticSearchDAO;
import alien4cloud.dao.ElasticSearchMapper;
import alien4cloud.exception.IndexingServiceException;
import alien4cloud.it.Context;
import alien4cloud.model.common.Tag;
import alien4cloud.rest.component.UpdateTagRequest;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.DataTable;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class UpdateDeleteTagDefinitionsSteps {
    private final ObjectMapper jsonMapper = ElasticSearchMapper.getInstance();

    private final Client esClient = Context.getEsClientInstance();

    @Given("^I have a component with id \"([^\"]*)\"$")
    public void I_have_a_component_with_id(String componentId) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/components/" + componentId));
        NodeType idnt = JsonUtil.read(Context.getInstance().takeRestResponse(), NodeType.class).getData();
        assertNotNull(idnt);
        Context.getInstance().registerComponentId(idnt.getId());
    }

    @Given("^I have a fake component with a bad id \"([^\"]*)\"$")
    public void I_have_a_fake_component_with_a_bad_id(String badComponentId) throws Throwable {
        Context.getInstance().registerComponentId(badComponentId);
    }

    @Given("^I have a component with and id \"([^\"]*)\" and an archive version \"([^\"]*)\" with tags:$")
    public void I_have_a_component_with_and_id_and_an_archive_version_with_tags(String componentId, String archiveVersion, DataTable tags) throws Throwable {
        List<Tag> nodeTypeTags = Lists.newArrayList();
        for (List<String> rows : tags.raw()) {
            nodeTypeTags.add(new Tag(rows.get(0), rows.get(1)));
        }
        createOneIndexNodeType(componentId, archiveVersion, nodeTypeTags, true);
    }

    @When("^I update a tag with key \"([^\"]*)\" and value \"([^\"]*)\"$")
    public void I_update_a_tag_with_key_and_value(String tagKey, String tagValue) throws Throwable {

        UpdateTagRequest updateTagRequest = new UpdateTagRequest();
        updateTagRequest.setTagKey(tagKey);
        updateTagRequest.setTagValue(tagValue);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance()
                .postJSon("/rest/v1/components/" + Context.getInstance().getComponentId(0) + "/tags", jsonMapper.writeValueAsString(updateTagRequest)));
    }

    @Given("^I have a tag \"([^\"]*)\"$")
    public void I_have_a_tag(String tag) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/components/" + Context.getInstance().getComponentId(0)));
        NodeType idnt = JsonUtil.read(Context.getInstance().takeRestResponse(), NodeType.class).getData();
        assertTrue(idnt.getTags().contains(new Tag(tag, null)));
    }

    @Then("^I should have tag \"([^\"]*)\" with value \"([^\"]*)\"$")
    public void I_should_have_tag_with_value(String tagKey, String tagValue) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/components/" + Context.getInstance().getComponentId(0)));
        NodeType idnt = JsonUtil.read(Context.getInstance().takeRestResponse(), NodeType.class).getData();
        assertNotNull(idnt);
        int index = idnt.getTags().indexOf(new Tag(tagKey, null));
        assertEquals(idnt.getTags().get(index).getValue(), tagValue);
    }

    @When("^I delete a tag with key \"([^\"]*)\"$")
    public void I_delete_a_tag_with_key(String tagId) throws Throwable {
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().delete("/rest/v1/components/" + Context.getInstance().getComponentId(0) + "/tags/" + tagId));
    }

    /**
     * Load components IndexedNodeType from a file to use it in tests
     * 
     * @param componentId
     * @param refresh
     * @throws IOException
     * @throws IndexingServiceException
     */
    @SuppressWarnings("unchecked")
    private void createOneIndexNodeType(String componentId, String archiveVersion, List<Tag> tags, boolean refresh)
            throws IOException, IndexingServiceException {

        String samplePathString = "src/test/resources/data/components/indexed_nodetypes.json";
        Path path = Paths.get(samplePathString);
        List<Object> tempList = jsonMapper.readValue(path.toFile(), ArrayList.class);
        List<NodeType> idntList = new ArrayList<>();
        for (Object ob : tempList) {
            idntList.add(jsonMapper.readValue(jsonMapper.writeValueAsString(ob), NodeType.class));
        }
        String typeName = MappingBuilder.indexTypeFromClass(NodeType.class);

        NodeType indexedNodeType = null;
        // Save on nodeType with
        if (componentId != null && archiveVersion != null && !componentId.trim().isEmpty()) {

            // Get the first nodetype to update its id and insert it
            indexedNodeType = idntList.get(0);
            indexedNodeType.setElementId(componentId);
            indexedNodeType.setArchiveVersion(archiveVersion);
            indexedNodeType.setWorkspace(AlienConstants.GLOBAL_WORKSPACE_ID);
            if (tags != null) {
                indexedNodeType.setTags(tags);
            }

            String serializeDatum = jsonMapper.writeValueAsString(indexedNodeType);
            esClient.prepareIndex(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, typeName).setSource(serializeDatum).setRefresh(refresh).execute().actionGet();
        }
    }
}