package alien4cloud.it.application;

import java.util.List;
import java.util.Set;

import org.junit.Assert;

import alien4cloud.cloud.CloudResourceTopologyMatchResult;
import alien4cloud.it.Context;
import alien4cloud.it.cloud.CloudComputeTemplateStepDefinitions;
import alien4cloud.it.cloudImage.CloudImageStepDefinitions;
import alien4cloud.model.cloud.ActivableComputeTemplate;
import alien4cloud.model.cloud.CloudImage;
import alien4cloud.model.cloud.CloudImageFlavor;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;

import com.google.common.collect.Sets;

import cucumber.api.DataTable;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class ApplicationResourceMatcherStepDefinitions {

    @When("^I match for resources for my application on the cloud$")
    public void I_match_for_resources_for_my_application_on_the_cloud() throws Throwable {
        String applicationId = Context.getInstance().getApplication().getId();
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/applications/" + applicationId + "/cloud-resources"));
    }

    @Then("^I should receive a match result with (\\d+) compute templates for the node \"([^\"]*)\":$")
    public void I_should_receive_a_match_result_with_compute_templates_for_the_node(int numberOfComputeTemplates, String nodeName,
                                                                                    DataTable expectedTemplatesTable) throws Throwable {
        RestResponse<CloudResourceTopologyMatchResult> matchResultResponse = JsonUtil.read(Context.getInstance().getRestResponse(),
                CloudResourceTopologyMatchResult.class);
        Assert.assertNull(matchResultResponse.getError());
        Assert.assertNotNull(matchResultResponse.getData());
        CloudComputeTemplateStepDefinitions.assertComputeTemplates(numberOfComputeTemplates,
                Sets.newHashSet(matchResultResponse.getData().getMatchResult().get(nodeName)), expectedTemplatesTable);
    }

    @And("^The match result should contain (\\d+) cloud images:$")
    public void The_match_result_should_contain_cloud_images(int numberOfImages, DataTable expectedImagesTable) throws Throwable {
        Set<CloudImage> images = Sets.newHashSet(JsonUtil.read(Context.getInstance().getRestResponse(), CloudResourceTopologyMatchResult.class).getData()
                .getImages().values());
        CloudImageStepDefinitions.assertCloudImages(numberOfImages, images, expectedImagesTable);
    }

    @And("^The match result should contain (\\d+) flavors:$")
    public void The_match_result_should_contain_flavors(int numberOfFlavors, DataTable expectedFlavorsTable) throws Throwable {
        Set<CloudImageFlavor> flavors = Sets.newHashSet(JsonUtil.read(Context.getInstance().getRestResponse(), CloudResourceTopologyMatchResult.class)
                .getData().getFlavors().values());
        CloudComputeTemplateStepDefinitions.assertFlavors(numberOfFlavors, flavors, expectedFlavorsTable);
    }

    @Then("^I should receive an empty match result$")
    public void I_should_receive_an_empty_match_result() throws Throwable {
        RestResponse<CloudResourceTopologyMatchResult> matchResultResponse = JsonUtil.read(Context.getInstance().getRestResponse(),
                CloudResourceTopologyMatchResult.class);
        Assert.assertTrue(matchResultResponse.getData().getImages().isEmpty());
        Assert.assertTrue(matchResultResponse.getData().getFlavors().isEmpty());
        for (List<ActivableComputeTemplate> templates : matchResultResponse.getData().getMatchResult().values()) {
            Assert.assertTrue(templates.isEmpty());
        }
    }
}
