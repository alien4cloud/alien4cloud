package alien4cloud.component.repository;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.component.repository.CsarFileRepository;
import alien4cloud.component.repository.ICsarRepositry;
import alien4cloud.component.repository.exception.CSARStorageFailureException;
import alien4cloud.component.repository.exception.CSARVersionAlreadyExistsException;
import alien4cloud.component.repository.exception.CSARVersionNotFoundException;
import alien4cloud.utils.FileUtil;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context-test.xml")
public class CsarRepositoryTest {

    @Resource
    private ICsarRepositry repo;
    private String tmpPath = "src/test/resources/data/test-file.zip";
    private String testFileName = "positive";
    private static final String ARCHIVE_EXTENSION = "csar";
    @Value("${directories.alien}/${directories.csar_repository}")
    private String alienRepoDir;

    @Test(expected = CSARVersionNotFoundException.class)
    public void CSARVersionNotFoundTest() throws CSARVersionNotFoundException {
        cleanup();
        repo.getCSAR(testFileName, "1.0");
    }

    @Test
    public void storeCSARTest() throws CSARVersionAlreadyExistsException {
        cleanup();
        testStoreSuccessful("1.0");
    }

    @Test
    public void getCASERTest() throws CSARVersionNotFoundException, CSARVersionAlreadyExistsException {
        cleanup();
        storeTestCSAR(testFileName, "1.0", tmpPath);
        testGetCSARSuccessul();
    }

    @Test(expected = CSARStorageFailureException.class)
    public void testBadTmpPathToStore() throws CSARVersionAlreadyExistsException {
        cleanup();
        storeTestCSAR(testFileName, "1.0", "src/test/files/positive.rar");
    }

    public void testStoreSuccessful(String version) throws CSARVersionAlreadyExistsException {
        Path path = storeTestCSAR(testFileName, version, tmpPath);

        assertTrue("File " + path + " Was supposed to be created but not the case.", fileExists(path, false));
    }

    @Test
    public void testOverrideExistingSnapshotVersion() throws CSARVersionAlreadyExistsException {
        cleanup();
        testStoreSuccessful("1.0-snapshot");
        testStoreSuccessful("1.0-snapshot");
    }

    private Path storeTestCSAR(String testFileName, String version, String tmpPath) throws CSARVersionAlreadyExistsException {
        Path path = Paths.get(tmpPath).toAbsolutePath();
        repo.storeCSAR(testFileName, version, path);
        return path;
    }

    public void testGetCSARSuccessul() throws CSARVersionNotFoundException {
        Path path = repo.getCSAR(testFileName, "1.0");
        assertNotNull(path);
        String[] splits = path.toString().split("[\\\\/]");
        String name = splits[splits.length - 1];
        String expectedName = testFileName.concat("-".concat("1.0").concat(".").concat(ARCHIVE_EXTENSION));
        assertEquals(expectedName, name);

        log.debug("GET Result: " + path);
    }

    private boolean fileExists(Path path, boolean isDirectory) {
        if (isDirectory) {
            return Files.isDirectory(path);
        }

        return Files.exists(path);
    }

    @After
    public void cleanup() {
        if (fileExists(Paths.get(alienRepoDir), true)) {
            log.debug("cleaning the test env");
            try {
                FileUtil.delete(Paths.get(alienRepoDir));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
