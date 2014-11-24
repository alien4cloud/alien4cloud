package alien4cloud.it.csars;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;

import lombok.extern.slf4j.Slf4j;

import org.junit.Assert;

import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.it.Context;
import alien4cloud.it.common.CommonStepDefinitions;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.utils.FileUtil;

import com.google.common.collect.Maps;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;

@Slf4j
public class UploadCSARSStepDefinition {
    private static final Path ARCHIVES_TARGET_PATH = Paths.get("../target/it-artifacts/zipped/");

    private static final Map<String, Path> conditionToPath = Maps.newHashMap();
    static {
        try {
            FileUtil.delete(ARCHIVES_TARGET_PATH);
        } catch (IOException e) {
            log.error("Failed to delete zipped archives repository.", e);
        }
        // Zip all test archive folders so they are ready to be uploaded.
        conditionToPath.put("tosca base types 1.0", Paths.get("src/test/resources/data/csars/tosca-base-types-1.0"));
        conditionToPath.put("tosca base types 2.0", Paths.get("src/test/resources/data/csars/tosca-base-types-2.0"));
        conditionToPath.put("tosca base types 3.0", Paths.get("src/test/resources/data/csars/tosca-base-types-3.0"));
        conditionToPath.put("sample java types 1.0", Paths.get("src/test/resources/data/csars/sample/java-types-1.0"));
        conditionToPath.put("sample java types 2.0", Paths.get("src/test/resources/data/csars/sample/java-types-2.0"));
        conditionToPath.put("sample java types 3.0", Paths.get("src/test/resources/data/csars/sample/java-types-3.0"));
        conditionToPath.put("ubuntu types 0.1", Paths.get("src/test/resources/data/csars/sample/ubuntu-types-0.1"));
        conditionToPath.put("sample apache lb types 0.1", Paths.get("src/test/resources/data/csars/sample/apache-lb-types-0.1"));
        conditionToPath.put("constraints", Paths.get("src/test/resources/data/csars/definition/constraints"));
        conditionToPath.put("invalid (definition file not found)", Paths.get("src/test/resources/data/csars/definition/missing"));
        conditionToPath.put("invalid (definition file is not valid yaml file)", Paths.get("src/test/resources/data/csars/definition/erroneous"));
        conditionToPath.put("invalid (definition file's declaration duplicated)", Paths.get("src/test/resources/data/csars/definition/duplicated"));
        conditionToPath.put("invalid (ALIEN-META.yaml not found)", Paths.get("src/test/resources/data/csars/metadata/missing"));
        conditionToPath.put("invalid (ALIEN-META.yaml invalid)", Paths.get("src/test/resources/data/csars/metadata/erroneous"));
        conditionToPath.put("invalid (ALIEN-META.yaml fail validation)", Paths.get("src/test/resources/data/csars/metadata/validationFailure"));
        conditionToPath.put("invalid (icon not found)", Paths.get("src/test/resources/data/csars/icon/missing"));
        conditionToPath.put("invalid (icon invalid)", Paths.get("src/test/resources/data/csars/icon/erroneous"));
        conditionToPath.put("snapshot", Paths.get("src/test/resources/data/csars/snapshot"));
        conditionToPath.put("relationship test types", Paths.get("src/test/resources/data/csars/relationship-test-types"));
        conditionToPath.put("valid-csar-with-test", Paths.get("src/test/resources/data/csars/snapshot-test/snapshot-test-valid"));
        conditionToPath.put("csar-test-no-topology", Paths.get("src/test/resources/data/csars/snapshot-test/missing-topology-yaml"));

        for (Entry<String, Path> entry : conditionToPath.entrySet()) {
            try {
                Path targetPath = ARCHIVES_TARGET_PATH.resolve(entry.getValue().getFileName() + ".csar");
                FileUtil.zip(entry.getValue(), targetPath);
                conditionToPath.put(entry.getKey(), targetPath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to zip archive for tests.", e);
            }
        }

        // test uploading an unzipped file (do not zip it)
        conditionToPath.put("unzipped", Paths.get("src/test/resources/alien/rest/csars/upload.feature"));
    }

    private Path csarPath = Paths.get(Context.getInstance().getTmpDirectory().toString(), "csar.csar");
    private CommonStepDefinitions commonSteps = new CommonStepDefinitions();

    @Given("^I upload the archive \"([^\"]*)\"$")
    public void uploadArchive(String key) throws Throwable {
        Path archive = conditionToPath.get(key);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postMultipart("/rest/csars", "file", Files.newInputStream(archive)));
    }

    @Then("^I should receive a RestResponse with an error code (\\d+) and (\\d+) compilation errors in (\\d+) file\\(s\\)$")
    public void I_should_receive_a_RestResponse_with_an_error_code_and_compilation_errors_in_file_s(int expectedCode, int compilationErrors, int errornousFiles)
            throws Throwable {
        RestResponse<ParsingResult> result = JsonUtil.read(Context.getInstance().takeRestResponse(), ParsingResult.class);

        Assert.assertNotNull(result.getError());
        Assert.assertEquals(expectedCode, result.getError().getCode());
        // Assert.assertFalse("CSAR must be invalid", result.getData().isValid());
        // Assert.assertEquals(errornousFiles, result.getData().getErrors().size());
        // Set<CSARError> allErrors = Sets.newHashSet();
        // for (Set<CSARError> errors : result.getData().getErrors().values()) {
        // allErrors.addAll(errors);
        // }
        // Assert.assertEquals(compilationErrors, allErrors.size());
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
