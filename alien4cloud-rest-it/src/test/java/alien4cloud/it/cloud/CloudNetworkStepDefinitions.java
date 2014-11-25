package alien4cloud.it.cloud;

import java.util.List;
import java.util.Set;

import org.junit.Assert;

import alien4cloud.it.Context;
import alien4cloud.model.cloud.Network;
import alien4cloud.rest.cloud.CloudDTO;
import alien4cloud.rest.utils.JsonUtil;

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
        Network network = new Network();
        network.setNetworkName(name);
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

    public static void assertNetworks(int numberOfNetworks, Set<Network> networks, DataTable expectedNetworksTable) {
        Assert.assertEquals(numberOfNetworks, networks.size());
        Set<Network> expectedNetworks = Sets.newHashSet();
        if (expectedNetworksTable != null) {
            for (List<String> rows : expectedNetworksTable.raw()) {
                String name = rows.get(0);
                String cidr = rows.get(1);
                int ipVersion = Integer.parseInt(rows.get(2));
                String gateWay = rows.get(3);
                Network network = new Network();
                network.setNetworkName(name);
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
}
