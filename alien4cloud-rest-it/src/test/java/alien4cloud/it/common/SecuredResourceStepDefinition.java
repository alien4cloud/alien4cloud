package alien4cloud.it.common;

import alien4cloud.it.Context;
import cucumber.api.java.en.When;

public class SecuredResourceStepDefinition {

    // Allowed resource types
    private enum RESOURCE_TYPE {
        APPLICATION, CLOUD;
    }

    @When("^I add a role \"([^\"]*)\" to group \"([^\"]*)\" on the resource type \"([^\"]*)\" named \"([^\"]*)\"$")
    public void I_add_a_role_to_group_on_the_resource_type_named(String roleName, String groupName, String resourceTypeId, String resourceName)
            throws Throwable {
        String request = getResourceRequest(resourceTypeId, resourceName);
        String groupId = Context.getInstance().getGroupId(groupName);
        // final call
        request += "/groupRoles/" + groupId + "/" + roleName;
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().put(request));
    }

    @When("^I add a role \"([^\"]*)\" to user \"([^\"]*)\" on the resource type \"([^\"]*)\" named \"([^\"]*)\"$")
    public void I_add_a_role_to_user_on_the_resource_type_named(String roleName, String userName, String resourceTypeId, String resourceName) throws Throwable {
        String request = getResourceRequest(resourceTypeId, resourceName);
        // final call
        request += "/userRoles/" + userName + "/" + roleName;
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().put(request));
    }

    @When("^I remove a role \"([^\"]*)\" to group \"([^\"]*)\" on the resource type \"([^\"]*)\" named \"([^\"]*)\"$")
    public void I_remove_a_role_to_group_on_the_resource_type_named(String roleName, String groupName, String resourceTypeId, String resourceName)
            throws Throwable {
        String request = getResourceRequest(resourceTypeId, resourceName);
        String groupId = Context.getInstance().getGroupId(groupName);
        // final call
        request += "/groupRoles/" + groupId + "/" + roleName;
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete(request));
    }

    @When("^I remove a role \"([^\"]*)\" to user \"([^\"]*)\" on the resource type \"([^\"]*)\" named \"([^\"]*)\"$")
    public void I_remove_a_role_to_user_on_the_resource_type_named(String roleName, String userName, String resourceTypeId, String resourceName)
            throws Throwable {
        String request = getResourceRequest(resourceTypeId, resourceName);
        // final call
        request += "/userRoles/" + userName + "/" + roleName;
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete(request));
    }

    private String getResourceRequest(String resourceTypeId, String resourceName) {
        String request = null;
        switch (RESOURCE_TYPE.valueOf(resourceTypeId)) {
        case APPLICATION:
            request = "/rest/applications/" + Context.getInstance().getApplication().getId();
            break;
        case CLOUD:
            request = "/rest/clouds/" + Context.getInstance().getCloudId(resourceName);
            break;
        default:
        }
        return request;
    }
}
