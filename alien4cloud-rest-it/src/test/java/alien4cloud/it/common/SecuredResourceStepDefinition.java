package alien4cloud.it.common;

import alien4cloud.it.Context;
import alien4cloud.it.application.ApplicationStepDefinitions;
import alien4cloud.it.orchestrators.LocationsDefinitionsSteps;
import alien4cloud.model.application.Application;
import cucumber.api.java.en.When;

public class SecuredResourceStepDefinition {

    // Allowed resource types
    private enum RESOURCE_TYPE {
        APPLICATION, ENVIRONMENT, LOCATION;
    }

    @When("^I add a role \"([^\"]*)\" to group \"([^\"]*)\" on the resource type \"([^\"]*)\" named \"([^\"]*)\"$")
    public void I_add_a_role_to_group_on_the_resource_type_named(String roleName, String groupName, String resourceTypeId, String resourceName)
            throws Throwable {
        String request = getResourceRequest(resourceTypeId, resourceName);
        String groupId = Context.getInstance().getGroupId(groupName);
        // final call
        request += "/roles/groups/" + groupId + "/" + roleName;
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().put(request));
    }

    @When("^I add a role \"([^\"]*)\" to user \"([^\"]*)\" on the resource type \"([^\"]*)\" named \"([^\"]*)\"$")
    public void I_add_a_role_to_user_on_the_resource_type_named(String roleName, String userName, String resourceTypeId, String resourceName) throws Throwable {
        String request = getResourceRequest(resourceTypeId, resourceName);
        // final call
        request += "/roles/users/" + userName + "/" + roleName;
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().put(request));
    }

    @When("^I remove a role \"([^\"]*)\" to group \"([^\"]*)\" on the resource type \"([^\"]*)\" named \"([^\"]*)\"$")
    public void I_remove_a_role_to_group_on_the_resource_type_named(String roleName, String groupName, String resourceTypeId, String resourceName)
            throws Throwable {
        String request = getResourceRequest(resourceTypeId, resourceName);
        String groupId = Context.getInstance().getGroupId(groupName);
        // final call
        request += "/roles/groups/" + groupId + "/" + roleName;
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete(request));
    }

    @When("^I remove a role \"([^\"]*)\" to user \"([^\"]*)\" on the resource type \"([^\"]*)\" named \"([^\"]*)\"$")
    public void I_remove_a_role_to_user_on_the_resource_type_named(String roleName, String userName, String resourceTypeId, String resourceName)
            throws Throwable {
        String request = getResourceRequest(resourceTypeId, resourceName);
        // final call
        request += "/roles/users/" + userName + "/" + roleName;
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete(request));
    }

    private String getResourceRequest(String resourceTypeId, String resourceName) throws Throwable {
        String request = null;
        switch (RESOURCE_TYPE.valueOf(resourceTypeId)) {
        case APPLICATION:
            request = "/rest/v1/applications/" + Context.getInstance().getApplicationId(resourceName);
            break;
        case ENVIRONMENT:
            Application application = ApplicationStepDefinitions.CURRENT_APPLICATION;
            request = "/rest/v1/applications/" + Context.getInstance().getApplicationId(resourceName) + "/environments/"
                    + Context.getInstance().getApplicationEnvironmentId(application.getName(), resourceName);
            break;
        case LOCATION:
            String orchestratorName = LocationsDefinitionsSteps.DEFAULT_ORCHESTRATOR_NAME;
            request = "/rest/v1/orchestrators/" + Context.getInstance().getOrchestratorId(orchestratorName) + "/locations/"
                    + LocationsDefinitionsSteps.getLocationIdFromName(orchestratorName, resourceName);
            break;
        default:
        }
        return request;
    }
}
