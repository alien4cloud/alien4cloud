//package alien4cloud.it.cloud;
//
//import static org.junit.Assert.*;
//
//import java.io.IOException;
//import java.util.List;
//import java.util.Map;
//
//import org.apache.commons.lang3.StringUtils;
//import org.apache.http.NameValuePair;
//import org.apache.http.message.BasicNameValuePair;
//
//import alien4cloud.dao.model.GetMultipleDataResult;
//import alien4cloud.it.Context;
//import alien4cloud.it.Entry;
//import alien4cloud.it.plugin.ProviderConfig;
//import alien4cloud.model.application.EnvironmentType;
//import alien4cloud.model.cloud.Cloud;
//import alien4cloud.model.cloud.IaaSType;
//import alien4cloud.paas.exception.NotSupportedException;
//import alien4cloud.rest.cloud.CloudDTO;
//import alien4cloud.rest.model.RestResponse;
//import alien4cloud.rest.utils.JsonUtil;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.google.common.collect.Lists;
//import com.google.common.collect.Maps;
//
//import cucumber.api.java.en.And;
//import cucumber.api.java.en.Given;
//import cucumber.api.java.en.Then;
//import cucumber.api.java.en.When;
//
//public class CloudDefinitionsSteps {
//
//    @Given("^I create a cloud with name \"([^\"]*)\" and plugin id \"([^\"]*)\" and bean name \"([^\"]*)\"$")
//    public void I_create_a_cloud_with_name_and_plugin_id_and_bean_name(String cloudName, String pluginId, String pluginBeanName) throws Throwable {
//        Cloud cloud = new Cloud();
//        cloud.setIaaSType(IaaSType.OPENSTACK);
//        cloud.setName(cloudName);
//        cloud.setPaasPluginId(pluginId);
//        cloud.setPaasPluginBean(pluginBeanName);
//        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/v1/clouds", JsonUtil.toString(cloud)));
//        RestResponse<String> cloudIdResponse = JsonUtil.read(Context.getInstance().getRestResponse(), String.class);
//        Context.getInstance().registerCloud(cloudIdResponse.getData(), cloudName);
//    }
//
//    @Given("^I enable the cloud \"([^\"]*)\"$")
//    public void I_enable_the_cloud(String cloudName) throws IOException {
//        String cloudId = Context.getInstance().getCloudId(cloudName);
//        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/clouds/" + cloudId + "/enable"));
//    }
//
//    @When("^I list clouds$")
//    public void I_list_clouds() throws IOException {
//        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/clouds/search"));
//    }
//
//    @Then("^Response should contains (\\d+) cloud$")
//    public void I_list_enabled_clouds(int exepectedCloudCount) throws IOException {
//        RestResponse<GetMultipleDataResult> response = JsonUtil.read(Context.getInstance().getRestResponse(), GetMultipleDataResult.class);
//        assertEquals(exepectedCloudCount, response.getData().getTotalResults());
//    }
//
//    @Then("^Response should contains a cloud with name \"([^\"]*)\"$")
//    public void Response_should_contains_a_cloud_with_name(String cloudName) throws IOException {
//        RestResponse<GetMultipleDataResult> response = JsonUtil.read(Context.getInstance().getRestResponse(), GetMultipleDataResult.class);
//        boolean contains = false;
//        for (Object cloudAsMap : response.getData().getData()) {
//            Cloud cloud = JsonUtil.readObject(JsonUtil.toString(cloudAsMap), Cloud.class);
//            if (cloudName.equals(cloud.getName())) {
//                contains = true;
//            }
//        }
//        assertTrue(contains);
//    }
//
//    @Then("^Response should contains a cloud with deploymentNamePattern \"([^\"]*)\"$")
//    public void Response_should_contains_a_cloud_with_deploymentNamePattern(String deploymentNamePattern) throws IOException {
//        RestResponse<GetMultipleDataResult> response = JsonUtil.read(Context.getInstance().getRestResponse(), GetMultipleDataResult.class);
//        boolean contains = false;
//        for (Object cloudAsMap : response.getData().getData()) {
//            Cloud cloud = JsonUtil.readObject(JsonUtil.toString(cloudAsMap), Cloud.class);
//            if (deploymentNamePattern.equals(cloud.getDeploymentNamePattern())) {
//                contains = true;
//            }
//        }
//        assertTrue(contains);
//    }
//
//    @When("^I update cloud name from \"([^\"]*)\" to \"([^\"]*)\"$")
//    public void I_update_cloud_name_from_to(String cloudName, String newCloudName) throws IOException {
//        Cloud cloud = new Cloud();
//        cloud.setName(newCloudName);
//        updateCloud(cloudName, cloud);
//        RestResponse<?> restResponse = JsonUtil.read(Context.getInstance().getRestResponse());
//        if (restResponse.getError() == null && !cloudName.equals(newCloudName)) {
//            Context.getInstance().unregisterCloud(cloudName);
//            Context.getInstance().registerCloud(cloud.getId(), newCloudName);
//        }
//    }
//
//    @When("^I update deployment name pattern of \"([^\"]*)\" to \"([^\"]*)\"$")
//    public void I_update_deployment_name_pattern_of_to(String cloudName, String newDeploymentNamePattern) throws IOException {
//        Cloud cloud = new Cloud();
//        cloud.setDeploymentNamePattern(newDeploymentNamePattern);
//        updateCloud(cloudName, cloud);
//        RestResponse<?> restResponse = JsonUtil.read(Context.getInstance().getRestResponse());
//        if (restResponse.getError() == null) {
//            Context.getInstance().unregisterCloud(cloudName);
//            Context.getInstance().registerCloud(cloud.getId(), cloudName);
//        }
//    }
//
//    @When("^I update cloud named \"([^\"]*)\" iaas type to \"([^\"]*)\"$")
//    public void I_update_cloud_named_iaas_type_to(String cloudName, String iaaSType) throws IOException {
//        Cloud cloud = new Cloud();
//        cloud.setIaaSType(IaaSType.valueOf(iaaSType));
//        updateCloud(cloudName, cloud);
//    }
//
//    @Then("^Response should contains a cloud with name \"([^\"]*)\" and iass type \"([^\"]*)\"$")
//    public void Response_should_contains_a_cloud_with_name_and_iass_type(String cloudName, String iaaSTypeStr) throws IOException {
//        IaaSType iaaSType = IaaSType.valueOf(iaaSTypeStr);
//        RestResponse<GetMultipleDataResult> response = JsonUtil.read(Context.getInstance().getRestResponse(), GetMultipleDataResult.class);
//        boolean contains = false;
//        for (Object cloudAsMap : response.getData().getData()) {
//            Cloud cloud = JsonUtil.readObject(JsonUtil.toString(cloudAsMap), Cloud.class);
//            if (cloudName.equals(cloud.getName()) && iaaSType.equals(cloud.getIaaSType())) {
//                contains = true;
//            }
//        }
//        assertTrue(contains);
//    }
//
//    @Then("^Response should contains a cloud with name \"([^\"]*)\" and environment type \"([^\"]*)\"$")
//    public void Response_should_contains_a_cloud_with_name_and_environment_type(String cloudName, String environmentTypeStr) throws IOException {
//        EnvironmentType environmentType = EnvironmentType.valueOf(environmentTypeStr);
//        RestResponse<GetMultipleDataResult> response = JsonUtil.read(Context.getInstance().getRestResponse(), GetMultipleDataResult.class);
//        boolean contains = false;
//        for (Object cloudAsMap : response.getData().getData()) {
//            Cloud cloud = JsonUtil.readObject(JsonUtil.toString(cloudAsMap), Cloud.class);
//            if (cloudName.equals(cloud.getName()) && environmentType.equals(cloud.getEnvironmentType())) {
//                contains = true;
//            }
//        }
//        assertTrue(contains);
//    }
//
//    @When("^I update cloud named \"([^\"]*)\" environment type to \"([^\"]*)\"$")
//    public void I_update_cloud_named_environment_type_to(String cloudName, String environmentType) throws IOException {
//        Cloud cloud = new Cloud();
//        cloud.setEnvironmentType(EnvironmentType.valueOf(environmentType));
//        updateCloud(cloudName, cloud);
//    }
//
//    private void updateCloud(String cloudName, Cloud updatedCloud) throws JsonProcessingException, IOException {
//        String cloudId = Context.getInstance().getCloudId(cloudName);
//        updatedCloud.setId(cloudId);
//        Context.getInstance().registerRestResponse(
//                Context.getRestClientInstance().putJSon("/rest/v1/clouds", Context.getInstance().getJsonMapper().writeValueAsString(updatedCloud)));
//    }
//
//    @When("^I delete a cloud with name \"([^\"]*)\"$")
//    public void I_delete_a_cloud_with_name(String cloudName) throws Throwable {
//        String cloudId = Context.getInstance().getCloudId(cloudName);
//        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete("/rest/v1/clouds/" + cloudId));
//    }
//
//    @When("^I disable cloud \"([^\"]*)\"$")
//    public void I_disable_cloud(String cloudName) throws Throwable {
//        String cloudId = Context.getInstance().getCloudId(cloudName);
//        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/clouds/" + cloudId + "/disable"));
//    }
//
//    @When("^I disable all clouds$")
//    public void I_disable_all_clouds() throws Throwable {
//        for (String cloudId : Context.getInstance().getOrchestratorIds()) {
//            Context.getRestClientInstance().get("/rest/v1/clouds/" + cloudId + "/disable");
//        }
//    }
//
//    @When("^I enable \"([^\"]*)\"$")
//    public void I_enable(String cloudName) throws Throwable {
//        String cloudId = Context.getInstance().getCloudId(cloudName);
//        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/clouds/" + cloudId + "/enable"));
//    }
//
//    @Then("^Response should contains a cloud with state enabled \"([^\"]*)\"$")
//    public void Response_should_contains_a_cloud_with_state_enabled(String enabledStr) throws Throwable {
//        Boolean enabled = Boolean.valueOf(enabledStr);
//        RestResponse<GetMultipleDataResult> response = JsonUtil.read(Context.getInstance().getRestResponse(), GetMultipleDataResult.class);
//        boolean contains = false;
//        for (Object cloudAsMap : response.getData().getData()) {
//            Cloud cloud = JsonUtil.readObject(JsonUtil.toString(cloudAsMap), Cloud.class);
//            if (enabled.equals(cloud.isEnabled())) {
//                contains = true;
//            }
//        }
//        assertTrue(contains);
//    }
//
//    @When("^I get configuration for cloud \"([^\"]*)\"$")
//    public void I_get_configuration_for_cloud(String cloudName) throws Throwable {
//        String cloudId = Context.getInstance().getCloudId(cloudName);
//        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/clouds/" + cloudId + "/configuration"));
//    }
//
//    @When("^I update configuration for cloud \"([^\"]*)\"$")
//    public void I_update_configuration_for_cloud(String cloudName) throws Throwable {
//        String cloudId = Context.getInstance().getCloudId(cloudName);
//
//        ProviderConfig config = new ProviderConfig();
//        config.setFirstArgument("firstArgument");
//        config.setSecondArgument("secondArgument");
//
//        Context.getInstance().registerRestResponse(
//                Context.getRestClientInstance().putJSon("/rest/v1/clouds/" + cloudId + "/configuration", JsonUtil.toString(config)));
//    }
//
//    @When("^I update configuration for cloud \"([^\"]*)\" with wrong configuration$")
//    public void I_update_configuration_for_cloud_with_wrong_configuration(String cloudName) throws Throwable {
//        String cloudId = Context.getInstance().getCloudId(cloudName);
//        Context.getInstance()
//                .registerRestResponse(Context.getRestClientInstance().putJSon("/rest/v1/clouds/" + cloudId + "/configuration", JsonUtil.toString("")));
//    }
//
//    @Then("^The cloud configuration should be null$")
//    public void The_cloud_configuration_should_be_null() throws Throwable {
//        RestResponse<ProviderConfig> response = JsonUtil.read(Context.getInstance().getRestResponse(), ProviderConfig.class);
//        assertNull(response.getData());
//    }
//
//    @Then("^The cloud configuration should not be null$")
//    public void The_cloud_configuration_should_not_be_null() throws Throwable {
//        RestResponse<ProviderConfig> response = JsonUtil.read(Context.getInstance().getRestResponse(), ProviderConfig.class);
//        assertNotNull(response.getData());
//    }
//
//    @When("^I get the cloud by name \"([^\"]*)\"$")
//    public void I_get_the_cloud_by_name(String name) throws Throwable {
//        NameValuePair nvp = new BasicNameValuePair("cloudName", name);
//        String restResponse = Context.getRestClientInstance().getUrlEncoded("/rest/v1/clouds/getByName", Lists.newArrayList(nvp));
//        Context.getInstance().registerRestResponse(restResponse);
//        RestResponse<Cloud> response = JsonUtil.read(Context.getInstance().getRestResponse(), Cloud.class);
//        if (response != null && response.getData() != null) {
//            String cloudId = response.getData().getId();
//            if (cloudId != null) {
//                Context.getInstance().registerCloud(cloudId, name);
//            }
//        }
//    }
//
//    @When("^I get the cloud \"([^\"]*)\"$")
//    public void I_get_the_cloud_by_id(String name) throws Throwable {
//        // get the cloud by its ID
//        String cloudId = Context.getInstance().getCloudId(name);
//        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/clouds/" + cloudId));
//    }
//
//    @Then("^I should receive a cloud with name \"([^\"]*)\"$")
//    public void I_should_receive_a_cloud_with_name(String expectedName) throws Throwable {
//        Cloud cloud = JsonUtil.read(Context.getInstance().getRestResponse(), Cloud.class).getData();
//        assertNotNull(cloud);
//        assertEquals(expectedName, cloud.getName());
//    }
//
//    @Then("^The Response should contains cloud with name \"([^\"]*)\" and iass type \"([^\"]*)\" and environment type \"([^\"]*)\"$")
//    public void the_Response_should_contains_cloud_with_name_and_iass_type_and_environment_type(String expectedName, String expectedIasSType,
//                                                                                                String expectedEnvType) throws Throwable {
//        // get cloud by id returns a CloudDTO whereas get cloud by name returns a Cloud object
//        // This assert will be used only after a get by ID
//        CloudDTO cloud = JsonUtil.read(Context.getInstance().getRestResponse(), CloudDTO.class).getData();
//        assertNotNull(cloud);
//        assertEquals(expectedName, cloud.getCloud().getName());
//        assertEquals(IaaSType.valueOf(expectedIasSType.toUpperCase()), cloud.getCloud().getIaaSType());
//        assertEquals(EnvironmentType.valueOf(expectedEnvType.toUpperCase()), cloud.getCloud().getEnvironmentType());
//    }
//
//    @When("^I get deployment properties for cloud \"([^\"]*)\"$")
//    public void I_get_deployment_properties_for_cloud(String cloudName) throws Throwable {
//        String cloudId = Context.getInstance().getCloudId(cloudName);
//        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/clouds/" + cloudId + "/deploymentpropertydefinitions"));
//    }
//
//    @Then("^The RestResponse should contain the following properties and values$")
//    public void The_RestResponse_should_contain_the_following_properties_and_values(List<Entry> properties) throws Throwable {
//        Map<String, String> props = (Map<String, String>) JsonUtil.read(Context.getInstance().getRestResponse()).getData();
//        assertNotNull(props);
//        for (Entry expectedProperty : properties) {
//            assertTrue(props.containsKey(expectedProperty.getName()));
//            String propValue = props.get(props.get(expectedProperty.getName()));
//            if (StringUtils.isBlank(expectedProperty.getValue())) {
//                assertNull(propValue);
//            } else {
//                assertNotNull(propValue);
//                assertEquals(expectedProperty.getValue(), propValue);
//            }
//        }
//    }
//
//    @When("^I clone the cloud with name \"([^\"]*)\"$")
//    public void I_clone_the_cloud_with_name(String cloudName) throws Throwable {
//        String cloudId = Context.getInstance().getCloudId(cloudName);
//        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/v1/clouds/" + cloudId + "/clone", "{}"));
//    }
//
//    @And("^I create a cloud with name \"([^\"]*)\" from cloudify (\\d+) PaaS provider$")
//    public void I_create_a_cloud_with_name_from_cloudify_PaaS_provider(String cloudName, int cloudifyVersion) throws Throwable {
//        String pluginId;
//        String beanName;
//        switch (cloudifyVersion) {
//            case 2:
//                pluginId = "alien-cloudify-2-paas-provider:" + Context.VERSION;
//                beanName = "cloudify-paas-provider";
//                break;
//            case 3:
//                pluginId = "alien-cloudify-3-paas-provider:" + Context.VERSION;
//                beanName = "cloudify-paas-provider";
//                break;
//            default:
//                throw new IllegalArgumentException("Cloudify version not supported " + cloudifyVersion);
//        }
//        I_create_a_cloud_with_name_and_plugin_id_and_bean_name(cloudName, pluginId, beanName);
//    }
//
//    @And("^I update cloudify (\\d+) manager's url to \"([^\"]*)\" for cloud with name \"([^\"]*)\"$")
//    public void I_update_cloudify_manager_s_url_to_for_cloud_with_name(int cloudifyVersion, String cloudifyUrl, String cloudName) throws Throwable {
//        I_update_cloudify_manager_s_url_to_with_login_and_password_for_cloud_with_name(cloudifyVersion, cloudifyUrl, null, null, cloudName);
//    }
//
//    @And("^I update cloudify (\\d+) manager's url to \"([^\"]*)\" with login \"([^\"]*)\" and password \"([^\"]*)\" for cloud with name \"([^\"]*)\"$")
//    public void I_update_cloudify_manager_s_url_to_with_login_and_password_for_cloud_with_name(int cloudifyVersion, String cloudifyUrl, String login,
//                                                                                               String password, String cloudName) throws Throwable {
//        String cloudId = Context.getInstance().getCloudId(cloudName);
//        Map<String, Object> config = Maps.newHashMap();
//        switch (cloudifyVersion) {
//            case 2:
//                config.put("cloudifyURLs", Lists.newArrayList(cloudifyUrl));
//                if (StringUtils.isNotBlank(login)) {
//                    config.put("username", login);
//                }
//                if (StringUtils.isNotBlank(password)) {
//                    config.put("password", password);
//                }
//                break;
//            case 3:
//                config.put("url", cloudifyUrl);
//                config.put("cloudInit",
//                        "#!/bin/sh\nsudo cp /etc/hosts /tmp/hosts\necho 127.0.0.1 `hostname` | sudo tee /etc/hosts > /dev/null\ncat  /tmp/hosts | sudo tee -a /etc/hosts > /dev/null");
//                break;
//            default:
//                throw new IllegalArgumentException("Cloudify version not supported " + cloudifyVersion);
//        }
//        Context.getInstance().registerRestResponse(
//                Context.getRestClientInstance().putJSon("/rest/v1/clouds/" + cloudId + "/configuration", JsonUtil.toString(config)));
//    }
//
//    @And("^I update cloudify (\\d+) manager's url to the OpenStack's jenkins management server for cloud with name \"([^\"]*)\"$")
//    public void I_update_cloudify_manager_s_url_to_the_OpenStack_s_jenkins_management_server_for_cloud_with_name(int cloudifyVersion, String cloudName)
//            throws Throwable {
//        switch (cloudifyVersion) {
//            case 2:
//                I_update_cloudify_manager_s_url_to_with_login_and_password_for_cloud_with_name(2, Context.getInstance().getCloudify2ManagerUrl(), Context
//                                .getInstance().getAppProperty("openstack.cfy2.manager_user"), Context.getInstance().getAppProperty("openstack.cfy2.manager_password"),
//                        cloudName);
//                break;
//            case 3:
//                I_update_cloudify_manager_s_url_to_with_login_and_password_for_cloud_with_name(3, Context.getInstance().getCloudify3ManagerUrl(), Context
//                                .getInstance().getAppProperty("openstack.cfy3.manager_user"), Context.getInstance().getAppProperty("openstack.cfy3.manager_password"),
//                        cloudName);
//                break;
//            default:
//                throw new NotSupportedException("Version " + cloudifyVersion + " of provider cloudify is not supported");
//        }
//    }
//}