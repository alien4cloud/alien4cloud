package alien4cloud.it.components;

import static org.junit.Assert.*;

import java.util.Map;

import org.alien4cloud.tosca.model.types.NodeType;
import org.elasticsearch.client.Client;
import org.junit.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;

import alien4cloud.dao.ElasticSearchMapper;
import alien4cloud.it.Context;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.utils.MapUtil;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class GetComponentDefinitionsSteps {

    private final ObjectMapper jsonMapper = ElasticSearchMapper.getInstance();
    private final Client esClient = Context.getEsClientInstance();

    @When("^I get the component with uuid \"([^\"]*)\"$")
    public void I_get_the_component_with_uuid(String componentId) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/components/" + componentId));
    }

    @Then("^I should retrieve a component detail with list of it's properties and interfaces.$")
    public void I_should_retrieve_a_component_detail_with_list_of_it_s_properties_and_interfaces() throws Throwable {
        NodeType idnt = JsonUtil.read(Context.getInstance().takeRestResponse(), NodeType.class).getData();
        assertNotNull(idnt);
        assertNotNull(idnt.getProperties());
        assertTrue(!idnt.getProperties().isEmpty());
        assertTrue(!idnt.getProperties().values().isEmpty());
        assertNotNull(idnt.getInterfaces());
    }

    @When("^I try to get a component with id \"([^\"]*)\"$")
    public void I_try_to_get_a_component_with_id(String componentId) throws Throwable {
        String restResponse = Context.getRestClientInstance().get("/rest/v1/components/" + componentId);
        Context.getInstance().registerRestResponse(restResponse);
    }

    @Then("^I should have a component with id \"([^\"]*)\"$")
    public void I_should_have_a_component_with_id(String componentId) throws Throwable {
        RestResponse<Map> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), Map.class);
        assertNotNull(restResponse.getData());
        String id = (String) MapUtil.get(restResponse.getData(), "id");
        assertEquals(componentId, id);
    }

    @Then("^I should not have any component$")
    public void i_should_not_have_any_component() throws Throwable {
        RestResponse<Map> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), Map.class);
        Assert.assertNull(restResponse.getData());
    }
    
}