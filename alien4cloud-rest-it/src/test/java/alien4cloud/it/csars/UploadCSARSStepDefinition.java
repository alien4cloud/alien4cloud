package alien4cloud.it.csars;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.common.collect.Sets;
import org.junit.Assert;

import alien4cloud.it.Context;
import alien4cloud.it.common.CommonStepDefinitions;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.tosca.container.validation.CSARError;
import alien4cloud.tosca.container.validation.CSARValidationResult;
import alien4cloud.utils.FileUtil;

import com.google.common.collect.Maps;

import cucumber.api.PendingException;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class UploadCSARSStepDefinition {

    private Path csarPath = Paths.get(Context.getInstance().getTmpDirectory().toString(), "csar.csar");

    private static final Map<String, String> conditionToPath = Maps.newHashMap();

    private CommonStepDefinitions commonSteps = new CommonStepDefinitions();

    static {
        conditionToPath.put("containing base types", "../alien4cloud-core/src/main/resources/tosca-base-types/1.0");
        conditionToPath.put("containing java types", "../alien4cloud-core/src/main/resources/java-types/1.0");

        conditionToPath.put("csar file containing base types", "../alien4cloud-core/src/test/resources/examples/tosca-base-types-1.0.csar");
        conditionToPath.put("csar file containing base types V2", "../alien4cloud-core/src/test/resources/examples/tosca-base-types-2.0.csar");
        conditionToPath.put("csar file containing base types V3", "../alien4cloud-core/src/test/resources/examples/tosca-base-types-3.0.csar");
        conditionToPath.put("csar file containing java types", "../alien4cloud-core/src/test/resources/examples/java-types-1.0.csar");
        conditionToPath.put("csar file containing java types V2", "../alien4cloud-core/src/test/resources/examples/java-types-2.0.csar");
        conditionToPath.put("csar file containing java types V3", "../alien4cloud-core/src/test/resources/examples/java-types-3.0.csar");
        conditionToPath.put("csar file containing ubuntu types V0.1", "../alien4cloud-core/src/test/resources/examples/ubuntu-types-0.1-snapshot.csar");

        conditionToPath.put("containing base types constraints", "src/test/resources/data/csars/definition/constraints");
        conditionToPath.put("valid", "../alien4cloud-core/src/main/resources/tosca-base-types/1.0");
        conditionToPath.put("snapshot", "src/test/resources/data/csars/snapshot");
        conditionToPath.put("unzipped", "src/test/resources/alien/rest/csars/upload.feature");
        conditionToPath.put("invalid (definition file not found)", "src/test/resources/data/csars/definition/missing");
        conditionToPath.put("invalid (definition file is not valid yaml file)", "src/test/resources/data/csars/definition/erroneous");
        conditionToPath.put("invalid (definition file's declaration duplicated)", "src/test/resources/data/csars/definition/duplicated");
        conditionToPath.put("invalid (ALIEN-META.yaml not found)", "src/test/resources/data/csars/metadata/missing");
        conditionToPath.put("invalid (ALIEN-META.yaml invalid)", "src/test/resources/data/csars/metadata/erroneous");
        conditionToPath.put("invalid (ALIEN-META.yaml fail validation)", "src/test/resources/data/csars/metadata/validationFailure");
        conditionToPath.put("invalid (icon not found)", "src/test/resources/data/csars/icon/missing");
        conditionToPath.put("invalid (icon invalid)", "src/test/resources/data/csars/icon/erroneous");
        conditionToPath.put("invalid (dependency in definition do not exist)", "../alien4cloud-core/src/main/resources/java-types/1.0");
        conditionToPath.put("containing default java types", "../alien4cloud-core/src/main/default-normative-types/java-types-1.0.zip");
        conditionToPath.put("containing default apacheLB types", "../alien4cloud-core/src/main/default-normative-types/apacheLB-types-0.1.zip");
        conditionToPath.put("containing default tosca base types", "../alien4cloud-core/src/main/default-normative-types/tosca-base-types-1.0.zip");
        conditionToPath.put("a test archive for valid source/target", "src/test/resources/data/csars/archive-for-test.zip");
        conditionToPath.put("valid-csar-with-test", "src/test/resources/data/csars/snapshot-test/valid");
        conditionToPath.put("csar-test-no-topology", "src/test/resources/data/csars/snapshot-test/missing-topology-yaml");

    }

    @Given("^I have a CSAR folder that is \"([^\"]*)\"$")
    public void I_have_a_CSAR_folder_at(String condition) throws Throwable {
        String csarFolder = conditionToPath.get(condition);
        if (csarFolder == null) {
            throw new PendingException();
        }
        FileUtil.zip(Paths.get(csarFolder), csarPath);
        Context.getInstance().registerCSAR(csarFolder);
    }

    @Given("^I upload the archive file that is \"([^\"]*)\"$")
    public void I_upload_the_archive_file_that_is(String condition) throws Throwable {
        String csarFile = conditionToPath.get(condition);
        if (csarFile == null) {
            throw new PendingException();
        }
        Files.copy(Paths.get(csarFile), csarPath);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postMultipart("/rest/csars", "file", Files.newInputStream(csarPath)));
        commonSteps.I_should_receive_a_RestResponse_with_no_error();
        FileUtil.delete(csarPath);
    }

    @Given("^I have a CSAR file that is \"([^\"]*)\"$")
    public void I_have_a_CSAR_file_at(String condition) throws Throwable {
        String csarFile = conditionToPath.get(condition);
        Files.copy(Paths.get(csarFile), csarPath);
    }

    @When("^I upload it$")
    public void I_upload_it() throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postMultipart("/rest/csars", "file", Files.newInputStream(csarPath)));
        FileUtil.delete(csarPath);
    }

    @And("^The CSAR is already uploaded in the system$")
    public void The_CSAR_is_already_uploaded_in_the_system() throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postMultipart("/rest/csars", "file", Files.newInputStream(csarPath)));
    }

    @Then("^I should receive a RestResponse with an error code (\\d+) and (\\d+) compilation errors in (\\d+) file\\(s\\)$")
    public void I_should_receive_a_RestResponse_with_an_error_code_and_compilation_errors_in_file_s(int expectedCode, int compilationErrors, int errornousFiles)
            throws Throwable {
        RestResponse<CSARValidationResult> result = JsonUtil.read(Context.getInstance().takeRestResponse(), CSARValidationResult.class);
        Assert.assertNotNull(result.getError());
        Assert.assertEquals(expectedCode, result.getError().getCode());
        Assert.assertFalse("CSAR must be invalid", result.getData().isValid());
        Assert.assertEquals(errornousFiles, result.getData().getErrors().size());
        Set<CSARError> allErrors = Sets.newHashSet();
        for (Set<CSARError> errors : result.getData().getErrors().values()) {
            allErrors.addAll(errors);
        }
        Assert.assertEquals(compilationErrors, allErrors.size());
    }
}
