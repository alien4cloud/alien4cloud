package alien4cloud.it.application.deployment;

import org.junit.Assert;

import alien4cloud.it.Context;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.deployment.DeploymentTopologyDTO;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.java.en.Then;

public class NodeMatchingStepDefinitions {
    @Then("^Available substitution should contains (\\d+) proposal including (\\d+) service$")
    public void checkSubstitutionCounts(int proposals, int services) throws Throwable {
        RestResponse<DeploymentTopologyDTO> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), DeploymentTopologyDTO.class);
        Assert.assertEquals(proposals, restResponse.getData().getAvailableSubstitutions().getSubstitutionsTemplates().size());
        int actualServices = 0;
        for (LocationResourceTemplate resourceTemplate : restResponse.getData().getAvailableSubstitutions().getSubstitutionsTemplates().values()) {
            if (resourceTemplate.isService()) {
                actualServices++;
            }
        }
        Assert.assertEquals(services, actualServices);
    }
}