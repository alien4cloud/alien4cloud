package alien4cloud.it.service;

import static alien4cloud.it.Context.getRestClientInstance;
import static alien4cloud.it.utils.TestUtils.nullAsString;
import static alien4cloud.it.utils.TestUtils.nullable;

import java.io.IOException;
import java.util.List;

import alien4cloud.dao.model.FacetedSearchResult;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.apache.commons.lang.StringUtils;
import org.apache.http.message.BasicNameValuePair;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.it.Context;
import alien4cloud.it.common.CommonStepDefinitions;
import alien4cloud.model.service.ServiceResource;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.service.model.CreateServiceResourceRequest;
import alien4cloud.rest.service.model.NodeInstanceDTO;
import alien4cloud.rest.service.model.PatchServiceResourceRequest;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.java.en.And;
import cucumber.api.java.en.When;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServiceStepDefinitions {
    public static String LAST_CREATED_ID;

    @When("^I (successfully\\s)?create a service with name \"(.*?)\", version \"(.*?)\", type \"(.*?)\", archive version \"(.*?)\"$")
    public void createService(String successfully, String serviceName, String serviceVersion, String type, String archiveVersion) throws Throwable {
        CreateServiceResourceRequest request = new CreateServiceResourceRequest(nullable(serviceName), nullable(serviceVersion), nullable(type),
                nullable(archiveVersion));
        Context.getInstance().registerRestResponse(getRestClientInstance().postJSon("/rest/v1/services/", JsonUtil.toString(request)));

        CommonStepDefinitions.validateIfNeeded(StringUtils.isNotBlank(successfully));
        try {
            LAST_CREATED_ID = JsonUtil.read(Context.getInstance().getRestResponse(), ServiceResource.class).getData().getId();
            Context.getInstance().registerService(LAST_CREATED_ID, serviceName);
        } catch (Throwable t) {
        }

    }

    @When("^I get the last created service$")
    public void getLastService() throws Throwable {
        getService(LAST_CREATED_ID);
    }

    @When("^I get a service with id \"(.*?)\"$")
    public void getService(String id) throws Throwable {
        Context.getInstance().registerRestResponse(getRestClientInstance().get("/rest/v1/services/" + id));
        registerServiceResultForSPEL();

    }

    @When("^I create (\\d+) services each of them having (\\d+) versions from type \"(.*?)\", archive version \"(.*?)\"$")
    public void createMultipleServices(int serviceCount, int versionCount, String type, String archiveVersion) throws Throwable {
        String nameRoot = "Service ";
        String versionRoot = "1.0.";
        for (int i = 0; i < serviceCount; i++) {
            String serviceName = nameRoot + i;
            for (int j = 0; j < versionCount; j++) {
                String serviceVersion = versionRoot + j;
                createService("successfully", serviceName, serviceVersion, type, archiveVersion);
            }
        }
    }

    @When("^I list services$")
    public void listServices() throws Throwable {
        Context.getInstance().registerRestResponse(getRestClientInstance().get("/rest/v1/services/"));
        registerListResultForSpel();
    }

    @When("^I list services from (\\d+) count (\\d+)$")
    public void listServices(int from, int count) throws Throwable {
        Context.getInstance().registerRestResponse(getRestClientInstance().getUrlEncoded("/rest/v1/services/",
                Lists.newArrayList(new BasicNameValuePair("from", String.valueOf(from)), new BasicNameValuePair("count", String.valueOf(count)))));
    }

    @And("^I register service list result for SPEL$")
    public void registerListResultForSpel() {
        // register for SPEL
        try {
            RestResponse<FacetedSearchResult> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), FacetedSearchResult.class);
            Context.getInstance().buildEvaluationContext(restResponse.getData());
        } catch (Throwable e) {
            // Registration is optional
            log.error("", e);
        }
    }

    public static void registerServiceResultForSPEL() {
        try {
            RestResponse<ServiceResource> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), ServiceResource.class, Context.getJsonMapper());
            if (restResponse.getData() != null) {
                Context.getInstance().buildEvaluationContext(restResponse.getData());
            }
        } catch (IOException e) {
            // Registration is optional
        }
    }

    @When("^I authorize these locations to use the service \"([^\"]*)\"$")
    public void iAuthorizeTheseLocationsToUseTheService(String serviceName, List<String> locations) throws Throwable {
        PatchServiceResourceRequest request = new PatchServiceResourceRequest();
        request.setLocationIds(locations.stream().map(name -> {
            String orchestratorId = Context.getInstance().getOrchestratorId(name.split("/")[0].trim());
            if (orchestratorId == null) {
                return nullAsString(null);
            }
            String locationName = name.split("/")[1].trim();
            return nullAsString(Context.getInstance().getLocationId(orchestratorId, locationName));
        }).toArray(String[]::new));
        String serviceId = Context.getInstance().getServiceId(serviceName);
        String response = Context.getRestClientInstance().patchJSon("/rest/v1/services/" + nullAsString(serviceId), JsonUtil.toString(request));
        Context.getInstance().registerRestResponse(response);
    }

    @And("^I set the property \"([^\"]*)\" to \"([^\"]*)\" for the service \"([^\"]*)\"$")
    public void iSetThePropertyToForTheService(String propertyName, String propertyValue, String serviceName) throws Throwable {
        String serviceId = Context.getInstance().getServiceId(serviceName);
        PatchServiceResourceRequest request = new PatchServiceResourceRequest();
        NodeInstanceDTO nodeInstance = new NodeInstanceDTO();
        nodeInstance.setProperties(Maps.newHashMap());
        nodeInstance.getProperties().put(propertyName, new ScalarPropertyValue(propertyValue));
        request.setNodeInstance(nodeInstance);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().patchJSon("/rest/v1/services/" + serviceId, JsonUtil.toString(request)));
    }

    @And("^I (successfully\\s)?start the service \"([^\"]*)\"$")
    public void iStartTheService(String successfully, String serviceName) throws Throwable {
        String serviceId = Context.getInstance().getServiceId(serviceName);
        PatchServiceResourceRequest request = new PatchServiceResourceRequest();
        NodeInstanceDTO nodeInstance = new NodeInstanceDTO();
        nodeInstance.setAttributeValues(Maps.newHashMap());
        nodeInstance.getAttributeValues().put("state", "started");
        request.setNodeInstance(nodeInstance);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().patchJSon("/rest/v1/services/" + serviceId, JsonUtil.toString(request)));

        CommonStepDefinitions.validateIfNeeded(StringUtils.isNotBlank(successfully));
    }
}
