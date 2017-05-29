package alien4cloud.it.setup;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import alien4cloud.git.RepositoryManager;
import alien4cloud.it.Context;
import alien4cloud.it.common.CommonStepDefinitions;
import alien4cloud.it.common.SecuredResourceStepDefinition;
import alien4cloud.it.csars.UploadCSARSStepDefinition;
import alien4cloud.it.orchestrators.LocationsDefinitionsSteps;
import alien4cloud.it.orchestrators.OrchestrationLocationResourceSteps;
import alien4cloud.it.orchestrators.OrchestratorsDefinitionsSteps;
import alien4cloud.it.plugin.PluginDefinitionsSteps;
import alien4cloud.it.security.AuthenticationStepDefinitions;
import alien4cloud.it.users.UsersDefinitionsSteps;
import alien4cloud.utils.FileUtil;
import cucumber.api.DataTable;
import cucumber.api.PendingException;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;

public class SetupStepDefinitions {
    private final static RepositoryManager REPOSITORY_MANAGER = new RepositoryManager();
    private static final CommonStepDefinitions COMMON_STEP_DEFINITIONS = new CommonStepDefinitions();

    @And("^I checkout the git archive from url \"([^\"]*)\" branch \"([^\"]*)\"$")
    public void I_checkout_the_git_archive_from_url_branch(String gitURL, String branch) throws Throwable {
        String localDirectoryName = gitURL.substring(gitURL.lastIndexOf('/') + 1);
        if (localDirectoryName.endsWith(Context.GIT_URL_SUFFIX)) {
            localDirectoryName = localDirectoryName.substring(0, localDirectoryName.length() - Context.GIT_URL_SUFFIX.length());
        }
        REPOSITORY_MANAGER.cloneOrCheckout(Context.GIT_ARTIFACT_TARGET_PATH, gitURL, branch, localDirectoryName);
    }

    public void uploadArchive(Path source) throws Throwable {
        Path csarTargetPath = Context.CSAR_TARGET_PATH.resolve(source.getFileName() + ".csar");
        FileUtil.zip(source, csarTargetPath);
        Context.getInstance()
                .registerRestResponse(Context.getRestClientInstance().postMultipart("/rest/v1/csars", "file", Files.newInputStream(csarTargetPath)));
        COMMON_STEP_DEFINITIONS.I_should_receive_a_RestResponse_with_no_error();
    }

    @And("^I (successfully\\s)?upload the git archive \"([^\"]*)\"$")
    public void I_upload_the_git_archive(String successfully, String folderToUpload) throws Throwable {
        Path csarSourceFolder = Context.GIT_ARTIFACT_TARGET_PATH.resolve(folderToUpload);
        uploadArchive(csarSourceFolder);
        CommonStepDefinitions.validateIfNeeded(org.apache.commons.lang.StringUtils.isNotBlank(successfully));
    }

    @Given("^I setup alien with$")
    public void i_setup_alien_with(DataTable params) throws Throwable {
        List<List<String>> rawParams = params.raw();

        for (List<String> paramRow : rawParams) {
            switch (paramRow.get(0)) {
            case "user":
                initUser(paramRow);
                break;
            case "archives":
                initArchives(paramRow);
                break;
            case "orchestrators":
                initOrchestrators(paramRow);
                break;
            case "location":
                initLocation(paramRow);
                break;
            case "resource":
                initLocationResource(paramRow);
                break;
            case "resourcesAuth":
                initLocationResourcesAuth(paramRow);
                break;
            default:
                throw new PendingException("Cannot initialize " + paramRow.get(0));
            }
        }
    }

    /** Users INIT */
    private final static AuthenticationStepDefinitions AUTHENTICATION_STEP_DEFINITIONS = new AuthenticationStepDefinitions();
    private final static UsersDefinitionsSteps USERS_DEFINITIONS_STEPS = new UsersDefinitionsSteps();

    private void initUser(List<String> userDef) throws Throwable {
        String[] userDefinition = userDef.get(1).split(",");
        AUTHENTICATION_STEP_DEFINITIONS.There_is_a_user_in_the_system(userDefinition[0]);
        for (int j = 1; j < userDefinition.length; j++) {
            USERS_DEFINITIONS_STEPS.I_add_a_role_to_user(userDefinition[j], userDefinition[0]);
        }
    }

    /** Archives INIT */
    private final static UploadCSARSStepDefinition UPLOAD_CSARS_STEP_DEFINITION = new UploadCSARSStepDefinition();

    private void initArchives(List<String> archivesDef) throws Throwable {
        String[] archiveDefinitions = archivesDef.get(1).split(",");

        for (int i = 0; i < archiveDefinitions.length; i++) {
            UPLOAD_CSARS_STEP_DEFINITION.uploadArchive(archiveDefinitions[i]);
        }
    }

    /** Orchestrator INIT */
    private final static PluginDefinitionsSteps PLUGIN_DEFINITIONS_STEPS = new PluginDefinitionsSteps();
    private final static OrchestratorsDefinitionsSteps ORCHESTRATORS_DEFINITIONS_STEPS = new OrchestratorsDefinitionsSteps();
    private final static String MOCK_PLUGIN_NAME = "alien4cloud-mock-paas-provider";
    private final static String MOCK_PLUGIN_BEAN = "mock-orchestrator-factory";

    private void initOrchestrators(List<String> orchestratorDefinitions) throws Throwable {
        // Upload mock plugin
        PLUGIN_DEFINITIONS_STEPS.I_upload_a_plugin();

        for (int i = 1; i < orchestratorDefinitions.size(); i++) {
            if (orchestratorDefinitions.get(i).isEmpty()) {
                return;
            }
            String[] orchestratorDefinition = orchestratorDefinitions.get(i).split(",");
            ORCHESTRATORS_DEFINITIONS_STEPS.I_create_an_orchestrator_named_and_plugin_id_and_bean_name("successfully", orchestratorDefinition[0],
                    MOCK_PLUGIN_NAME, MOCK_PLUGIN_BEAN);
            ORCHESTRATORS_DEFINITIONS_STEPS.I_enable_the_orchestrator("successfully", orchestratorDefinition[0]);
        }
    }

    /** Location INIT */
    private final static LocationsDefinitionsSteps LOCATIONS_DEFINITIONS_STEPS = new LocationsDefinitionsSteps();
    private final static SecuredResourceStepDefinition SECURED_RESOURCE_STEP_DEFINITION = new SecuredResourceStepDefinition();

    private void initLocation(List<String> locationDef) throws Throwable {
        String[] locationDefinition = locationDef.get(1).split(",");
        LOCATIONS_DEFINITIONS_STEPS.I_create_a_location_named_and_infrastructure_type_to_the_orchestrator(locationDefinition[1], locationDefinition[2],
                locationDefinition[0]);
        for (int i = 3; i < locationDefinition.length; i++) {
            SECURED_RESOURCE_STEP_DEFINITION.iGrantAccessToTheResourceTypeNamedToTheUser("", "LOCATION", locationDefinition[1], locationDefinition[i]);
        }
    }

    /** Location Resource INIT */
    private final static OrchestrationLocationResourceSteps ORCHESTRATION_LOCATION_RESOURCE_STEPS = new OrchestrationLocationResourceSteps();

    private void initLocationResource(List<String> locationResourceDef) throws Throwable {
        String[] locationResourceDefinition = locationResourceDef.get(1).split(",");
        String orchestratorName = locationResourceDefinition[0];
        String locationName = locationResourceDefinition[1];
        String resourceType = locationResourceDefinition[2];
        String resourceName = locationResourceDefinition[3];

        ORCHESTRATION_LOCATION_RESOURCE_STEPS.I_create_a_resource_of_type_named_related_to_the_location_(resourceType, resourceName, orchestratorName,
                locationName);
        for (int i = 4; i < locationResourceDefinition.length; i += 2) {
            ORCHESTRATION_LOCATION_RESOURCE_STEPS.I_update_the_property_to_for_the_resource_named_related_to_the_location_(locationResourceDefinition[i],
                    locationResourceDefinition[i + 1], resourceName, orchestratorName, locationName);
        }
    }

    private void initLocationResourcesAuth(List<String> locationResourceAuthDef) throws Throwable {
        String[] locationResourceAuthDefinition = locationResourceAuthDef.get(1).split(",");
        String orchestratorName = locationResourceAuthDefinition[0];
        String locationName = locationResourceAuthDefinition[1];

        ORCHESTRATION_LOCATION_RESOURCE_STEPS.I_autogenerate_the_on_demand_resources_for_the_location_(orchestratorName, locationName);

        for (int i = 2; i < locationResourceAuthDefinition.length; i += 2) {
            SECURED_RESOURCE_STEP_DEFINITION.iGrantAccessToTheResourceTypeNamedToTheUser("", "LOCATION_RESOURCE",
                    orchestratorName + "/" + locationName + "/" + locationResourceAuthDefinition[i], locationResourceAuthDefinition[i + 1]);
        }
    }
}
