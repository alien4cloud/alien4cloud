package alien4cloud.it.cloud;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Assert;

import alien4cloud.it.Context;
import alien4cloud.model.cloud.ActivableComputeTemplate;
import alien4cloud.model.cloud.CloudImageFlavor;
import alien4cloud.model.cloud.ComputeTemplate;
import alien4cloud.rest.cloud.CloudDTO;
import alien4cloud.rest.utils.JsonUtil;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import cucumber.api.DataTable;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class CloudComputeTemplateStepDefinitions {

    @When("^I add the cloud image \"([^\"]*)\" to the cloud \"([^\"]*)\"$")
    public void I_add_the_cloud_image_to_the_cloud(String cloudImageName, String cloudName) throws Throwable {
        String cloudId = Context.getInstance().getCloudId(cloudName);
        String cloudImageId = Context.getInstance().getCloudImageId(cloudImageName);
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postJSon("/rest/clouds/" + cloudId + "/images", JsonUtil.toString(new String[] { cloudImageId })));
    }

    @Then("^I should receive a RestResponse with (\\d+) compute templates:$")
    public void I_should_receive_a_RestResponse_with_compute_templates(int numberOfTemplate, DataTable expectedTemplatesTable) throws Throwable {
        Set<ActivableComputeTemplate> templates = Sets.newHashSet(JsonUtil.read(Context.getInstance().getRestResponse(), ActivableComputeTemplate[].class)
                .getData());
        assertComputeTemplates(numberOfTemplate, templates, expectedTemplatesTable);
    }

    @Then("^I should receive a RestResponse with no compute templates$")
    public void I_should_receive_a_RestResponse_with_no_compute_templates() throws Throwable {
        I_should_receive_a_RestResponse_with_compute_templates(0, null);
    }

    @And("^The cloud with name \"([^\"]*)\" should have (\\d+) compute templates as resources:$")
    public void The_cloud_with_name_should_have_compute_templates_as_resources(String cloudName, int numberOfTemplate, DataTable expectedTemplatesTable)
            throws Throwable {
        String cloudId = Context.getInstance().getCloudId(cloudName);
        CloudDTO cloudDTO = JsonUtil.read(Context.getRestClientInstance().get("/rest/clouds/" + cloudId), CloudDTO.class).getData();
        assertComputeTemplates(numberOfTemplate, cloudDTO.getCloud().getComputeTemplates(), expectedTemplatesTable);
    }

    @And("^The cloud with name \"([^\"]*)\" should not have compute templates as resources$")
    public void The_cloud_with_name_should_not_have_compute_templates_as_resources(String cloudName) throws Throwable {
        The_cloud_with_name_should_have_compute_templates_as_resources(cloudName, 0, null);
    }

    public static void assertComputeTemplates(int numberOfTemplate, Set<? extends ComputeTemplate> templates, DataTable expectedTemplatesTable) {
        Assert.assertEquals(numberOfTemplate, templates.size());
        Set<ComputeTemplate> expectedTemplates = Sets.newHashSet();
        if (expectedTemplatesTable != null) {
            for (List<String> rows : expectedTemplatesTable.raw()) {
                String imageName = rows.get(0);
                String flavorName = rows.get(1);
                if (rows.size() == 3) {
                    boolean enabled = "enabled".equals(rows.get(2));
                    expectedTemplates.add(new ActivableComputeTemplate(Context.getInstance().getCloudImageId(imageName), flavorName, enabled));
                } else {
                    expectedTemplates.add(new ComputeTemplate(Context.getInstance().getCloudImageId(imageName), flavorName));
                }
            }
        }
        Assert.assertEquals(expectedTemplates, templates);
    }

    @And("^I add the flavor with name \"([^\"]*)\", number of CPUs (\\d+), disk size (\\d+) and memory size (\\d+) to the cloud \"([^\"]*)\"$")
    public void I_add_the_flavor_with_name_number_of_CPUs_disk_size_and_memory_size_to_the_cloud(String flavorId, int nbCPUs, long diskSize, int memSize,
            String cloudName) throws Throwable {
        String cloudId = Context.getInstance().getCloudId(cloudName);
        CloudImageFlavor cloudImageFlavor = new CloudImageFlavor();
        cloudImageFlavor.setId(flavorId);
        cloudImageFlavor.setDiskSize(diskSize);
        cloudImageFlavor.setMemSize(memSize);
        cloudImageFlavor.setNumCPUs(nbCPUs);
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postJSon("/rest/clouds/" + cloudId + "/flavors", JsonUtil.toString(cloudImageFlavor)));
    }

    public static void assertFlavors(int numberOfFlavors, Set<CloudImageFlavor> flavors, DataTable expectedFlavorsTable) {
        Assert.assertEquals(numberOfFlavors, flavors.size());
        Set<CloudImageFlavor> expectedFlavors = Sets.newHashSet();
        if (expectedFlavorsTable != null) {
            for (List<String> rows : expectedFlavorsTable.raw()) {
                String name = rows.get(0);
                int numCPUs = Integer.parseInt(rows.get(1));
                long diskSize = Long.parseLong(rows.get(2));
                long memSize = Long.parseLong(rows.get(3));
                CloudImageFlavor flavor = new CloudImageFlavor();
                flavor.setDiskSize(diskSize);
                flavor.setMemSize(memSize);
                flavor.setNumCPUs(numCPUs);
                flavor.setId(name);
                expectedFlavors.add(flavor);
            }
        }
        Assert.assertEquals(expectedFlavors, flavors);
    }

    @When("^I disable the compute template of the cloud \"([^\"]*)\" constituted of image \"([^\"]*)\" and flavor \"([^\"]*)\"$")
    public void I_disable_the_compute_template_of_the_cloud_constituted_of_image_and_flavor(String cloudName, String cloudImageName, String flavorId)
            throws Throwable {
        toggleTemplateStatus(cloudName, cloudImageName, flavorId, false);
    }

    @When("^I enable the compute template of the cloud \"([^\"]*)\" constituted of image \"([^\"]*)\" and flavor \"([^\"]*)\"$")
    public void I_enable_the_compute_template_of_the_cloud_constituted_of_image_and_flavor(String cloudName, String cloudImageName, String flavorId)
            throws IOException, URISyntaxException {
        toggleTemplateStatus(cloudName, cloudImageName, flavorId, true);
    }

    private void toggleTemplateStatus(String cloudName, String cloudImageName, String flavorId, boolean status) throws IOException, URISyntaxException {
        String cloudId = Context.getInstance().getCloudId(cloudName);
        String cloudImageId = Context.getInstance().getCloudImageId(cloudImageName);
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postUrlEncoded("/rest/clouds/" + cloudId + "/templates/" + cloudImageId + "/" + flavorId + "/status",
                        Lists.<NameValuePair> newArrayList(new BasicNameValuePair("enabled", String.valueOf(status)))));
    }

    @When("^I remove the cloud image \"([^\"]*)\" from the cloud \"([^\"]*)\"$")
    public void I_remove_the_cloud_image_from_the_cloud(String cloudImageName, String cloudName) throws Throwable {
        String cloudId = Context.getInstance().getCloudId(cloudName);
        String cloudImageId = Context.getInstance().getCloudImageId(cloudImageName);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete("/rest/clouds/" + cloudId + "/images/" + cloudImageId));
    }

    @When("^I remove the flavor \"([^\"]*)\" from the cloud \"([^\"]*)\"$")
    public void I_remove_the_flavor_from_the_cloud(String flavorId, String cloudName) throws Throwable {
        String cloudId = Context.getInstance().getCloudId(cloudName);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete("/rest/clouds/" + cloudId + "/flavors/" + flavorId));
    }
}
