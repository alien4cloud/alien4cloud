package alien4cloud.it.setup;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import alien4cloud.utils.FileUtil;

/**
 * Simple class that package the test archives as zip.
 */
@Slf4j
public class PrepareTestData {
    public static String ARCHIVES_TARGET_PATH = "../target/it-artifacts/zipped/";
    public static String BASEDIR = "";

    public static void main(String[] args) {
        String baseDir = args[0];
        ARCHIVES_TARGET_PATH = baseDir + "/" + ARCHIVES_TARGET_PATH;
        BASEDIR = baseDir + "/";
        for (Map.Entry<Path, Path> entry : TestDataRegistry.FOLDER_TO_ZIP.entrySet()) {
            try {
                FileUtil.zip(entry.getKey(), entry.getValue());
            } catch (IOException e) {
                throw new RuntimeException("Failed to zip archive for tests.", e);
            }
        }
    }
}