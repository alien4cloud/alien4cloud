package alien4cloud.it.orchestrators;

import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class OrchestrationPolicyLocationResourceSteps {

    private static String POLICIES_LOCATION_RESOURCES_BASE_ENDPOINT = "/rest/v1/orchestrators/%s/locations/%s/resources/policies";

    private OrchestrationLocationResourceSteps locationResourceSteps = new OrchestrationLocationResourceSteps();

    @When("^I create a policy resource of type \"([^\"]*)\" named \"([^\"]*)\" related to the location \"([^\"]*)\"/\"([^\"]*)\"$")
    public void I_create_a_policy_of_type_named_related_to_the_location_(String resourceType, String resourceName, String orchestratorName, String locationName)
            throws Throwable {
        locationResourceSteps.createResourceTemplate(resourceType, resourceName, null, null, orchestratorName, locationName,
                POLICIES_LOCATION_RESOURCES_BASE_ENDPOINT);
    }

    @When("^I create a policy resource of type \"([^\"]*)\" named \"([^\"]*)\" from archive \"([^\"]*)\" in version \"([^\"]*)\" related to the location \"([^\"]*)\"/\"([^\"]*)\"$")
    public void I_create_a_policy_of_type_named_from_archive_related_to_the_location_(String resourceType, String resourceName, String archiveName,
            String archiveVersion, String orchestratorName, String locationName) throws Throwable {
        locationResourceSteps.createResourceTemplate(resourceType, resourceName, archiveName, archiveVersion, orchestratorName, locationName,
                POLICIES_LOCATION_RESOURCES_BASE_ENDPOINT);
    }

    @Then("^The location should contains a policy resource with name \"([^\"]*)\" and type \"([^\"]*)\"$")
    public void The_location_should_contains_a_resource_with_name_and_type(String resourceName, String resourceType) throws Throwable {
        locationResourceSteps.locationShouldContainResource(resourceName, resourceType, locationResources -> locationResources.getPolicyTemplates());
    }

    @Then("^The location should not contain a policy resource with name \"([^\"]*)\" and type \"([^\"]*)\"$")
    public void theLocationShouldNotContainAResourceWithNameAndType(String resourceName, String resourceType) throws Throwable {
        locationResourceSteps.locationShouldNotContainResource(resourceName, resourceType, locationResources -> locationResources.getPolicyTemplates());
    }

    @When("^I delete the policy resource named \"([^\"]*)\" related to the location \"([^\"]*)\"/\"([^\"]*)\"$")
    public void iDeleteTheLocationResourceNamedRelatedToTheLocation(String resourceName, String orchestratorName, String locationName) throws Throwable {
        locationResourceSteps.deleteResourceTemplate(resourceName, orchestratorName, locationName,
                "/rest/v1/orchestrators/%s/locations/%s/resources/policies/%s");
    }

    @When("^I update the property \"([^\"]*)\" to \"([^\"]*)\" for the policy resource named \"([^\"]*)\" related to the location \"([^\"]*)\"/\"([^\"]*)\"$")
    public void I_update_the_property_to_for_the_resource_named_related_to_the_location_(String propertyName, String propertyValue, String resourceName,
            String orchestratorName, String locationName) throws Throwable {
        locationResourceSteps.updatePropertyValue(orchestratorName, locationName, resourceName, propertyName, propertyValue,
                "/rest/v1/orchestrators/%s/locations/%s/resources/policies/%s/properties");
    }

    // private void updatePropertyValue(String orchestratorName, String locationName, String resourceName, String propertyName, Object propertyValue,
    // String restUrlFormat, String... extraArgs) throws IOException {
    // String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
    // String locationId = Context.getInstance().getLocationId(orchestratorId, locationName);
    // String resourceId = Context.getInstance().getLocationResourceId(orchestratorId, locationId, resourceName);
    // String restUrl;
    // if (extraArgs.length > 0) {
    // List<String> args = Lists.newArrayList(orchestratorId, locationId, resourceId);
    // args.addAll(Arrays.asList(extraArgs));
    // restUrl = String.format(restUrlFormat, args.toArray());
    // } else {
    // restUrl = String.format(restUrlFormat, orchestratorId, locationId, resourceId);
    // }
    // UpdateLocationResourceTemplatePropertyRequest request = new UpdateLocationResourceTemplatePropertyRequest();
    // request.setPropertyName(propertyName);
    // request.setPropertyValue(propertyValue);
    // String resp = Context.getRestClientInstance().postJSon(restUrl, JsonUtil.toString(request));
    // Context.getInstance().registerRestResponse(resp);
    // }

    //
    // @When("^I update the complex property \"([^\"]*)\" to \"\"\"(.*?)\"\"\" for the resource named \"([^\"]*)\" related to the location
    // \"([^\"]*)\"/\"([^\"]*)\"$")
    // public void I_update_the_complex_property_to_for_the_resource_named_related_to_the_location_(String propertyName, String propertyValue, String
    // resourceName,
    // String orchestratorName, String locationName) throws Throwable {
    // updatePropertyValue(orchestratorName, locationName, resourceName, propertyName, JsonUtil.toMap(propertyValue),
    // "/rest/v1/orchestrators/%s/locations/%s/resources/%s/template/properties");
    // }
    //
    // @When("^I update the capability \"([^\"]*)\" property \"([^\"]*)\" to \"([^\"]*)\" for the resource named \"([^\"]*)\" related to the location
    // \"([^\"]*)\"/\"([^\"]*)\"$")
    // public void I_update_the_capability_property_to_for_the_resource_named_related_to_the_location_(String capabilityName, String propertyName,
    // String propertyValue, String resourceName, String orchestratorName, String locationName) throws Throwable {
    // updatePropertyValue(orchestratorName, locationName, resourceName, propertyName, propertyValue,
    // "/rest/v1/orchestrators/%s/locations/%s/resources/%s/template/capabilities/%s/properties", capabilityName);
    // }
    //
    // @When("^I autogenerate the on-demand resources for the location \"([^\"]*)\"/\"([^\"]*)\"$")
    // public void I_autogenerate_the_on_demand_resources_for_the_location_(String orchestratorName, String locationName) throws Throwable {
    // String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
    // String locationId = Context.getInstance().getLocationId(orchestratorId, locationName);
    // String restUrl = String.format("/rest/v1/orchestrators/%s/locations/%s/resources/auto-configure", orchestratorId, locationId);
    // String resp = Context.getRestClientInstance().get(restUrl);
    // Context.getInstance().registerRestResponse(resp);
    //
    // RestResponse<List> response = JsonUtil.read(resp, List.class, Context.getJsonMapper());
    // if (response.getData() != null) {
    // List<LocationResourceTemplate> resources = JsonUtil.toList(JsonUtil.toString(response.getData()), LocationResourceTemplate.class,
    // Context.getJsonMapper());
    // for (LocationResourceTemplate locationResourceTemplate : resources) {
    // Context.getInstance().registerOrchestratorLocationResource(orchestratorId, locationId, locationResourceTemplate.getId(),
    // locationResourceTemplate.getName());
    // }
    // }
    //
    // }
    //
    // @And("^I update the property \"([^\"]*)\" to the environment variable \"([^\"]*)\" for the resource named \"([^\"]*)\" related to the location
    // \"([^\"]*)\"/\"([^\"]*)\"$")
    // public void iUpdateThePropertyToTheEnvironmentVariableForTheResourceNamedRelatedToTheLocation(String propertyName, String envVar, String resourceName,
    // String orchestratorName, String locationName) throws Throwable {
    // String keyName = System.getenv(envVar);
    // Assert.assertTrue(keyName + " must be defined as environment variable", StringUtils.isNotBlank(keyName));
    // I_update_the_property_to_for_the_resource_named_related_to_the_location_(propertyName, keyName, resourceName, orchestratorName, locationName);
    // }
    //

    //

}
