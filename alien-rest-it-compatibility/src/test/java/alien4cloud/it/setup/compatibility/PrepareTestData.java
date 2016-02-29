package alien4cloud.it.setup.compatibility;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import alien4cloud.git.RepositoryManager;
import alien4cloud.it.setup.TestDataRegistry;
import alien4cloud.utils.FileUtil;

/**
 * Simple class that package the test archives as zip.
 */
@Slf4j
public class PrepareTestData {

    private final static RepositoryManager repositoryManager = new RepositoryManager();

    public static Path getPathForResource(String resource) throws URISyntaxException, IOException {
        URL resourcesUrl = Thread.currentThread().getContextClassLoader().getResource(resource);
        if (resourcesUrl == null) {
            return null;
        } else {
            URI resourcesUri = resourcesUrl.toURI();
            if (!"file".equals(resourcesUrl.getProtocol())) {
                log.info("Not a regular file,  url {}, uri {}, create zip file system", resourcesUrl, resourcesUri);
                Map<String, String> env = new HashMap<>();
                env.put("create", "true");
                return FileSystems.newFileSystem(resourcesUri, env).getPath("/" + resource);
            } else {
                return Paths.get(resourcesUri);
            }
        }
    }

    public static void main(String[] args) throws URISyntaxException {
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

        repositoryManager.cloneOrCheckout(TestDataRegistry.GIT_ARTIFACTS_DIR, "https://github.com/alien4cloud/tosca-normative-types.git", "1.0.0.wd06.alien",
                "tosca-normative-types-wd06");

        repositoryManager.cloneOrCheckout(TestDataRegistry.GIT_ARTIFACTS_DIR, "https://github.com/alien4cloud/tosca-normative-types.git", "master",
                "tosca-normative-types-1.0.0-SNAPSHOT");

        // TODO: Tests stills runs on wd03 data based on 1.1.0-SM4 tag
        repositoryManager.cloneOrCheckout(TestDataRegistry.GIT_ARTIFACTS_DIR, "https://github.com/alien4cloud/tosca-normative-types.git", "1.1.0-SM4",
                "tosca-normative-types");

        repositoryManager.cloneOrCheckout(TestDataRegistry.GIT_ARTIFACTS_DIR, "https://github.com/alien4cloud/alien4cloud-extended-types.git", "1.1.0",
                "alien4cloud-extended-types");
        repositoryManager.cloneOrCheckout(TestDataRegistry.GIT_ARTIFACTS_DIR, "https://github.com/alien4cloud/samples.git", "1.1.0", "samples");

        for (Map.Entry<Path, Path> entry : TestDataRegistry.SOURCE_TO_TARGET_ARTIFACT_MAPPING.entrySet()) {
            try {
                Path from;
                String fromRelativePath = FileUtil.getChildEntryRelativePath(TestDataRegistry.BASE_DIR, entry.getKey(), true);
                log.info("Zip from relative path [{}]", fromRelativePath);
                if (fromRelativePath.startsWith("src/test/resources")) {
                    Path fromClassPath = getPathForResource(fromRelativePath.replace("src/test/resources/", ""));
                    from = Files.createTempDirectory("");
                    FileUtil.copy(fromClassPath, from);
                    fromClassPath.getFileSystem().close();
                } else {
                    from = entry.getKey();
                }
                Path to = entry.getValue();
                log.info("Zip from [{}] to [{}]", from, to);
                FileUtil.zip(from, to);
            } catch (IOException e) {
                throw new RuntimeException("Failed to zip archive for tests.", e);
            }
        }
    }
}
