package alien4cloud.it.setup;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import alien4cloud.git.RepositoryManager;
import alien4cloud.utils.FileUtil;

/**
 * Simple class that package the test archives as zip.
 */
@Slf4j
public class PrepareTestData {

    private final static RepositoryManager repositoryManager = new RepositoryManager();

    public static void main(String[] args) {
        System.setProperty("basedir", args[0]);
        try {
            FileUtil.delete(TestDataRegistry.IT_ARTIFACTS_DIR);
        } catch (IOException e) {
            log.error("Failed to delete zipped archives repository.", e);
        }

        try {
            FileUtil.delete(TestDataRegistry.GIT_ARTIFACTS_DIR);
        } catch (IOException e) {
            log.error("Failed to delete zipped archives repository.", e);
        }

        repositoryManager.cloneOrCheckout(TestDataRegistry.GIT_ARTIFACTS_DIR, "https://github.com/alien4cloud/tosca-normative-types.git", "master",
                "tosca-normative-types");
        // TODO: for the moment we checkout both master and 1.0.0.wd06.alien branches
        repositoryManager.cloneOrCheckout(TestDataRegistry.GIT_ARTIFACTS_DIR, "https://github.com/alien4cloud/tosca-normative-types.git", "1.0.0.wd06.alien",
                "tosca-normative-types-wd06");
        repositoryManager.cloneOrCheckout(TestDataRegistry.GIT_ARTIFACTS_DIR, "https://github.com/alien4cloud/alien4cloud-extended-types.git", "master",
                "alien4cloud-extended-types");
        repositoryManager.cloneOrCheckout(TestDataRegistry.GIT_ARTIFACTS_DIR, "https://github.com/alien4cloud/samples.git", "master", "samples");
        for (Map.Entry<Path, Path> entry : TestDataRegistry.SOURCE_TO_TARGET_ARTIFACT_MAPPING.entrySet()) {
            try {
                Path from = entry.getKey();
                Path to = entry.getValue();
                log.info("Zip from [{}] to [{}]", from, to);
                FileUtil.zip(from, to);
            } catch (IOException e) {
                throw new RuntimeException("Failed to zip archive for tests.", e);
            }
        }
    }
}