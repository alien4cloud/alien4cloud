package alien4cloud.it.csars;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import alien4cloud.tosca.parser.ParsingError;
import lombok.extern.slf4j.Slf4j;

import org.junit.Assert;

import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.it.Context;
import alien4cloud.it.common.CommonStepDefinitions;
import alien4cloud.it.setup.TestDataRegistry;
import alien4cloud.rest.csar.CsarUploadResult;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;

@Slf4j
public class UploadCSARSStepDefinition {

    private Path csarPath = Paths.get(Context.getInstance().getTmpDirectory().toString(), "csar.csar");
    private CommonStepDefinitions commonSteps = new CommonStepDefinitions();

    @Given("^I upload the archive \"([^\"]*)\"$")
    public void uploadArchive(String key) throws Throwable {
        Path archive = TestDataRegistry.CONDITION_TO_PATH.get(key);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postMultipart("/rest/csars", "file", Files.newInputStream(archive)));
    }

    @Then("^I should receive a RestResponse with an error code (\\d+) and (\\d+) compilation errors in (\\d+) file\\(s\\)$")
    public void I_should_receive_a_RestResponse_with_an_error_code_and_compilation_errors_in_file_s(int expectedCode, int compilationErrors, int errornousFiles)
            throws Throwable {

        RestResponse<CsarUploadResult> result = JsonUtil.read(Context.getInstance().takeRestResponse(), CsarUploadResult.class);

        Assert.assertNotNull(result.getError());
        Assert.assertEquals(expectedCode, result.getError().getCode());
        Assert.assertFalse("CSAR must be invalid", result.getData().getErrors().isEmpty());
        Assert.assertEquals(errornousFiles, result.getData().getErrors().size());
        int errorCount = 0;
        for (Map.Entry<String, List<ParsingError>> errorEntry :  result.getData().getErrors().entrySet()) {
            errorCount += errorEntry.getValue().size();
        }
        Assert.assertEquals(compilationErrors, errorCount);
    }

    @Then("^I should have last update date greater than creation date$")
    public void I_should_have_last_update_date_greater_than_creation_date() throws Throwable {
        IndexedNodeType idnt = JsonUtil.read(Context.getInstance().takeRestResponse(), IndexedNodeType.class).getData();
        Assert.assertTrue(idnt.getLastUpdateDate().after(idnt.getCreationDate()));
    }

    @Then("^I should have last update date equals to creation date$")
    public void I_should_have_last_update_date_equals_to_creation_date() throws Throwable {
        IndexedNodeType idnt = JsonUtil.read(Context.getInstance().takeRestResponse(), IndexedNodeType.class).getData();
        Assert.assertTrue(idnt.getLastUpdateDate().equals(idnt.getCreationDate()));
    }
}
