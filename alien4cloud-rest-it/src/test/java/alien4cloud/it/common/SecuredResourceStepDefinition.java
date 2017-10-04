package alien4cloud.it.common;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
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
import alien4cloud.rest.orchestrator.model.SubjectsAuthorizationRequest;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.security.model.Group;
import alien4cloud.security.model.User;
import alien4cloud.utils.AlienUtils;
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
        case "LOCATION_POLICY":
            // resrouceName == orchestratorName/locationName/resourceName
            String[] decomposed = StringUtils.split(resourceName, "/");
            orchestratorName = decomposed.length == 1 ? orchestratorName : decomposed[0].trim();
            String locationName = decomposed.length == 1 ? LocationsDefinitionsSteps.DEFAULT_LOCATION_NAME : decomposed[1].trim();
            resourceName = decomposed[decomposed.length - 1].trim();
            String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
            String locationId = Context.getInstance().getLocationId(orchestratorId, locationName);
            String locationResourceId = Context.getInstance().getLocationResourceId(Context.getInstance().getOrchestratorId(orchestratorName), locationId, resourceName);
            String customEnpointPrefix = getCustomEndpointSuffix(resourceType);
            String urlFormat = "/rest/v1/orchestrators/%s/locations/%s/%s/%s/security";
            url = String.format(urlFormat, orchestratorId, locationId, customEnpointPrefix, locationResourceId);
            // url = "/rest/v1/orchestrators/" + Context.getInstance().getOrchestratorId(orchestratorName) + "/locations/"
            // + locationId + "/resources/" + locationResourceId + "/security";
            break;
        case "SERVICE" :
            String serviceId = Context.getInstance().getServiceId(resourceName);
            url = "/rest/v1/services/" + serviceId + "/security";
            break;

        default:
            Assert.fail("Dot not support resource type " + resourceType);
        }
        return url;
    }

    private String getCustomEndpointSuffix(String resourceType) {
        String customEndpointSuffix;
        switch (resourceType) {
        case "LOCATION_RESOURCE":
            customEndpointSuffix = "resources";
            break;
        case "LOCATION_POLICY":
            customEndpointSuffix = "policies";
            break;
        default:
            customEndpointSuffix = "";
        }
        return customEndpointSuffix;
    }

    private String getBatchSecuredResourceBaseURL(String resourceType, String resourceName) throws Throwable {
        String url = null;
        String orchestratorName = LocationsDefinitionsSteps.DEFAULT_ORCHESTRATOR_NAME;
        switch (resourceType) {
        case "LOCATION_RESOURCE":
        case "LOCATION_POLICY":
            // resrouceName == orchestratorName/locationName/resourceName
            String[] decomposed = StringUtils.split(resourceName, "/");
            orchestratorName = decomposed.length == 1 ? orchestratorName : decomposed[0].trim();
            String locationName = decomposed.length == 1 ? LocationsDefinitionsSteps.DEFAULT_LOCATION_NAME : decomposed[1].trim();
            String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
            String locationId = Context.getInstance().getLocationId(orchestratorId, locationName);
            String customEndpointSuffix = getCustomEndpointSuffix(resourceType);
            String urlFormat = "/rest/v1/orchestrators/%s/locations/%s/%s/security";
            url = String.format(urlFormat, orchestratorId, locationId, customEndpointSuffix);
            break;
        default:
            Assert.fail("Dot not support batch operation on resource type " + resourceType);
        }
        return url;
    }

    @Given("^I (successfully\\s)?grant access to the resource type \"([^\"]*)\" named \"([^\"]*)\" to the user \"([^\"]*)\"$")
    public void iGrantAccessToTheResourceTypeNamedToTheUser(String successfully, String resourceType, String resourceName, String userName) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon(getSecuredResourceBaseURL(resourceType, resourceName) + "/users",
                JsonUtil.toString(new String[] { userName })));
        CommonStepDefinitions.validateIfNeeded(StringUtils.isNotBlank(successfully));
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

    @Given("^I (successfully\\s)?grant access to the resource type \"([^\"]*)\" named \"([^\"]*)\" to the group \"([^\"]*)\"$")
    public void iGrantAccessToTheResourceTypeNamedToTheGroup(String successfully, String resourceType, String resourceName, String groupName) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon(getSecuredResourceBaseURL(resourceType, resourceName) + "/groups",
                JsonUtil.toString(new String[] { Context.getInstance().getGroupId(groupName) })));
        CommonStepDefinitions.validateIfNeeded(StringUtils.isNotBlank(successfully));
    }

    @Given("^I (successfully\\s)?grant access to the location named \"([^\"]*)\"/\"([^\"]*)\" to the group \"([^\"]*)\"$")
    public void iGrantAccessToTheLocationNamedToTheGroup(String successfully, String orchestratorName, String locationName, String groupName) throws Throwable {
        String url = "/rest/v1/orchestrators/" + Context.getInstance().getOrchestratorId(orchestratorName) + "/locations/"
                + LocationsDefinitionsSteps.getLocationIdFromName(orchestratorName, locationName) + "/security/groups";
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postJSon(url, JsonUtil.toString(new String[] { Context.getInstance().getGroupId(groupName) })));
        CommonStepDefinitions.validateIfNeeded(StringUtils.isNotBlank(successfully));
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

    @Given("^I (successfully\\s)?grant access to the resource type \"([^\"]*)\" named \"([^\"]*)\" to the application \"([^\"]*)\"$")
    public void iGrantAccessToTheResourceTypeNamedToTheApplication(String successfully, String resourceType, String resourceName, String applicationName)
            throws Throwable {
        ApplicationEnvironmentAuthorizationUpdateRequest request = new ApplicationEnvironmentAuthorizationUpdateRequest();
        request.setApplicationsToAdd(new String[] { Context.getInstance().getApplicationId(applicationName) });
        Context.getInstance().registerRestResponse(Context.getRestClientInstance()
                .postJSon(getSecuredResourceBaseURL(resourceType, resourceName) + "/environmentsPerApplication/", JsonUtil.toString(request)));

        CommonStepDefinitions.validateIfNeeded(StringUtils.isNotBlank(successfully));
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

    private Set<String> getAuthorisedEnvironmentTypes() throws IOException {
        ApplicationEnvironmentAuthorizationDTO[] dtos  = getApplicationEnvironmentAuthorizationDTOS();
        Set<String> envTypes = Sets.newHashSet();
        for (ApplicationEnvironmentAuthorizationDTO dto : dtos) {
            if (dto.getEnvironmentTypes() != null && !dto.getEnvironmentTypes().isEmpty()) {
                envTypes.addAll(dto.getEnvironmentTypes());
            }
        }
        return envTypes;
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

    @Given("^I (successfully\\s)?grant access to the resource type \"([^\"]*)\" named \"([^\"]*)\" to the environment \"([^\"]*)\" of the application \"([^\"]*)\"$")
    public void iGrantAccessToTheResourceTypeNamedToTheEnvironmentOfTheApplication(String successfully, String resourceType, String resourceName,
            String environmentName,
            String applicationName) throws Throwable {
        ApplicationEnvironmentAuthorizationUpdateRequest request = new ApplicationEnvironmentAuthorizationUpdateRequest();
        request.setEnvironmentsToAdd(new String[] { Context.getInstance().getApplicationEnvironmentId(applicationName, environmentName) });
        Context.getInstance().registerRestResponse(Context.getRestClientInstance()
                .postJSon(getSecuredResourceBaseURL(resourceType, resourceName) + "/environmentsPerApplication/", JsonUtil.toString(request)));

        CommonStepDefinitions.validateIfNeeded(StringUtils.isNotBlank(successfully));
    }

    @Given("^I (successfully\\s)?grant access to the resource type \"([^\"]*)\" named \"([^\"]*)\" to the environment type \"([^\"]*)\" of the application \"([^\"]*)\"$")
    public void iGrantAccessToTheResourceTypeNamedToTheEnvironmentTypeOfTheApplication(String successfully, String resourceType, String resourceName,
                                                                                   String environmentType,
                                                                                   String applicationName) throws Throwable {
        ApplicationEnvironmentAuthorizationUpdateRequest request = new ApplicationEnvironmentAuthorizationUpdateRequest();
        request.setEnvironmentTypesToAdd(new String[] { Context.getInstance().getApplicationId(applicationName) + ":" + environmentType });
        Context.getInstance().registerRestResponse(Context.getRestClientInstance()
                .postJSon(getSecuredResourceBaseURL(resourceType, resourceName) + "/environmentsPerApplication/", JsonUtil.toString(request)));

        CommonStepDefinitions.validateIfNeeded(StringUtils.isNotBlank(successfully));
    }

    @Then("^I should have following list of environments:$")
    public void iShouldHaveFollowingListOfEnvironments(DataTable rawExpectedEnvironments) throws Throwable {
        Assert.assertEquals(getExpectedNames(rawExpectedEnvironments), getAuthorisedEnvironments());
    }

    @Then("^I should have following list of environment types:$")
    public void iShouldHaveFollowingListOfEnvironmentTypes(DataTable rawExpectedEnvironmentTypes) throws Throwable {
        Assert.assertEquals(getExpectedNames(rawExpectedEnvironmentTypes), getAuthorisedEnvironmentTypes());
    }


    @Given("^I revoke access to the resource type \"([^\"]*)\" named \"([^\"]*)\" from the environment \"([^\"]*)\" of the application \"([^\"]*)\"$")
    public void iRevokeAccessToTheResourceTypeNamedFromTheEnvironmentOfTheApplication(String resourceType, String resourceName, String environmentName,
            String applicationName) throws Throwable {
        ApplicationEnvironmentAuthorizationUpdateRequest request = new ApplicationEnvironmentAuthorizationUpdateRequest();
        request.setEnvironmentsToDelete(new String[] { Context.getInstance().getApplicationEnvironmentId(applicationName, environmentName) });
        Context.getInstance().registerRestResponse(Context.getRestClientInstance()
                .postJSon(getSecuredResourceBaseURL(resourceType, resourceName) + "/environmentsPerApplication/", JsonUtil.toString(request)));
    }

    @Given("^I revoke access to the resource type \"([^\"]*)\" named \"([^\"]*)\" from the environment type \"([^\"]*)\" of the application \"([^\"]*)\"$")
    public void iRevokeAccessToTheResourceTypeNamedFromTheEnvironmentTypeOfTheApplication(String resourceType, String resourceName, String environmentType,
                                                                                      String applicationName) throws Throwable {
        ApplicationEnvironmentAuthorizationUpdateRequest request = new ApplicationEnvironmentAuthorizationUpdateRequest();
        request.setEnvironmentTypesToDelete(new String[] { Context.getInstance().getApplicationId(applicationName) + ":" + environmentType });
        Context.getInstance().registerRestResponse(Context.getRestClientInstance()
                .postJSon(getSecuredResourceBaseURL(resourceType, resourceName) + "/environmentsPerApplication/", JsonUtil.toString(request)));
    }

    @Then("^I should not have any authorized environments$")
    public void iShouldNotHaveAnyAuthorizedEnvironments() throws Throwable {
        Assert.assertTrue(getAuthorisedEnvironments().isEmpty());
    }

    @Then("^I should not have any authorized environment types$")
    public void iShouldNotHaveAnyAuthorizedEnvironmentTypes() throws Throwable {
        Assert.assertTrue(getAuthorisedEnvironmentTypes().isEmpty());
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

    /**
     *
     *
     * @param successfully
     * @param resourceType
     * @param username
     * @param resourcesNames
     * @throws Throwable
     */
    @Given("^I (successfully\\s)?grant access to the resources type \"([^\"]*)\" to the user \"([^\"]*)\"$")
    public void iGrantAccessToTheResourcesTypeToTheUser(String successfully, String resourceType, String username,
            List<String> resourcesNames) throws Throwable {
        SubjectsAuthorizationRequest request = new SubjectsAuthorizationRequest();
        request.setResources(getResourcesIds(resourcesNames));
        request.setCreate(new String[] { username });
        String oneResourceName = CollectionUtils.isEmpty(resourcesNames) ? null : resourcesNames.get(0);
        String url = getBatchSecuredResourceBaseURL(resourceType, oneResourceName);
        url += "/users";

        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon(url, JsonUtil.toString(request)));
        CommonStepDefinitions.validateIfNeeded(StringUtils.isNotBlank(successfully));
    }

    @Given("^I (successfully\\s)?revoke access to the resources type \"([^\"]*)\" from the user \"([^\"]*)\"$")
    public void iRevokeAccessToTheResourcesTypeToTheUser(String successfully, String resourceType, String username, List<String> resourcesNames)
            throws Throwable {
        SubjectsAuthorizationRequest request = new SubjectsAuthorizationRequest();
        request.setResources(getResourcesIds(resourcesNames));
        request.setDelete(new String[] { username });
        String oneResourceName = CollectionUtils.isEmpty(resourcesNames) ? null : resourcesNames.get(0);
        String url = getBatchSecuredResourceBaseURL(resourceType, oneResourceName);
        url += "/users";

        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon(url, JsonUtil.toString(request)));
        CommonStepDefinitions.validateIfNeeded(StringUtils.isNotBlank(successfully));
    }

    @Given("^I (successfully\\s)?grant access to the resources type \"([^\"]*)\" to the group \"([^\"]*)\"$")
    public void iGrantAccessToTheResourcesTypeToTheGroup(String successfully, String resourceType, String groupName,
            List<String> resourcesNames) throws Throwable {
        SubjectsAuthorizationRequest request = new SubjectsAuthorizationRequest();
        request.setResources(getResourcesIds(resourcesNames));
        request.setCreate(new String[] { Context.getInstance().getGroupId(groupName) });
        String oneResourceName = CollectionUtils.isEmpty(resourcesNames) ? null : resourcesNames.get(0);
        String url = getBatchSecuredResourceBaseURL(resourceType, oneResourceName);
        url += "/groups";

        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon(url, JsonUtil.toString(request)));
        CommonStepDefinitions.validateIfNeeded(StringUtils.isNotBlank(successfully));
    }

    @Given("^I (successfully\\s)?revoke access to the resources type \"([^\"]*)\" from the group \"([^\"]*)\"$")
    public void iRevokeAccessToTheResourcesTypeToTheGroup(String successfully, String resourceType, String groupName, List<String> resourcesNames)
            throws Throwable {
        SubjectsAuthorizationRequest request = new SubjectsAuthorizationRequest();
        request.setResources(getResourcesIds(resourcesNames));
        request.setDelete(new String[] { Context.getInstance().getGroupId(groupName) });
        String oneResourceName = CollectionUtils.isEmpty(resourcesNames) ? null : resourcesNames.get(0);
        String url = getBatchSecuredResourceBaseURL(resourceType, oneResourceName);
        url += "/groups";

        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon(url, JsonUtil.toString(request)));
        CommonStepDefinitions.validateIfNeeded(StringUtils.isNotBlank(successfully));
    }

    @Given("^I (successfully\\s)?grant access to the resources type \"([^\"]*)\" to the application \"([^\"]*)\"$")
    public void iGrantAccessToTheResourcesTypeToTheApplication(String successfully, String resourceType, String applicationName,
            List<String> resourcesNames) throws Throwable {

        ApplicationEnvironmentAuthorizationUpdateRequest request = new ApplicationEnvironmentAuthorizationUpdateRequest();
        request.setApplicationsToAdd(new String[] { Context.getInstance().getApplicationId(applicationName) });
        request.setResources(getResourcesIds(resourcesNames));
        String oneResourceName = CollectionUtils.isEmpty(resourcesNames) ? null : resourcesNames.get(0);
        String url = getBatchSecuredResourceBaseURL(resourceType, oneResourceName);
        url += "/environmentsPerApplication";

        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon(url, JsonUtil.toString(request)));
        CommonStepDefinitions.validateIfNeeded(StringUtils.isNotBlank(successfully));
    }

    @Given("^I (successfully\\s)?revoke access to the resources type \"([^\"]*)\" from the application \"([^\"]*)\"$")
    public void iRevokeAccessToTheResourcesTypeToTheApplication(String successfully, String resourceType, String applicationName, List<String> resourcesNames)
            throws Throwable {

        ApplicationEnvironmentAuthorizationUpdateRequest request = new ApplicationEnvironmentAuthorizationUpdateRequest();
        request.setApplicationsToDelete(new String[] { Context.getInstance().getApplicationId(applicationName) });
        request.setResources(getResourcesIds(resourcesNames));
        String oneResourceName = CollectionUtils.isEmpty(resourcesNames) ? null : resourcesNames.get(0);
        String url = getBatchSecuredResourceBaseURL(resourceType, oneResourceName);
        url += "/environmentsPerApplication";

        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon(url, JsonUtil.toString(request)));
        CommonStepDefinitions.validateIfNeeded(StringUtils.isNotBlank(successfully));
    }

    @Given("^I (successfully\\s)?grant access to the resources type \"([^\"]*)\" to the environment \"([^\"]*)\" of the application \"([^\"]*)\"$")
    public void iGrantAccessToTheResourcesTypeToTheApplicationEnvironment(String successfully, String resourceType, String environmentName,
            String applicationName, List<String> resourcesNames) throws Throwable {

        ApplicationEnvironmentAuthorizationUpdateRequest request = new ApplicationEnvironmentAuthorizationUpdateRequest();
        request.setEnvironmentsToAdd(new String[] { Context.getInstance().getApplicationEnvironmentId(applicationName, environmentName) });
        request.setResources(getResourcesIds(resourcesNames));
        String oneResourceName = CollectionUtils.isEmpty(resourcesNames) ? null : resourcesNames.get(0);
        String url = getBatchSecuredResourceBaseURL(resourceType, oneResourceName);
        url += "/environmentsPerApplication";

        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon(url, JsonUtil.toString(request)));
        CommonStepDefinitions.validateIfNeeded(StringUtils.isNotBlank(successfully));
    }

    @Given("^I (successfully\\s)?revoke access to the resources type \"([^\"]*)\" from the environment \"([^\"]*)\" of the application \"([^\"]*)\"$")
    public void iRevokeAccessToTheResourcesTypeToTheApplicationEnvironment(String successfully, String resourceType, String environmentName,
            String applicationName, List<String> resourcesNames) throws Throwable {

        ApplicationEnvironmentAuthorizationUpdateRequest request = new ApplicationEnvironmentAuthorizationUpdateRequest();
        request.setEnvironmentsToDelete(new String[] { Context.getInstance().getApplicationEnvironmentId(applicationName, environmentName) });
        request.setResources(getResourcesIds(resourcesNames));
        String oneResourceName = CollectionUtils.isEmpty(resourcesNames) ? null : resourcesNames.get(0);
        String url = getBatchSecuredResourceBaseURL(resourceType, oneResourceName);
        url += "/environmentsPerApplication";

        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon(url, JsonUtil.toString(request)));
        CommonStepDefinitions.validateIfNeeded(StringUtils.isNotBlank(successfully));
    }

    private String[] getResourcesIds(List<String> resourcesNames) {
        // resourceName == orchestratorName/locationName/resourceName
        Set<String> ids = Sets.newHashSet();
        AlienUtils.safe(resourcesNames).forEach(resourceName -> {
            String[] decomposed = StringUtils.split(resourceName, "/");
            String orchestratorName = decomposed.length == 1 ? LocationsDefinitionsSteps.DEFAULT_ORCHESTRATOR_NAME : decomposed[0].trim();
            String locationName = decomposed.length == 1 ? LocationsDefinitionsSteps.DEFAULT_LOCATION_NAME : decomposed[1].trim();
            resourceName = decomposed[decomposed.length - 1].trim();
            String locationId = Context.getInstance().getLocationId(Context.getInstance().getOrchestratorId(orchestratorName), locationName);
            String locationResourceId = Context.getInstance().getLocationResourceId(Context.getInstance().getOrchestratorId(orchestratorName), locationId,
                    resourceName);
            Assert.assertNotNull("Resource <" + resourceName + "> not found", locationResourceId);
            ids.add(locationResourceId);
        });

        return ids.toArray(new String[ids.size()]);
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

    // Allowed resource types
    private enum RESOURCE_TYPE {
        APPLICATION, ENVIRONMENT, LOCATION, ORCHESTRATOR
    }

}
