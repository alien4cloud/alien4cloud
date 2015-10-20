package alien4cloud.it.application.deployment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;

import alien4cloud.common.AlienConstants;
import alien4cloud.it.Context;
import alien4cloud.model.application.Application;
import alien4cloud.model.deployment.matching.LocationMatch;
import alien4cloud.rest.application.model.SetLocationPoliciesRequest;
import alien4cloud.rest.deployment.DeploymentTopologyDTO;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.utils.MapUtil;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class ApplicationMatchingStepDefinitions {
    @When("^I ask for the locations matching for the current application$")
    public void I_ask_for_the_locations_matching_for_the_current_application() throws Throwable {
        // now matching result is in object DeploymentSetupMatchInfo
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().get("/rest/topology/" + Context.getInstance().getTopologyId() + "/locations"));
    }

    @Then("^I should receive a match result with (\\d+) locations$")
    public void I_should_receive_a_match_result_with_locations(int expectedCount, List<String> locationNames) throws Throwable {
        RestResponse<?> response = JsonUtil.read(Context.getInstance().getRestResponse());
        assertNull(response.getError());
        assertNotNull(response.getData());
        List<LocationMatch> locationMatches = JsonUtil.toList(JsonUtil.toString(response.getData()), LocationMatch.class);
        assertLocationMatches(locationMatches, expectedCount, locationNames);
    }

    @Then("^I should receive a match result with no locations$")
    public void I_should_receive_a_match_result_with_locations() throws Throwable {
        RestResponse<?> response = JsonUtil.read(Context.getInstance().getRestResponse());
        assertNull(response.getError());
        assertNull(response.getData());
    }

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
            String expectLocationId = Context.getInstance()
                    .getLocationId(context.getOrchestratorId(expected.getOrchestratorName()), expected.getLocationName());
            assertEquals(expectLocationId, policies.get(expected.getGroupName()));
        }
    }

    @When("^I Set a unique location policy with location \"([^\"]*)\" on orchestrator \"([^\"]*)\" for all nodes$")
    public void i_Set_a_unique_location_policy__with_location_on_orchestrator_for_all_nodes(String locationName, String orchestratorName) throws Throwable {
        I_Set_the_following_location_policies_for_groups(orchestratorName,
                MapUtil.newHashMap(new String[] { AlienConstants.GROUP_ALL }, new String[] { locationName }));
    }

    private static void assertLocationMatches(List<LocationMatch> matches, int expectedCount, List<String> expectedNames) {
        if (CollectionUtils.isEmpty(matches)) {
            matches = Lists.newArrayList();
        }

        assertEquals(matches.size(), expectedCount);

        Set<String> names = Sets.newHashSet();
        for (LocationMatch locationMatch : matches) {
            names.add(locationMatch.getLocation().getName());
        }
        assertTrue(SetUtils.isEqualSet(names, expectedNames));
    }

    @Getter
    @AllArgsConstructor
    private static class LocationPolicySetting {
        String groupName;
        String orchestratorName;
        String locationName;
    }
}