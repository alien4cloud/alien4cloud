package alien4cloud.it.application.deployment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;

import alien4cloud.it.Context;
import alien4cloud.model.deployment.matching.LocationMatch;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class ApplicationMatchingStepDefinitions {
    @When("^I ask for the locations matching for the current application$")
    public void I_ask_for_the_locations_matching_for_the_current_application() throws Throwable {
        // now matching result is in object DeploymentSetupMatchInfo
        String restUrl = String.format("/rest/topology/%s//locations", Context.getInstance().getTopologyId());
        String response = Context.getRestClientInstance().get(restUrl);
        Context.getInstance().registerRestResponse(response);
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

}