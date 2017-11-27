package alien4cloud.it.provider;

import java.io.IOException;

import alien4cloud.it.provider.util.RuntimePropertiesUtil;
import org.jclouds.openstack.cinder.v1.domain.Volume;
import org.junit.Assert;

import alien4cloud.it.Context;
import alien4cloud.deployment.DeploymentTopologyDTO;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.utils.PropertyUtil;
import cucumber.api.java.en.And;

public class IAASStepDefinitions {

    private String getVolumeId(String propertyName, String nodeName, String appName) throws IOException {
        String topologyResponseText = Context.getRestClientInstance().get(
                "/rest/v1/applications/" + Context.getInstance().getApplicationId(appName) + "/environments/"
                        + Context.getInstance().getDefaultApplicationEnvironmentId(appName) + "/deployment-topology");
        RestResponse<DeploymentTopologyDTO> topologyResponse = JsonUtil.read(topologyResponseText, DeploymentTopologyDTO.class, Context.getJsonMapper());
        String volumeId = PropertyUtil.getScalarValue(topologyResponse.getData().getTopology().getNodeTemplates().get(nodeName).getProperties()
                .get(propertyName));
        Assert.assertNotNull(volumeId);
        int indexOfEndRegion = volumeId.indexOf('/');
        if (indexOfEndRegion > 0) {
            volumeId = volumeId.substring(indexOfEndRegion + 1);
        }
        return volumeId;
    }

    @And("^I should have a volume on OpenStack with id defined in property \"([^\"]*)\" of the node \"([^\"]*)\" for \"([^\"]*)\"$")
    public void I_should_have_a_volume_on_OpenStack_with_id_defined_in_property_of_the_node(String propertyName, String nodeName, String appName)
            throws Throwable {
        Assert.assertNotNull(Context.getInstance().getOpenStackClient().getVolume(getVolumeId(propertyName, nodeName, appName)));
    }

    @And("^I should have a volume on OpenStack with id defined in runtime property \"([^\"]*)\" of the node \"([^\"]*)\"$")
    public void I_should_have_a_volume_on_OpenStack_with_id_defined_in_runtime_property_of_the_node(String propertyName, String nodeName)
            throws Throwable {
        String externalId = RuntimePropertiesUtil.getProperty(nodeName, propertyName);
        Context.getInstance().setCurrentExternalId(externalId);
        Assert.assertNotNull(Context.getInstance().getOpenStackClient().getVolume(externalId));
    }

    @And("^I should not have a volume on OpenStack with id defined in runtime property \"([^\"]*)\" of the node \"([^\"]*)\"$")
    public void I_should_not_have_a_volume_on_OpenStack_with_id_defined_in_property_of_the_node(String propertyName, String nodeName)
            throws Throwable {
        String externalId = Context.getInstance().getCurrentExternalId();
        Assert.assertNull(Context.getInstance().getOpenStackClient().getVolume(externalId));
    }

    @And("^I should have a volume on AWS with id defined in property \"([^\"]*)\" of the node \"([^\"]*)\" for \"([^\"]*)\"$")
    public void I_should_have_a_volume_on_AWS_with_id_defined_in_property_of_the_node(String propertyName, String nodeName, String appName) throws Throwable {
        Assert.assertNotNull(Context.getInstance().getAwsClient().getVolume(getVolumeId(propertyName, nodeName, appName)));
    }

    @And("^I should have a volume on AWS with id defined in runtime property \"([^\"]*)\" of the node \"([^\"]*)\"$")
    public void I_should_have_a_volume_on_AWS_with_id_defined_in_runtime_property_of_the_node(String propertyName, String nodeName) throws Throwable {
        String externalId = RuntimePropertiesUtil.getProperty(nodeName, propertyName);
        Context.getInstance().setCurrentExternalId(externalId);
        Assert.assertNotNull(Context.getInstance().getAwsClient().getVolume(externalId));
    }

    @And("^I should not have a volume on AWS with id defined in runtime property \"([^\"]*)\" of the node \"([^\"]*)\"$")
    public void I_should_not_have_a_volume_on_AWS_with_id_defined_in_runtime_property_of_the_node(String propertyName, String nodeName) throws Throwable {
        String externalId = Context.getInstance().getCurrentExternalId();
        Assert.assertNull(Context.getInstance().getAwsClient().getVolume(externalId));
    }

    @And("^I should have volumes on OpenStack with ids defined in property \"([^\"]*)\" of the node \"([^\"]*)\" for \"([^\"]*)\"$")
    public void I_should_have_volumes_on_OpenStack_with_ids_defined_in_property_of_the_node(String propertyName, String nodeName, String appName)
            throws Throwable {
        String ids = getVolumeId(propertyName, nodeName, appName);
        String[] idsArr = ids.split(",");
        for (String id : idsArr) {
            Volume volume = Context.getInstance().getOpenStackClient().getVolume(id);
            Assert.assertNotNull(volume);
        }
    }

    @And("^I delete the volume on OpenStack with id defined in property \"([^\"]*)\" of the node \"([^\"]*)\" for \"([^\"]*)\"$")
    public void I_delete_the_volume_on_OpenStack_with_id_defined_in_property_of_the_node(String propertyName, String nodeName, String appName) throws Throwable {
        Assert.assertTrue(Context.getInstance().getOpenStackClient().deleteVolume(getVolumeId(propertyName, nodeName, appName)));
    }

    @And("^I delete the volume on AWS with id defined in property \"([^\"]*)\" of the node \"([^\"]*)\" for \"([^\"]*)\"$")
    public void I_delete_the_volume_on_AWS_with_id_defined_in_property_of_the_node(String propertyName, String nodeName, String appName) throws Throwable {
        Context.getInstance().getAwsClient().deleteVolume(getVolumeId(propertyName, nodeName, appName));
    }

    @And("^I delete volumes on OpenStack with ids defined in property \"([^\"]*)\" of the node \"([^\"]*)\" for \"([^\"]*)\"$")
    public void I_delete_volumes_on_OpenStack_with_ids_defined_in_property_of_the_node(String propertyName, String nodeName, String appName) throws Throwable {
        String ids = getVolumeId(propertyName, nodeName, appName);
        String[] idsArr = ids.split(",");
        for (String id : idsArr) {
            Assert.assertTrue(Context.getInstance().getOpenStackClient().deleteVolume(id));
        }
    }
}
