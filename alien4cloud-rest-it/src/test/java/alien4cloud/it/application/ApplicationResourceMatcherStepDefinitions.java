package alien4cloud.it.application;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;

import alien4cloud.cloud.CloudResourceTopologyMatchResult;
import alien4cloud.it.Context;
import alien4cloud.it.cloud.CloudComputeTemplateStepDefinitions;
import alien4cloud.it.cloudImage.CloudImageStepDefinitions;
import alien4cloud.model.application.DeploymentSetup;
import alien4cloud.model.cloud.CloudImage;
import alien4cloud.model.cloud.CloudImageFlavor;
import alien4cloud.model.cloud.ComputeTemplate;
import alien4cloud.rest.application.UpdateDeploymentSetupRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import cucumber.api.DataTable;
import cucumber.api.PendingException;
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
                Sets.newHashSet(matchResultResponse.getData().getComputeMatchResult().get(nodeName)), expectedTemplatesTable);
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
        for (List<ComputeTemplate> templates : matchResultResponse.getData().getComputeMatchResult().values()) {
            Assert.assertTrue(templates.isEmpty());
        }
    }

    @When("^I select the the template composed of image \"([^\"]*)\" and flavor \"([^\"]*)\" for my node \"([^\"]*)\"$")
    public void I_select_the_the_template_composed_of_image_and_flavor_for_my_node(String cloudImageName, String flavorId, String nodeName) throws Throwable {
        Map<String, ComputeTemplate> cloudResourcesMatching = Maps.newHashMap();
        cloudResourcesMatching.put(nodeName, new ComputeTemplate(Context.getInstance().getCloudImageId(cloudImageName), flavorId));
        UpdateDeploymentSetupRequest request = new UpdateDeploymentSetupRequest(null, null, cloudResourcesMatching);
        String response = Context.getRestClientInstance().putJSon("/rest/applications/" + Context.getInstance().getApplication().getId() + "/deployment-setup",
                JsonUtil.toString(request));
        Context.getInstance().registerRestResponse(response);
    }

    @And("^The deployment setup of the application should contain following resources mapping:$")
    public void The_deployment_setup_of_the_application_should_contain_following_resources_mapping(DataTable resourcesMatching) throws Throwable {
        Map<String, ComputeTemplate> expectedCloudResourcesMatching = Maps.newHashMap();
        for (List<String> resourceMatching : resourcesMatching.raw()) {
            String cloudImageId = Context.getInstance().getCloudImageId(resourceMatching.get(1));
            String cloudImageFlavorId = resourceMatching.get(2);
            String nodeTemplate = resourceMatching.get(0);
            expectedCloudResourcesMatching.put(nodeTemplate, new ComputeTemplate(cloudImageId, cloudImageFlavorId));
        }
        DeploymentSetup deploymentSetup = JsonUtil.read(
                Context.getRestClientInstance().get("/rest/applications/" + ApplicationStepDefinitions.CURRENT_APPLICATION.getId() + "/deployment-setup"),
                DeploymentSetup.class).getData();
        Assert.assertNotNull(deploymentSetup.getCloudResourcesMapping());
        Assert.assertEquals(expectedCloudResourcesMatching, deploymentSetup.getCloudResourcesMapping());
    }

    @Then("^The deployment setup of the application should contain no resources mapping$")
    public void The_deployment_setup_of_the_application_should_contain_no_resources_mapping() throws Throwable {
        DeploymentSetup deploymentSetup = JsonUtil.read(
                Context.getRestClientInstance().get("/rest/applications/" + ApplicationStepDefinitions.CURRENT_APPLICATION.getId() + "/deployment-setup"),
                DeploymentSetup.class).getData();
        Assert.assertTrue(deploymentSetup.getCloudResourcesMapping() == null || deploymentSetup.getCloudResourcesMapping().isEmpty());
    }
}
