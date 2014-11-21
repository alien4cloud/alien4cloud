package alien4cloud.it.application;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.collect.Sets;
import org.junit.Assert;

import alien4cloud.it.Context;
import alien4cloud.it.common.CommonStepDefinitions;
import alien4cloud.it.exception.ITException;
import alien4cloud.it.topology.TopologyStepDefinitions;
import alien4cloud.it.utils.websocket.IStompDataFuture;
import alien4cloud.it.utils.websocket.StompConnection;
import alien4cloud.it.utils.websocket.StompData;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.DeploymentSetup;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.paas.model.PaaSDeploymentStatusMonitorEvent;
import alien4cloud.paas.model.PaaSInstanceStateMonitorEvent;
import alien4cloud.paas.model.PaaSInstanceStorageMonitorEvent;
import alien4cloud.rest.application.DeployApplicationRequest;
import alien4cloud.rest.application.UpdateDeploymentSetupRequest;
import alien4cloud.rest.deployment.DeploymentDTO;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.plugin.CloudDeploymentPropertyValidationRequest;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.tosca.container.model.template.PropertyValue;
import cucumber.api.DataTable;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

@Slf4j
public class ApplicationsDeploymentStepDefinitions {
    private Map<String, DeploymentStatus> applicationStatuses = Maps.newHashMap();
    private TopologyStepDefinitions topologyStepDefinitions = new TopologyStepDefinitions();
    private CommonStepDefinitions commonSteps = new CommonStepDefinitions();
    private static Map<String, DeploymentStatus> pendingStatuses;

    static {
        pendingStatuses = Maps.newHashMap();
        pendingStatuses.put("deployment", DeploymentStatus.DEPLOYMENT_IN_PROGRESS);
        pendingStatuses.put("undeployment", DeploymentStatus.UNDEPLOYMENT_IN_PROGRESS);
    }

    @When("^I deploy it$")
    public void I_deploy_it() throws Throwable {
        DeployApplicationRequest deployApplicationRequest = getDeploymentAppRequest(null);
        Context.getRestClientInstance().postJSon("/rest/applications/deployment", JsonUtil.toString(deployApplicationRequest));
    }

    private DeployApplicationRequest getDeploymentAppRequest(String applicationId) throws IOException {
        DeployApplicationRequest deployApplicationRequest = new DeployApplicationRequest();
        deployApplicationRequest.setApplicationId(applicationId == null ? ApplicationStepDefinitions.CURRENT_APPLICATION.getId() : applicationId);

        Map<String, String> deploymentProperties = Context.getInstance().getDeployApplicationProperties();
        if (deploymentProperties != null) {
            UpdateDeploymentSetupRequest request = new UpdateDeploymentSetupRequest();
            request.setProviderDeploymentProperties(deploymentProperties);
            String response = Context.getRestClientInstance().putJSon(
                    "/rest/applications/" + deployApplicationRequest.getApplicationId() + "/deployment-setup", JsonUtil.toString(request));
            RestResponse<?> marshaledResponse = JsonUtil.read(response);
            Assert.assertNull(marshaledResponse.getError());
        }
        return deployApplicationRequest;
    }

    private void assertStatus(String applicationId, DeploymentStatus expectedStatus, DeploymentStatus pendingStatus, long timeout) throws Throwable {
        checkStatus(applicationId, null, expectedStatus, pendingStatus, timeout);
    }

    private void assertDeploymentStatus(String deploymentId, DeploymentStatus expectedStatus, DeploymentStatus pendingStatus, long timeout) throws Throwable {
        checkStatus(null, deploymentId, expectedStatus, pendingStatus, timeout);
    }

    private void checkStatus(String applicationId, String deploymentId, DeploymentStatus expectedStatus, DeploymentStatus pendingStatus, long timeout)
            throws IOException, InterruptedException {
        String statusRequest = null;
        if (deploymentId != null) {
            statusRequest = "/rest/deployments/" + deploymentId + "/status";
        } else if (applicationId != null) {
            statusRequest = "/rest/applications/" + applicationId + "/deployment";
        } else {
            throw new ITException("Expected at least application ID OR deployment ID to check the status.");
        }
        long now = System.currentTimeMillis();
        while (true) {
            if (System.currentTimeMillis() - now > timeout) {
                throw new ITException("Expected deployment to be [" + expectedStatus + "] but Test has timeouted");
            }
            // get the current status
            String restResponseText = Context.getRestClientInstance().get(statusRequest);
            RestResponse<String> statusResponse = JsonUtil.read(restResponseText, String.class);
            assertNull(statusResponse.getError());

            DeploymentStatus deploymentStatus = DeploymentStatus.valueOf(statusResponse.getData());
            if (deploymentStatus.equals(expectedStatus)) {
                if (applicationId != null) {
                    String restInfoResponseText = Context.getRestClientInstance().get("/rest/applications/" + applicationId + "/deployment/informations");
                    RestResponse<?> infoResponse = JsonUtil.read(restInfoResponseText);
                    assertNull(infoResponse.getError());
                }
                return;
            } else if (deploymentStatus.equals(pendingStatus)) {
                Thread.sleep(1000L);
            } else {
                if (applicationId != null) {
                    throw new ITException("Expected deployment of [" + applicationId + "] to be [" + expectedStatus + "] but was [" + deploymentStatus + "]");
                } else {
                    throw new ITException("Expected deployment of [" + deploymentId + "] to be [" + expectedStatus + "] but was [" + deploymentStatus + "]");
                }
            }
        }
    }

    @Then("^The application's deployment must succeed$")
    public void The_application_s_deployment_must_succeed() throws Throwable {
        assertStatus(ApplicationStepDefinitions.CURRENT_APPLICATION.getId(), DeploymentStatus.DEPLOYED, DeploymentStatus.DEPLOYMENT_IN_PROGRESS, 15000L);
    }

    @Then("^The application's deployment must fail$")
    public void The_application_s_deployment_must_fail() throws Throwable {
        assertStatus(ApplicationStepDefinitions.CURRENT_APPLICATION.getId(), DeploymentStatus.FAILURE, DeploymentStatus.DEPLOYMENT_IN_PROGRESS, 10000L);
    }

    @Then("^The deployment must succeed$")
    public void The_deployment_must_succeed() throws Throwable {
        String deploymentId = Context.getInstance().getTopologyDeploymentId();
        assertDeploymentStatus(deploymentId, DeploymentStatus.DEPLOYED, DeploymentStatus.DEPLOYMENT_IN_PROGRESS, 10000L);
    }

    @Then("^The deployment must fail$")
    public void The_deployment_must_fail() throws Throwable {
        String deploymentId = Context.getInstance().getTopologyDeploymentId();
        assertDeploymentStatus(deploymentId, DeploymentStatus.FAILURE, DeploymentStatus.DEPLOYMENT_IN_PROGRESS, 10000L);
    }

    @Then("^The application's deployment must finish with warning$")
    public void The_application_s_deployment_must_finish_with_warning() throws Throwable {
        assertStatus(ApplicationStepDefinitions.CURRENT_APPLICATION.getId(), DeploymentStatus.WARNING, DeploymentStatus.DEPLOYMENT_IN_PROGRESS, 10000L);
    }

    @When("^I can get applications statuses$")
    public void I_get_applications_statuses() throws Throwable {

        List<String> applicationIds = Lists.newArrayList();
        Iterator<String> appNames = ApplicationStepDefinitions.CURRENT_APPLICATIONS.keySet().iterator();
        while (appNames.hasNext()) {
            applicationIds.add(ApplicationStepDefinitions.CURRENT_APPLICATIONS.get(appNames.next()).getId());
        }

        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/applications/statuses", JsonUtil.toString(applicationIds)));
        RestResponse<?> reponse = JsonUtil.read(Context.getInstance().getRestResponse());
        applicationStatuses = JsonUtil.toMap(JsonUtil.toString(reponse.getData()), String.class, DeploymentStatus.class);

        assertEquals(ApplicationStepDefinitions.CURRENT_APPLICATIONS.size(), applicationStatuses.size());
    }

    @When("^I assign the cloud with name \"([^\"]*)\" for the application$")
    public void I_assign_the_cloud_with_name_for_the_application(String cloudName) throws Throwable {
        String cloudId = Context.getInstance().getCloudId(cloudName);

        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().putJSon("/rest/applications/" + Context.getInstance().getApplication().getId() + "/cloud/", cloudId));

        // Register paas provider details
        Context.getInstance().registerCloudForTopology(cloudId);
    }

    @When("^I deploy all applications with cloud \"([^\"]*)\"$")
    public void I_deploy_all_applications_with_cloud(String cloudName) throws Throwable {
        assertNotNull(ApplicationStepDefinitions.CURRENT_APPLICATIONS);
        for (String key : ApplicationStepDefinitions.CURRENT_APPLICATIONS.keySet()) {
            Application app = ApplicationStepDefinitions.CURRENT_APPLICATIONS.get(key);

            Context.getInstance().registerApplication(app);
            I_assign_the_cloud_with_name_for_the_application(cloudName);
            DeployApplicationRequest deployApplicationRequest = new DeployApplicationRequest();
            deployApplicationRequest.setApplicationId(app.getId());

            Context.getInstance().registerRestResponse(
                    Context.getRestClientInstance().postJSon("/rest/applications/deployment", JsonUtil.toString(deployApplicationRequest)));
            commonSteps.I_should_receive_a_RestResponse_with_no_error();
        }
    }

    @When("^I have expected applications statuses for \"([^\"]*)\" operation$")
    public void I_have_expected_applications_statuses(String operation, DataTable appsStatuses) throws Throwable {

        for (List<String> app : appsStatuses.raw()) {
            String name = app.get(0).trim();
            String expectedStatus = app.get(1).trim();
            assertStatus(ApplicationStepDefinitions.CURRENT_APPLICATIONS.get(name).getId(), DeploymentStatus.valueOf(expectedStatus),
                    pendingStatuses.get(operation), 15000L);
        }
    }

    @Given("^I deploy the application \"([^\"]*)\" with cloud \"([^\"]*)\" for the topology$")
    public void I_deploy_the_application_with_cloud_for_the_topology(String appName, String cloudName) throws Throwable {
        I_deploy_the_application_with_cloud_for_the_topology_without_waiting_for_the_end_of_deployment(appName, cloudName);
        The_application_s_deployment_must_succeed();
    }

    @Given("^I give deployment properties$")
    public void I_give_deployment_properties(DataTable deploymentProperties) throws Throwable {

        String deploymentPropertyName = null;
        String deploymentPropertyValue = null;

        CloudDeploymentPropertyValidationRequest checkDeploymentPropertyRequest = new CloudDeploymentPropertyValidationRequest();
        checkDeploymentPropertyRequest.setCloudId(Context.getInstance().getCloudForTopology());

        // check properties validity
        Map<String, String> finalDeploymentProperties = Maps.newHashMap();
        for (List<String> app : deploymentProperties.raw()) {
            deploymentPropertyName = app.get(0).trim();
            deploymentPropertyValue = app.get(1).trim();
            checkDeploymentPropertyRequest.setDeploymentPropertyName(deploymentPropertyName);
            checkDeploymentPropertyRequest.setDeploymentPropertyValue(deploymentPropertyValue);
            Context.getInstance().registerRestResponse(
                    Context.getRestClientInstance().postJSon("/rest/applications/checkDeploymentProperty", JsonUtil.toString(checkDeploymentPropertyRequest)));
            finalDeploymentProperties.put(deploymentPropertyName, deploymentPropertyValue);
        }
        // register deployment application properties to use it
        Context.getInstance().registerDeployApplicationProperties(finalDeploymentProperties);
    }

    @Given("^I undeploy all applications$")
    public void I_undeploy_all_applications() throws Throwable {
        assertNotNull(ApplicationStepDefinitions.CURRENT_APPLICATIONS);

        for (String key : ApplicationStepDefinitions.CURRENT_APPLICATIONS.keySet()) {
            Application application = ApplicationStepDefinitions.CURRENT_APPLICATIONS.get(key);

            Context.getRestClientInstance().delete("/rest/applications/" + application.getId() + "/deployment");
        }
    }

    @Then("^I should not get a deployment if I ask one for application \"([^\"]*)\" on cloud \"([^\"]*)\"$")
    public void I_should_not_get_a_deployment_if_I_ask_one_for_application(String applicationName, String cloudName) throws Throwable {
        String cloudId = Context.getInstance().getCloudId(cloudName);
        assertNotNull(ApplicationStepDefinitions.CURRENT_APPLICATIONS);
        Application app = ApplicationStepDefinitions.CURRENT_APPLICATIONS.get(applicationName);
        NameValuePair nvp = new BasicNameValuePair("applicationId", app.getId());
        NameValuePair nvp1 = new BasicNameValuePair("cloudId", cloudId);
        String responseStr = Context.getRestClientInstance().getUrlEncoded("/rest/deployments", Lists.newArrayList(nvp, nvp1));
        RestResponse<?> response = JsonUtil.read(Context.getInstance().getRestResponse());
        assertNull(response.getError());
        assertNull(response.getData());
    }

    @When("^I ask for detailed deployments for cloud \"([^\"]*)\"$")
    public void I_ask_for_detailed_deployments_for_cloud(String cloudName) throws Throwable {
        List<NameValuePair> nvps = Lists.newArrayList();
        NameValuePair nvp0 = new BasicNameValuePair("includeAppSummary", "true");
        nvps.add(nvp0);
        if (cloudName != null) {
            String cloudId = Context.getInstance().getCloudId(cloudName);
            NameValuePair nvp1 = new BasicNameValuePair("cloudId", cloudId);
            nvps.add(nvp1);
        }

        Context.getInstance().registerRestResponse(Context.getRestClientInstance().getUrlEncoded("/rest/deployments", nvps));
    }

    @When("^I ask for detailed deployments for all cloud$")
    public void I_ask_for_deployments_for_all_cloud() throws Throwable {
        I_ask_for_detailed_deployments_for_cloud(null);
    }

    @Then("^the response should contains (\\d+) deployments DTO and applications$")
    public void the_response_should_contains_deployments_DTO_and_applications(int deploymentsCount, DataTable applicationNames) throws Throwable {

        RestResponse<?> response = JsonUtil.read(Context.getInstance().getRestResponse());
        assertNotNull(response.getData());
        List<DeploymentDTO> dtoList = JsonUtil.toList(JsonUtil.toString(response.getData()), DeploymentDTO.class, Application.class);
        assertNotNull(dtoList);
        assertEquals(deploymentsCount, dtoList.size());
        String[] expectedNames = null;
        for (List<String> appName : applicationNames.raw()) {
            expectedNames = ArrayUtils.add(expectedNames, appName.get(0));
        }
        Arrays.sort(expectedNames);
        String[] actualNames = getApplicationNames(dtoList);
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
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/deployments/" + deploymentId + "/undeploy"));
    }

    private StompConnection stompConnection = null;

    private Map<String, IStompDataFuture> stompDataFutures = Maps.newHashMap();

    private String getActiveDeploymentId(String applicationId) throws IOException {
        Deployment deployment = JsonUtil.read(Context.getRestClientInstance().get("/rest/applications/" + applicationId + "/active-deployment"),
                Deployment.class).getData();
        return deployment.getId();
    }

    @Given("^I start listening to \"([^\"]*)\" event$")
    public void I_start_listening_to_event(String eventTopic) throws Throwable {
        Map<String, String> headers = Maps.newHashMap();
        Header cookieHeader = Context.getRestClientInstance().getCookieHeader();
        headers.put(cookieHeader.getName(), cookieHeader.getValue());
        String topic = null;
        switch (eventTopic) {
        case "deployment-status":
            topic = "/topic/deployment-events/" + getActiveDeploymentId(Context.getInstance().getApplication().getId()) + "/"
                    + PaaSDeploymentStatusMonitorEvent.class.getSimpleName().toLowerCase();
            stompConnection = new StompConnection(Context.HOST, Context.PORT, headers, Context.CONTEXT_PATH + Context.WEB_SOCKET_END_POINT);
            this.stompDataFutures.put(eventTopic, stompConnection.getData(topic, PaaSDeploymentStatusMonitorEvent.class));
            break;
        case "instance-state":
            topic = "/topic/deployment-events/" + getActiveDeploymentId(Context.getInstance().getApplication().getId()) + "/"
                    + PaaSInstanceStateMonitorEvent.class.getSimpleName().toLowerCase();
            stompConnection = new StompConnection(Context.HOST, Context.PORT, headers, Context.CONTEXT_PATH + Context.WEB_SOCKET_END_POINT);
            this.stompDataFutures.put(eventTopic, stompConnection.getData(topic, PaaSInstanceStateMonitorEvent.class));
            break;
        case "storage":
            topic = "/topic/deployment-events/" + getActiveDeploymentId(Context.getInstance().getApplication().getId()) + "/"
                    + PaaSInstanceStorageMonitorEvent.class.getSimpleName().toLowerCase();
            stompConnection = new StompConnection(Context.HOST, Context.PORT, headers, Context.CONTEXT_PATH + Context.WEB_SOCKET_END_POINT);
            this.stompDataFutures.put(eventTopic, stompConnection.getData(topic, PaaSInstanceStorageMonitorEvent.class));
            break;
        }
    }

    @And("^I should receive \"([^\"]*)\" events that containing$")
    public void I_should_receive_events_that_containing(String eventTopic, List<String> expectedEvents) throws Throwable {
        Assert.assertTrue(this.stompDataFutures.containsKey(eventTopic));
        List<String> actualEvents = Lists.newArrayList();
        try {
            switch (eventTopic) {
            case "deployment-status":
                StompData<PaaSDeploymentStatusMonitorEvent>[] deploymentStatusEvents = this.stompDataFutures.get(eventTopic).getData(expectedEvents.size(), 10,
                        TimeUnit.SECONDS);
                for (StompData<PaaSDeploymentStatusMonitorEvent> data : deploymentStatusEvents) {
                    actualEvents.add(data.getData().getDeploymentStatus().toString());
                }
                break;
            case "instance-state":
                StompData<PaaSInstanceStateMonitorEvent>[] instanceStateEvents = this.stompDataFutures.get(eventTopic).getData(expectedEvents.size(), 10,
                        TimeUnit.SECONDS);
                for (StompData<PaaSInstanceStateMonitorEvent> data : instanceStateEvents) {
                    actualEvents.add(data.getData().getInstanceState());
                }
                break;
            case "storage":
                StompData<PaaSInstanceStorageMonitorEvent>[] storageEvents = this.stompDataFutures.get(eventTopic).getData(expectedEvents.size(), 10,
                        TimeUnit.SECONDS);
                for (StompData<PaaSInstanceStorageMonitorEvent> data : storageEvents) {
                    actualEvents.add(data.getData().getInstanceState());
                }
                break;
            }
            Assert.assertEquals(expectedEvents.size(), actualEvents.size());
            Assert.assertArrayEquals(expectedEvents.toArray(), actualEvents.toArray());
        } finally {
            this.stompConnection.close();
            this.stompDataFutures.remove(eventTopic);
        }
    }

    @Given("^I deploy the application \"([^\"]*)\" with cloud \"([^\"]*)\" for the topology without waiting for the end of deployment$")
    public void I_deploy_the_application_with_cloud_for_the_topology_without_waiting_for_the_end_of_deployment(String appName, String cloudName)
            throws Throwable {
        I_assign_the_cloud_with_name_for_the_application(cloudName);
        DeployApplicationRequest deployApplicationRequest = getDeploymentAppRequest(null);
        deployApplicationRequest.setApplicationId(Context.getInstance().getApplication().getId());
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postJSon("/rest/applications/deployment", JsonUtil.toString(deployApplicationRequest)));
    }

    @And("^I deploy the application with following properties:$")
    public void I_deploy_the_application_with_following_properties(DataTable deploymentProperties) throws Throwable {
        I_give_deployment_properties(deploymentProperties);
        I_deploy_it();
    }

    @And("^The deployment setup of the application should contain following deployment properties:$")
    public void The_deployment_setup_of_the_application_should_contain_following_deployment_properties(DataTable deploymentProperties) throws Throwable {
        Map<String, PropertyValue> expectedDeploymentProperties = Maps.newHashMap();
        for (List<String> deploymentProperty : deploymentProperties.raw()) {
            String deploymentPropertyName = deploymentProperty.get(0).trim();
            String deploymentPropertyValue = deploymentProperty.get(1).trim();
            expectedDeploymentProperties.put(deploymentPropertyName, new PropertyValue(deploymentPropertyValue));
        }
        DeploymentSetup deploymentSetup = JsonUtil.read(
                Context.getRestClientInstance().get("/rest/applications/" + ApplicationStepDefinitions.CURRENT_APPLICATION.getId() + "/deployment-setup"),
                DeploymentSetup.class).getData();
        Assert.assertNotNull(deploymentSetup.getProviderDeploymentProperties());
        Assert.assertEquals(expectedDeploymentProperties, deploymentSetup.getProviderDeploymentProperties());
    }
}
