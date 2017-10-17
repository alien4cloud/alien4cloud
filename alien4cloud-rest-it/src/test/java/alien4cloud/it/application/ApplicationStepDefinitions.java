package alien4cloud.it.application;

import static alien4cloud.it.Context.getInstance;
import static alien4cloud.it.Context.getRestClientInstance;
import static alien4cloud.it.utils.TestUtils.nullAsString;
import static alien4cloud.it.utils.TestUtils.nullable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.collect.Maps;
import org.junit.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.it.Context;
import alien4cloud.it.common.CommonStepDefinitions;
import alien4cloud.it.security.AuthenticationStepDefinitions;
import alien4cloud.it.topology.EditorStepDefinitions;
import alien4cloud.it.topology.TopologyStepDefinitions;
import alien4cloud.it.topology.TopologyTemplateStepDefinitions;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.application.EnvironmentType;
import alien4cloud.model.common.Tag;
import alien4cloud.rest.application.model.ApplicationEnvironmentDTO;
import alien4cloud.rest.application.model.ApplicationEnvironmentRequest;
import alien4cloud.rest.application.model.CreateApplicationRequest;
import alien4cloud.rest.application.model.UpdateApplicationEnvironmentRequest;
import alien4cloud.rest.component.ComponentSearchRequest;
import alien4cloud.rest.component.UpdateTagRequest;
import alien4cloud.rest.model.FilteredSearchRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.topology.TopologyDTO;
import alien4cloud.utils.ReflectionUtil;
import alien4cloud.utils.VersionUtil;
import cucumber.api.DataTable;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApplicationStepDefinitions {
    private static final String TEST_APPLICATION_IMAGE = "src/test/resources/data/test-image.png";
    public static Application CURRENT_APPLICATION;
    private final ObjectMapper jsonMapper = new ObjectMapper();
    public static Map<String, Application> CURRENT_APPLICATIONS = Maps.newHashMap(); // | APP_NAME -> APP_OBJECT |

    private AuthenticationStepDefinitions authSteps = new AuthenticationStepDefinitions();
    private CommonStepDefinitions commonStepDefinitions = new CommonStepDefinitions();
    private TopologyStepDefinitions topoSteps = new TopologyStepDefinitions();

    private void setAppVersionIdToContext(String appId) throws IOException {
        String applicationVersionJson = getRestClientInstance().get("/rest/v1/applications/" + appId + "/versions");
        RestResponse<ApplicationVersion> appVersion = JsonUtil.read(applicationVersionJson, ApplicationVersion.class);
        Context.getInstance().registerApplicationVersionId(appVersion.getData().getVersion(), appVersion.getData().getId());
    }

    @SuppressWarnings("rawtypes")
    public void setAppEnvironmentIdToContext(String applicationName) throws IOException {
        String applicationId = Context.getInstance().getApplicationId(applicationName);
        FilteredSearchRequest request = new FilteredSearchRequest();
        request.setFrom(0);
        request.setSize(10);
        String applicationEnvironmentsJson = getRestClientInstance().postJSon("/rest/v1/applications/" + applicationId + "/environments/search",
                JsonUtil.toString(request));
        RestResponse<GetMultipleDataResult> restResponse = JsonUtil.read(applicationEnvironmentsJson, GetMultipleDataResult.class);
        GetMultipleDataResult searchResp = restResponse.getData();
        ApplicationEnvironmentDTO appEnvDTO = JsonUtil.readObject(JsonUtil.toString(searchResp.getData()[0]), ApplicationEnvironmentDTO.class);
        Context.getInstance().registerApplicationEnvironmentId(applicationName, appEnvDTO.getName(), appEnvDTO.getId());
    }

    @When("^I create an application with name \"([^\"]*)\", archive name \"([^\"]*)\", description \"([^\"]*)\" and topology template id \"([^\"]*)\"$")
    public void stepCreateApplication(String name, String archiveName, String description, String topologyTemplateId) throws Throwable {
        createApplication(nullable(name), nullable(archiveName), nullable(description), nullable(topologyTemplateId));
    }

    private void createApplication(String name, String archiveName, String description, String topologyTemplateId) throws Throwable {
        doCreateApplication(name, archiveName, description, topologyTemplateId, true);
    }

    private void doCreateApplication(String name, String archiveName, String description, String topologyTemplateId, boolean register) throws IOException {
        CreateApplicationRequest request = new CreateApplicationRequest(archiveName, name, description, topologyTemplateId);
        Context.getInstance().registerRestResponse(getRestClientInstance().postJSon("/rest/v1/applications/", JsonUtil.toString(request)));

        // Registration makes a lot of implicit calls for simplification of features files but make them not fully meaningful. We should stop using group things
        // and rely on complete features.
        if (!register) {
            return;
        }
        try {
            // check the created application (topologyId)
            RestResponse<String> response = JsonUtil.read(Context.getInstance().getRestResponse(), String.class);
            String applicationJson = getRestClientInstance().get("/rest/v1/applications/" + response.getData());
            Application application = JsonUtil.read(applicationJson, Application.class).getData();
            if (application != null) {
                CURRENT_APPLICATION = application;
                CURRENT_APPLICATIONS.put(name, application);
                Context.getInstance().registerApplication(application);
                Context.getInstance().registerApplicationId(name, application.getId());
                setAppEnvironmentIdToContext(application.getName());
                setAppVersionIdToContext(application.getId());
                String topologyId = Csar.createId(application.getId(), VersionUtil.DEFAULT_VERSION_NAME);
                assertNotNull(topologyId);
                Context.getInstance().registerTopologyId(topologyId);
            }
        } catch (Throwable t) {
        }
    }

    @When("^I get the application with id \"([^\"]*)\"$")
    public void getApplication(String id) throws Throwable {
        Context.getInstance().registerRestResponse(getRestClientInstance().get("/rest/v1/applications/" + id));
        // Try to register the application (this works only if the operations is successful)
        try {
            CURRENT_APPLICATION = null;
            CURRENT_APPLICATION = JsonUtil.read(Context.getInstance().getRestResponse(), Application.class).getData();
        } catch (IOException e) {
            // Registration is optional
        }
    }

    private String appToArchName(String name) {
        return name.replaceAll("\\W", "");
    }

    @When("^I retrieve the newly created application$")
    public void I_retrieve_the_newly_created_application() throws Throwable {
        // App from context
        Application contextApp = Context.getInstance().getApplication();
        Context.getInstance().registerRestResponse(getRestClientInstance().get("/rest/v1/applications/" + contextApp.getId()));
    }

    @Given("^There is a \"([^\"]*)\" application$")
    public void There_is_a_application(String applicationName) throws Throwable {
        FilteredSearchRequest searchRequest = new FilteredSearchRequest(applicationName, 0, 50, null);
        String searchResponse = getRestClientInstance().postJSon("/rest/v1/applications/search", JsonUtil.toString(searchRequest));
        RestResponse<FacetedSearchResult> response = JsonUtil.read(searchResponse, FacetedSearchResult.class);
        boolean hasApplication = false;
        for (Object appAsObj : response.getData().getData()) {
            Application app = JsonUtil.readObject(JsonUtil.toString(appAsObj), Application.class);
            if (applicationName.equals(app.getName())) {
                hasApplication = true;
                CURRENT_APPLICATION = app;
            }
        }
        if (!hasApplication) {
            doCreateApplication(applicationName, appToArchName(applicationName), null, null, true);
        }
    }

    @When("^I add a tag with key \"([^\"]*)\" and value \"([^\"]*)\" to the application$")
    public void I_add_a_tag_with_key_and_value_to_the_component(String key, String value) throws Throwable {
        addTag(CURRENT_APPLICATION.getId(), key, value);
    }

    private void addTag(String applicationId, String key, String value) throws JsonProcessingException, IOException {
        UpdateTagRequest updateTagRequest = new UpdateTagRequest();
        updateTagRequest.setTagKey(key);
        updateTagRequest.setTagValue(value);
        Context.getInstance().registerRestResponse(
                getRestClientInstance().postJSon("/rest/v1/applications/" + applicationId + "/tags", jsonMapper.writeValueAsString(updateTagRequest)));

    }

    @Given("^There is a \"([^\"]*)\" application with tags:$")
    public void There_is_a_application_with_tags(String applicationName, DataTable tags) throws Throwable {
        // Create a new application with tags
        doCreateApplication(applicationName, appToArchName(applicationName), null, null, true);
        String responseAsJson = Context.getInstance().getRestResponse();
        String applicationId = JsonUtil.read(responseAsJson, String.class).getData();
        Context.getInstance().registerApplicationId(applicationName, applicationId);
        // Add tags to the application
        for (List<String> rows : tags.raw()) {
            addTag(applicationId, rows.get(0), rows.get(1));
        }
        setAppEnvironmentIdToContext(applicationName);
        Context.getInstance().registerRestResponse(responseAsJson);
    }

    @Given("^I have an application tag \"([^\"]*)\"$")
    public boolean I_have_and_a_tag(String tag) throws Throwable {
        Context.getInstance().registerRestResponse(getRestClientInstance().get("/rest/v1/applications/" + CURRENT_APPLICATION.getId()));
        Application application = JsonUtil.read(Context.getInstance().takeRestResponse(), Application.class).getData();
        assertTrue(application.getTags().contains(new Tag(tag, null)));
        return application.getTags().contains(new Tag(tag, null));
    }

    @When("^I delete an application tag with key \"([^\"]*)\"$")
    public void I_delete_a_tag_with_key(String tagId) throws Throwable {
        Context.getInstance().registerRestResponse(getRestClientInstance().delete("/rest/v1/applications/" + CURRENT_APPLICATION.getId() + "/tags/" + tagId));
    }

    private Set<String> registeredApps = Sets.newHashSet();
    private String previousRestResponse;

    @Given("^There is (\\d+) applications indexed in ALIEN$")
    public void There_is_applications_indexed_in_ALIEN(int applicationCount) throws Throwable {
        CURRENT_APPLICATIONS.clear();
        registeredApps.clear();
        for (int i = 0; i < applicationCount; i++) {
            String appName = "name" + i;

            doCreateApplication(appName, appToArchName(appName), "", null, true);
            registeredApps.add(appName);
            CURRENT_APPLICATIONS.put(appName, CURRENT_APPLICATION);
        }
    }

    @When("^I search applications from (\\d+) with result size of (\\d+)$")
    public void I_search_applications_from_with_result_size_of(int from, int to) throws Throwable {
        FilteredSearchRequest searchRequest = new FilteredSearchRequest("", from, to, null);
        previousRestResponse = Context.getInstance().getRestResponse();
        Context.getInstance().registerRestResponse(getRestClientInstance().postJSon("/rest/v1/applications/search", JsonUtil.toString(searchRequest)));
    }

    @Then("^The RestResponse must contain (\\d+) applications.$")
    public void The_RestResponse_must_contain_applications(int count) throws Throwable {
        RestResponse<FacetedSearchResult> response = JsonUtil.read(Context.getInstance().getRestResponse(), FacetedSearchResult.class);
        assertEquals(count, response.getData().getTotalResults());
        assertEquals(count, response.getData().getData().length);
    }

    @Then("^I should be able to view the (\\d+) other applications.$")
    public void I_should_be_able_to_view_the_other_applications(int count) throws Throwable {

        removeFromRegisteredApps(previousRestResponse);
        removeFromRegisteredApps(Context.getInstance().getRestResponse());

        assertEquals(0, registeredApps.size());
    }

    @SuppressWarnings("unchecked")
    private void removeFromRegisteredApps(String responseAsJson) throws Throwable {
        RestResponse<FacetedSearchResult> response = JsonUtil.read(responseAsJson, FacetedSearchResult.class);
        for (Object appAsObj : response.getData().getData()) {
            Map<String, Object> appAsMap = (Map<String, Object>) appAsObj;
            registeredApps.remove(appAsMap.get("name"));
        }
    }

    @When("^i update its image$")
    public void i_update_its_image() throws Throwable {
        String appId = JsonUtil.read(Context.getInstance().getRestResponse(), String.class).getData();
        RestResponse<String> response = JsonUtil.read(getRestClientInstance().postMultipart("/rest/v1/applications/" + appId + "/image", "file",
                Files.newInputStream(Paths.get(TEST_APPLICATION_IMAGE))), String.class);
        assertNull(response.getError());
    }

    @Then("^the application can be found in ALIEN$")
    public Application the_application_can_be_found_in_ALIEN() throws Throwable {
        String appId = CURRENT_APPLICATION.getId();
        RestResponse<Application> response = JsonUtil.read(getRestClientInstance().get("/rest/v1/applications/" + appId), Application.class);
        assertNotNull(response.getData());
        return response.getData();
    }

    @Then("^the application can be found in ALIEN with its new image$")
    public void the_application_can_be_found_in_ALIEN_with_its_new_image() throws Throwable {
        Application app = the_application_can_be_found_in_ALIEN();
        assertNotNull(app.getImageId());
    }

    @Given("^I create a new application with name \"([^\"]*)\" and description \"([^\"]*)\" and node templates$")
    public void I_create_a_new_application_with_name_and_description_and_node_templates(String applicationName, String applicationDescription,
            DataTable nodeTemplates) throws Throwable {
        // create the topology
        doCreateApplication(applicationName, appToArchName(applicationName), applicationDescription, null, true);

        EditorStepDefinitions.do_i_get_the_current_topology();

        // add all specified node template to a specific topology (from Application or Topology template)
        for (List<String> row : nodeTemplates.raw()) {
            Map<String, String> operationMap = Maps.newHashMap();
            operationMap.put("type", AddNodeOperation.class.getName());
            operationMap.put("nodeName", row.get(0));
            operationMap.put("indexedNodeTypeId", row.get(1));

            EditorStepDefinitions.do_i_execute_the_operation(operationMap);
        }
        // Save the topology
        EditorStepDefinitions.do_i_save_the_topology();
    }

    @When("^I add a role \"([^\"]*)\" to user \"([^\"]*)\" on the application \"([^\"]*)\"$")
    public void I_add_a_role_to_user_on_the_application(String role, String username, String applicationName) throws Throwable {
        I_search_for_application(applicationName);
        Context.getInstance().registerRestResponse(
                getRestClientInstance().put("/rest/v1/applications/" + CURRENT_APPLICATION.getId() + "/roles/users/" + username + "/" + role));
    }

    @When("^I search for \"([^\"]*)\" application$")
    public void I_search_for_application(String applicationName) throws Throwable {
        ComponentSearchRequest searchRequest = new ComponentSearchRequest(null, applicationName, 0, 10, null);
        String searchResponse = getRestClientInstance().postJSon("/rest/v1/applications/search", JsonUtil.toString(searchRequest));
        Context.getInstance().registerRestResponse(searchResponse);
        RestResponse<FacetedSearchResult> response = JsonUtil.read(searchResponse, FacetedSearchResult.class);
        for (Object appAsObj : response.getData().getData()) {
            Application app = JsonUtil.readObject(JsonUtil.toString(appAsObj), Application.class);
            if (applicationName.equals(app.getName())) {
                CURRENT_APPLICATION = app;
            }
        }
    }

    @Then("^The application should have a user \"([^\"]*)\" having \"([^\"]*)\" role$")
    public void The_application_should_have_a_user_having_role(String username, String expectedRole) throws Throwable {
        assertNotNull(CURRENT_APPLICATION);
        assertNotNull(CURRENT_APPLICATION.getUserRoles());
        Set<String> userRoles = CURRENT_APPLICATION.getUserRoles().get(username);
        assertNotNull(userRoles);
        assertTrue(userRoles.contains(expectedRole));

    }

    @Given("^there is a user \"([^\"]*)\" with the \"([^\"]*)\" role on the application \"([^\"]*)\"$")
    public void there_is_a_user_with_the_role_on_the_application(String username, String expectedRole, String applicationName) throws Throwable {
        authSteps.There_is_a_user_in_the_system(username);
        I_search_for_application(applicationName);
        Map<String, Set<String>> userRoles = CURRENT_APPLICATION.getUserRoles();
        if (userRoles != null && userRoles.containsKey(username)) {
            if (userRoles.get(username) != null && userRoles.get(username).contains(expectedRole)) {
                return;
            }
        }

        I_add_a_role_to_user_on_the_application(expectedRole, username, applicationName);
    }

    @Given("^there is a user \"([^\"]*)\" with the following roles on the application \"([^\"]*)\"$")
    public void there_is_a_user_with_the_following_roles_on_the_application(String username, String applicationName, List<String> expectedRoles)
            throws Throwable {

        for (String expectedRole : expectedRoles) {
            there_is_a_user_with_the_role_on_the_application(username, expectedRole, applicationName);
        }
    }

    @When("^I remove a role \"([^\"]*)\" to user \"([^\"]*)\" on the application \"([^\"]*)\"$")
    public void I_remove_a_role_to_user_on_the_application(String role, String username, String applicationName) throws Throwable {
        I_search_for_application(applicationName);
        Context.getInstance().registerRestResponse(
                getRestClientInstance().delete("/rest/v1/applications/" + CURRENT_APPLICATION.getId() + "/roles/users/" + username + "/" + role));
    }

    @Then("^The application should have a user \"([^\"]*)\" not having \"([^\"]*)\" role$")
    public void The_application_should_have_a_user_not_having_role(String username, String expectedRole) throws Throwable {
        if (CURRENT_APPLICATION != null && CURRENT_APPLICATION.getUserRoles() != null) {
            Set<String> userRoles = CURRENT_APPLICATION.getUserRoles().get(username);
            assertTrue(userRoles == null || !userRoles.contains(expectedRole));
        }
    }

    @When("^I delete the application \"([^\"]*)\"$")
    public void I_delete_the_application(String applicationName) throws Throwable {
        String id = CURRENT_APPLICATION.getName().equals(applicationName) ? CURRENT_APPLICATION.getId() : CURRENT_APPLICATIONS.get(applicationName).getId();
        Context.getInstance().registerRestResponse(getRestClientInstance().delete("/rest/v1/applications/" + id));
    }

    @Then("^the application should not be found$")
    public Application the_application_should_not_be_found() throws Throwable {
        RestResponse<Application> response = JsonUtil.read(getRestClientInstance().get("/rest/v1/applications/" + CURRENT_APPLICATION.getId()),
                Application.class);
        assertNull(response.getData());
        return response.getData();
    }

    @Given("^I have applications with names and descriptions$")
    public void I_have_applications_with_names_and_description(DataTable applicationNames) throws Throwable {
        CURRENT_APPLICATIONS.clear();
        // Create each application and store in CURRENT_APPS
        for (List<String> app : applicationNames.raw()) {
            doCreateApplication(app.get(0), appToArchName(app.get(0)), app.get(1), null, true);
            RestResponse<String> reponse = JsonUtil.read(Context.getInstance().getRestResponse(), String.class);
            String applicationJson = getRestClientInstance().get("/rest/v1/applications/" + reponse.getData());
            RestResponse<Application> application = JsonUtil.read(applicationJson, Application.class);
            CURRENT_APPLICATIONS.put(app.get(0), application.getData());
            Context.getInstance().registerApplicationId(application.getData().getName(), application.getData().getId());
            setAppEnvironmentIdToContext(application.getData().getName());
        }

        assertEquals(CURRENT_APPLICATIONS.size(), applicationNames.raw().size());

    }

    @Given("^I have applications with names and descriptions and a topology containing a nodeTemplate \"([^\"]*)\" related to \"([^\"]*)\"$")
    public void I_have_applications_with_names_and_description_containing_nodetemplate(String nodeName, String componentType,
            Map<String, String> applicationRequests) throws Throwable {
        CURRENT_APPLICATIONS.clear();

        // Prepare a cucumber data table using the node infos.
        List<String> nodeData = Lists.newArrayList(nodeName, componentType);
        List<List<String>> raw = Lists.newArrayList();
        raw.add(nodeData);
        DataTable dataTable = DataTable.create(raw);

        // Create each application and store in CURRENT_APPS
        for (java.util.Map.Entry<String, String> request : applicationRequests.entrySet()) {
            I_create_a_new_application_with_name_and_description_and_node_templates(request.getKey(), request.getValue(), dataTable);
            CURRENT_APPLICATIONS.put(request.getKey(), CURRENT_APPLICATION);
        }

        assertEquals(CURRENT_APPLICATIONS.size(), applicationRequests.size());
    }

    @Given("^I add a role \"([^\"]*)\" to group \"([^\"]*)\" on the application \"([^\"]*)\"$")
    public void I_add_a_role_to_group_on_the_application(String role, String groupName, String applicationName) throws Throwable {
        I_search_for_application(applicationName);
        Context.getInstance().registerRestResponse(getRestClientInstance()
                .put("/rest/v1/applications/" + CURRENT_APPLICATION.getId() + "/roles/groups/" + Context.getInstance().getGroupId(groupName) + "/" + role));
    }

    @And("^The application should have a group \"([^\"]*)\" having \"([^\"]*)\" role$")
    public void The_application_should_have_a_group_having_role(String groupName, String role) throws Throwable {
        assertNotNull(CURRENT_APPLICATION);
        assertNotNull(CURRENT_APPLICATION.getGroupRoles());
        Set<String> groupRoles = CURRENT_APPLICATION.getGroupRoles().get(Context.getInstance().getGroupId(groupName));
        assertNotNull(groupRoles);
        assertTrue(groupRoles.contains(role));
    }

    @And("^There is a group \"([^\"]*)\" with the following roles on the application \"([^\"]*)\"$")
    public void There_is_a_group_with_the_following_roles_on_the_application(String groupName, String applicationName, List<String> expectedRoles)
            throws Throwable {
        for (String expectedRole : expectedRoles) {
            I_add_a_role_to_group_on_the_application(expectedRole, groupName, applicationName);
        }
    }

    @When("^I remove a role \"([^\"]*)\" from group \"([^\"]*)\" on the application \"([^\"]*)\"$")
    public void I_remove_a_role_from_group_on_the_application(String role, String groupName, String applicationName) throws Throwable {
        I_search_for_application(applicationName);
        Context.getInstance().registerRestResponse(getRestClientInstance()
                .delete("/rest/v1/applications/" + CURRENT_APPLICATION.getId() + "/roles/groups/" + Context.getInstance().getGroupId(groupName) + "/" + role));
    }

    @And("^The application should have the group \"([^\"]*)\" not having \"([^\"]*)\" role$")
    public void The_application_should_have_the_group_not_having_role(String groupName, String role) throws Throwable {
        if (CURRENT_APPLICATION.getGroupRoles() != null) {
            Set<String> groupRoles = CURRENT_APPLICATION.getGroupRoles().get(groupName);
            if (groupRoles != null) {
                assertFalse(groupRoles.contains(role));
            }
        }
    }

    @And("^The application should have the group \"([^\"]*)\" having \"([^\"]*)\" role$")
    public void The_application_should_have_the_group_having_role(String groupName, String role) throws Throwable {
        assertNotNull(CURRENT_APPLICATION.getGroupRoles());
        Set<String> groupRoles = CURRENT_APPLICATION.getGroupRoles().get(Context.getInstance().getGroupId(groupName));
        assertNotNull(groupRoles);
        assertTrue(groupRoles.contains(role));
    }

    @And("^The RestResponse must contain these applications$")
    public void The_RestResponse_must_contain_these_applications(List<String> expectedApplications) throws Throwable {
        RestResponse<FacetedSearchResult> response = JsonUtil.read(Context.getInstance().getRestResponse(), FacetedSearchResult.class);
        assertNotNull(response.getData());
        assertEquals(expectedApplications.size(), response.getData().getTotalResults());
        assertEquals(expectedApplications.size(), response.getData().getData().length);
        Set<String> actualApplications = Sets.newHashSet();
        for (Object appObj : response.getData().getData()) {
            actualApplications.add(((Map) appObj).get("name").toString());
        }
        assertEquals(Sets.newHashSet(expectedApplications), actualApplications);
    }

    @Then("^I should receive an application without \"([^\"]*)\" as user$")
    public void I_should_receive_an_application_without_as_user(String userName) throws Throwable {
        RestResponse<Application> response = JsonUtil.read(Context.getInstance().getRestResponse(), Application.class);
        Assert.assertNull(response.getError());
        Assert.assertNotNull(response.getData());
        Application application = response.getData();
        if (application.getUserRoles() != null) {
            Assert.assertFalse(application.getUserRoles().containsKey(userName));
        }
    }

    @When("^I set the \"([^\"]*)\" of this application to \"([^\"]*)\"$")
    public void I_set_the_of_this_application_to(String fieldName, String fieldValue) throws Throwable {
        Map<String, Object> request = Maps.newHashMap();
        request.put(fieldName, fieldValue);
        Context.getInstance()
                .registerRestResponse(getRestClientInstance().putJSon("/rest/v1/applications/" + CURRENT_APPLICATION.getId(), JsonUtil.toString(request)));
        ReflectionUtil.setPropertyValue(CURRENT_APPLICATION, fieldName, fieldValue);
    }

    @And("^The application can be found in ALIEN with its \"([^\"]*)\" set to \"([^\"]*)\"$")
    public void The_application_can_be_found_in_ALIEN_with_its_set_to(String fieldName, String fieldValue) throws Throwable {
        Application application = the_application_can_be_found_in_ALIEN();
        Assert.assertEquals(fieldValue, ReflectionUtil.getPropertyValue(application, fieldName).toString());
    }

    @Given("^I create an application environment of type \"([^\"]*)\" with name \"([^\"]*)\", with inputs from environment \"([^\"]*)\" and description \"([^\"]*)\" for the newly created application$")
    public void iCreateAnApplicationEnvironmentOfTypeWithNameWithInputsFromEnvironmentAndDescriptionForTheNewlyCreatedApplication(String appEnvType,
            String appEnvName, String copyInputsFrom, String appEnvDescription) throws Throwable {
        Assert.assertTrue(EnvironmentType.valueOf(appEnvType).toString().equals(appEnvType));
        Assert.assertNotNull(appEnvName);

        ApplicationEnvironmentRequest appEnvRequest = new ApplicationEnvironmentRequest();
        appEnvRequest.setEnvironmentType(EnvironmentType.valueOf(appEnvType));
        appEnvRequest.setName(appEnvName);
        appEnvRequest.setDescription(appEnvDescription);
        appEnvRequest.setVersionId("0.1.0-SNAPSHOT");
        if (CURRENT_APPLICATION != null) {
            appEnvRequest.setInputCandidate(Context.getInstance().getApplicationEnvironmentId(CURRENT_APPLICATION.getName(), copyInputsFrom));
        }
        Context.getInstance()
                .registerRestResponse(getRestClientInstance().postJSon(
                        "/rest/v1/applications/" + nullAsString(CURRENT_APPLICATION == null ? null : CURRENT_APPLICATION.getId()) + "/environments",
                        JsonUtil.toString(appEnvRequest)));
        RestResponse<String> appEnvId = JsonUtil.read(Context.getInstance().getRestResponse(), String.class);
        if (appEnvId.getError() == null && appEnvId.getData() != null) {
            Context.getInstance().registerApplicationEnvironmentId(CURRENT_APPLICATION.getName(), appEnvName, appEnvId.getData());
        }
    }

    @When("^I create an application environment of type \"([^\"]*)\" with name \"([^\"]*)\" and description \"([^\"]*)\" for the newly created application$")
    public void I_create_an_application_environment_of_type_with_name_and_description_for_the_newly_created_application(String appEnvType, String appEnvName,
            String appEnvDescription) throws Throwable {
        iCreateAnApplicationEnvironmentOfTypeWithNameWithInputsFromEnvironmentAndDescriptionForTheNewlyCreatedApplication(appEnvType, appEnvName, null,
                appEnvDescription);
    }

    @When("^I get the application environment named \"([^\"]*)\"$")
    public void I_get_the_application_environment_named(String applicationEnvironmentName) throws Throwable {
        Assert.assertNotNull(CURRENT_APPLICATION);
        String applicationEnvId = Context.getInstance().getApplicationEnvironmentId(CURRENT_APPLICATION.getName(), applicationEnvironmentName);
        Context.getInstance().registerRestResponse(
                getRestClientInstance().get("/rest/v1/applications/" + CURRENT_APPLICATION.getId() + "/environments/" + applicationEnvId));
        RestResponse<ApplicationEnvironmentDTO> appEnvironment = JsonUtil.read(Context.getInstance().getRestResponse(), ApplicationEnvironmentDTO.class);
        Assert.assertNotNull(appEnvironment.getData());
        Assert.assertEquals(appEnvironment.getData().getId(), applicationEnvId);
    }

    @When("^I update the application environment named \"([^\"]*)\" with values$")
    public void I_update_the_application_environment_named_with_values(String applicationEnvironmentName, DataTable appEnvAttributeValues) throws Throwable {
        UpdateApplicationEnvironmentRequest appEnvRequest = new UpdateApplicationEnvironmentRequest();
        String attribute = null, attributeValue = null;
        for (List<String> attributesToUpdate : appEnvAttributeValues.raw()) {
            attribute = attributesToUpdate.get(0);
            attributeValue = attributesToUpdate.get(1);
            switch (attribute) {
            case "name":
                appEnvRequest.setName(attributeValue);
                break;
            case "description":
                appEnvRequest.setDescription(attributeValue);
                break;
            case "environmentType":
                appEnvRequest.setEnvironmentType(EnvironmentType.valueOf(attributeValue));
                break;
            case "currentVersionId":
                appEnvRequest.setCurrentVersionId(attributeValue);
                break;
            default:
                log.info("Attribute [ {} ] not found in ApplicationEnvironmentRequest object", attribute);
                break;
            }
        }
        String environmentId = Context.getInstance().getApplicationEnvironmentId(CURRENT_APPLICATION.getName(), applicationEnvironmentName);
        // send the update request
        Context.getInstance().registerRestResponse(getRestClientInstance()
                .putJSon("/rest/v1/applications/" + CURRENT_APPLICATION.getId() + "/environments/" + environmentId, JsonUtil.toString(appEnvRequest)));
        if (StringUtils.isNotBlank(appEnvRequest.getName())) {
            Context.getInstance().registerApplicationEnvironmentId(CURRENT_APPLICATION.getId(), appEnvRequest.getName(), environmentId);
        }
    }

    @When("^I update the environment named \"([^\"]*)\" to use cloud \"([^\"]*)\" for application \"([^\"]*)\"$")
    public void I_update_the_environment_named_to_use_cloud_for_application(String envName, String cloudName, String appName) throws Throwable {
        UpdateApplicationEnvironmentRequest appEnvRequest = new UpdateApplicationEnvironmentRequest();
        // appEnvRequest.setCloudId(Context.getInstance().getCloudId(cloudName));
        Assert.fail("Fix test");
        String applicationId = Context.getInstance().getApplicationId(appName);
        String applicationEnvironmentId = Context.getInstance().getApplicationEnvironmentId(appName, envName);
        // send the update request
        Context.getInstance().registerRestResponse(getRestClientInstance()
                .putJSon("/rest/v1/applications/" + applicationId + "/environments/" + applicationEnvironmentId, JsonUtil.toString(appEnvRequest)));
    }

    @When("^I delete the registered application environment named \"([^\"]*)\" from its id$")
    public void I_delete_the_registered_application_environment_named_from_its_id(String applicationEnvironmentName) throws Throwable {
        Context.getInstance().registerRestResponse(getRestClientInstance().delete("/rest/v1/applications/" + CURRENT_APPLICATION.getId() + "/environments/"
                + Context.getInstance().getApplicationEnvironmentId(CURRENT_APPLICATION.getName(), applicationEnvironmentName)));
        // RestResponse<Boolean> appEnvironment = JsonUtil.read(Context.getInstance().getRestResponse(), Boolean.class);
        // Assert.assertNotNull(appEnvironment.getData());
    }

    @Given("^I must have an environment named \"([^\"]*)\" for application \"([^\"]*)\"$")
    public void I_must_have_an_environment_named_for_application(String envName, String appName) throws Throwable {
        Assert.assertNotNull(envName);
        Assert.assertNotNull(appName);
        String environmentId = Context.getInstance().getApplicationEnvironmentId(appName, envName);
        Assert.assertNotNull(environmentId);
    }

    @Then("^The application update date has changed$")
    public void The_application_update_date_has_changed() throws Throwable {
        Application application = CURRENT_APPLICATION;
        Assert.assertNotEquals(application.getCreationDate(), application.getLastUpdateDate());
    }

    @When("^I create a new application with name \"([^\"]*)\" and description \"([^\"]*)\" based on this created template$")
    public void I_create_a_new_application_with_name_and_description_based_this_created_template(String name, String description) throws Throwable {
        String topologyTemplateId = TopologyTemplateStepDefinitions.CURRENT_TOPOLOGY_TEMP_ID;
        assertFalse(StringUtils.isBlank(topologyTemplateId));
        createApplication(name, appToArchName(name), description, topologyTemplateId);
    }

    @Then("^The created application topology is the same as the one in the base topology template$")
    public void The_created_application_topology_is_the_same_as_the_one_in_the_base_topology_template() throws Throwable {
        // created topology
        String topologyId = Csar.createId(CURRENT_APPLICATION.getId(), VersionUtil.DEFAULT_VERSION_NAME);
        Context.getInstance().registerRestResponse(getRestClientInstance().get("/rest/v1/topologies/" + topologyId));
        TopologyDTO createdTopology = JsonUtil.read(Context.getInstance().getRestResponse(), TopologyDTO.class, Context.getJsonMapper()).getData();

        // base topology template
        authSteps.I_am_authenticated_with_role("ARCHITECT"); // quick win solution
        String topoResponse = Context.getRestClientInstance().get("/rest/v1/catalog/topologies/" + TopologyTemplateStepDefinitions.CURRENT_TOPOLOGY_TEMP_ID);
        Topology topologyTemplateBase = JsonUtil.read(topoResponse, Topology.class, Context.getJsonMapper()).getData();

        Map<String, NodeTemplate> nodeTemplates = topologyTemplateBase.getNodeTemplates();

        // node templates count test
        assertEquals(createdTopology.getTopology().getNodeTemplates().size(), nodeTemplates.size());

        // node templates name / type test
        for (Map.Entry<String, NodeTemplate> entry : createdTopology.getTopology().getNodeTemplates().entrySet()) {
            assertTrue(nodeTemplates.containsKey(entry.getKey()));
            assertTrue(nodeTemplates.get(entry.getKey()).getType().equals(entry.getValue().getType()));
        }

    }

    @And("^I create a new application with name \"([^\"]*)\" and description \"([^\"]*)\" based on the template with name \"([^\"]*)\"$")
    public void iCreateANewApplicationWithNameAndDescriptionBasedOnTheTemplateWithName(String name, String description, String templateName) throws Throwable {
        String topologyTemplateId = TopologyTemplateStepDefinitions.getTopologyTemplateIdFromName(templateName);
        assertFalse(StringUtils.isBlank(topologyTemplateId));
        createApplication(name, appToArchName(name), description, topologyTemplateId);
    }

    @When("^I get the application named \"([^\"]*)\"$")
    public void iGetTheApplicationNamed(String name) throws Throwable {
        getInstance().registerRestResponse(getRestClientInstance().get("/rest/v1/applications/" + Context.getInstance().getApplicationId(name)));
    }
}
