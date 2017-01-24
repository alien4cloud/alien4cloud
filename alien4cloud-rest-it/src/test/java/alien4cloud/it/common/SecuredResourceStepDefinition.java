package alien4cloud.it.common;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;

import alien4cloud.it.Context;
import alien4cloud.it.application.ApplicationStepDefinitions;
import alien4cloud.it.orchestrators.LocationsDefinitionsSteps;
import alien4cloud.model.application.Application;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.security.model.Group;
import alien4cloud.security.model.User;
import cucumber.api.DataTable;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class SecuredResourceStepDefinition {

    private String getSecuredResourceBaseURL(String resourceType, String resourceName) throws Throwable {
        String url = null;
        switch (resourceType) {
        case "LOCATION":
            String orchestratorName = LocationsDefinitionsSteps.DEFAULT_ORCHESTRATOR_NAME;
            url = "/rest/v1/orchestrators/" + Context.getInstance().getOrchestratorId(orchestratorName) + "/locations/"
                    + LocationsDefinitionsSteps.getLocationIdFromName(orchestratorName, resourceName) + "/security";
            break;
        default:
            Assert.fail("Dot not support resource type " + resourceType);
        }
        return url;
    }

    @Given("^I grant access to the resource type \"([^\"]*)\" named \"([^\"]*)\" to the user \"([^\"]*)\"$")
    public void iGrantAccessToTheResourceTypeNamedToTheUser(String resourceType, String resourceName, String userName) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon(getSecuredResourceBaseURL(resourceType, resourceName) + "/users",
                JsonUtil.toString(new String[] { userName })));
    }

    @When("^I get the authorised users for the resource type \"([^\"]*)\" named \"([^\"]*)\"$")
    public void iGetTheAuthorisedUsersForTheResourceTypeNamed(String resourceType, String resourceName) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get(getSecuredResourceBaseURL(resourceType, resourceName) + "/users"));
    }

    @When("^I revoke access to the resource type \"([^\"]*)\" named \"([^\"]*)\" from the user \"([^\"]*)\"$")
    public void iRevokeAccessToTheResourceTypeNamedFromTheUser(String resourceType, String resourceName, String userName) throws Throwable {
        Context.getInstance()
                .registerRestResponse(Context.getRestClientInstance().delete(getSecuredResourceBaseURL(resourceType, resourceName) + "/users/" + userName));
    }

    @Then("^I should have following list of users:$")
    public void iShouldHaveFollowingListOfUsers(DataTable rawExpectedUsers) throws Throwable {
        RestResponse<User[]> users = JsonUtil.read(Context.getInstance().getRestResponse(), User[].class);
        Assert.assertTrue(users.getError() == null);
        Assert.assertTrue(users.getData() != null);
        Assert.assertTrue(users.getData().length > 0);
        Set<String> currentUserNames = Arrays.stream(users.getData()).map(User::getUsername).collect(Collectors.toSet());
        Set<String> expectedUserNames = rawExpectedUsers.raw().stream().map(line -> StringUtils.trim(line.get(0))).collect(Collectors.toSet());
        Assert.assertEquals(expectedUserNames, currentUserNames);
    }

    @Given("^I grant access to the resource type \"([^\"]*)\" named \"([^\"]*)\" to the group \"([^\"]*)\"$")
    public void iGrantAccessToTheResourceTypeNamedToTheGroup(String resourceType, String resourceName, String groupName) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon(getSecuredResourceBaseURL(resourceType, resourceName) + "/groups",
                JsonUtil.toString(new String[] { Context.getInstance().getGroupId(groupName) })));
    }

    @When("^I get the authorised groups for the resource type \"([^\"]*)\" named \"([^\"]*)\"$")
    public void iGetTheAuthorisedGroupsForTheResourceTypeNamed(String resourceType, String resourceName) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get(getSecuredResourceBaseURL(resourceType, resourceName) + "/groups"));
    }

    @Then("^I should have following list of groups:$")
    public void iShouldHaveFollowingListOfGroups(DataTable rawExpectedGroups) throws Throwable {
        RestResponse<Group[]> groups = JsonUtil.read(Context.getInstance().getRestResponse(), Group[].class);
        Assert.assertTrue(groups.getError() == null);
        Assert.assertTrue(groups.getData() != null);
        Assert.assertTrue(groups.getData().length > 0);
        Set<String> currentGroupNames = Arrays.stream(groups.getData()).map(Group::getName).collect(Collectors.toSet());
        Set<String> expectedGroupNames = rawExpectedGroups.raw().stream().map(line -> StringUtils.trim(line.get(0))).collect(Collectors.toSet());
        Assert.assertEquals(expectedGroupNames, currentGroupNames);
    }

    @Given("^I revoke access to the resource type \"([^\"]*)\" named \"([^\"]*)\" from the group \"([^\"]*)\"$")
    public void iRevokeAccessToTheResourceTypeNamedFromTheGroup(String resourceType, String resourceName, String groupName) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance()
                .delete(getSecuredResourceBaseURL(resourceType, resourceName) + "/groups/" + Context.getInstance().getGroupId(groupName)));
    }

    // Allowed resource types
    private enum RESOURCE_TYPE {
        APPLICATION, ENVIRONMENT, LOCATION, ORCHESTRATOR;
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
        String orchestratorName = LocationsDefinitionsSteps.DEFAULT_ORCHESTRATOR_NAME;
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

            request = "/rest/v1/orchestrators/" + Context.getInstance().getOrchestratorId(orchestratorName) + "/locations/"
                    + LocationsDefinitionsSteps.getLocationIdFromName(orchestratorName, resourceName);
            break;
        case ORCHESTRATOR:
            request = "/rest/v1/orchestrators/" + Context.getInstance().getOrchestratorId(orchestratorName);
            break;
        default:
        }
        return request;
    }

}
