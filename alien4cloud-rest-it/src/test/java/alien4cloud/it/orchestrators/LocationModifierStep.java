package alien4cloud.it.orchestrators;

import alien4cloud.it.Context;
import alien4cloud.model.orchestrators.locations.LocationModifierReference;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import com.google.common.collect.Lists;
import cucumber.api.java.en.And;
import cucumber.api.java.en.When;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

public class LocationModifierStep {

    private ArrayList<String> previousModifiersOrder;

    @When("^I create a location modifier with plugin id \"([^\"]*)\" and bean name \"([^\"]*)\" and phase \"([^\"]*)\" to the location \"([^\"]*)\" of the orchestrator \"([^\"]*)\"$")
    public void iCreateALocationModifierWithPluginIdAndBeanNameAndPhaseToTheLocationOfTheOrchestrator(String pluginId, String beanName, String phase, String locationName, String orchestratorName) throws Throwable {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        String locationId = Context.getInstance().getLocationId(orchestratorId, locationName);
        LocationModifierReference modifier = new LocationModifierReference();
        modifier.setPluginId(pluginId);
        modifier.setBeanName(beanName);
        modifier.setPhase(phase);
        String resp = Context.getRestClientInstance().postJSon(String.format("/rest/v1/orchestrators/%s/locations/%s/modifiers", orchestratorId, locationId), JsonUtil.toString(modifier));
        Context.getInstance().registerRestResponse(resp);
    }

    @When("^I list location modifiers of the location \"([^\"]*)\" of the orchestrator \"([^\"]*)\"$")
    public void iListLocationModifiersOfTheLocationOfTheOrchestrator(String locationName, String orchestratorName) throws Throwable {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        String locationId = Context.getInstance().getLocationId(orchestratorId, locationName);
        String resp = Context.getRestClientInstance().get(String.format("/rest/v1/orchestrators/%s/locations/%s/modifiers", orchestratorId, locationId));
        Context.getInstance().registerRestResponse(resp);
    }

    @And("^Response should contains (\\d+) location modifier$")
    public void responseShouldContainsLocationModifier(int count) throws Throwable {
        RestResponse<List> response = JsonUtil.read(Context.getInstance().getRestResponse(), List.class);
        Assert.assertEquals(count, response.getData().size());
    }

    @And("^Save location modifier order$")
    public void saveLocationModifierOrder() throws Throwable {
        if (this.previousModifiersOrder == null) {
            this.previousModifiersOrder = Lists.newArrayList();
        } else {
            this.previousModifiersOrder.clear();
        }
        RestResponse<List> response = JsonUtil.read(Context.getInstance().getRestResponse(), List.class);
        for (Object obj : response.getData()) {
            LocationModifierReference modifier = Context.getInstance().getJsonMapper().readValue(Context.getInstance().getJsonMapper().writeValueAsString(obj), LocationModifierReference.class);
            this.previousModifiersOrder.add(modifier.toString());
        }
    }

    @And("^Response should contains a location modifier with plugin id \"([^\"]*)\" and bean name \"([^\"]*)\" and phase \"([^\"]*)\"$")
    public void responseShouldContainsALocationModifierWithPluginIdAndBeanNameAndPhase(String pluginId, String beanName, String phase) throws Throwable {
        RestResponse<List> response = JsonUtil.read(Context.getInstance().getRestResponse(), List.class);
        boolean contains = false;
        for (Object obj : response.getData()) {
            LocationModifierReference modifier = Context.getInstance().getJsonMapper().readValue(Context.getInstance().getJsonMapper().writeValueAsString(obj), LocationModifierReference.class);
            if (pluginId.equals(modifier.getPluginId()) && beanName.equals(modifier.getBeanName()) && phase.equals(modifier.getPhase())) {
                contains = true;
                break;
            }
        }
        Assert.assertTrue(contains);
    }

    @When("^I delete a location modifier at index (\\d+) on the location \"([^\"]*)\" to the orchestrator \"([^\"]*)\"$")
    public void iDeleteALocationModifierAtIndexOnTheLocationToTheOrchestrator(int index, String locationName, String orchestratorName) throws Throwable {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        String locationId = Context.getInstance().getLocationId(orchestratorId, locationName);
        String resp = Context.getRestClientInstance().delete(String.format("/rest/v1/orchestrators/%s/locations/%s/modifiers/%d", orchestratorId, locationId, index));
        Context.getInstance().registerRestResponse(resp);
    }

    @And("^the location at index (\\d+) should have the phase \"([^\"]*)\"$")
    public void theLocationAtIndexShouldHaveThePluginId(int index, String phase) throws Throwable {
        RestResponse<List> response = JsonUtil.read(Context.getInstance().getRestResponse(), List.class);
        Object obj = response.getData().get(index);
        LocationModifierReference modifier = Context.getInstance().getJsonMapper().readValue(Context.getInstance().getJsonMapper().writeValueAsString(obj), LocationModifierReference.class);
        Assert.assertEquals(phase, modifier.getPhase());
    }

    @When("^I move a location modifier from index (\\d+) to index (\\d+) for the location \"([^\"]*)\" of the orchestrator \"([^\"]*)\"$")
    public void iMoveALocationModifierFromIndexToIndexForTheLocationOfTheOrchestrator(int from, int to, String locationName, String orchestratorName) throws Throwable {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        String locationId = Context.getInstance().getLocationId(orchestratorId, locationName);
        String resp = Context.getRestClientInstance().put(String.format("/rest/v1/orchestrators/%s/locations/%s/modifiers/from/%d/to/%d", orchestratorId, locationId, from, to));
        Context.getInstance().registerRestResponse(resp);
    }
}
