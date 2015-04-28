package alien4cloud.it.cloud;

import java.util.List;
import java.util.Set;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Assert;

import alien4cloud.it.Context;
import alien4cloud.model.cloud.AvailabilityZone;
import alien4cloud.rest.utils.JsonUtil;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import cucumber.api.DataTable;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Then;

public class CloudAVZStepDefinitions {

    @And("^I add the availability zone with id \"([^\"]*)\" and description \"([^\"]*)\" to the cloud \"([^\"]*)\"$")
    public void I_add_the_availability_zone_with_id_and_description_to_the_cloud(String avzId, String avzDesc, String cloudName) throws Throwable {
        String cloudId = Context.getInstance().getCloudId(cloudName);
        AvailabilityZone avz = new AvailabilityZone(avzId, avzDesc);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/clouds/" + cloudId + "/zones", JsonUtil.toString(avz)));
    }

    @And("^I match the availability zone with name \"([^\"]*)\" of the cloud \"([^\"]*)\" to the PaaS resource \"([^\"]*)\"$")
    public void I_match_the_availability_zone_with_name_of_the_cloud_to_the_PaaS_resource(String avzId, String cloudName, String paaSResourceId)
            throws Throwable {
        String cloudId = Context.getInstance().getCloudId(cloudName);
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postUrlEncoded("/rest/clouds/" + cloudId + "/zones/" + avzId + "/resource",
                        Lists.<NameValuePair> newArrayList(new BasicNameValuePair("pasSResourceId", paaSResourceId))));
    }

    public static void assertZones(int numberOfZones, Set<AvailabilityZone> zones, DataTable expectedZoneTable) {
        Assert.assertEquals(numberOfZones, zones.size());
        Set<AvailabilityZone> expectedZones = Sets.newHashSet();
        if (expectedZoneTable != null) {
            for (List<String> rows : expectedZoneTable.raw()) {
                expectedZones.add(new AvailabilityZone(rows.get(0), rows.get(1)));
            }
        }
        Assert.assertEquals(expectedZones, zones);
    }

    @Then("^I remove the availability zone with name \"([^\"]*)\" from the cloud \"([^\"]*)\"$")
    public void I_remove_the_availability_zone_with_name_from_the_cloud(String avzId, String cloudName) throws Throwable {
        String cloudId = Context.getInstance().getCloudId(cloudName);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete("/rest/clouds/" + cloudId + "/zones/" + avzId));
    }
}
