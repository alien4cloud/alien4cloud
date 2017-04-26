package alien4cloud.it.application;

import static alien4cloud.it.Context.getRestClientInstance;
import static alien4cloud.it.utils.TestUtils.nullable;

import org.apache.commons.lang3.StringUtils;

import alien4cloud.it.Context;
import alien4cloud.it.common.CommonStepDefinitions;
import alien4cloud.rest.application.model.CreateApplicationTopologyVersionRequest;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.java.en.When;

public class ApplicationTopologyVersionStepDefinitions {
    @When("^I (successfully\\s)?create an application topology version for application \"([^\"]*)\" version \"([^\"]*)\" with qualifier \"([^\"]*)\", description \"([^\"]*)\", topology template id \"([^\"]*)\" and previous version id \"([^\"]*)\"$")
    public void stepCreateApplicationTopologyVersion(String successfully, String applicationId, String version, String qualifier, String description,
            String topologyTemplateId, String fromVersionId) throws Throwable {
        createApplicationVersion(applicationId, nullable(version), nullable(qualifier), nullable(description), nullable(topologyTemplateId),
                nullable(fromVersionId));
        CommonStepDefinitions.validateIfNeeded(StringUtils.isNotBlank(successfully));
    }

    private void createApplicationVersion(String applicationId, String version, String qualifier, String description, String topologyTemplateId,
            String fromVersionId) throws Throwable {
        CreateApplicationTopologyVersionRequest request = new CreateApplicationTopologyVersionRequest(qualifier, description, topologyTemplateId,
                fromVersionId);
        Context.getInstance().registerRestResponse(getRestClientInstance()
                .postJSon("/rest/v1/applications/" + applicationId + "/versions/" + version + "/topologyVersions", JsonUtil.toString(request)));
    }

    @When("^I delete the application topology version for application \"(.*?)\", version id \"(.*?)\" with topology version id \"(.*?)\"$")
    public void stepDeleteApplicationTopologyVersion(String applicationId, String version, String topologyVersion) throws Throwable {
        Context.getInstance().registerRestResponse(
                getRestClientInstance().delete("/rest/v1/applications/" + applicationId + "/versions/" + version + "/topologyVersions/" + topologyVersion));
    }
}