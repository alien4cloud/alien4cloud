package alien4cloud.it.tags;

import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import org.junit.Assert;

import alien4cloud.it.Context;
import alien4cloud.it.common.CommonStepDefinitions;
import alien4cloud.model.common.MetaPropConfiguration;
import alien4cloud.rest.tags.TagConfigurationSaveResponse;
import alien4cloud.rest.tags.TagConfigurationValidationError;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.utils.FileUtil;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class TagConfigurationStepDefinitions {

    private static final String TAG_TEST_DATA_PACKAGE = "./src/test/resources/data/tagConfigurations/";
    private static final String CONFIGURATION_TAGS = "./src/test/resources/data/tagConfigurations/configuration-tags.json";

    private CommonStepDefinitions commonStepDefinitions = new CommonStepDefinitions();
    private String lastTagId;
    private Set<TagConfigurationValidationError> lastErrors;

    @When("^I add the tag configuration with name \"([^\"]*)\" of type \"([^\"]*)\"$")
    public void I_add_the_tag_configuration_with_name_of_type(String tagName, String type) throws Throwable {
        String tagConfigurationJson = FileUtil.readTextFile(Paths.get(TAG_TEST_DATA_PACKAGE + tagName + type + ".json"));
        String response = Context.getRestClientInstance().postJSon("/rest/tagconfigurations", tagConfigurationJson);
        TagConfigurationSaveResponse tagReceived = JsonUtil.read(response, TagConfigurationSaveResponse.class).getData();
        lastTagId = tagReceived.getId();
        lastErrors = tagReceived.getValidationErrors();
        Context.getInstance().registerRestResponse(response);
    }

    @Then("^The RestResponse should contain valid tag configuration$")
    public void The_RestResponse_should_contain_valid_tag_configuration() throws Throwable {
        Assert.assertNotNull(lastTagId);
        Assert.assertNull(lastErrors);
    }

    @Then("^The tag configuration must exist in ALIEN$")
    public void The_tag_configuration_must_exist_in_ALIEN() throws Throwable {
        String tagConfigurationJson = Context.getRestClientInstance().get("/rest/tagconfigurations/" + lastTagId);
        MetaPropConfiguration tagConfiguration = JsonUtil.read(tagConfigurationJson, MetaPropConfiguration.class).getData();
        Assert.assertEquals(lastTagId, tagConfiguration.getId());
    }

    @When("^I delete the tag configuration$")
    public void I_delete_the_tag_configuration_with_name_of_type() throws Throwable {
        String response = Context.getRestClientInstance().delete("/rest/tagconfigurations/" + lastTagId);
        Context.getInstance().registerRestResponse(response);
    }

    @Then("^The tag configuration must not exist in ALIEN$")
    public void The_tag_configuration_must_not_exist_in_ALIEN() throws Throwable {
        String errorResponse = Context.getRestClientInstance().get("/rest/tagconfigurations/" + lastTagId);
        Context.getInstance().registerRestResponse(errorResponse);
        commonStepDefinitions.I_should_receive_a_RestResponse_with_an_error_code(504);
    }

    @Given("^I load several configuration tags$")
    public void I_load_several_configuration_tags() throws Throwable {
        String tagConfigurationsJson = FileUtil.readTextFile(Paths.get(CONFIGURATION_TAGS));
        List<MetaPropConfiguration> tagsArray = JsonUtil.readObject(tagConfigurationsJson);

        // registering all configuration tags
        for (Object tag : tagsArray) {
            MetaPropConfiguration tagObject = JsonUtil.readObject(JsonUtil.toString(tag), MetaPropConfiguration.class);
            Context.getInstance().registerConfigurationTag(tagObject.getName(), tagObject);
        }

        Assert.assertNotNull(Context.getInstance().getConfigurationTags());
    }

    @Then("^I should have (\\d+) configuration tags loaded$")
    public void I_should_have_configuration_tags_loaded(int tagsNumber) throws Throwable {
        Assert.assertEquals(tagsNumber, Context.getInstance().getConfigurationTags().size());
    }

    @Given("^I have the tag \"([^\"]*)\" registered for \"([^\"]*)\"$")
    public void I_have_the_tag_registered_for_applications(String tagName, String target) throws Throwable {
        Assert.assertTrue(Context.getInstance().getConfigurationTags().containsKey(tagName));
        // possible targets : application / component / cloud
        Assert.assertEquals(target, Context.getInstance().getConfigurationTags().get(tagName).getTarget());
    }
}
