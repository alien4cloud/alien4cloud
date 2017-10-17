package alien4cloud.it.orchestrators;

import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Map;

import org.junit.Assert;

import alien4cloud.it.Context;
import alien4cloud.model.common.MetaPropConfiguration;
import alien4cloud.rest.internal.model.PropertyValidationRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.orchestrator.model.CreateLocationRequest;
import alien4cloud.rest.orchestrator.model.LocationDTO;
import alien4cloud.rest.orchestrator.model.UpdateLocationRequest;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class LocationsDefinitionsSteps {

    public static final String DEFAULT_ORCHESTRATOR_NAME = "Mount doom orchestrator";
    public static final String DEFAULT_LOCATION_NAME = "middle_earth";

    private Map<String, String> currentMetaProperties = null;

    public static final String getLocationIdFromName(final String orchestratorName, final String locationName) throws Throwable {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        String resp = Context.getRestClientInstance().get(String.format("/rest/v1/orchestrators/%s/locations", orchestratorId));
        RestResponse<List> response = JsonUtil.read(resp, List.class);
        String locationId = null;
        for (Object listItem : response.getData()) {
            Map map = (Map) listItem;
            String id = ((Map) map.get("location")).get("id").toString();
            String name = ((Map) map.get("location")).get("name").toString();
            if (locationName.equals(name)) {
                locationId = id;
            }
        }
        return locationId;
    }

    @When("^I create a location named \"([^\"]*)\" and infrastructure type \"([^\"]*)\" to the orchestrator \"([^\"]*)\"$")
    public void I_create_a_location_named_and_infrastructure_type_to_the_orchestrator(String locationName, String infrastructureType, String orchestratorName)
            throws Throwable {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        CreateLocationRequest request = new CreateLocationRequest();
        request.setName(locationName);
        request.setInfrastructureType(infrastructureType);
        String resp = Context.getRestClientInstance().postJSon(String.format("/rest/v1/orchestrators/%s/locations", orchestratorId), JsonUtil.toString(request));
        Context.getInstance().registerRestResponse(resp);

        RestResponse<String> idResponse = JsonUtil.read(resp, String.class);
        Context.getInstance().registerOrchestratorLocation(orchestratorId, idResponse.getData(), locationName);
    }

    @When("^I list locations of the orchestrator \"([^\"]*)\"$")
    public void I_list_locations_of_the_orchestrator(String orchestratorName) throws Throwable {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        String resp = Context.getRestClientInstance().get(String.format("/rest/v1/orchestrators/%s/locations", orchestratorId));
        Context.getInstance().registerRestResponse(resp);
    }

    @Then("^Response should contains (\\d+) location$")
    public void Response_should_contains_location(int count) throws Throwable {
        RestResponse<List> response = JsonUtil.read(Context.getInstance().getRestResponse(), List.class);
        Assert.assertEquals(count, response.getData().size());
    }

    @Then("^Response should contains a location with name \"([^\"]*)\"$")
    public void Response_should_contains_a_location_with_name(String locationName) throws Throwable {
        RestResponse<List> response = JsonUtil.read(Context.getInstance().getRestResponse(), List.class);
        boolean contains = false;
        for (Object listItem : response.getData()) {
            Map map = (Map) listItem;
            String name = ((Map) map.get("location")).get("name").toString();
            if (locationName.equals(name)) {
                contains = true;
            }
        }
        Assert.assertTrue(contains);
    }

    @When("^I delete a location with name \"([^\"]*)\" to the orchestrator \"([^\"]*)\"$")
    public void I_delete_a_location_with_name_to_the_orchestrator(String locationName, String orchestratorName) throws Throwable {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        String locationId = getLocationIdFromName(orchestratorName, locationName);
        String restUrl = String.format("/rest/v1/orchestrators/%s/locations/%s", orchestratorId, locationId);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete(restUrl));
    }

    @When("^I update location name from \"([^\"]*)\" to \"([^\"]*)\" of the orchestrator \"([^\"]*)\"$")
    public void I_update_location_name_from_to_of_the_orchestrator(String locationName, String newName, String orchestratorName) throws Throwable {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        String locationId = getLocationIdFromName(orchestratorName, locationName);
        UpdateLocationRequest request = new UpdateLocationRequest();
        request.setName(newName);
        String restUrl = String.format("/rest/v1/orchestrators/%s/locations/%s", orchestratorId, locationId);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().putJSon(restUrl, JsonUtil.toString(request)));
    }

    @When("^I update environment type to \"([^\"]*)\" of the location \"([^\"]*)\" of the orchestrator \"([^\"]*)\"$")
    public void I_update_environment_type_to_of_the_location_of_the_orchestrator(String newEnvType, String locationName, String orchestratorName) throws Throwable {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        String locationId = getLocationIdFromName(orchestratorName, locationName);
        UpdateLocationRequest request = new UpdateLocationRequest();
        request.setEnvironmentType(newEnvType);
        String restUrl = String.format("/rest/v1/orchestrators/%s/locations/%s", orchestratorId, locationId);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().putJSon(restUrl, JsonUtil.toString(request)));
    }

    @When("^I set the value \"([^\"]*)\" to the location meta-property \"([^\"]*)\" of the location \"([^\"]*)\" of the orchestrator \"([^\"]*)\"$")
    public void I_set_the_value_to_the_location_meta_property_of_the_location_of_the_orchestrator(String value, String metaPropertyName, String locationName, String orchestratorName) throws Throwable {
        MetaPropConfiguration propertyDefinition = Context.getInstance().getConfigurationTag(metaPropertyName);
        PropertyValidationRequest propertyCheckRequest = new PropertyValidationRequest(value, propertyDefinition.getId(), propertyDefinition);
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        String locationId = getLocationIdFromName(orchestratorName, locationName);
        String restUrl = String.format("/rest/v1/orchestrators/%s/locations/%s/properties", orchestratorId, locationId);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon(restUrl, JsonUtil.toString(propertyCheckRequest)));
    }

    @Then("^Response should contains (\\d+) meta-property for the location \"([^\"]*)\"$")
    public void Response_should_contains_meta_property_for_the_location(int count, String locationName) throws Throwable {
        RestResponse<List> response = JsonUtil.read(Context.getInstance().getRestResponse(), List.class);
        assertNotNull(response);
        for (Object obj : response.getData()) {
            LocationDTO location = Context.getInstance().getJsonMapper().readValue(Context.getInstance().getJsonMapper().writeValueAsString(obj), LocationDTO.class);
            if (locationName.equals(location.getLocation().getName())) {
                currentMetaProperties = location.getLocation().getMetaProperties();
                break;
            }
        }
        Assert.assertEquals(count, currentMetaProperties.size());
    }

    // Only work after a Response_should_contains_meta_property_for_the_location call
    @Then("^Response should contains a meta-property with value \"([^\"]*)\" for \"([^\"]*)\"$")
    public void Response_should_contains_a_meta_property_with_value_for(String metaPropertyValue, String metaPropertyName) throws Throwable {
        for (String tagId : currentMetaProperties.keySet()) {
            String resp = Context.getRestClientInstance().get(String.format("/rest/v1/metaproperties/%s", tagId));
            RestResponse<MetaPropConfiguration> response = JsonUtil.read(resp, MetaPropConfiguration.class);
            if (metaPropertyName.equals(response.getData().getName())) {
                Assert.assertEquals(metaPropertyValue, currentMetaProperties.get(tagId));
                break;
            }
        }
    }

    @When("^I get the location \"([^\"]*)\"/\"([^\"]*)\"$")
    public void I_get_the_location_(String orchestratorName, String locationName) throws Throwable {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        String locationId = Context.getInstance().getLocationId(orchestratorId, locationName);
        String restUrl = String.format("/rest/v1/orchestrators/%s/locations/%s", orchestratorId, locationId);
        String resp = Context.getRestClientInstance().get(restUrl);
        Context.getInstance().registerRestResponse(resp);

        // build the eval context if possible
        String restResponse = Context.getInstance().getRestResponse();
        RestResponse<LocationDTO> response = JsonUtil.read(restResponse, LocationDTO.class, Context.getJsonMapper());
        if (response.getError() == null) {
            Context.getInstance().buildEvaluationContext(response.getData());
        }
    }
}
