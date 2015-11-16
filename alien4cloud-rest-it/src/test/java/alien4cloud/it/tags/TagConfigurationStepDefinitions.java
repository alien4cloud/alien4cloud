package alien4cloud.it.tags;

import java.nio.file.Paths;
import java.util.List;

import org.junit.Assert;

import alien4cloud.it.Context;
import alien4cloud.model.common.MetaPropConfiguration;
import alien4cloud.rest.tags.TagConfigurationSaveResponse;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.utils.FileUtil;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class TagConfigurationStepDefinitions {

    private static final String CONFIGURATION_TAGS = "./src/test/resources/data/tagConfigurations/configuration-tags.json";

    @When("^I delete the tag configuration \"([^\"]*)\"$")
    public void I_delete_the_tag_configuration(String tagName) throws Throwable {
        // delete in ES
        MetaPropConfiguration tagConfiguration = Context.getInstance().getConfigurationTags().get(tagName);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete("/rest/metaproperties/" + tagConfiguration.getId()));
        // delete in context
        Context.getInstance().getConfigurationTags().remove(tagName);
    }

    @Then("^The tag configuration \"([^\"]*)\" must not exist in ALIEN$")
    public void The_tag_configuration_must_not_exist_in_ALIEN(String tagName) throws Throwable {
        // not present in the context only
        MetaPropConfiguration tagConfiguration = Context.getInstance().getConfigurationTags().get(tagName);
        Assert.assertNull(tagConfiguration);
    }

    @Given("^I load several configuration tags$")
    public void I_load_several_configuration_tags() throws Throwable {
        String tagConfigurationsJson = FileUtil.readTextFile(Paths.get(CONFIGURATION_TAGS));
        List<MetaPropConfiguration> tagsArray = JsonUtil.readObject(tagConfigurationsJson);
        // registering all configuration tags
        for (Object tag : tagsArray) {
            // register in ES
            MetaPropConfiguration tagObject = JsonUtil.readObject(JsonUtil.toString(tag), MetaPropConfiguration.class);
            String tagConfigurationJson = JsonUtil.toString(tagObject);
            String response = Context.getRestClientInstance().postJSon("/rest/metaproperties", tagConfigurationJson);
            TagConfigurationSaveResponse tagReceived = JsonUtil.read(response, TagConfigurationSaveResponse.class).getData();
            // register in context
            tagObject.setId(tagReceived.getId());
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
        // we assume that all saved tags are also in the context
        Assert.assertTrue(Context.getInstance().getConfigurationTags().containsKey(tagName));
        // possible targets : application / component / location
        Assert.assertEquals(target, Context.getInstance().getConfigurationTags().get(tagName).getTarget());
    }

    @Given("^I create a new tag with name \"([^\"]*)\" and the target \"([^\"]*)\"$")
    public void I_create_a_new_tag_with_name_and_the_target(String name, String target) throws Throwable {
        MetaPropConfiguration tagObject = new MetaPropConfiguration();
        tagObject.setName(name);
        tagObject.setTarget(target);
        // we just save the response, we don't add the tag in the Context
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/metaproperties", JsonUtil.toString(tagObject)));
    }

}
