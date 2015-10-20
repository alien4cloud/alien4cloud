package alien4cloud.it.application.deployment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.apache.commons.collections4.MapUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import alien4cloud.common.AlienConstants;
import alien4cloud.it.Context;
import alien4cloud.it.common.CommonStepDefinitions;
import alien4cloud.model.application.Application;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.rest.application.model.SetLocationPoliciesRequest;
import alien4cloud.rest.deployment.DeploymentTopologyDTO;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.utils.MapUtil;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class DeploymentTopologyStepDefinitions {
    private static String CURRENT_ORCHESTRATOR_ID;
    private static String CURRENT_LOCATION_ID;

    private CommonStepDefinitions commonSteps = new CommonStepDefinitions();

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
        String environmentId = Context.getInstance().getDefaultApplicationEnvironmentId(application.getName());
        String restUrl = String.format("/rest/applications/%s/environments/%s/deployment-topology/location-policies", application.getId(), environmentId);
        String response = Context.getRestClientInstance().postJSon(restUrl, JsonUtil.toString(request));
        Context.getInstance().registerRestResponse(response);
    }

    @Then("^the deployment topology shoud have the following location policies$")
    public void The_deployment_topology_shoud_have_the_following_location_policies(List<LocationPolicySetting> expectedLocationPoliciesSettings)
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

    @When("^I Set a unique location policy to \"([^\"]*)\"/\"([^\"]*)\" for all nodes$")
    public void I_Set_a_unique_location_policy_to_for_all_nodes(String orchestratorName, String locationName) throws Throwable {
        I_Set_the_following_location_policies_for_groups(orchestratorName,
                MapUtil.newHashMap(new String[] { AlienConstants.GROUP_ALL }, new String[] { locationName }));
    }

    @When("^I get the deployment toology for the current application$")
    public void I_get_the_deployment_toology_for_the_current_application() throws Throwable {
        Application application = Context.getInstance().getApplication();
        String environmentId = Context.getInstance().getDefaultApplicationEnvironmentId(application.getName());
        String restUrl = String.format("/rest/applications/%s/environments/%s/deployment-topology/", application.getId(), environmentId);
        String response = Context.getRestClientInstance().get(restUrl);
        Context.getInstance().registerRestResponse(response);
    }

    @When("^I substitute for the current application the node \"(.*?)\" with the location resource \"(.*?)\"/\"(.*?)\"/\"(.*?)\"$")
    public void I_substitute_for_the_current_application_the_node_with_the_location_resource(String nodeName, String orchestratorName, String locationName,
            String resourceName) throws Throwable {
        Context context = Context.getInstance();
        Application application = context.getApplication();
        String envId = context.getDefaultApplicationEnvironmentId(application.getName());
        String orchestratorId = context.getOrchestratorId(orchestratorName);
        String locationId = context.getLocationId(orchestratorId, locationName);
        String resourceId = context.getLocationResourceId(orchestratorId, locationId, resourceName);

        String restUrl = String.format("/rest/applications/%s/environments/%s/deployment-topology/substitutions/%s", application.getId(), envId, nodeName);
        NameValuePair resourceParam = new BasicNameValuePair("locationResourceTemplateId", resourceId);
        String response = Context.getRestClientInstance().postUrlEncoded(restUrl, Lists.newArrayList(resourceParam));
        context.registerRestResponse(response);
    }

    @Then("^The deployment topology sould have the substituted nodes$")
    public void The_deployment_topology_sould_have_the_substituted_nodes(List<NodeSubstitutionSetting> expectedSubstitutionSettings) throws Throwable {
        String response = Context.getInstance().getRestResponse();
        DeploymentTopologyDTO dto = JsonUtil.read(response, DeploymentTopologyDTO.class, Context.getJsonMapper()).getData();
        assertNotNull(dto);
        Map<String, String> substitutions = dto.getTopology().getSubstitutedNodes();
        Map<String, LocationResourceTemplate> resources = dto.getLocationResourceTemplates();
        assertTrue(MapUtils.isNotEmpty(substitutions));
        assertTrue(MapUtils.isNotEmpty(resources));
        for (NodeSubstitutionSetting nodeSubstitutionSetting : expectedSubstitutionSettings) {
            String substituteName = substitutions.get(nodeSubstitutionSetting.getNodeName());
            assertEquals(nodeSubstitutionSetting.getResourceName(), substituteName);
            LocationResourceTemplate substitute = resources.get(substituteName);
            assertNotNull(substitute);
            assertEquals(nodeSubstitutionSetting.getResourceType(), substitute.getTypes());
        }
    }

    @Getter
    @AllArgsConstructor
    private static class LocationPolicySetting {
        String groupName;
        String orchestratorName;
        String locationName;
    }

    @Getter
    @AllArgsConstructor
    private static class NodeSubstitutionSetting {
        String nodeName;
        String resourceName;
        String resourceType;
    }
}