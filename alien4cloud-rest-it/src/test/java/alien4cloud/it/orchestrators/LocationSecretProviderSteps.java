package alien4cloud.it.orchestrators;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.junit.Assert;

import alien4cloud.it.Context;
import alien4cloud.it.utils.ConfigurationStringUtils;
import alien4cloud.model.secret.SecretProviderConfiguration;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.orchestrator.model.UpdateLocationRequest;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.DataTable;
import cucumber.api.java.en.And;

/**
 * The steps for the location secret provider management
 */
public class LocationSecretProviderSteps {

    private static final String SECRET_BASE_URL = "/rest/v1/secret";

    @And("^I should have a secret provider named \"([^\"]*)\" in the list of secret providers$")
    public void iShouldHaveASecretProviderNamedInTheListOfSecretProviders(String providerName) throws Throwable {
        String restUrl = SECRET_BASE_URL + "/plugin-names";
        String resp = Context.getRestClientInstance().get(restUrl);
        RestResponse<List> response = JsonUtil.read(resp, List.class);
        Assert.assertEquals(true, response.getData().contains(providerName));
    }

    @And("^I use \"([^\"]*)\" as the secret provider and I update the configuration of secret provider related to the location \"([^\"]*)\" of the orchestrator \"([^\"]*)\"$")
    public void iUseAsTheSecretProviderAndIUpdateTheConfigurationOfSecretProviderRelatedToTheLocationOfTheOrchestrator(String pluginName, String locationName,
            String orchestratorName, DataTable table) throws Throwable {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        String locationId = Context.getInstance().getLocationId(orchestratorId, locationName);
        String restUrl = String.format("/rest/v1/orchestrators/%s/locations/%s", orchestratorId, locationId);
        SecretProviderConfiguration secretProviderConfiguration = new SecretProviderConfiguration();
        secretProviderConfiguration.setPluginName(pluginName);
        Map<String, Object> configuration = ConfigurationStringUtils.dataTableToMap(table);
        // Read the certificate file by passing the path of this file
        configuration.put("certificate", new String(Files.readAllBytes(Paths.get((String) configuration.get("certificate")))));
        secretProviderConfiguration.setConfiguration(configuration);
        UpdateLocationRequest request = new UpdateLocationRequest();
        request.setName(locationName);
        request.setSecretProviderConfiguration(secretProviderConfiguration);
        String restResponse = Context.getRestClientInstance().putJSon(restUrl, JsonUtil.toString(request));
        Context.getInstance().registerRestResponse(restResponse);
    }

}
