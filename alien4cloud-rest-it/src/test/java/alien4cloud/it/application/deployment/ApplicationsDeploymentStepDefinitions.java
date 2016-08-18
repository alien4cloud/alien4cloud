package alien4cloud.it.application.deployment;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.collect.Sets;
import org.junit.Assert;

import alien4cloud.it.Context;
import alien4cloud.it.application.ApplicationStepDefinitions;
import alien4cloud.it.common.CommonStepDefinitions;
import alien4cloud.it.exception.ITException;
import alien4cloud.it.utils.websocket.IStompDataFuture;
import alien4cloud.it.utils.websocket.StompConnection;
import alien4cloud.it.utils.websocket.StompData;
import alien4cloud.model.application.Application;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.paas.model.*;
import alien4cloud.rest.application.model.DeployApplicationRequest;
import alien4cloud.rest.deployment.DeploymentDTO;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.DataTable;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApplicationsDeploymentStepDefinitions {
    private CommonStepDefinitions commonSteps = new CommonStepDefinitions();
    private DeploymentTopologyStepDefinitions deploymentTopoSteps = new DeploymentTopologyStepDefinitions();
    private static Map<String, Set<DeploymentStatus>> pendingStatuses;

    static {
        pendingStatuses = Maps.newHashMap();
        pendingStatuses.put("deployment", Sets.newHashSet(DeploymentStatus.DEPLOYMENT_IN_PROGRESS, DeploymentStatus.INIT_DEPLOYMENT));
        pendingStatuses.put("undeployment", Sets.newHashSet(DeploymentStatus.UNDEPLOYMENT_IN_PROGRESS));
    }

    @When("I failsafe undeploy it")
    public void I_failsafe_undeploy_it() throws Throwable {
        I_undeploy_it(ApplicationStepDefinitions.CURRENT_APPLICATION, true);
    }

    @When("I undeploy it")
    public void I_undeploy_it() throws Throwable {
        I_undeploy_it(ApplicationStepDefinitions.CURRENT_APPLICATION, false);
    }

    public void I_undeploy_it(Application application, boolean failsafe) throws Throwable {
        String envId = Context.getInstance().getDefaultApplicationEnvironmentId(application.getName());
        String statusRequest = "/rest/v1/applications/" + application.getId() + "/environments/" + envId + "/status";
        RestResponse<String> statusResponse = JsonUtil.read(Context.getRestClientInstance().get(statusRequest), String.class);
        if (failsafe) {
            if (statusResponse.getError() != null) {
                log.warn("Error was supposed to be null but was : ", statusResponse.getError());
            }
        } else {
            assertNull(statusResponse.getError());
        }
        DeploymentStatus deploymentStatus = DeploymentStatus.valueOf(statusResponse.getData());
        if (!DeploymentStatus.UNDEPLOYED.equals(deploymentStatus) || !DeploymentStatus.UNDEPLOYMENT_IN_PROGRESS.equals(deploymentStatus)) {
            Context.getInstance().registerRestResponse(
                    Context.getRestClientInstance().delete("/rest/v1/applications/" + application.getId() + "/environments/" + envId + "/deployment"));
        }
        assertStatus(application.getName(), DeploymentStatus.UNDEPLOYED, Sets.newHashSet(DeploymentStatus.UNDEPLOYMENT_IN_PROGRESS), 10 * 60L * 1000L, null,
                failsafe);
    }

    @When("^I deploy it$")
    public void I_deploy_it() throws Throwable {
        // deploys the current application on default "Environment"
        log.info("Deploy : Deploying the application " + ApplicationStepDefinitions.CURRENT_APPLICATION.getName());
        DeployApplicationRequest deployApplicationRequest = getDeploymentAppRequest(ApplicationStepDefinitions.CURRENT_APPLICATION.getName(), null);
        String response = deploy(deployApplicationRequest);
        Context.getInstance().registerRestResponse(response);
    }

    private void setOrchestratorProperties() throws Throwable {
        if (Context.getInstance().getPreRegisteredOrchestratorProperties() != null) {
            deploymentTopoSteps.I_set_the_following_orchestrator_properties(Context.getInstance().getPreRegisteredOrchestratorProperties());
        }
    }

    public static DeployApplicationRequest getDeploymentAppRequest(String applicationName, String environmentName) throws IOException {
        DeployApplicationRequest deployApplicationRequest = new DeployApplicationRequest();
        Application application = ApplicationStepDefinitions.CURRENT_APPLICATION;

        // set application id and environment id for the request
        String applicationId = (applicationName == null) ? application.getId() : Context.getInstance().getApplicationId(applicationName);
        deployApplicationRequest.setApplicationId(applicationId);
        String environmentId = (environmentName == null) ? Context.getInstance().getDefaultApplicationEnvironmentId(applicationName)
                : Context.getInstance().getApplicationEnvironmentId(applicationName, environmentName);
        deployApplicationRequest.setApplicationEnvironmentId(environmentId);

        return deployApplicationRequest;
    }

    private void assertStatus(String applicationName, DeploymentStatus expectedStatus, Set<DeploymentStatus> pendingStatus, long timeout,
            String applicationEnvironmentName) throws Throwable {
        checkStatus(applicationName, null, expectedStatus, pendingStatus, timeout, applicationEnvironmentName, false);
    }

    private void assertStatus(String applicationName, DeploymentStatus expectedStatus, Set<DeploymentStatus> pendingStatus, long timeout,
            String applicationEnvironmentName, boolean failover) throws Throwable {
        checkStatus(applicationName, null, expectedStatus, pendingStatus, timeout, applicationEnvironmentName, failover);
    }

    private void assertDeploymentStatus(String deploymentId, DeploymentStatus expectedStatus, Set<DeploymentStatus> pendingStatus, long timeout)
            throws Throwable {
        checkStatus(null, deploymentId, expectedStatus, pendingStatus, timeout, null, false);
    }

    @SuppressWarnings("rawtypes")
    private void checkStatus(String applicationName, String deploymentId, DeploymentStatus expectedStatus, Set<DeploymentStatus> pendingStatus, long timeout,
            String applicationEnvironmentName, boolean failover) throws Throwable {
        String statusRequest = null;
        String applicationEnvironmentId = null;
        String applicationId = applicationName != null ? Context.getInstance().getApplicationId(applicationName) : null;
        if (deploymentId != null) {
            statusRequest = "/rest/v1/deployments/" + deploymentId + "/status";
        } else if (applicationId != null) {
            applicationEnvironmentId = applicationEnvironmentName != null
                    ? Context.getInstance().getApplicationEnvironmentId(applicationName, applicationEnvironmentName)
                    : Context.getInstance().getDefaultApplicationEnvironmentId(applicationName);
            statusRequest = "/rest/v1/applications/" + applicationId + "/environments/" + applicationEnvironmentId + "/status";
        } else {
            throw new ITException("Expected at least application ID OR deployment ID to check the status.");
        }
        long now = System.currentTimeMillis();
        while (true) {
            if (System.currentTimeMillis() - now > timeout) {
                if (failover) {
                    log.warn("Expected deployment to be [" + expectedStatus + "] but Test has timeouted");
                    return;
                } else {
                    throw new ITException("Expected deployment to be [" + expectedStatus + "] but Test has timeouted");
                }
            }
            // get the current status
            String restResponseText = Context.getRestClientInstance().get(statusRequest);
            RestResponse<String> statusResponse = JsonUtil.read(restResponseText, String.class);
            assertNull(statusResponse.getError());
            DeploymentStatus deploymentStatus = DeploymentStatus.valueOf(statusResponse.getData());

            if (deploymentStatus.equals(expectedStatus)) {
                if (applicationId != null) {
                    String restInfoResponseText = Context.getRestClientInstance()
                            .get("/rest/v1/applications/" + applicationId + "/environments/" + applicationEnvironmentId + "/deployment/informations");
                    RestResponse<?> infoResponse = JsonUtil.read(restInfoResponseText);
                    assertNull(infoResponse.getError());
                }
                return;
            } else if (pendingStatus.contains(deploymentStatus)) {
                Thread.sleep(1000L);
            } else {
                if (applicationId != null) {
                    if (failover) {
                        log.warn("Expected deployment of app [" + applicationId + "] to be [" + expectedStatus + "] but was [" + deploymentStatus + "]");
                        return;
                    } else {
                        throw new ITException(
                                "Expected deployment of app [" + applicationId + "] to be [" + expectedStatus + "] but was [" + deploymentStatus + "]");
                    }
                } else {
                    if (failover) {
                        log.warn("Expected deployment [" + deploymentId + "] to be [" + expectedStatus + "] but was [" + deploymentStatus + "]");
                        return;
                    } else {
                        throw new ITException("Expected deployment [" + deploymentId + "] to be [" + expectedStatus + "] but was [" + deploymentStatus + "]");
                    }
                }
            }
        }
    }

    private boolean checkNodeInstancesState(String key, Map<String, InstanceInformation> nodeInstancesInfos, String expectedState) {
        for (Entry<String, InstanceInformation> entry : nodeInstancesInfos.entrySet()) {
            if (!Objects.equals(expectedState, entry.getValue().getState())) {
                return false;
            }
        }
        return true;
    }

    @Then("^The application's deployment must succeed$")
    public void The_application_s_deployment_must_succeed() throws Throwable {
        // null value for environmentName => use default environment
        assertStatus(ApplicationStepDefinitions.CURRENT_APPLICATION.getName(), DeploymentStatus.DEPLOYED,
                Sets.newHashSet(DeploymentStatus.INIT_DEPLOYMENT, DeploymentStatus.DEPLOYMENT_IN_PROGRESS), 15000L, null);
    }

    @Then("^The application's deployment must fail$")
    public void The_application_s_deployment_must_fail() throws Throwable {
        assertStatus(ApplicationStepDefinitions.CURRENT_APPLICATION.getName(), DeploymentStatus.FAILURE,
                Sets.newHashSet(DeploymentStatus.INIT_DEPLOYMENT, DeploymentStatus.DEPLOYMENT_IN_PROGRESS), 10000L, null);
    }

    @Then("^The deployment must succeed$")
    public void The_deployment_must_succeed() throws Throwable {
        String deploymentId = Context.getInstance().getTopologyDeploymentId();
        assertDeploymentStatus(deploymentId, DeploymentStatus.DEPLOYED,
                Sets.newHashSet(DeploymentStatus.INIT_DEPLOYMENT, DeploymentStatus.DEPLOYMENT_IN_PROGRESS), 10000L);
    }

    @Then("^The deployment must fail$")
    public void The_deployment_must_fail() throws Throwable {
        String deploymentId = Context.getInstance().getTopologyDeploymentId();
        assertDeploymentStatus(deploymentId, DeploymentStatus.FAILURE,
                Sets.newHashSet(DeploymentStatus.INIT_DEPLOYMENT, DeploymentStatus.DEPLOYMENT_IN_PROGRESS), 10000L);
    }

    @Then("^The application's deployment must finish with warning$")
    public void The_application_s_deployment_must_finish_with_warning() throws Throwable {
        assertStatus(ApplicationStepDefinitions.CURRENT_APPLICATION.getName(), DeploymentStatus.WARNING,
                Sets.newHashSet(DeploymentStatus.INIT_DEPLOYMENT, DeploymentStatus.DEPLOYMENT_IN_PROGRESS), 10000L, null);
    }

    @When("^I can get applications statuses$")
    public void I_get_applications_statuses() throws Throwable {
        List<String> applicationIds = Lists.newArrayList();
        Iterator<String> appNames = ApplicationStepDefinitions.CURRENT_APPLICATIONS.keySet().iterator();
        while (appNames.hasNext()) {
            applicationIds.add(ApplicationStepDefinitions.CURRENT_APPLICATIONS.get(appNames.next()).getId());
        }

        Context.getInstance()
                .registerRestResponse(Context.getRestClientInstance().postJSon("/rest/v1/applications/statuses", JsonUtil.toString(applicationIds)));
        RestResponse<?> reponse = JsonUtil.read(Context.getInstance().getRestResponse());
        Map<String, Object> applicationStatuses = JsonUtil.toMap(JsonUtil.toString(reponse.getData()));
        assertEquals(ApplicationStepDefinitions.CURRENT_APPLICATIONS.size(), applicationStatuses.size());
    }

    @When("^I deploy all applications on the location \"([^\"]*)\"/\"([^\"]*)\"$")
    public void I_deploy_all_applications_on_the_location(String orchestratorName, String locationName) throws Throwable {
        assertNotNull(ApplicationStepDefinitions.CURRENT_APPLICATIONS);
        for (String key : ApplicationStepDefinitions.CURRENT_APPLICATIONS.keySet()) {
            Application app = ApplicationStepDefinitions.CURRENT_APPLICATIONS.get(key);
            Context.getInstance().registerApplication(app);
            deploymentTopoSteps.I_Set_a_unique_location_policy_to_for_all_nodes(orchestratorName, locationName);
            String appName = app.getName();
            Map<String, String> environments = Context.getInstance().getAllEnvironmentForApplication(appName);
            DeployApplicationRequest deployApplicationRequest = null;
            for (Map.Entry<String, String> env : environments.entrySet()) {
                String envName = env.getKey();
                deployApplicationRequest = getDeploymentAppRequest(appName, envName);
                String response = deploy(deployApplicationRequest);
                Context.getInstance().registerRestResponse(response);
            }
            commonSteps.I_should_receive_a_RestResponse_with_no_error();
        }
    }

    private String deploy(DeployApplicationRequest deployApplicationRequest) throws Throwable {
        setOrchestratorProperties();
        return Context.getRestClientInstance().postJSon("/rest/v1/applications/deployment", JsonUtil.toString(deployApplicationRequest));
    }

    @When("^I have expected applications statuses for \"([^\"]*)\" operation$")
    public void I_have_expected_applications_statuses(String operation, DataTable appsStatuses) throws Throwable {
        for (List<String> app : appsStatuses.raw()) {
            String name = app.get(0).trim();
            String expectedStatus = app.get(1).trim();
            assertStatus(name, DeploymentStatus.valueOf(expectedStatus), pendingStatuses.get(operation), 15000L, null);
        }
    }

    @Given("^I deploy the application \"([^\"]*)\" on the location \"([^\"]*)\"/\"([^\"]*)\"$")
    public void I_deploy_the_application_on_location(String appName, String orchestratorName, String locationName) throws Throwable {
        I_deploy_the_application_on_the_location_without_waiting_for_the_end_of_deployment(appName, orchestratorName, locationName);
        The_application_s_deployment_must_succeed();
    }

    @Given("^I pre register orchestrator properties$")
    public void I_pre_register_orchestrator_properties(Map<String, String> orchestratorProperties) throws Throwable {
        // // register deployment application properties to use it
        Context.getInstance().registerOrchestratorProperties(orchestratorProperties);
    }

    @Given("^I undeploy all environments for applications$")
    public void I_undeploy_all_applications() throws Throwable {
        assertNotNull(ApplicationStepDefinitions.CURRENT_APPLICATIONS);
        for (String applicationName : ApplicationStepDefinitions.CURRENT_APPLICATIONS.keySet()) {
            Application application = ApplicationStepDefinitions.CURRENT_APPLICATIONS.get(applicationName);
            log.info("APPLICATION : {} - {}", application.getName(), application.getId());
            // for each application undeploy all environment
            Map<String, String> environments = Context.getInstance().getAllEnvironmentForApplication(applicationName);
            for (Map.Entry<String, String> env : environments.entrySet()) {
                log.info(env.getKey() + "/" + env.getValue());
                log.info("ENVIRONMENT to undeploy : {} - {}", env.getKey(), env.getValue());
                Context.getRestClientInstance().delete("/rest/v1/applications/" + application.getId() + "/environments/" + env.getValue() + "/deployment");
                assertStatus(application.getName(), DeploymentStatus.UNDEPLOYED, Sets.newHashSet(DeploymentStatus.UNDEPLOYMENT_IN_PROGRESS), 10 * 60L * 1000L,
                        null);
            }
        }
    }

    @Then("^I should not get a deployment if I ask one for application \"([^\"]*)\" on orchestrator \"([^\"]*)\"$")
    public void I_should_not_get_a_deployment_if_I_ask_one_for_application(String applicationName, String orchestrator) throws Throwable {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestrator);
        assertNotNull(ApplicationStepDefinitions.CURRENT_APPLICATIONS);
        Application app = ApplicationStepDefinitions.CURRENT_APPLICATIONS.get(applicationName);
        NameValuePair nvp = new BasicNameValuePair("sourceId", app.getId());
        NameValuePair nvp1 = new BasicNameValuePair("orchestratorId", orchestratorId);
        String responseString = Context.getRestClientInstance().getUrlEncoded("/rest/v1/deployments", Lists.newArrayList(nvp, nvp1));
        RestResponse<?> response = JsonUtil.read(responseString);
        assertNull(response.getError());
        List<DeploymentDTO> deployments = JsonUtil.toList(JsonUtil.toString(response.getData()), DeploymentDTO.class, Application.class,
                Context.getJsonMapper());
        Assert.assertTrue(CollectionUtils.isEmpty(deployments));
    }

    @When("^I ask for detailed deployments for orchestrator \"([^\"]*)\"$")
    public void I_ask_for_detailed_deployments_for_orchestrator(String orchestratorName) throws Throwable {
        List<NameValuePair> nvps = Lists.newArrayList();
        NameValuePair nvp0 = new BasicNameValuePair("includeAppSummary", "true");
        nvps.add(nvp0);
        if (orchestratorName != null) {
            String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
            NameValuePair nvp1 = new BasicNameValuePair("orchestratorId", orchestratorId);
            nvps.add(nvp1);
        }
        String response = Context.getRestClientInstance().getUrlEncoded("/rest/v1/deployments", nvps);
        Context.getInstance().registerRestResponse(response);
    }

    @When("^I ask for the deployment topology of the application \"([^\"]*)\"$")
    public void I_ask_for_the_deployment_topology_of_the_application(String applicationName) throws Throwable {
        String appId = Context.getInstance().getApplicationId(applicationName);
        String environmentId = Context.getInstance().getApplicationEnvironmentId(applicationName, "Environment");
        String restUrl = String.format("/rest/v1/applications/%s/environments/%s/deployment-topology", appId, environmentId);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get(restUrl));
    }

    @When("^I ask for detailed deployments for all orchestrators$")
    public void I_ask_for_deployments_for_all_orchestrators() throws Throwable {
        I_ask_for_detailed_deployments_for_orchestrator(null);
    }

    @Then("^the response should contains (\\d+) deployments DTO and applications$")
    public void the_response_should_contains_deployments_DTO_and_applications(int deploymentsCount, List<String> applicationNames) throws Throwable {

        RestResponse<?> response = JsonUtil.read(Context.getInstance().getRestResponse());
        assertNotNull(response.getData());
        List<DeploymentDTO> deployments = JsonUtil.toList(JsonUtil.toString(response.getData()), DeploymentDTO.class, Application.class);
        assertNotNull(deployments);
        assertEquals(deploymentsCount, deployments.size());
        String[] expectedNames = null;
        for (String appName : applicationNames) {
            expectedNames = ArrayUtils.add(expectedNames, appName);
        }
        Arrays.sort(expectedNames);
        String[] actualNames = getApplicationNames(deployments);
        assertArrayEquals(expectedNames, actualNames);
    }

    @Then("^the response should contains (\\d+) deployments DTO and applications with an end date set$")
    public void the_response_should_contains_deployments_DTO_and_applications_with_an_en_date_set(int deploymentsCount, List<String> applicationNames)
            throws Throwable {

        RestResponse<?> response = JsonUtil.read(Context.getInstance().getRestResponse());
        assertNotNull(response.getData());
        List<DeploymentDTO> dtoList = JsonUtil.toList(JsonUtil.toString(response.getData()), DeploymentDTO.class, Application.class);
        assertNotNull(dtoList);
        assertEquals(deploymentsCount, dtoList.size());
        Set<String> expectedNames = Sets.newHashSet(applicationNames);
        Set<String> actualNames = getUndeployedApplicationNames(dtoList);
        assertEquals(expectedNames, actualNames);
    }

    private Set<String> getUndeployedApplicationNames(List<DeploymentDTO> list) {
        Set<String> names = Sets.newHashSet();
        for (DeploymentDTO dto : list) {
            if (dto.getDeployment().getEndDate() != null) {
                names.add(dto.getSource().getName());
            }
        }
        return names;
    }

    private String[] getApplicationNames(Collection<DeploymentDTO> list) {
        String[] names = null;
        for (DeploymentDTO dto : list) {
            names = ArrayUtils.add(names, dto.getSource().getName());
        }
        Arrays.sort(names);
        return names;
    }

    @When("^I undeploy the topology from its deployment id \"([^\"]*)\"$")
    public void I_undeploy_the_topology_from_its_deployment_id(String deploymentId) throws Throwable {

        if (deploymentId.equals("null")) {// take the registered deployment id
            deploymentId = Context.getInstance().getTopologyDeploymentId();
        }
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/deployments/" + deploymentId + "/undeploy"));
    }

    private StompConnection stompConnection = null;

    private Map<String, IStompDataFuture> stompDataFutures = Maps.newHashMap();

    private String getActiveDeploymentId(String applicationName) throws IOException {
        Deployment deployment = JsonUtil
                .read(Context.getRestClientInstance()
                        .get("/rest/v1/applications/" + Context.getInstance().getApplicationId(applicationName) + "/environments/"
                                + Context.getInstance().getDefaultApplicationEnvironmentId(applicationName) + "/active-deployment"),
                        Deployment.class)
                .getData();
        return deployment.getId();
    }

    @Given("^I start listening to \"([^\"]*)\" event$")
    public void I_start_listening_to_event(String eventTopic) throws Throwable {
        Map<String, String> headers = Maps.newHashMap();
        Header cookieHeader = Context.getRestClientInstance().getCookieHeader();
        headers.put(cookieHeader.getName(), cookieHeader.getValue());
        String topic = null;
        if (stompConnection == null) {
            stompConnection = new StompConnection(Context.HOST, Context.PORT, headers, Context.CONTEXT_PATH + Context.WEB_SOCKET_END_POINT);
        }
        switch (eventTopic) {
        case "deployment-status":
            topic = "/topic/deployment-events/" + getActiveDeploymentId(Context.getInstance().getApplication().getName()) + "/"
                    + PaaSDeploymentStatusMonitorEvent.class.getSimpleName().toLowerCase();
            this.stompDataFutures.put(eventTopic, stompConnection.getData(topic, PaaSDeploymentStatusMonitorEvent.class));
            break;
        case "instance-state":
            topic = "/topic/deployment-events/" + getActiveDeploymentId(Context.getInstance().getApplication().getName()) + "/"
                    + PaaSInstanceStateMonitorEvent.class.getSimpleName().toLowerCase();
            this.stompDataFutures.put(eventTopic, stompConnection.getData(topic, PaaSInstanceStateMonitorEvent.class));
            break;
        case "storage":
            topic = "/topic/deployment-events/" + getActiveDeploymentId(Context.getInstance().getApplication().getName()) + "/"
                    + PaaSInstancePersistentResourceMonitorEvent.class.getSimpleName().toLowerCase();
            this.stompDataFutures.put(eventTopic, stompConnection.getData(topic, PaaSInstancePersistentResourceMonitorEvent.class));
            break;
        }
    }

    private static final long WAIT_TIME = 30;

    @And("^I should receive \"([^\"]*)\" events that contains$")
    public void I_should_receive_events_that_containing(String eventTopic, List<String> expectedEvents) throws Throwable {
        Assert.assertTrue(this.stompDataFutures.containsKey(eventTopic));
        List<String> actualEvents = Lists.newArrayList();
        try {
            switch (eventTopic) {
            case "deployment-status":
                StompData<PaaSDeploymentStatusMonitorEvent>[] deploymentStatusEvents = this.stompDataFutures.get(eventTopic).getData(expectedEvents.size(),
                        WAIT_TIME, TimeUnit.SECONDS);
                for (StompData<PaaSDeploymentStatusMonitorEvent> data : deploymentStatusEvents) {
                    actualEvents.add(data.getData().getDeploymentStatus().toString());
                }
                break;
            case "instance-state":
                StompData<PaaSInstanceStateMonitorEvent>[] instanceStateEvents = this.stompDataFutures.get(eventTopic).getData(expectedEvents.size(), WAIT_TIME,
                        TimeUnit.SECONDS);
                for (StompData<PaaSInstanceStateMonitorEvent> data : instanceStateEvents) {
                    actualEvents.add(data.getData().getInstanceState());
                }
                break;
            case "storage":
                StompData<PaaSInstancePersistentResourceMonitorEvent>[] storageEvents = this.stompDataFutures.get(eventTopic).getData(expectedEvents.size(),
                        WAIT_TIME, TimeUnit.SECONDS);
                for (StompData<PaaSInstancePersistentResourceMonitorEvent> data : storageEvents) {
                    // FIXME actualEvents.add(data.getData().getInstanceState());
                }
                break;
            }
            Assert.assertEquals(expectedEvents.size(), actualEvents.size());
            Assert.assertArrayEquals(expectedEvents.toArray(), actualEvents.toArray());
        } finally {
            this.stompDataFutures.remove(eventTopic);
            if (this.stompDataFutures.isEmpty()) {
                this.stompConnection.close();
                this.stompConnection = null;
            }
        }
    }

    @Given("^I deploy the application \"([^\"]*)\" on the location \"([^\"]*)\"/\"([^\"]*)\" without waiting for the end of deployment$")
    public void I_deploy_the_application_on_the_location_without_waiting_for_the_end_of_deployment(String appName, String orchestratorName, String locationName)
            throws Throwable {
        deploymentTopoSteps.I_Set_a_unique_location_policy_to_for_all_nodes(orchestratorName, locationName);
        DeployApplicationRequest deployApplicationRequest = getDeploymentAppRequest(appName, null);
        deployApplicationRequest.setApplicationId(Context.getInstance().getApplication().getId());
        String response = deploy(deployApplicationRequest);
        Context.getInstance().registerRestResponse(response);
    }

    @And("^The deployment setup of the application should contain following deployment properties:$")
    public void The_deployment_setup_of_the_application_should_contain_following_deployment_properties(DataTable deploymentProperties) throws Throwable {
        Map<String, String> expectedDeploymentProperties = Maps.newHashMap();
        for (List<String> deploymentProperty : deploymentProperties.raw()) {
            String deploymentPropertyName = deploymentProperty.get(0).trim();
            String deploymentPropertyValue = deploymentProperty.get(1).trim();
            expectedDeploymentProperties.put(deploymentPropertyName, deploymentPropertyValue);
        }
        Assert.fail("Fix test");
        // DeploymentSetupMatchInfo deploymentSetupMatchInfo = JsonUtil.read(
        // Context.getRestClientInstance().get(
        // "/rest/v1/applications/" + ApplicationStepDefinitions.CURRENT_APPLICATION.getId() + "/environments/"
        // + Context.getInstance().getDefaultApplicationEnvironmentId(ApplicationStepDefinitions.CURRENT_APPLICATION.getName())
        // + "/deployment-setup"), DeploymentSetupMatchInfo.class).getData();
        // Assert.assertNotNull(deploymentSetupMatchInfo.getProviderDeploymentProperties());
        // Assert.assertEquals(expectedDeploymentProperties, deploymentSetupMatchInfo.getProviderDeploymentProperties());
    }

    @Given("^I deploy an application environment \"([^\"]*)\" for application \"([^\"]*)\"$")
    public void I_deploy_an_application_environment_for_application(String envName, String appName) throws Throwable {
        DeployApplicationRequest deployApplicationRequest = getDeploymentAppRequest(appName, envName);
        deployApplicationRequest.setApplicationId(Context.getInstance().getApplicationId(appName));
        String response = deploy(deployApplicationRequest);
        Context.getInstance().registerRestResponse(response);
    }

    @When("^I have the environment \"([^\"]*)\" with status \"([^\"]*)\" for the application \"([^\"]*)\"$")
    public void I_have_the_environment_with_status_for_the_application(String envName, String expectedStatus, String appName) throws Throwable {
        assertStatus(appName, DeploymentStatus.valueOf(expectedStatus),
                Sets.newHashSet(DeploymentStatus.INIT_DEPLOYMENT, DeploymentStatus.DEPLOYMENT_IN_PROGRESS), 10000L, envName);
    }

    @And("^The application's deployment must succeed after (\\d+) minutes$")
    public void The_application_s_deployment_must_succeed_after_minutes(long numberOfMinutes) throws Throwable {
        // null value for environmentName => use default environment
        assertStatus(ApplicationStepDefinitions.CURRENT_APPLICATION.getName(), DeploymentStatus.DEPLOYED,
                Sets.newHashSet(DeploymentStatus.INIT_DEPLOYMENT, DeploymentStatus.DEPLOYMENT_IN_PROGRESS), numberOfMinutes * 60L * 1000L, null, false);
    }

    @And("^The application's deployment should succeed after (\\d+) minutes$")
    public void The_application_s_deployment_should_succeed_after_minutes(long numberOfMinutes) throws Throwable {
        // null value for environmentName => use default environment
        assertStatus(ApplicationStepDefinitions.CURRENT_APPLICATION.getName(), DeploymentStatus.DEPLOYED,
                Sets.newHashSet(DeploymentStatus.INIT_DEPLOYMENT, DeploymentStatus.DEPLOYMENT_IN_PROGRESS), numberOfMinutes * 60L * 1000L, null, true);
    }

    @Then("^all nodes instances must be in \"([^\"]*)\" state after (\\d+) minutes$")
    public void all_nodes_instances_must_be_in_state_after_minutes(String expectedState, long numberOfMinutes) throws Throwable {
        long timeout = numberOfMinutes * 60L * 1000L;
        String applicationId = Context.getInstance().getApplicationId(ApplicationStepDefinitions.CURRENT_APPLICATION.getName());
        String applicationEnvironmentId = Context.getInstance().getDefaultApplicationEnvironmentId(ApplicationStepDefinitions.CURRENT_APPLICATION.getName());
        long now = System.currentTimeMillis();
        while (true) {
            if (System.currentTimeMillis() - now > timeout) {
                throw new ITException("Expected All instances to be in state [" + expectedState + "] but Test has timeouted");
            }

            String restInfoResponseText = Context.getRestClientInstance()
                    .get("/rest/v1/applications/" + applicationId + "/environments/" + applicationEnvironmentId + "/deployment/informations");
            RestResponse<?> infoResponse = JsonUtil.read(restInfoResponseText);
            assertNull(infoResponse.getError());
            Assert.assertNotNull(infoResponse.getData());
            Map<String, Object> instancesInformation = (Map<String, Object>) infoResponse.getData();
            boolean ok = true;
            for (Entry<String, Object> entry : instancesInformation.entrySet()) {
                Map<String, InstanceInformation> nodeInstancesInfos = JsonUtil.toMap(JsonUtil.toString(entry.getValue()), String.class,
                        InstanceInformation.class, Context.getJsonMapper());
                if (!checkNodeInstancesState(entry.getKey(), nodeInstancesInfos, expectedState)) {
                    Thread.sleep(1000L);
                    ok = false;
                    break;
                }
            }

            if (ok) {
                return;
            }
        }
    }

    @And("^I re-deploy the application$")
    public void I_re_deploy_the_application() throws Throwable {
        log.info("Re-deploy : Un-deploying application " + ApplicationStepDefinitions.CURRENT_APPLICATION.getName());
        I_undeploy_it();
        log.info("Re-deploy : Finished undeployment the application " + ApplicationStepDefinitions.CURRENT_APPLICATION.getName());
        // TODO For asynchronous problem of cloudify
        Thread.sleep(60L * 1000L);
        log.info("Re-deploy : Deploying the application " + ApplicationStepDefinitions.CURRENT_APPLICATION.getName());
        I_deploy_it();
        // Sleep some seconds to be sure that the application status has changed
        Thread.sleep(60000L);
        log.info("Re-deploy : Finished deployment of the application " + ApplicationStepDefinitions.CURRENT_APPLICATION.getName());
    }

    @And("^The node \"([^\"]*)\" should contain (\\d+) instance\\(s\\) not started$")
    public void The_node_should_contain_instances_not_started(String nodeName, int expectedNumberOfInstancesNotStarted) throws Throwable {
        RestResponse<?> response = JsonUtil.read(Context.getRestClientInstance()
                .get("/rest/v1/applications/" + ApplicationStepDefinitions.CURRENT_APPLICATION.getId() + "/environments/"
                        + Context.getInstance().getDefaultApplicationEnvironmentId(ApplicationStepDefinitions.CURRENT_APPLICATION.getName())
                        + "/deployment/informations"));
        Assert.assertNotNull(response.getData());
        Map<String, Object> instancesInformation = (Map<String, Object>) response.getData();
        Assert.assertNotNull(instancesInformation.get(nodeName));
        Map<String, Object> nodeInformation = (Map<String, Object>) instancesInformation.get(nodeName);
        int countNotStarted = 0;
        for (Map.Entry<String, Object> instanceInformationEntry : nodeInformation.entrySet()) {
            Map<String, Object> instanceInformation = (Map<String, Object>) instanceInformationEntry.getValue();
            if (!Objects.equals(InstanceStatus.SUCCESS, instanceInformation.get("instanceStatus"))) {
                countNotStarted++;
            }
        }
        assertEquals("should have " + expectedNumberOfInstancesNotStarted + " instances not started, but got theses instances: "
                + JsonUtil.toString(nodeInformation), expectedNumberOfInstancesNotStarted, countNotStarted);
    }

    @And("^The node \"([^\"]*)\" should contain (\\d+) instance\\(s\\)$")
    public void The_node_should_contain_instance_s(String nodeName, int numberOfInstances) throws Throwable {
        RestResponse<?> response = JsonUtil.read(Context.getRestClientInstance()
                .get("/rest/v1/applications/" + ApplicationStepDefinitions.CURRENT_APPLICATION.getId() + "/environments/"
                        + Context.getInstance().getDefaultApplicationEnvironmentId(ApplicationStepDefinitions.CURRENT_APPLICATION.getName())
                        + "/deployment/informations"));
        Assert.assertNotNull(response.getData());
        Map<String, Object> instancesInformation = (Map<String, Object>) response.getData();
        Assert.assertNotNull(instancesInformation.get(nodeName));
        Map<String, Object> nodeInformation = (Map<String, Object>) instancesInformation.get(nodeName);
        Assert.assertEquals(numberOfInstances, nodeInformation.size());
        for (Map.Entry<String, Object> instanceInformationEntry : nodeInformation.entrySet()) {
            Map<String, Object> instanceInformation = (Map<String, Object>) instanceInformationEntry.getValue();
            Assert.assertEquals(InstanceStatus.SUCCESS.toString(), instanceInformation.get("instanceStatus"));
        }
    }

    @And("^The node \"([^\"]*)\" should contain (\\d+) instance\\(s\\) after at maximum (\\d+) minutes$")
    public void The_node_should_contain_instance_s_after_at_maximum_minutes(String nodeName, int numberOfInstances, int waitTimeInMinutes) throws Throwable {
        long waitTimeInMillis = waitTimeInMinutes * 60L * 1000L;
        long before = System.currentTimeMillis();
        while (true) {
            try {
                The_node_should_contain_instance_s(nodeName, numberOfInstances);
                log.info("The node " + nodeName + " contains " + numberOfInstances + " instances after " + (System.currentTimeMillis() - before)
                        + " milliseconds");
                break;
            } catch (AssertionError e) {
                long currentDuration = System.currentTimeMillis() - before;
                if (currentDuration > waitTimeInMillis) {
                    throw e;
                }
            }
        }
    }
}
