package alien4cloud.it.orchestrators;

import java.util.List;
import java.util.Map;

import org.junit.Assert;

import alien4cloud.it.Context;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.orchestrator.model.CreateLocationRequest;
import alien4cloud.rest.orchestrator.model.UpdateLocationRequest;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class LocationsDefinitionsSteps {

    @When("^I create a location named \"([^\"]*)\" and infrastructure type \"([^\"]*)\" to the orchestrator \"([^\"]*)\"$")
    public void I_create_a_location_named_and_infrastructure_type_to_the_orchestrator(String locationName, String infrastructureType, String orchestratorName)
            throws Throwable {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        CreateLocationRequest request = new CreateLocationRequest();
        request.setName(locationName);
        request.setInfrastructureType(infrastructureType);
        String resp = Context.getRestClientInstance().postJSon(String.format("/rest/orchestrators/%s/locations", orchestratorId), JsonUtil.toString(request));
        Context.getInstance().registerRestResponse(resp);

        RestResponse<String> idResponse = JsonUtil.read(resp, String.class);
        Context.getInstance().registerOrchestratorLocation(orchestratorId, idResponse.getData(), locationName);
    }

    @When("^I list locations of the orchestrator \"([^\"]*)\"$")
    public void I_list_locations_of_the_orchestrator(String orchestratorName) throws Throwable {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        String resp = Context.getRestClientInstance().get(String.format("/rest/orchestrators/%s/locations", orchestratorId));
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

    private String getLocationIdFromName(String orchestratorName, String locationName) throws Throwable {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        String resp = Context.getRestClientInstance().get(String.format("/rest/orchestrators/%s/locations", orchestratorId));
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

    @When("^I delete a location with name \"([^\"]*)\" to the orchestrator \"([^\"]*)\"$")
    public void I_delete_a_location_with_name_to_the_orchestrator(String locationName, String orchestratorName) throws Throwable {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        String locationId = getLocationIdFromName(orchestratorName, locationName);
        String restUrl = String.format("/rest/orchestrators/%s/locations/%s", orchestratorId, locationId);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete(restUrl));
    }

    @When("^I update location name from \"([^\"]*)\" to \"([^\"]*)\" of the orchestrator \"([^\"]*)\"$")
    public void I_update_location_name_from_to_of_the_orchestrator(String locationName, String newName, String orchestratorName) throws Throwable {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        String locationId = getLocationIdFromName(orchestratorName, locationName);
        UpdateLocationRequest request = new UpdateLocationRequest();
        request.setName(newName);
        String restUrl = String.format("/rest/orchestrators/%s/locations/%s", orchestratorId, locationId);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().putJSon(restUrl, JsonUtil.toString(request)));
    }

}
