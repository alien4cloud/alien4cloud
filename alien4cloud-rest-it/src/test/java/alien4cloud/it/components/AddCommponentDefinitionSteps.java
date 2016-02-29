package alien4cloud.it.components;

import java.io.IOException;
import java.nio.file.Paths;

import alien4cloud.dao.ElasticSearchMapper;
import alien4cloud.it.Context;
import alien4cloud.it.csars.CrudCSARSStepDefinition;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.utils.FileUtil;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.When;

public class AddCommponentDefinitionSteps {
    private static final String COMPONENT_TEST_DATA_PACKAGE = "./src/test/resources/data/components/";

    private CrudCSARSStepDefinition crudCSARSStepDefinition = new CrudCSARSStepDefinition();

    @Given("^I already had a csar with name \"([^\"]*)\" and version \"([^\"]*)\"$")
    public void I_already_had_a_csar_with_name_and_version(String name, String version) throws Throwable {
        crudCSARSStepDefinition.I_create_a_CSAR_with_name_and_version(name, version);
    }

    @When("^I upload the component \"([^\"]*)\"$")
    public void I_upload_the_component(String componentName) throws Throwable {
        Context.getInstance().registerRestResponse(uploadComponent(componentName));
    }

    public String uploadComponent(String componentName) throws IOException {
        String csarId = JsonUtil.read(Context.getInstance().getRestResponse(), String.class).getData();
        return AddCommponentDefinitionSteps.uploadComponent(csarId, componentName);
    }

    public static String uploadComponent(String csarId, String componentName) throws IOException {
        String componentJson = FileUtil.readTextFile(Paths.get(COMPONENT_TEST_DATA_PACKAGE + componentName + ".json"));
        // parse the json using an ES mapper
        IndexedNodeType nodeType = ElasticSearchMapper.getInstance().readValue(componentJson, IndexedNodeType.class);
        nodeType.setArchiveVersion("1.0");
        // and send it to rest API through the REST mapper
        componentJson = Context.getInstance().getJsonMapper().writeValueAsString(nodeType);
        return Context.getRestClientInstance().postJSon("/rest/v1/csars/" + csarId + "/nodetypes", componentJson);
    }
}