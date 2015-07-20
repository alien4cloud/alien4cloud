package alien4cloud.it.provider;

import java.io.IOException;

import org.jclouds.openstack.cinder.v1.domain.Volume;
import org.junit.Assert;

import alien4cloud.it.Context;
import alien4cloud.it.utils.JsonTestUtil;
import alien4cloud.paas.function.FunctionEvaluator;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.topology.TopologyDTO;
import cucumber.api.java.en.And;

public class OpenStacksStepDefinitions {

    private String getVolumeId(String propertyName, String nodeName) throws IOException {
        String topologyResponseText = Context.getRestClientInstance().get("/rest/topologies/" + Context.getInstance().getTopologyId());
        RestResponse<TopologyDTO> topologyResponse = JsonTestUtil.read(topologyResponseText, TopologyDTO.class);
        String volumeId = FunctionEvaluator.getScalarValue(topologyResponse.getData().getTopology().getNodeTemplates().get(nodeName).getProperties()
                .get(propertyName));
        int indexOfEndRegion = volumeId.indexOf('/');
        if (indexOfEndRegion > 0) {
            volumeId = volumeId.substring(indexOfEndRegion + 1);
        }
        return volumeId;
    }

    @And("^I should have a volume on OpenStack with id defined in property \"([^\"]*)\" of the node \"([^\"]*)\"$")
    public void I_should_have_a_volume_on_OpenStack_with_id_defined_in_property_of_the_node(String propertyName, String nodeName) throws Throwable {
        Volume volume = Context.getInstance().getOpenStackClient().getVolume(getVolumeId(propertyName, nodeName));
        Assert.assertNotNull(volume);
    }

    @And("^I delete the volume on OpenStack with id defined in property \"([^\"]*)\" of the node \"([^\"]*)\"$")
    public void I_delete_the_volume_on_OpenStack_with_id_defined_in_property_of_the_node(String propertyName, String nodeName) throws Throwable {
        Assert.assertTrue(Context.getInstance().getOpenStackClient().deleteVolume(getVolumeId(propertyName, nodeName)));
    }
}
