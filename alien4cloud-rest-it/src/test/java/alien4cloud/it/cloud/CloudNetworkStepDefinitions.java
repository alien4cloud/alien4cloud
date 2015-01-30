package alien4cloud.it.cloud;

import java.util.List;
import java.util.Set;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Assert;

import alien4cloud.it.Context;
import alien4cloud.model.cloud.MatchedNetworkTemplate;
import alien4cloud.model.cloud.NetworkTemplate;
import alien4cloud.rest.cloud.CloudDTO;
import alien4cloud.rest.utils.JsonUtil;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import cucumber.api.DataTable;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class CloudNetworkStepDefinitions {

    @When("^I add the network with name \"([^\"]*)\" and CIDR \"([^\"]*)\" and IP version (\\d+) and gateway \"([^\"]*)\" to the cloud \"([^\"]*)\"$")
    public void I_add_the_network_with_name_and_CIDR_and_IP_version_and_gateway_to_the_cloud(String name, String cidr, int ipVersion, String gateWay,
            String cloudName) throws Throwable {
        String cloudId = Context.getInstance().getCloudId(cloudName);
        NetworkTemplate network = new NetworkTemplate();
        network.setId(name);
        network.setIpVersion(ipVersion);
        network.setCidr(cidr);
        network.setGatewayIp(gateWay);
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postJSon("/rest/clouds/" + cloudId + "/networks", JsonUtil.toString(network)));
    }

    @And("^The cloud with name \"([^\"]*)\" should have (\\d+) networks as resources:$")
    public void The_cloud_with_name_should_have_networks_as_resources(String cloudName, int numberOfNetworks, DataTable expectedNetworksTable) throws Throwable {
        String cloudId = Context.getInstance().getCloudId(cloudName);
        CloudDTO cloudDTO = JsonUtil.read(Context.getRestClientInstance().get("/rest/clouds/" + cloudId), CloudDTO.class).getData();
        assertNetworks(numberOfNetworks, cloudDTO.getCloud().getNetworks(), expectedNetworksTable);
    }

    public static void assertNetworks(int numberOfNetworks, Set<NetworkTemplate> networks, DataTable expectedNetworksTable) {
        Assert.assertEquals(numberOfNetworks, networks.size());
        Set<NetworkTemplate> expectedNetworks = Sets.newHashSet();
        if (expectedNetworksTable != null) {
            for (List<String> rows : expectedNetworksTable.raw()) {
                String name = rows.get(0);
                String cidr = rows.get(1);
                int ipVersion = Integer.parseInt(rows.get(2));
                String gateWay = rows.get(3);
                NetworkTemplate network = new NetworkTemplate();
                network.setId(name);
                network.setIpVersion(ipVersion);
                network.setCidr(cidr);
                network.setGatewayIp(gateWay);
                expectedNetworks.add(network);
            }
        }
        Assert.assertEquals(expectedNetworks, networks);
    }

    @When("^I remove the network with name \"([^\"]*)\" from the cloud \"([^\"]*)\"$")
    public void I_remove_the_network_with_name_from_the_cloud(String networkName, String cloudName) throws Throwable {
        String cloudId = Context.getInstance().getCloudId(cloudName);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete("/rest/clouds/" + cloudId + "/networks/" + networkName));
    }

    @Then("^The cloud with name \"([^\"]*)\" should not have any network as resources$")
    public void The_cloud_with_name_should_not_have_any_network_as_resources(String cloudName) throws Throwable {
        String cloudId = Context.getInstance().getCloudId(cloudName);
        CloudDTO cloudDTO = JsonUtil.read(Context.getRestClientInstance().get("/rest/clouds/" + cloudId), CloudDTO.class).getData();
        Assert.assertTrue(cloudDTO.getNetworks() == null || cloudDTO.getNetworks().isEmpty());
    }

    @When("^I match the network with name \"([^\"]*)\" of the cloud \"([^\"]*)\" to the PaaS resource \"([^\"]*)\"$")
    public void I_match_the_network_with_name_of_the_cloud_to_the_PaaS_resource(String networkName, String cloudName, String paaSResourceId) throws Throwable {
        String cloudId = Context.getInstance().getCloudId(cloudName);
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postUrlEncoded("/rest/clouds/" + cloudId + "/networks/" + networkName + "/resource",
                        Lists.<NameValuePair> newArrayList(new BasicNameValuePair("resourceId", paaSResourceId))));
    }

//    @And("^The cloud \"([^\"]*)\" should have network mapping configuration as below:$")
//    public void The_cloud_should_have_network_mapping_configuration_as_below(String cloudName, DataTable expectedMappings) throws Throwable {
//        new CloudDefinitionsSteps().I_get_the_cloud_by_id(cloudName);
//        CloudDTO cloudDTO = JsonUtil.read(Context.getInstance().getRestResponse(), CloudDTO.class).getData();
//        Assert.assertNotNull(cloudDTO.getCloudResourceMatcher());
//        Assert.assertNotNull(cloudDTO.getCloudResourceMatcher().getMatcherConfig());
//        Assert.assertNotNull(cloudDTO.getCloudResourceMatcher().getMatcherConfig().getNetworkMapping());
//        Set<MatchedNetworkTemplate> actualNetworks = Sets.newHashSet(cloudDTO.getCloudResourceMatcher().getMatcherConfig().getMatchedNetworks());
//        Set<MatchedNetworkTemplate> expectedNetworks = Sets.newHashSet();
//        for (List<String> rows : expectedMappings.raw()) {
//            NetworkTemplate network = new NetworkTemplate();
//            String networkName = rows.get(0);
//            network.setId(networkName);
//            String cidr = rows.get(1);
//            network.setCidr(cidr);
//            int ipVersion = Integer.parseInt(rows.get(2));
//            network.setIpVersion(ipVersion);
//            String gatewayIp = rows.get(3);
//            network.setGatewayIp(gatewayIp);
//            String pasSResourceId = rows.get(4);
//            expectedNetworks.add(new MatchedNetworkTemplate(network, pasSResourceId));
//        }
//        Assert.assertEquals(expectedNetworks, actualNetworks);
//    }

    @When("^I delete the mapping for the network \"([^\"]*)\" of the cloud \"([^\"]*)\"$")
    public void I_delete_the_mapping_for_the_network_of_the_cloud(String networkName, String cloudName) throws Throwable {
        String cloudId = Context.getInstance().getCloudId(cloudName);
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postUrlEncoded("/rest/clouds/" + cloudId + "/networks/" + networkName + "/resource",
                        Lists.<NameValuePair> newArrayList()));
    }

//    @Then("^The cloud \"([^\"]*)\" should have empty network mapping configuration$")
//    public void The_cloud_should_have_empty_network_mapping_configuration(String cloudName) throws Throwable {
//        new CloudDefinitionsSteps().I_get_the_cloud_by_id(cloudName);
//        CloudDTO cloudDTO = JsonUtil.read(Context.getInstance().getRestResponse(), CloudDTO.class).getData();
//        Assert.assertTrue(cloudDTO.getCloudResourceMatcher() == null || cloudDTO.getCloudResourceMatcher().getMatcherConfig() == null
//                || cloudDTO.getCloudResourceMatcher().getMatcherConfig().getMatchedNetworks() == null
//                || cloudDTO.getCloudResourceMatcher().getMatcherConfig().getMatchedNetworks().isEmpty());
//    }
}
