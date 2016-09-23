package alien4cloud.it.csars;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.alien4cloud.test.setup.TestDataRegistry;
import org.alien4cloud.tosca.model.types.NodeType;
import org.junit.Assert;

import alien4cloud.it.Context;
import alien4cloud.it.common.CommonStepDefinitions;
import alien4cloud.rest.csar.CsarUploadResult;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.utils.FileUtil;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UploadCSARSStepDefinition {
    private static final CommonStepDefinitions COMMON_STEP_DEFINITIONS = new CommonStepDefinitions();

    private void uploadArchive(Path source) throws Throwable {
        Path csarTargetPath = Context.CSAR_TARGET_PATH.resolve(source.getFileName() + ".csar");
        FileUtil.zip(source, csarTargetPath);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postMultipart("/rest/csars", "file", Files.newInputStream(csarTargetPath)));
    }

    @And("^I upload the local archive \"([^\"]*)\"$")
    public void I_upload_the_local_archive(String archive) throws Throwable {
        Path archivePath = Context.LOCAL_TEST_DATA_PATH.resolve(archive);
        uploadArchive(archivePath);
    }

    @Given("^I upload the archive \"([^\"]*)\"$")
    public void uploadArchive(String key) throws Throwable {
        Path archive = TestDataRegistry.TEST_ARTIFACTS.get(key);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postMultipart("/rest/v1/csars", "file", Files.newInputStream(archive)));
    }

    @Given("^I have uploaded the archive \"([^\"]*)\"$")
    public void I_have_uploaded_the_archive(String key) throws Throwable {
        uploadArchive(key);
        COMMON_STEP_DEFINITIONS.I_should_receive_a_RestResponse_with_no_error();
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
        for (Map.Entry<String, List<ParsingError>> errorEntry : result.getData().getErrors().entrySet()) {
            errorCount += errorEntry.getValue().size();
        }
        Assert.assertEquals(compilationErrors, errorCount);
    }

    @Then("^I should receive a RestResponse with (\\d+) alerts in (\\d+) files : (\\d+) errors (\\d+) warnings and (\\d+) infos$")
    public void I_should_receive_a_RestResponse_with_compilation_alerts_in_file_s(int expectedAlertCount, int errornousFiles, int exptectedErrorCount,
            int exptectedWarningCount, int exptectedInfoCount) throws Throwable {

        RestResponse<CsarUploadResult> result = JsonUtil.read(Context.getInstance().takeRestResponse(), CsarUploadResult.class);

        Assert.assertFalse("We should have alerts", result.getData().getErrors().isEmpty());
        Assert.assertEquals(errornousFiles, result.getData().getErrors().size());

        int alertCount = 0;
        int errorCount = 0;
        int warningCount = 0;
        int infoCount = 0;

        for (Map.Entry<String, List<ParsingError>> errorEntry : result.getData().getErrors().entrySet()) {
            alertCount += errorEntry.getValue().size();
            for (ParsingError pe : errorEntry.getValue()) {
                switch (pe.getErrorLevel()) {
                case ERROR:
                    errorCount++;
                    break;
                case WARNING:
                    warningCount++;
                    break;
                case INFO:
                    infoCount++;
                    break;
                }
            }
        }
        Assert.assertEquals(expectedAlertCount, alertCount);
        Assert.assertEquals(exptectedErrorCount, errorCount);
        Assert.assertEquals(exptectedWarningCount, warningCount);
        Assert.assertEquals(exptectedInfoCount, infoCount);
    }

    @Then("^I should have last update date greater than creation date$")
    public void I_should_have_last_update_date_greater_than_creation_date() throws Throwable {
        NodeType idnt = JsonUtil.read(Context.getInstance().takeRestResponse(), NodeType.class).getData();
        Assert.assertTrue(idnt.getLastUpdateDate().after(idnt.getCreationDate()));
    }

    @Then("^I should have last update date equals to creation date$")
    public void I_should_have_last_update_date_equals_to_creation_date() throws Throwable {
        NodeType idnt = JsonUtil.read(Context.getInstance().takeRestResponse(), NodeType.class).getData();
        Assert.assertTrue(idnt.getLastUpdateDate().equals(idnt.getCreationDate()));
    }

    @And("^I there should be a parsing error level \"([^\"]*)\" and code \"([^\"]*)\"$")
    public void iThereShouldBeAParsingErrorLevelAndCode(ParsingErrorLevel errorLevel, ErrorCode expectedCode) throws Throwable {
        RestResponse<CsarUploadResult> result = JsonUtil.read(Context.getInstance().takeRestResponse(), CsarUploadResult.class);

        Assert.assertFalse("There must have messages after parsing the csar", result.getData().getErrors().isEmpty());
        int errorCount = 0;
        boolean found = false;
        for (List<ParsingError> errors : result.getData().getErrors().values()) {
            for (ParsingError error : errors) {
                if (Objects.equals(error.getErrorCode(), expectedCode) && Objects.equals(error.getErrorLevel(), errorLevel)) {
                    found = true;
                    break;
                }
            }
            if (found) {
                break;
            }
        }
        Assert.assertTrue(found);
    }
}
