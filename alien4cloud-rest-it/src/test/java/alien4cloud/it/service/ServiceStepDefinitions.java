package alien4cloud.it.service;

import static alien4cloud.it.Context.getRestClientInstance;
import static alien4cloud.it.utils.TestUtils.nullable;

import java.io.IOException;

import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.it.Context;
import alien4cloud.model.service.ServiceResource;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.service.model.CreateServiceResourceRequest;
import alien4cloud.rest.utils.JsonUtil;
import com.google.common.collect.Lists;
import cucumber.api.java.en.When;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.message.BasicNameValuePair;

@Slf4j
public class ServiceStepDefinitions {
    public static String LAST_CREATED_ID;

    @When("^I create a service with name \"(.*?)\", version \"(.*?)\", type \"(.*?)\", archive version \"(.*?)\"$")
    public void createService(String serviceName, String serviceVersion, String type, String archiveVersion) throws Throwable {
        CreateServiceResourceRequest request = new CreateServiceResourceRequest(nullable(serviceName), nullable(serviceVersion), nullable(type),
                nullable(archiveVersion));
        Context.getInstance().registerRestResponse(getRestClientInstance().postJSon("/rest/v1/services/", JsonUtil.toString(request)));
        try {
            LAST_CREATED_ID = JsonUtil.read(Context.getInstance().getRestResponse(), String.class).getData();
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
        // register for SPEL
        try {
            RestResponse<ServiceResource> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), ServiceResource.class);
            if (restResponse.getData() != null) {
                Context.getInstance().buildEvaluationContext(restResponse.getData());
            }
        } catch (IOException e) {
            // Registration is optional
        }
    }

    @When("^I create (\\d+) services each of them having (\\d+) versions from type \"(.*?)\", archive version \"(.*?)\"$")
    public void createMultipleServices(int serviceCount, int versionCount, String type, String archiveVersion) throws Throwable {
        String nameRoot = "Service ";
        String versionRoot = "1.0.";
        for (int i = 0; i < serviceCount; i++) {
            String serviceName = nameRoot + i;
            for (int j = 0; j < versionCount; j++) {
                String serviceVersion = versionRoot + j;
                createService(serviceName, serviceVersion, type, archiveVersion);
            }
        }
    }

    @When("^I list services$")
    public void listServices() throws Throwable {
        Context.getInstance().registerRestResponse(getRestClientInstance().get("/rest/v1/services/"));
        // register for SPEL
        try {
            RestResponse<GetMultipleDataResult> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), GetMultipleDataResult.class);
            Context.getInstance().buildEvaluationContext(restResponse.getData());
        } catch (Throwable e) {
            // Registration is optional
        }
    }

    @When("^I list services from (\\d+) count (\\d+)$")
    public void listServices(int from, int count) throws Throwable {
        Context.getInstance().registerRestResponse(getRestClientInstance().getUrlEncoded("/rest/v1/services/",
                Lists.newArrayList(new BasicNameValuePair("from", String.valueOf(from)), new BasicNameValuePair("count", String.valueOf(count)))));
    }
}
