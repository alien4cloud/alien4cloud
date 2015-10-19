//package alien4cloud.it.cloudImage;
//
//import static org.junit.Assert.assertNotNull;
//
//import java.io.IOException;
//import java.util.List;
//import java.util.Set;
//
//import org.junit.Assert;
//import org.springframework.beans.BeanWrapper;
//import org.springframework.beans.BeanWrapperImpl;
//
//import alien4cloud.dao.model.GetMultipleDataResult;
//import alien4cloud.it.Context;
//import alien4cloud.model.cloud.CloudImage;
//import alien4cloud.rest.cloud.CloudImageCreateRequest;
//import alien4cloud.rest.cloud.CloudImageSearchRequest;
//import alien4cloud.rest.cloud.CloudImageUpdateRequest;
//import alien4cloud.rest.model.RestResponse;
//import alien4cloud.rest.utils.JsonUtil;
//
//import com.google.common.collect.Sets;
//
//import cucumber.api.DataTable;
//import cucumber.api.java.en.And;
//import cucumber.api.java.en.Given;
//import cucumber.api.java.en.Then;
//import cucumber.api.java.en.When;
//
///**
// * @author Minh Khang VU
// */
//public class CloudImageStepDefinitions {
//
//    private String currentCloudImageId;
//
//    @And("^I have already created a cloud image with name \"([^\"]*)\", architecture \"([^\"]*)\", type \"([^\"]*)\", distribution \"([^\"]*)\" and version \"([^\"]*)\"$")
//    public void I_have_already_created_a_cloud_image_with_name_architecture_type_distribution_and_version(String name, String architecture, String type,
//                                                                                                          String distribution,
//                                                                                                          String version) throws Throwable {
//        I_create_a_cloud_image_with_name_architecture_type_distribution_and_version(name, architecture, type, distribution, version);
//        Context.getInstance().registerCloudImageId(name, currentCloudImageId);
//    }
//
//    @Given("^I have already created a cloud image with name \"([^\"]*)\", architecture \"([^\"]*)\", type \"([^\"]*)\", distribution \"([^\"]*)\", version \"([^\"]*)\", min CPUs (\\d+), min memory (\\d+), min disk (\\d+)$")
//    public void I_have_already_created_a_cloud_image_with_name_architecture_type_distribution_version_min_CPUs_min_memory_min_disk(String name,
//                                                                                                                                   String architecture,
//                                                                                                                                   String type, String distribution, String version, int minCPUs, long minMemory, long minDisk) throws Throwable {
//        I_create_a_cloud_image_with_name_architecture_type_distribution_version_min_CPUs_min_memory_min_disk(name, architecture, type, distribution, version,
//                minCPUs, minMemory, minDisk);
//        Context.getInstance().registerCloudImageId(name, currentCloudImageId);
//    }
//
//    @When("^I create a cloud image with name \"([^\"]*)\", architecture \"([^\"]*)\", type \"([^\"]*)\", distribution \"([^\"]*)\" and version \"([^\"]*)\"$")
//    public void I_create_a_cloud_image_with_name_architecture_type_distribution_and_version(String name, String architecture, String type, String distribution,
//                                                                                            String version) throws Throwable {
//        CloudImageCreateRequest request = new CloudImageCreateRequest();
//        request.setName(name);
//        request.setOsArch(architecture);
//        request.setOsType(type);
//        request.setOsDistribution(distribution);
//        request.setOsVersion(version);
//        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/cloud-images", JsonUtil.toString(request)));
//        currentCloudImageId = JsonUtil.read(Context.getInstance().getRestResponse(), String.class).getData();
//    }
//
//    @And("^The recently created cloud image should be in available in Alien with name \"([^\"]*)\", architecture \"([^\"]*)\", type \"([^\"]*)\", distribution \"([^\"]*)\" and version \"([^\"]*)\"$")
//    public void The_recently_created_cloud_image_should_be_in_available_in_Alien_with_name_architecture_type_distribution_and_version(String name,
//                                                                                                                                      String architecture, String type, String distribution, String version) throws Throwable {
//        CloudImage cloudImage = JsonUtil.read(Context.getRestClientInstance().get("/rest/cloud-images/" + currentCloudImageId), CloudImage.class).getData();
//        Assert.assertEquals(name, cloudImage.getName());
//        Assert.assertEquals(architecture, cloudImage.getOsArch());
//        Assert.assertEquals(type, cloudImage.getOsType());
//        Assert.assertEquals(distribution, cloudImage.getOsDistribution());
//        Assert.assertEquals(version, cloudImage.getOsVersion());
//    }
//
//    @When("^I create a cloud image with name \"([^\"]*)\", architecture \"([^\"]*)\", type \"([^\"]*)\", distribution \"([^\"]*)\", version \"([^\"]*)\", min CPUs (\\d+), min memory (\\d+), min disk (\\d+)$")
//    public void I_create_a_cloud_image_with_name_architecture_type_distribution_version_min_CPUs_min_memory_min_disk(String name, String architecture,
//                                                                                                                     String type, String distribution, String version, int minCPUs, long minMemory, long minDisk) throws Throwable {
//        CloudImageCreateRequest request = new CloudImageCreateRequest();
//        request.setName(name);
//        request.setOsArch(architecture);
//        request.setOsType(type);
//        request.setOsDistribution(distribution);
//        request.setOsVersion(version);
//        request.setDiskSize(minDisk);
//        request.setMemSize(minMemory);
//        request.setNumCPUs(minCPUs);
//        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/cloud-images", JsonUtil.toString(request)));
//        currentCloudImageId = JsonUtil.read(Context.getInstance().getRestResponse(), String.class).getData();
//    }
//
//    @And("^The recently created cloud image should be in available in Alien with name \"([^\"]*)\", architecture \"([^\"]*)\", type \"([^\"]*)\", distribution \"([^\"]*)\", version \"([^\"]*)\", min CPUs (\\d+), min memory (\\d+), min disk (\\d+)$")
//    public void The_recently_created_cloud_image_should_be_in_available_in_Alien_with_name_architecture_type_distribution_and_version_min_CPUs_min_memory_min_disk(
//            String name, String architecture, String type, String distribution, String version, int minCPUs, long minMemory, long minDisk) throws Throwable {
//        CloudImage cloudImage = JsonUtil.read(Context.getRestClientInstance().get("/rest/cloud-images/" + currentCloudImageId), CloudImage.class).getData();
//        Assert.assertEquals(name, cloudImage.getName());
//        Assert.assertEquals(architecture, cloudImage.getOsArch());
//        Assert.assertEquals(type, cloudImage.getOsType());
//        Assert.assertEquals(distribution, cloudImage.getOsDistribution());
//        Assert.assertEquals(version, cloudImage.getOsVersion());
//        Assert.assertEquals(minCPUs, cloudImage.getRequirement().getNumCPUs().intValue());
//        Assert.assertEquals(minMemory, cloudImage.getRequirement().getMemSize().longValue());
//        Assert.assertEquals(minDisk, cloudImage.getRequirement().getDiskSize().longValue());
//    }
//
//    @And("^I update the \"([^\"]*)\" of the recently created cloud image to \"([^\"]*)\"$")
//    public void I_update_the_of_the_recently_created_cloud_image_to(String fieldName, String fieldValue) throws Throwable {
//        CloudImageUpdateRequest updateRequest = new CloudImageUpdateRequest();
//        BeanWrapper wrapper = new BeanWrapperImpl(updateRequest);
//        wrapper.setPropertyValue(fieldName, fieldValue);
//        Context.getInstance().registerRestResponse(
//                Context.getRestClientInstance().putJSon("/rest/cloud-images/" + currentCloudImageId, JsonUtil.toString(updateRequest)));
//    }
//
//    @And("^I delete the recently created cloud image$")
//    public void I_delete_the_recently_created_cloud_image() throws Throwable {
//        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete("/rest/cloud-images/" + currentCloudImageId));
//    }
//
//    @And("^The recently created cloud image must not exist any more in Alien$")
//    public void The_recently_created_cloud_image_must_not_exist_any_more_in_Alien() throws Throwable {
//        Assert.assertEquals(504, JsonUtil.read(Context.getRestClientInstance().get("/rest/cloud-images/" + currentCloudImageId)).getError().getCode());
//    }
//
//    public static void assertCloudImages(int numberOfImages, Set<CloudImage> images, DataTable expectedImagesTable) {
//        Assert.assertEquals(numberOfImages, images.size());
//        Set<CloudImage> actualImages = Sets.newHashSet();
//        for (CloudImage image : images) {
//            image.setIconId(null);
//            image.setId(null);
//            actualImages.add(image);
//        }
//        Set<CloudImage> expectedImages = Sets.newHashSet();
//        if (expectedImagesTable != null) {
//            for (List<String> rows : expectedImagesTable.raw()) {
//                CloudImage cloudImage = new CloudImage();
//                cloudImage.setName(rows.get(0));
//                cloudImage.setOsArch(rows.get(1));
//                cloudImage.setOsType(rows.get(2));
//                cloudImage.setOsDistribution(rows.get(3));
//                cloudImage.setOsVersion(rows.get(4));
//                expectedImages.add(cloudImage);
//            }
//        }
//        Assert.assertEquals(expectedImages, actualImages);
//    }
//
//    private void search(Set<String> excluded) throws IOException {
//        CloudImageSearchRequest request = new CloudImageSearchRequest();
//        request.setFrom(0);
//        request.setSize(Integer.MAX_VALUE);
//        request.setExclude(excluded);
//        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/cloud-images/search", JsonUtil.toString(request)));
//    }
//
//    @When("^I search for cloud images without excluding any image$")
//    public void I_search_for_cloud_images_without_excluding_any_image() throws Throwable {
//        search(null);
//    }
//
//    @Then("^I should receive a (\\d+) cloud images in the search result:$")
//    public void I_should_receive_a_cloud_images_in_the_search_result(int numberOfCloudImage, DataTable expectedImages) throws Throwable {
//        RestResponse<GetMultipleDataResult> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), GetMultipleDataResult.class);
//        GetMultipleDataResult searchResp = restResponse.getData();
//        assertNotNull(searchResp);
//        assertNotNull(searchResp.getTypes());
//        assertNotNull(searchResp.getData());
//        Assert.assertEquals(numberOfCloudImage, searchResp.getTypes().length);
//        Assert.assertEquals(numberOfCloudImage, searchResp.getData().length);
//        Set<CloudImage> actualImages = Sets.newHashSet();
//        for (Object image : searchResp.getData()) {
//            actualImages.add(JsonUtil.readObject(JsonUtil.toString(image), CloudImage.class));
//        }
//        assertCloudImages(numberOfCloudImage, actualImages, expectedImages);
//    }
//
//    @When("^I search for cloud images excluding \"([^\"]*)\"$")
//    public void I_search_for_cloud_images_excluding(String imageName) throws Throwable {
//        search(Sets.newHashSet(Context.getInstance().getCloudImageId(imageName)));
//    }
//}
