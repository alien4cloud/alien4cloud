package alien4cloud.it.orchestrators;

import java.util.List;

import org.alien4cloud.tosca.model.CSARDependency;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;

import alien4cloud.it.Context;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplateWithDependencies;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class OrchestrationLocationResourceSteps extends AbstractLocationResourceSteps {

    private static String LOCATION_RESOURCES_BASE_ENDPOINT = "/rest/v1/orchestrators/%s/locations/%s/resources";

    @When("^I create a resource of type \"([^\"]*)\" named \"([^\"]*)\" related to the location \"([^\"]*)\"/\"([^\"]*)\"$")
    public void I_create_a_resource_of_type_named_related_to_the_location_(String resourceType, String resourceName, String orchestratorName,
            String locationName) throws Throwable {
        createResourceTemplate(resourceType, resourceName, null, null, orchestratorName, locationName);
    }

    @When("^I create a resource of type \"([^\"]*)\" named \"([^\"]*)\" from archive \"([^\"]*)\" in version \"([^\"]*)\" related to the location \"([^\"]*)\"/\"([^\"]*)\"$")
    public void I_create_a_resource_of_type_named_from_archive_related_to_the_location_(String resourceType, String resourceName, String archiveName, String archiveVersion,
           String orchestratorName, String locationName) throws Throwable {
        createResourceTemplate(resourceType, resourceName, archiveName, archiveVersion, orchestratorName, locationName);
    }

    @And("^The created resource response should contain a new dependency named \"([^\"]*)\" in version \"([^\"]*)\"$")
    public void The_create_resource_response_should_contain_a_new_dependency(String archiveName, String archiveVersion) throws Throwable {
        final RestResponse<LocationResourceTemplateWithDependencies> response = JsonUtil.read(Context.getInstance().getRestResponse(), LocationResourceTemplateWithDependencies.class, Context.getJsonMapper());
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getData());
        Assert.assertNotNull(response.getData().getResourceTemplate());
        Assert.assertNotNull(response.getData().getNewDependencies());
        Assert.assertTrue(response.getData().getNewDependencies().contains(new CSARDependency(archiveName, archiveVersion)));
    }

    @Then("^The location should contains a resource with name \"([^\"]*)\" and type \"([^\"]*)\"$")
    public void The_location_should_contains_a_resource_with_name_and_type(String resourceName, String resourceType) throws Throwable {
        locationShouldContainResource(resourceName, resourceType, locationResources -> locationResources.getConfigurationTemplates());
    }

    @Then("^The location should contains an on-demand resource with name \"([^\"]*)\" and type \"([^\"]*)\"$")
    public void The_location_should_contains_an_on_demand_resource_with_name_and_type(String resourceName, String resourceType) throws Throwable {
        locationShouldContainResource(resourceName, resourceType, locationResources -> locationResources.getNodeTemplates());
    }

    @When("^I update the property \"([^\"]*)\" to \"([^\"]*)\" for the resource named \"([^\"]*)\" related to the location \"([^\"]*)\"/\"([^\"]*)\"$")
    public void I_update_the_property_to_for_the_resource_named_related_to_the_location_(String propertyName, String propertyValue, String resourceName,
            String orchestratorName, String locationName) throws Throwable {
        updatePropertyValue(orchestratorName, locationName, resourceName, propertyName, propertyValue, getUpdatePropertyUrlFormat());
    }

    @When("^I update the complex property \"([^\"]*)\" to \"\"\"(.*?)\"\"\" for the resource named \"([^\"]*)\" related to the location \"([^\"]*)\"/\"([^\"]*)\"$")
    public void I_update_the_complex_property_to_for_the_resource_named_related_to_the_location_(String propertyName, String propertyValue,
            String resourceName, String orchestratorName, String locationName) throws Throwable {
        updatePropertyValue(orchestratorName, locationName, resourceName, propertyName, JsonUtil.toMap(propertyValue), getUpdatePropertyUrlFormat());
    }

    @When("^I update the capability \"([^\"]*)\" property \"([^\"]*)\" to \"([^\"]*)\" for the resource named \"([^\"]*)\" related to the location \"([^\"]*)\"/\"([^\"]*)\"$")
    public void I_update_the_capability_property_to_for_the_resource_named_related_to_the_location_(String capabilityName, String propertyName,
            String propertyValue, String resourceName, String orchestratorName, String locationName) throws Throwable {
        updatePropertyValue(orchestratorName, locationName, resourceName, propertyName, propertyValue, getUpdateCapabilityPropertyUrlFormat(), capabilityName);
    }

    @When("^I autogenerate the on-demand resources for the location \"([^\"]*)\"/\"([^\"]*)\"$")
    public void I_autogenerate_the_on_demand_resources_for_the_location_(String orchestratorName, String locationName) throws Throwable {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        String locationId = Context.getInstance().getLocationId(orchestratorId, locationName);
        String restUrl = String.format(LOCATION_RESOURCES_BASE_ENDPOINT + "/auto-configure", orchestratorId, locationId);
        String resp = Context.getRestClientInstance().get(restUrl);
        Context.getInstance().registerRestResponse(resp);

        RestResponse<List> response = JsonUtil.read(resp, List.class, Context.getJsonMapper());
        if (response.getData() != null) {
            List<LocationResourceTemplate> resources = JsonUtil.toList(JsonUtil.toString(response.getData()), LocationResourceTemplate.class,
                    Context.getJsonMapper());
            for (LocationResourceTemplate locationResourceTemplate : resources) {
                Context.getInstance().registerOrchestratorLocationResource(orchestratorId, locationId, locationResourceTemplate.getId(),
                        locationResourceTemplate.getName());
            }
        }

    }

    @And("^I update the property \"([^\"]*)\" to the environment variable \"([^\"]*)\" for the resource named \"([^\"]*)\" related to the location \"([^\"]*)\"/\"([^\"]*)\"$")
    public void iUpdateThePropertyToTheEnvironmentVariableForTheResourceNamedRelatedToTheLocation(String propertyName, String envVar, String resourceName,
            String orchestratorName, String locationName) throws Throwable {
        String keyName = System.getenv(envVar);
        Assert.assertTrue(keyName + " must be defined as environment variable", StringUtils.isNotBlank(keyName));
        I_update_the_property_to_for_the_resource_named_related_to_the_location_(propertyName, keyName, resourceName, orchestratorName, locationName);
    }

    @When("^I delete the location resource named \"([^\"]*)\" related to the location \"([^\"]*)\"/\"([^\"]*)\"$")
    public void iDeleteTheLocationResourceNamedRelatedToTheLocation(String resourceName, String orchestratorName, String locationName) throws Throwable {
        deleteResourceTemplate(resourceName, orchestratorName, locationName);
    }

    @Then("^The location should not contain a resource with name \"([^\"]*)\" and type \"([^\"]*)\"$")
    public void theLocationShouldNotContainAResourceWithNameAndType(String resourceName, String resourceType) throws Throwable {
        locationShouldNotContainResource(resourceName, resourceType, locationResources -> locationResources.getConfigurationTemplates());
    }

    @Then("^The location should not contain an on-demand resource with name \"([^\"]*)\" and type \"([^\"]*)\"$")
    public void theLocationShouldNotContainAnOnDemandResourceWithNameAndType(String resourceName, String resourceType) throws Throwable {
        locationShouldNotContainResource(resourceName, resourceType, locationResources -> locationResources.getNodeTemplates());
    }

    @Override
    protected String getBaseUrlFormat() {
        return LOCATION_RESOURCES_BASE_ENDPOINT;
    }

    private String getUpdateCapabilityPropertyUrlFormat() {
        return getBaseUrlFormat() + "/%s/template/capabilities/%s/properties";
    }

}
