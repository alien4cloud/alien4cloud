package alien4cloud.it.application.deployment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lombok.AllArgsConstructor;
import lombok.Getter;
import alien4cloud.common.AlienConstants;
import alien4cloud.it.Context;
import alien4cloud.model.application.Application;
import alien4cloud.rest.application.model.SetLocationPoliciesRequest;
import alien4cloud.rest.deployment.DeploymentTopologyDTO;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.utils.MapUtil;

import com.google.common.collect.Maps;

import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class DeploymentTopologyStepDefinitions {
    @When("^I Set the following location policies with orchestrator \"([^\"]*)\" for groups$")
    public void I_Set_the_following_location_policies_for_groups(String orchestratorName, Map<String, String> locationPolicies) throws Throwable {
        SetLocationPoliciesRequest request = new SetLocationPoliciesRequest();
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        request.setOrchestratorId(orchestratorId);
        Map<String, String> formatedPolicies = Maps.newHashMap();
        for (Entry<String, String> entry : locationPolicies.entrySet()) {
            formatedPolicies.put(entry.getKey(), Context.getInstance().getLocationId(orchestratorId, entry.getValue()));
        }
        request.setGroupsToLocations(formatedPolicies);
        Application application = Context.getInstance().getApplication();
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postJSon(
                        "/rest/applications/" + application.getId() + "/environments/"
                                + Context.getInstance().getDefaultApplicationEnvironmentId(application.getName()) + "/deployment-topology/location-policies",
                        JsonUtil.toString(request)));
    }

    @Then("^the deployment topology shoud have the following location policies$")
    public void the_deployment_topology_shoud_have_the_following_location_policies(List<LocationPolicySetting> expectedLocationPoliciesSettings)
            throws Throwable {
        String response = Context.getInstance().getRestResponse();
        RestResponse<DeploymentTopologyDTO> deploymentTopologyDTO = JsonUtil.read(response, DeploymentTopologyDTO.class, Context.getJsonMapper());
        assertNotNull(deploymentTopologyDTO.getData());
        Map<String, String> policies = deploymentTopologyDTO.getData().getLocationPolicies();
        assertNotNull(policies);
        Context context = Context.getInstance();
        for (LocationPolicySetting expected : expectedLocationPoliciesSettings) {
            String expectLocationId = context.getLocationId(context.getOrchestratorId(expected.getOrchestratorName()), expected.getLocationName());
            assertEquals(expectLocationId, policies.get(expected.getGroupName()));
        }
    }

    @When("^I Set a unique location policy with location \"([^\"]*)\" on orchestrator \"([^\"]*)\" for all nodes$")
    public void i_Set_a_unique_location_policy__with_location_on_orchestrator_for_all_nodes(String locationName, String orchestratorName) throws Throwable {
        I_Set_the_following_location_policies_for_groups(orchestratorName,
                MapUtil.newHashMap(new String[] { AlienConstants.GROUP_ALL }, new String[] { locationName }));
    }

    @When("^I get the deployment toology for the current application$")
    public void I_get_the_deployment_toology_for_the_current_application() throws Throwable {
        Application application = Context.getInstance().getApplication();
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().get(
                        "/rest/applications/" + application.getId() + "/environments/"
                                + Context.getInstance().getDefaultApplicationEnvironmentId(application.getName()) + "/deployment-topology/"));
    }

    @Getter
    @AllArgsConstructor
    private static class LocationPolicySetting {
        String groupName;
        String orchestratorName;
        String locationName;
    }
}