package alien4cloud.tosca.container;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.dao.IGenericIdDAO;
import alien4cloud.images.IImageDAO;
import alien4cloud.images.ImageData;
import alien4cloud.tosca.container.archive.ArchiveImageLoader;
import alien4cloud.tosca.container.archive.ArchiveParser;
import alien4cloud.tosca.container.archive.ArchivePostProcessor;
import alien4cloud.tosca.container.exception.CSARImportImageException;
import alien4cloud.tosca.container.exception.CSARParsingException;
import alien4cloud.tosca.container.model.CloudServiceArchive;
import alien4cloud.tosca.container.model.ToscaInheritableElement;
import alien4cloud.utils.FileUtil;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context-test.xml")
public class ArchiveImageLoaderTest {

    private static final Path CSAR_OUTPUT_FOLDER = Paths.get("./target/csarTests");
    private static final String tmpArchiveName = "tosca-base-types-tags.csar";
    private static final String tmpArchiveNameWithError = "tosca-base-types-tags-error.csar";

    private static final Path PATH_TOSCA_BASE_TYPES = Paths.get("src/test/resources/alien/tosca/container/csar/tosca-base-types-tags");
    private static final Path PATH_TOSCA_BASE_TYPES_ERROR = Paths.get("src/test/resources/alien/tosca/container/csar/tosca-base-types-tags-error");

    @Resource
    private ArchiveImageLoader imageLoader;

    @Resource
    private ArchiveParser parser;

    @Resource
    private ArchivePostProcessor processor;

    @Resource
    private IImageDAO imageDAO;
    @Resource(name = "image-dao")
    private IGenericIdDAO imageGenericIdDAO;

    @Before
    public void prepareCloudServiceArchive() throws IOException {
        FileUtil.delete(CSAR_OUTPUT_FOLDER);
    }

    @Test
    public void importToscaElementImages() throws IOException, CSARParsingException {

        Path csarFileForTesting = Paths.get(CSAR_OUTPUT_FOLDER.toString(), tmpArchiveName);

        // Zip the csarSourceFolder and write it to csarFileForTesting
        FileUtil.zip(PATH_TOSCA_BASE_TYPES, csarFileForTesting);

        // Parse the archive for definitions
        CloudServiceArchive csa = parser.parseArchive(csarFileForTesting);
        processor.postProcessArchive(csa);

        imageLoader.importImages(csarFileForTesting, csa);

        boolean elementHasTags = false;
        String currentUUID = null;
        ImageData image = null;
        for (Map.Entry<String, ToscaInheritableElement> toscaInheritableElement : csa.getArchiveInheritableElements().entrySet()) {

            elementHasTags = toscaInheritableElement.getValue().getTags() != null;

            if (elementHasTags && toscaInheritableElement.getValue().getTags().containsKey("icon")) {
                currentUUID = toscaInheritableElement.getValue().getTags().get("icon");
                image = imageGenericIdDAO.findById(ImageData.class, currentUUID);
                // get registered images in ES by the UUID
                assertEquals(currentUUID, image.getId());
            }
        }
    }

    @Test(expected = CSARImportImageException.class)
    public void importToscaElementWithBadImageUri() throws IOException, CSARParsingException {

        Path csarFileForTesting = Paths.get(CSAR_OUTPUT_FOLDER.toString(), tmpArchiveNameWithError);

        // Zip the csarSourceFolder and write it to csarFileForTesting
        FileUtil.zip(PATH_TOSCA_BASE_TYPES_ERROR, csarFileForTesting);

        // Parse the archive for definitions
        CloudServiceArchive csa = parser.parseArchive(csarFileForTesting);
        processor.postProcessArchive(csa);

        imageLoader.importImages(csarFileForTesting, csa);

    }

}
