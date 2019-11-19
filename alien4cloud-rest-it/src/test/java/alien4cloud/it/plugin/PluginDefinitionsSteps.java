package alien4cloud.it.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import com.google.common.collect.Lists;
import org.junit.Assert;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.it.Context;
import alien4cloud.it.common.CommonStepDefinitions;
import alien4cloud.model.common.Tag;
import alien4cloud.plugin.model.PluginUsage;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.PendingException;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PluginDefinitionsSteps {
    private static final CommonStepDefinitions COMMON_STEP_DEFINITIONS = new CommonStepDefinitions();

    private static final Map<String, Path> conditionToPath = Maps.newHashMap();
    private static final Path PLUGIN_PATH = Paths.get("../alien4cloud-mock-paas-provider-plugin/target/alien4cloud-mock-paas-provider-plugin-1.0-"
            + Context.VERSION + ".zip");

    private static final Path INVALID_PLUGIN_PATH = Paths.get("../alien4cloud-mock-paas-provider-plugin/target/alien4cloud-mock-paas-provider-plugin-invalid-"
            + Context.VERSION + ".zip");
    private static final String PLUGIN_ID = "alien4cloud-mock-paas-provider";
    private static final String NEXT_VERSION_PLUGIN_ID = "alien4cloud-mock-paas-provider";
    private final ObjectMapper mapper = new ObjectMapper();

    private CommonStepDefinitions commonSteps = new CommonStepDefinitions();

    static {
        Path nextVersionSameConfiguration = Paths.get("../alien4cloud-mock-paas-provider-plugin/target/alien4cloud-mock-paas-provider-plugin-1.1-"
                + Context.VERSION + ".zip");
        Path nextVersionDifferentConfiguration = Paths
                .get("../alien4cloud-mock-paas-provider-plugin/target/alien4cloud-mock-paas-provider-plugin-1.1-different-conf-" + Context.VERSION + ".zip");
        conditionToPath.put("has the same configuration type", nextVersionSameConfiguration);
        conditionToPath.put("has a different configuration type", nextVersionDifferentConfiguration);
    }

    @Given("^I upload a plugin$")
    public void I_upload_a_plugin() throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postMultipart("/rest/v1/plugins", "file", Files.newInputStream(PLUGIN_PATH)));
    }

    @Given("^I have uploaded a plugin$")
    public void I_have_uploaded_a_plugin() throws Throwable {
        I_upload_a_plugin();
        commonSteps.I_should_receive_a_RestResponse_with_no_error();
    }

    @Given("^I upload an invalid plugin$")
    public void I_upload_an_invalid_plugin() throws Throwable {
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postMultipart("/rest/v1/plugins", "file", Files.newInputStream(INVALID_PLUGIN_PATH)));
    }

    @When("^I search for plugins$")
    public void I_search_for_plugins() throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/plugins"));
    }

    @Then("^The plugin response should contains (\\d+) plugin$")
    public void The_plugin_response_should_contains_plugin(int count) throws Throwable {
        RestResponse<GetMultipleDataResult> restResponse = JsonUtil.read(Context.getInstance().takeRestResponse(), GetMultipleDataResult.class);
        assertNotNull(restResponse);
        assertNotNull(restResponse.getData());
        assertNotNull(restResponse.getData().getData());
        assertEquals(count, restResponse.getData().getData().length);
    }

    @When("^I enable the plugin$")
    public void I_enable_the_plugin() throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/plugins/" + PLUGIN_ID + "/enable"));
    }

    @When("^I disable the plugin$")
    public void I_disable_the_plugin() throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/plugins/" + PLUGIN_ID + "/disable"));
    }

    @Given("^I use the plugin$")
    public void I_use_the_plugin() throws Throwable {
        throw new PendingException();
    }

    @Then("^I should receive a RestResponse with a non-empty list of plugin usages.$")
    public void I_should_receive_a_RestResponse_with_a_non_empty_list_of_plugin_usages() throws Throwable {
        List<PluginUsage> restResponse = mapper.readValue(Context.getInstance().takeRestResponse(), new TypeReference<List<PluginUsage>>() {
        });
        assertNotNull(restResponse);
        assertTrue(restResponse.size() > 0);
    }

    @When("^I remove the plugin$")
    public void I_remove_the_plugin() throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete("/rest/v1/plugins/" + PLUGIN_ID));
    }

    @Then("^the result should not be empty$")
    public void the_result_should_not_be_empty() throws Throwable {
        RestResponse<?> response = JsonUtil.read(Context.getInstance().takeRestResponse());
        // List<PaaSProviderDTO> providersResponse = JsonUtil.toList(JsonUtil.toString(response.getData()), PaaSProviderDTO.class);
        // assertNotNull(providersResponse);
        // assertTrue(providersResponse.size() > 0);
        Assert.fail("Fix test");
    }

    @When("^I get the plugin configuration$")
    public void I_get_the_plugin_configuration() throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/plugins/" + PLUGIN_ID + "/config"));
    }

    @Then("^there should be a configuration object in the response$")
    public void there_should_be_a_configuration_object_in_the_response() throws Throwable {
        RestResponse<?> response = JsonUtil.read(Context.getInstance().takeRestResponse());
        Object pluginConfig = response.getData();
        assertNotNull(pluginConfig);
    }

    @When("^I set the plugin configuration with a valid configuration object$")
    public void I_set_the_plugin_configuration_with_a_valid_configuration_object() throws Throwable {
        ProviderConfig config = new ProviderConfig();
        config.setFirstArgument("haha");
        config.setSecondArgument("55");
        List<Tag> tags = Lists.newArrayList();
        tags.add(new Tag("version", "1.0"));
        tags.add(new Tag("maturity", "none"));
        tags.add(new Tag("usefull", "no"));
        config.setTags(tags);
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postJSon("/rest/v1/plugins/" + PLUGIN_ID + "/config", JsonUtil.toString(config)));
    }

    @When("^I set the plugin configuration with an invalid configuration object$")
    public void I_set_the_plugin_configuration_with_an_invalid_configuration_object() throws Throwable {
        List<Tag> tags = Lists.newArrayList();
        tags.add(new Tag("version", "1.0"));
        tags.add(new Tag("maturity", "none"));
        tags.add(new Tag("usefull", "no"));
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/v1/plugins/" + PLUGIN_ID + "/config", JsonUtil.toString(tags)));
    }

    @Given("^I have set the plugin configuration with a valid configuration object$")
    public void I_have_set_the_plugin_configuration_with_a_valid_configuration_object() throws Throwable {
        I_set_the_plugin_configuration_with_a_valid_configuration_object();
        commonSteps.I_should_receive_a_RestResponse_with_no_error();
    }

    @Then("^there should be a non empty configuration object in the response$")
    public void there_should_be_a_non_empty_configuration_object_in_the_response() throws Throwable {
        RestResponse<ProviderConfig> response = JsonUtil.read(Context.getInstance().takeRestResponse(), ProviderConfig.class);
        ProviderConfig pluginConfig = response.getData();
        assertNotNull(pluginConfig);
        assertNotNull(pluginConfig.getTags());
    }

    @When("^I upload a plugin which \"([^\"]*)\"$")
    public void I_upload_a_plugin_which(String pluginCondition) throws Throwable {
        Path path = conditionToPath.get(pluginCondition);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postMultipart("/rest/v1/plugins", "file", Files.newInputStream(path)));
    }

    @Then("^the new plugin configuration should be the same as for the previous version$")
    @Deprecated
    public void the_new_plugin_configuration_should_be_the_same_as_for_the_previous_version() throws Throwable {
        String prevPluginResp = Context.getRestClientInstance().get("/rest/v1/plugins/" + PLUGIN_ID + "/config");
        String pluginResp = Context.getRestClientInstance().get("/rest/v1/plugins/" + NEXT_VERSION_PLUGIN_ID + "/config");
        Object pluginConfig = JsonUtil.read(pluginResp).getData();
        Object prevPluginConfig = JsonUtil.read(prevPluginResp).getData();

        String pluginConfigStr = JsonUtil.toString(pluginConfig);
        String prevPluginConfigStr = JsonUtil.toString(prevPluginConfig);
        assertEquals(prevPluginConfigStr, pluginConfigStr);
    }

    @Then("^the new plugin configuration should not be the same as for the previous version$")
    @Deprecated
    public void the_new_plugin_configuration_should_not_be_the_same_as_for_the_previous_version() throws Throwable {
        String prevPluginResp = Context.getRestClientInstance().get("/rest/v1/plugins/" + PLUGIN_ID + "/config");
        String pluginResp = Context.getRestClientInstance().get("/rest/v1/plugins/" + NEXT_VERSION_PLUGIN_ID + "/config");
        Object pluginConfig = JsonUtil.read(pluginResp).getData();
        Object prevPluginConfig = JsonUtil.read(prevPluginResp).getData();

        String pluginConfigStr = JsonUtil.toString(pluginConfig);
        String prevPluginConfigStr = JsonUtil.toString(prevPluginConfig);
        assertNotEquals(prevPluginConfigStr, pluginConfigStr);
    }

    @And("^I (successfully\\s)?upload a plugin from \"([^\"]*)\"$")
    public void I_sucesssfully_upload_a_plugin_from(String successfully, String pluginPathText) throws Throwable {
        Path pluginDirPath = Paths.get(pluginPathText);
        String pluginName = pluginDirPath.getFileName().toString();
        Path pluginPath = pluginDirPath.resolve("target").resolve(pluginName + "-" + Context.VERSION + ".zip");
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postMultipart("/rest/v1/plugins", "file", Files.newInputStream(pluginPath)));
        if (StringUtils.isNotBlank(successfully)) {
            this.COMMON_STEP_DEFINITIONS.I_should_receive_a_RestResponse_with_no_error();
        }
    }

    @And("^I upload a plugin \"([^\"]*)\" from \"([^\"]*)\"$")
    public void I_upload_a_plugin_from(String pluginName, String pluginPathText) throws Throwable {
        Path pluginDirPath = Paths.get(pluginPathText);
        Path pluginPath = pluginDirPath.resolve("target").resolve(pluginName + "-" + Context.VERSION + ".zip");
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postMultipart("/rest/v1/plugins", "file", Files.newInputStream(pluginPath)));
    }
}
