package alien4cloud.it.setup;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import alien4cloud.git.RepositoryManager;
import alien4cloud.utils.FileUtil;

/**
 * Simple class that package the test archives as zip.
 */
@Slf4j
public class PrepareTestData {
    public static String ARCHIVES_TARGET_PATH_ROOT = "../target/it-artifacts/";
    public static String ARCHIVES_TARGET_PATH = ARCHIVES_TARGET_PATH_ROOT + "zipped/";
    public static String BASEDIR = "";
    private final static RepositoryManager repositoryManager = new RepositoryManager();

    private static void checkoutArchiveFromGit(String localName, String nameFolder, String url, String branch) {
        Path archivesTargetPath = Paths.get(ARCHIVES_TARGET_PATH_ROOT);
        repositoryManager.cloneOrCheckout(archivesTargetPath, url, branch, localName);

        Path typesPath = archivesTargetPath.resolve(nameFolder);
        Path typesZipPath = archivesTargetPath.resolve(nameFolder + ".zip");
        try {
            FileUtil.zip(typesPath, typesZipPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to zip archive for tests.", e);
        }
    }

    public static void main(String[] args) {
        String baseDir = args[0];
        ARCHIVES_TARGET_PATH = baseDir + "/" + ARCHIVES_TARGET_PATH;
        BASEDIR = baseDir + "/";

        try {
            FileUtil.delete(Paths.get(PrepareTestData.ARCHIVES_TARGET_PATH));
        } catch (IOException e) {
            log.error("Failed to delete zipped archives repository.", e);
        }

        for (Map.Entry<Path, Path> entry : TestDataRegistry.FOLDER_TO_ZIP.entrySet()) {
            try {
                FileUtil.zip(entry.getKey(), entry.getValue());
            } catch (IOException e) {
                throw new RuntimeException("Failed to zip archive for tests.", e);
            }
        }

        checkoutArchiveFromGit("alien-base-types", "alien-base-types/alien-base-types-1.0-SNAPSHOT",
                "https://github.com/alien4cloud/alien4cloud-extended-types.git", "master");
        checkoutArchiveFromGit("alien-extended-storage-types", "alien-base-types/alien-extended-storage-types-1.0-SNAPSHOT",
                "https://github.com/alien4cloud/alien4cloud-extended-types.git", "master");

    }
}