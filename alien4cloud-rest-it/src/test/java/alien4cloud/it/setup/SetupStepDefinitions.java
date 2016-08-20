package alien4cloud.it.setup;

import alien4cloud.git.RepositoryManager;
import alien4cloud.it.Context;
import alien4cloud.it.common.CommonStepDefinitions;
import alien4cloud.utils.FileUtil;
import cucumber.api.java.en.And;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
        uploadWithoutChecking(source);
        COMMON_STEP_DEFINITIONS.I_should_receive_a_RestResponse_with_no_error();
    }

    private void uploadWithoutChecking(Path archive) throws IOException {
        Path csarTargetPath = Context.CSAR_TARGET_PATH.resolve(archive.getFileName() + ".csar");
        FileUtil.zip(archive, csarTargetPath);
        Context.getInstance()
                .registerRestResponse(Context.getRestClientInstance().postMultipart("/rest/v1/csars", "file", Files.newInputStream(csarTargetPath)));
    }

    @And("^I upload the git archive \"([^\"]*)\"$")
    public void I_upload_the_git_archive(String folderToUpload) throws Throwable {
        Path csarSourceFolder = Context.GIT_ARTIFACT_TARGET_PATH.resolve(folderToUpload);
        uploadArchive(csarSourceFolder);
    }

    @And("^I upload the local archive \"([^\"]*)\"$")
    public void I_upload_the_local_archive(String archive) throws Throwable {
        Path archivePath = Context.LOCAL_TEST_DATA_PATH.resolve(archive);
        uploadArchive(archivePath);
    }
}
