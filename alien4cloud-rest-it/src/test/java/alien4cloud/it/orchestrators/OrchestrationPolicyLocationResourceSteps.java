package alien4cloud.it.orchestrators;

import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class OrchestrationPolicyLocationResourceSteps extends AbstractLocationResourceSteps {

    private static String POLICIES_LOCATION_RESOURCES_BASE_ENDPOINT = "/rest/v1/orchestrators/%s/locations/%s/policies";

    @When("^I create a policy resource of type \"([^\"]*)\" named \"([^\"]*)\" related to the location \"([^\"]*)\"/\"([^\"]*)\"$")
    public void I_create_a_policy_of_type_named_related_to_the_location_(String resourceType, String resourceName, String orchestratorName, String locationName)
            throws Throwable {
        createResourceTemplate(resourceType, resourceName, null, null, orchestratorName, locationName);
    }

    @When("^I create a policy resource of type \"([^\"]*)\" named \"([^\"]*)\" from archive \"([^\"]*)\" in version \"([^\"]*)\" related to the location \"([^\"]*)\"/\"([^\"]*)\"$")
    public void I_create_a_policy_of_type_named_from_archive_related_to_the_location_(String resourceType, String resourceName, String archiveName,
            String archiveVersion, String orchestratorName, String locationName) throws Throwable {
        createResourceTemplate(resourceType, resourceName, archiveName, archiveVersion, orchestratorName, locationName);
    }

    @Then("^The location should contains a policy resource with name \"([^\"]*)\" and type \"([^\"]*)\"$")
    public void The_location_should_contains_a_resource_with_name_and_type(String resourceName, String resourceType) throws Throwable {
        locationShouldContainResource(resourceName, resourceType, locationResources -> locationResources.getPolicyTemplates());
    }

    @Then("^The location should not contain a policy resource with name \"([^\"]*)\" and type \"([^\"]*)\"$")
    public void theLocationShouldNotContainAResourceWithNameAndType(String resourceName, String resourceType) throws Throwable {
        locationShouldNotContainResource(resourceName, resourceType, locationResources -> locationResources.getPolicyTemplates());
    }

    @When("^I delete the policy resource named \"([^\"]*)\" related to the location \"([^\"]*)\"/\"([^\"]*)\"$")
    public void iDeleteTheLocationResourceNamedRelatedToTheLocation(String resourceName, String orchestratorName, String locationName) throws Throwable {
        deleteResourceTemplate(resourceName, orchestratorName, locationName);
    }

    @When("^I update the property \"([^\"]*)\" to \"([^\"]*)\" for the policy resource named \"([^\"]*)\" related to the location \"([^\"]*)\"/\"([^\"]*)\"$")
    public void I_update_the_property_to_for_the_resource_named_related_to_the_location_(String propertyName, String propertyValue, String resourceName,
            String orchestratorName, String locationName) throws Throwable {
        updatePropertyValue(orchestratorName, locationName, resourceName, propertyName, propertyValue, getUpdatePropertyUrlFormat());
    }

    @When("^I update the policy resource name from \"([^\"]*)\" to \"([^\"]*)\" related to the location \"([^\"]*)\"/\"([^\"]*)\"$")
    public void IUpdateThePolicyResourceNameFromRelatedToTheLocation(String oldName, String newName, String orchestratorName,
            String locationName) throws Throwable {
        updateLocationResource(orchestratorName, locationName, oldName, newName);
    }

    @Override
    protected String getBaseUrlFormat() {
        return POLICIES_LOCATION_RESOURCES_BASE_ENDPOINT;
    }

}
