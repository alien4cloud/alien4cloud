package alien4cloud.it.common;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;

import com.google.common.collect.Sets;

import alien4cloud.it.Context;
import alien4cloud.it.application.ApplicationStepDefinitions;
import alien4cloud.it.orchestrators.LocationsDefinitionsSteps;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.orchestrator.model.ApplicationEnvironmentAuthorizationDTO;
import alien4cloud.rest.orchestrator.model.ApplicationEnvironmentAuthorizationUpdateRequest;
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
        String orchestratorName = LocationsDefinitionsSteps.DEFAULT_ORCHESTRATOR_NAME;
        switch (resourceType) {
        case "LOCATION":
            url = "/rest/v1/orchestrators/" + Context.getInstance().getOrchestratorId(orchestratorName) + "/locations/"
                    + LocationsDefinitionsSteps.getLocationIdFromName(orchestratorName, resourceName) + "/security";
            break;
        case "LOCATION_RESOURCE":
            String locationId = Context.getInstance().getLocationId(Context.getInstance().getOrchestratorId(orchestratorName), LocationsDefinitionsSteps.DEFAULT_LOCATION_NAME);
            String locationResourceId = Context.getInstance().getLocationResourceId(Context.getInstance().getOrchestratorId(orchestratorName), locationId, resourceName);
            url = "/rest/v1/orchestrators/" + Context.getInstance().getOrchestratorId(orchestratorName) + "/locations/"
                    + locationId + "/resources/" + locationResourceId + "/security";
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

    private Set<String> getAuthorizedUsers() throws IOException {
        RestResponse<User[]> users = JsonUtil.read(Context.getInstance().getRestResponse(), User[].class);
        Assert.assertTrue(users.getError() == null);
        if (users.getData() == null) {
            return Sets.newHashSet();
        }
        return Arrays.stream(users.getData()).map(User::getUsername).collect(Collectors.toSet());
    }

    @Then("^I should have following list of users:$")
    public void iShouldHaveFollowingListOfUsers(DataTable rawExpectedUsers) throws Throwable {
        Assert.assertEquals(getExpectedNames(rawExpectedUsers), getAuthorizedUsers());
    }

    @Given("^I grant access to the resource type \"([^\"]*)\" named \"([^\"]*)\" to the group \"([^\"]*)\"$")
    public void iGrantAccessToTheResourceTypeNamedToTheGroup(String resourceType, String resourceName, String groupName) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon(getSecuredResourceBaseURL(resourceType, resourceName) + "/groups",
                JsonUtil.toString(new String[] { Context.getInstance().getGroupId(groupName) })));
    }

    @Given("^I grant access to the location named \"([^\"]*)\"/\"([^\"]*)\" to the group \"([^\"]*)\"$")
    public void iGrantAccessToTheLocationNamedToTheGroup(String orchestratorName, String locationName, String groupName) throws Throwable {
        String url = "/rest/v1/orchestrators/" + Context.getInstance().getOrchestratorId(orchestratorName) + "/locations/"
                + LocationsDefinitionsSteps.getLocationIdFromName(orchestratorName, locationName) + "/security/groups";
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postJSon(url, JsonUtil.toString(new String[] { Context.getInstance().getGroupId(groupName) })));
    }

    @When("^I get the authorised groups for the resource type \"([^\"]*)\" named \"([^\"]*)\"$")
    public void iGetTheAuthorisedGroupsForTheResourceTypeNamed(String resourceType, String resourceName) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get(getSecuredResourceBaseURL(resourceType, resourceName) + "/groups"));
    }

    private Set<String> getAuthorizedGroups() throws IOException {
        RestResponse<Group[]> groups = JsonUtil.read(Context.getInstance().getRestResponse(), Group[].class);
        Assert.assertTrue(groups.getError() == null);
        if (groups.getData() == null) {
            return Sets.newHashSet();
        }
        return Arrays.stream(groups.getData()).map(Group::getName).collect(Collectors.toSet());
    }

    @Then("^I should have following list of groups:$")
    public void iShouldHaveFollowingListOfGroups(DataTable rawExpectedGroups) throws Throwable {
        Assert.assertEquals(getExpectedNames(rawExpectedGroups), getAuthorizedGroups());
    }

    @Given("^I revoke access to the resource type \"([^\"]*)\" named \"([^\"]*)\" from the group \"([^\"]*)\"$")
    public void iRevokeAccessToTheResourceTypeNamedFromTheGroup(String resourceType, String resourceName, String groupName) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance()
                .delete(getSecuredResourceBaseURL(resourceType, resourceName) + "/groups/" + Context.getInstance().getGroupId(groupName)));
    }

    @Given("^I grant access to the resource type \"([^\"]*)\" named \"([^\"]*)\" to the application \"([^\"]*)\"$")
    public void iGrantAccessToTheResourceTypeNamedToTheApplication(String resourceType, String resourceName, String applicationName) throws Throwable {
        ApplicationEnvironmentAuthorizationUpdateRequest request = new ApplicationEnvironmentAuthorizationUpdateRequest();
        request.setApplicationsToAdd(new String[] { Context.getInstance().getApplicationId(applicationName) });
        Context.getInstance().registerRestResponse(Context.getRestClientInstance()
                .postJSon(getSecuredResourceBaseURL(resourceType, resourceName) + "/environmentsPerApplication/", JsonUtil.toString(request)));
    }

    @When("^I get the authorised applications for the resource type \"([^\"]*)\" named \"([^\"]*)\"$")
    public void iGetTheAuthorisedApplicationsForTheResourceTypeNamed(String resourceType, String resourceName) throws Throwable {
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().get(getSecuredResourceBaseURL(resourceType, resourceName) + "/environmentsPerApplication"));
    }

    private ApplicationEnvironmentAuthorizationDTO[] getApplicationEnvironmentAuthorizationDTOS() throws IOException {
        RestResponse<ApplicationEnvironmentAuthorizationDTO[]> appDTOsResponse = JsonUtil.read(Context.getInstance().getRestResponse(),
                ApplicationEnvironmentAuthorizationDTO[].class);
        Assert.assertTrue(appDTOsResponse.getError() == null);
        return appDTOsResponse.getData() != null ? appDTOsResponse.getData() : new ApplicationEnvironmentAuthorizationDTO[0];
    }

    private Set<String> getAuthorisedEnvironments() throws IOException {
        return Arrays.stream(getApplicationEnvironmentAuthorizationDTOS())
                .filter(appDTO -> appDTO.getEnvironments() != null && !appDTO.getEnvironments().isEmpty())
                .flatMap(appDTO -> appDTO.getEnvironments().stream().map(ApplicationEnvironment::getName)).collect(Collectors.toSet());
    }

    private Set<String> getAuthorisedApplications() throws IOException {
        return Arrays.stream(getApplicationEnvironmentAuthorizationDTOS()).filter(appDTO -> appDTO.getApplication() != null)
                .map(appDTO -> appDTO.getApplication().getName()).collect(Collectors.toSet());
    }

    private Set<String> getExpectedNames(DataTable rawExpected) {
        return rawExpected.raw().stream().map(line -> StringUtils.trim(line.get(0))).collect(Collectors.toSet());
    }

    @Then("^I should have following list of applications:$")
    public void iShouldHaveFollowingListOfApplications(DataTable rawExpectedApplications) throws Throwable {
        Assert.assertEquals(getExpectedNames(rawExpectedApplications), getAuthorisedApplications());
    }

    @Given("^I revoke access to the resource type \"([^\"]*)\" named \"([^\"]*)\" from the application \"([^\"]*)\"$")
    public void iRevokeAccessToTheResourceTypeNamedFromTheApplication(String resourceType, String resourceName, String applicationName) throws Throwable {
        ApplicationEnvironmentAuthorizationUpdateRequest request = new ApplicationEnvironmentAuthorizationUpdateRequest();
        request.setApplicationsToDelete(new String[] { Context.getInstance().getApplicationId(applicationName) });
        Context.getInstance().registerRestResponse(Context.getRestClientInstance()
                .postJSon(getSecuredResourceBaseURL(resourceType, resourceName) + "/environmentsPerApplication/", JsonUtil.toString(request)));
    }

    @Given("^I grant access to the resource type \"([^\"]*)\" named \"([^\"]*)\" to the environment \"([^\"]*)\" of the application \"([^\"]*)\"$")
    public void iGrantAccessToTheResourceTypeNamedToTheEnvironmentOfTheApplication(String resourceType, String resourceName, String environmentName,
            String applicationName) throws Throwable {
        ApplicationEnvironmentAuthorizationUpdateRequest request = new ApplicationEnvironmentAuthorizationUpdateRequest();
        request.setEnvironmentsToAdd(new String[] { Context.getInstance().getApplicationEnvironmentId(applicationName, environmentName) });
        Context.getInstance().registerRestResponse(Context.getRestClientInstance()
                .postJSon(getSecuredResourceBaseURL(resourceType, resourceName) + "/environmentsPerApplication/", JsonUtil.toString(request)));
    }

    @Then("^I should have following list of environments:$")
    public void iShouldHaveFollowingListOfEnvironments(DataTable rawExpectedEnvironments) throws Throwable {
        Assert.assertEquals(getExpectedNames(rawExpectedEnvironments), getAuthorisedEnvironments());
    }

    @Given("^I revoke access to the resource type \"([^\"]*)\" named \"([^\"]*)\" from the environment \"([^\"]*)\" of the application \"([^\"]*)\"$")
    public void iRevokeAccessToTheResourceTypeNamedFromTheEnvironmentOfTheApplication(String resourceType, String resourceName, String environmentName,
            String applicationName) throws Throwable {
        ApplicationEnvironmentAuthorizationUpdateRequest request = new ApplicationEnvironmentAuthorizationUpdateRequest();
        request.setEnvironmentsToDelete(new String[] { Context.getInstance().getApplicationEnvironmentId(applicationName, environmentName) });
        Context.getInstance().registerRestResponse(Context.getRestClientInstance()
                .postJSon(getSecuredResourceBaseURL(resourceType, resourceName) + "/environmentsPerApplication/", JsonUtil.toString(request)));
    }

    @Then("^I should not have any authorized environments$")
    public void iShouldNotHaveAnyAuthorizedEnvironments() throws Throwable {
        Assert.assertTrue(getAuthorisedEnvironments().isEmpty());
    }

    @Then("^I should not have any authorized applications$")
    public void iShouldNotHaveAnyAuthorizedApplications() throws Throwable {
        Assert.assertTrue(getAuthorisedApplications().isEmpty());
    }

    @Then("^I should not have any authorized users$")
    public void iShouldNotHaveAnyAuthorizedUsers() throws Throwable {
        Assert.assertTrue(getAuthorizedUsers().isEmpty());
    }

    @Then("^I should not have any authorized groups$")
    public void iShouldNotHaveAnyAuthorizedGroups() throws Throwable {
        Assert.assertTrue(getAuthorizedGroups().isEmpty());
    }

    // Allowed resource types
    private enum RESOURCE_TYPE {
        APPLICATION, ENVIRONMENT, LOCATION, ORCHESTRATOR
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
