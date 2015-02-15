package alien4cloud.it.cloud;

import java.util.List;
import java.util.Set;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Assert;

import alien4cloud.it.Context;
import alien4cloud.model.cloud.MatchedStorageTemplate;
import alien4cloud.model.cloud.StorageTemplate;
import alien4cloud.rest.cloud.CloudDTO;
import alien4cloud.rest.utils.JsonUtil;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import cucumber.api.DataTable;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class CloudStorageStepDefinitions {

    @When("^I add the storage with id \"([^\"]*)\" and device \"([^\"]*)\" and size (\\d+) to the cloud \"([^\"]*)\"$")
    public void I_add_the_storage_with_id_and_device_and_size_to_the_cloud(String id, String device, long size,
            String cloudName) throws Throwable {
        String cloudId = Context.getInstance().getCloudId(cloudName);
        StorageTemplate storage = new StorageTemplate();
        storage.setId(id);
        storage.setDevice(device);
        storage.setSize(size);
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postJSon("/rest/clouds/" + cloudId + "/storages", JsonUtil.toString(storage)));
    }

    @And("^The cloud with name \"([^\"]*)\" should have (\\d+) storages as resources:$")
    public void The_cloud_with_name_should_have_storage_as_resources(String cloudName, int numberOfStorage, DataTable expectedStorageTable) throws Throwable {
        String cloudId = Context.getInstance().getCloudId(cloudName);
        CloudDTO cloudDTO = JsonUtil.read(Context.getRestClientInstance().get("/rest/clouds/" + cloudId), CloudDTO.class).getData();
        assertStorages(numberOfStorage, cloudDTO.getCloud().getStorages(), expectedStorageTable);
    }

    public static void assertStorages(int numberOfStorage, Set<StorageTemplate> storages, DataTable expectedStorageTable) {
        Assert.assertEquals(numberOfStorage, storages.size());
        Set<StorageTemplate> expectedStorages = Sets.newHashSet();
        if (expectedStorageTable != null) {
            for (List<String> rows : expectedStorageTable.raw()) {
                String id = rows.get(0);
                String device = rows.get(1);
                long size = Long.parseLong(rows.get(2));
                StorageTemplate storage = new StorageTemplate();
                storage.setId(id);
                storage.setDevice(device);
                storage.setSize(size);
                expectedStorages.add(storage);
            }
        }
        Assert.assertEquals(expectedStorages, storages);
    }

    @When("^I remove the storage with name \"([^\"]*)\" from the cloud \"([^\"]*)\"$")
    public void I_remove_the_storage_with_name_from_the_cloud(String storageName, String cloudName) throws Throwable {
        String cloudId = Context.getInstance().getCloudId(cloudName);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete("/rest/clouds/" + cloudId + "/storages/" + storageName));
    }

    @Then("^The cloud with name \"([^\"]*)\" should not have any storage as resources$")
    public void The_cloud_with_name_should_not_have_any_storage_as_resources(String cloudName) throws Throwable {
        String cloudId = Context.getInstance().getCloudId(cloudName);
        CloudDTO cloudDTO = JsonUtil.read(Context.getRestClientInstance().get("/rest/clouds/" + cloudId), CloudDTO.class).getData();
        Assert.assertTrue(cloudDTO.getStorages() == null || cloudDTO.getStorages().isEmpty());
    }

    @When("^I match the storage with name \"([^\"]*)\" of the cloud \"([^\"]*)\" to the PaaS resource \"([^\"]*)\"$")
    public void I_match_the_storage_with_name_of_the_cloud_to_the_PaaS_resource(String storageName, String cloudName, String paaSResourceId) throws Throwable {
        String cloudId = Context.getInstance().getCloudId(cloudName);
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postUrlEncoded("/rest/clouds/" + cloudId + "/storages/" + storageName + "/resource",
                        Lists.<NameValuePair> newArrayList(new BasicNameValuePair("pasSResourceId", paaSResourceId))));
    }

    @And("^The cloud \"([^\"]*)\" should have storage mapping configuration as below:$")
    public void The_cloud_should_have_storage_mapping_configuration_as_below(String cloudName, DataTable expectedMappings) throws Throwable {
        new CloudDefinitionsSteps().I_get_the_cloud_by_id(cloudName);
        CloudDTO cloudDTO = JsonUtil.read(Context.getInstance().getRestResponse(), CloudDTO.class).getData();
        Set<MatchedStorageTemplate> actualStorages = Sets.newHashSet(cloudDTO.getStorages().values());
        Set<MatchedStorageTemplate> expectedStorages = Sets.newHashSet();
        for (List<String> rows : expectedMappings.raw()) {
            StorageTemplate storage = new StorageTemplate();
            String id = rows.get(0);
            long size = Long.parseLong(rows.get(1));
            String device = rows.get(2);
            storage.setId(id);
            storage.setDevice(device);
            storage.setSize(size);
            String pasSResourceId = rows.get(3);
            if (pasSResourceId.isEmpty()) {
                pasSResourceId = null;
            }
            expectedStorages.add(new MatchedStorageTemplate(storage, pasSResourceId));
        }
        Assert.assertEquals(expectedStorages, actualStorages);
    }

    @When("^I delete the mapping for the storage \"([^\"]*)\" of the cloud \"([^\"]*)\"$")
    public void I_delete_the_mapping_for_the_storage_of_the_cloud(String storageName, String cloudName) throws Throwable {
        String cloudId = Context.getInstance().getCloudId(cloudName);
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postUrlEncoded("/rest/clouds/" + cloudId + "/storages/" + storageName + "/resource",
                        Lists.<NameValuePair> newArrayList()));
    }

    @Then("^The cloud \"([^\"]*)\" should have empty storage mapping configuration$")
    public void The_cloud_should_have_empty_storage_mapping_configuration(String cloudName) throws Throwable {
        new CloudDefinitionsSteps().I_get_the_cloud_by_id(cloudName);
        CloudDTO cloudDTO = JsonUtil.read(Context.getInstance().getRestResponse(), CloudDTO.class).getData();
        Assert.assertTrue(cloudDTO.getStorages().isEmpty());
    }
}
