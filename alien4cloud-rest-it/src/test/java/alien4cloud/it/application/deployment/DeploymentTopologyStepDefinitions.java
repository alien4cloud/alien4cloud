package alien4cloud.it.application.deployment;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import alien4cloud.model.components.PropertyValue;
import org.apache.commons.collections4.MapUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.common.AlienConstants;
import alien4cloud.it.Context;
import alien4cloud.model.application.Application;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.paas.function.FunctionEvaluator;
import alien4cloud.rest.application.model.SetLocationPoliciesRequest;
import alien4cloud.rest.application.model.UpdateDeploymentTopologyRequest;
import alien4cloud.rest.deployment.DeploymentTopologyDTO;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.topology.UpdatePropertyRequest;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.utils.AlienUtils;
import alien4cloud.utils.MapUtil;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import lombok.AllArgsConstructor;
import lombok.Getter;

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
        String environmentId = Context.getInstance().getDefaultApplicationEnvironmentId(application.getName());
        String restUrl = String.format("/rest/v1/applications/%s/environments/%s/deployment-topology/location-policies", application.getId(), environmentId);
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
        String restUrl = String.format("/rest/v1/applications/%s/environments/%s/deployment-topology/", application.getId(), environmentId);
        String response = Context.getRestClientInstance().get(restUrl);
        Context.getInstance().registerRestResponse(response);
    }

    @When("^I substitute on the current application the node \"(.*?)\" with the location resource \"(.*?)\"/\"(.*?)\"/\"(.*?)\"$")
    public void I_substitute_on_the_current_application_the_node_with_the_location_resource(String nodeName, String orchestratorName, String locationName,
            String resourceName) throws Throwable {
        Context context = Context.getInstance();
        Application application = context.getApplication();
        String envId = context.getDefaultApplicationEnvironmentId(application.getName());
        String orchestratorId = context.getOrchestratorId(orchestratorName);
        String locationId = context.getLocationId(orchestratorId, locationName);
        String resourceId = context.getLocationResourceId(orchestratorId, locationId, resourceName);

        String restUrl = String.format("/rest/v1/applications/%s/environments/%s/deployment-topology/substitutions/%s", application.getId(), envId, nodeName);
        NameValuePair resourceParam = new BasicNameValuePair("locationResourceTemplateId", resourceId);
        String response = Context.getRestClientInstance().postUrlEncoded(restUrl, Lists.newArrayList(resourceParam));
        context.registerRestResponse(response);
    }

    private DeploymentTopologyDTO getDTOAndassertNotNull() throws IOException {
        String response = Context.getInstance().getRestResponse();
        DeploymentTopologyDTO dto = JsonUtil.read(response, DeploymentTopologyDTO.class, Context.getJsonMapper()).getData();
        assertNotNull(dto);
        return dto;
    }

    @Then("^The deployment topology sould have the substituted nodes$")
    public void The_deployment_topology_sould_have_the_substituted_nodes(List<NodeSubstitutionSetting> expectedSubstitutionSettings) throws Throwable {
        DeploymentTopologyDTO dto = getDTOAndassertNotNull();
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

    @When("^I update the property \"(.*?)\" to \"(.*?)\" for the subtituted node \"(.*?)\"$")
    public void I_update_the_property_to_for_the_subtituted_node(String propertyName, String propertyValue, String nodeName) throws Throwable {
        Context context = Context.getInstance();
        Application application = context.getApplication();
        String envId = context.getDefaultApplicationEnvironmentId(application.getName());
        UpdatePropertyRequest request = new UpdatePropertyRequest(propertyName, propertyValue);
        String restUrl = String.format("/rest/v1/applications/%s/environments/%s/deployment-topology/substitutions/%s/properties", application.getId(), envId,
                nodeName);
        String response = Context.getRestClientInstance().postJSon(restUrl, JsonUtil.toString(request));
        context.registerRestResponse(response);
    }

    @Then("^The node \"(.*?)\" in the deployment topology should have the property \"(.*?)\" with value \"(.*?)\"$")
    public void the_node_in_the_deployment_topology_should_have_the_property_with_value(String nodeName, String propertyName, String expectPropertyValue)
            throws Throwable {
        DeploymentTopologyDTO dto = JsonUtil.read(Context.getInstance().getRestResponse(), DeploymentTopologyDTO.class, Context.getJsonMapper()).getData();
        assertNotNull(dto);
        NodeTemplate node = dto.getTopology().getNodeTemplates().get(nodeName);
        assertNodePropertyValueEquals(node, propertyName, expectPropertyValue);
    }

    @When("^I update the capability \"(.*?)\" property \"(.*?)\" to \"(.*?)\" for the subtituted node \"(.*?)\"$")
    public void i_update_the_capability_property_to_for_the_subtituted_node(String capabilityName, String propertyName, String propertyValue, String nodeName)
            throws Throwable {
        Context context = Context.getInstance();
        Application application = context.getApplication();
        String envId = context.getDefaultApplicationEnvironmentId(application.getName());
        UpdatePropertyRequest request = new UpdatePropertyRequest(propertyName, propertyValue);
        String restUrl = String.format("/rest/v1/applications/%s/environments/%s/deployment-topology/substitutions/%s/capabilities/%s/properties",
                application.getId(), envId, nodeName, capabilityName);
        String response = Context.getRestClientInstance().postJSon(restUrl, JsonUtil.toString(request));
        context.registerRestResponse(response);
    }

    @Then("^The the node \"(.*?)\" in the deployment topology should have the capability \"(.*?)\"'s property \"(.*?)\" with value \"(.*?)\"$")
    public void the_the_node_in_the_deployment_topology_should_have_the_capability_s_property_with_value(String nodeName, String capabilityName,
            String propertyName, String expectedPropertyValue) throws Throwable {
        DeploymentTopologyDTO dto = JsonUtil.read(Context.getInstance().getRestResponse(), DeploymentTopologyDTO.class, Context.getJsonMapper()).getData();
        assertNotNull(dto);
        NodeTemplate node = dto.getTopology().getNodeTemplates().get(nodeName);
        assertCapabilityPropertyValueEquals(node, capabilityName, propertyName, expectedPropertyValue);
    }

    @When("^I set the following inputs properties$")
    public void I_set_the_following_inputs_properties(Map<String, Object> inputProperties) throws Throwable {
        UpdateDeploymentTopologyRequest request = new UpdateDeploymentTopologyRequest();
        request.setInputProperties(inputProperties);
        executeUpdateDeploymentTopologyCall(request);
    }

    @When("^I set the following orchestrator properties$")
    public void I_set_the_following_orchestrator_properties(Map<String, String> orchestratorProperties) throws Throwable {
        UpdateDeploymentTopologyRequest request = new UpdateDeploymentTopologyRequest();
        request.setProviderDeploymentProperties(orchestratorProperties);
        executeUpdateDeploymentTopologyCall(request);
    }

    @Then("^the deployment topology should have the following inputs properties$")
    public void The_deployment_topology_sould_have_the_following_input_properties(Map<String, String> expectedStringInputProperties) throws Throwable {
        DeploymentTopologyDTO dto = getDTOAndassertNotNull();
        Map<String, AbstractPropertyValue> expectedInputProperties = Maps.newHashMap();
        for (Entry<String, String> inputEntry : expectedStringInputProperties.entrySet()) {
            expectedInputProperties.put(inputEntry.getKey(), new ScalarPropertyValue(inputEntry.getValue()));
        }
        assertPropMapContains(dto.getTopology().getInputProperties(), expectedInputProperties);
    }

    @Then("^the deployment topology should have the following orchestrator properties$")
    public void The_deployment_topology_sould_have_the_following_orchestrator_properties(Map<String, String> expectedInputProperties) throws Throwable {
        DeploymentTopologyDTO dto = getDTOAndassertNotNull();
        assertMapContains(dto.getTopology().getProviderDeploymentProperties(), expectedInputProperties);
    }

    @Then("^the following nodes properties values sould be \"(.*?)\"$")
    public void The_following_nodes_properties_values_should_be(String expectedValue, Map<String, String> nodesProperties) throws Throwable {
        DeploymentTopologyDTO dto = getDTOAndassertNotNull();
        for (Entry<String, String> entry : nodesProperties.entrySet()) {
            NodeTemplate template = MapUtils.getObject(dto.getTopology().getNodeTemplates(), entry.getKey());
            assertNodePropertyValueEquals(template, entry.getValue(), expectedValue);
        }
    }

    private void executeUpdateDeploymentTopologyCall(UpdateDeploymentTopologyRequest request) throws IOException, JsonProcessingException {
        Application application = Context.getInstance().getApplication();
        String envId = Context.getInstance().getDefaultApplicationEnvironmentId(application.getName());
        String restUrl = String.format("/rest/v1/applications/%s/environments/%s/deployment-topology", application.getId(), envId);
        String response = Context.getRestClientInstance().putJSon(restUrl, JsonUtil.toString(request));
        Context.getInstance().registerRestResponse(response);
    }

    private void assertNodePropertyValueEquals(NodeTemplate node, String propertyName, String expectedPropertyValue) {
        assertNotNull(node);
        AbstractPropertyValue abstractProperty = MapUtils.getObject(node.getProperties(), propertyName);
        assertEquals(expectedPropertyValue, FunctionEvaluator.getScalarValue(abstractProperty));
    }

    private void assertCapabilityPropertyValueEquals(NodeTemplate node, String capabilityName, String propertyName, String expectedPropertyValue) {
        assertNotNull(node);
        Capability capability = MapUtils.getObject(node.getCapabilities(), capabilityName);
        assertNotNull(capability);
        AbstractPropertyValue abstractProperty = MapUtils.getObject(capability.getProperties(), propertyName);
        assertEquals(expectedPropertyValue, FunctionEvaluator.getScalarValue(abstractProperty));
    }

    private void assertMapContains(Map<String, String> map, Map<String, String> expectedMap) {
        for (Entry<String, String> entry : expectedMap.entrySet()) {
            assertEquals(entry.getValue(), MapUtils.getObject(map, entry.getKey()));
        }
    }

    private void assertPropMapContains(Map<String, PropertyValue> map, Map<String, AbstractPropertyValue> expectedMap) {
        map = AlienUtils.safe(map);
        for (Entry<String, AbstractPropertyValue> entry : expectedMap.entrySet()) {
            assertEquals(entry.getValue(), map.get(entry.getKey()));
        }
    }

    @Getter
    @AllArgsConstructor(suppressConstructorProperties = true)
    private static class LocationPolicySetting {
        String groupName;
        String orchestratorName;
        String locationName;
    }

    @Getter
    @AllArgsConstructor(suppressConstructorProperties = true)
    private static class NodeSubstitutionSetting {
        String nodeName;
        String resourceName;
        String resourceType;
    }
}