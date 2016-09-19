package alien4cloud.tosca.container;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import javax.annotation.Resource;

import org.alien4cloud.tosca.catalog.ArchiveParser;
import org.alien4cloud.tosca.catalog.index.ArchiveImageLoader;
import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.common.AlienConstants;
import alien4cloud.dao.IGenericIdDAO;
import alien4cloud.images.IImageDAO;
import alien4cloud.images.ImageData;
import alien4cloud.model.common.Tag;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
    private IImageDAO imageDAO;
    @Resource(name = "image-dao")
    private IGenericIdDAO imageGenericIdDAO;

    @Before
    public void prepareCloudServiceArchive() throws IOException {
        FileUtil.delete(CSAR_OUTPUT_FOLDER);
    }

    @Test
    public void importToscaElementImages() throws IOException, ParsingException {
        Path csarFileForTesting = Paths.get(CSAR_OUTPUT_FOLDER.toString(), tmpArchiveName);

        // Zip the csarSourceFolder and write it to csarFileForTesting
        FileUtil.zip(PATH_TOSCA_BASE_TYPES, csarFileForTesting);

        Path imagesPath = Paths.get("target/alien/images");
        if (!Files.exists(imagesPath)) {
            Files.createDirectories(imagesPath);
        }

        // Parse the archive for definitions
        ParsingResult<ArchiveRoot> result = parser.parse(csarFileForTesting, AlienConstants.GLOBAL_WORKSPACE_ID);
        imageLoader.importImages(csarFileForTesting, result.getResult(), result.getContext().getParsingErrors());

        Assert.assertFalse(result.hasError(ParsingErrorLevel.ERROR));
        Assert.assertFalse(result.hasError(ParsingErrorLevel.WARNING));

        checkImages(result.getResult().getNodeTypes());
    }

    private void checkImages(Map<String, ? extends AbstractInheritableToscaType> elements) {
        boolean elementHasTags = false;
        String currentUUID = null;
        ImageData image = null;
        Tag iconTag = new Tag("icon", "");

        for (Map.Entry<String, ? extends AbstractInheritableToscaType> toscaInheritableElement : elements.entrySet()) {
            elementHasTags = toscaInheritableElement.getValue().getTags() != null;
            if (elementHasTags) {
                int indexOfIcon = toscaInheritableElement.getValue().getTags().indexOf(iconTag);
                if (indexOfIcon >= 0) {
                    currentUUID = toscaInheritableElement.getValue().getTags().get(indexOfIcon).getValue();
                    image = imageGenericIdDAO.findById(ImageData.class, currentUUID);
                    // get registered images in ES by the UUID
                    assertEquals(currentUUID, image.getId());
                }
            }
        }
    }

    @Test
    public void importToscaElementWithBadImageUri() throws IOException, ParsingException {
        Path csarFileForTesting = Paths.get(CSAR_OUTPUT_FOLDER.toString(), tmpArchiveNameWithError);

        // Zip the csarSourceFolder and write it to csarFileForTesting
        FileUtil.zip(PATH_TOSCA_BASE_TYPES_ERROR, csarFileForTesting);
        // Parse the archive for definitions
        ParsingResult<ArchiveRoot> result = parser.parse(csarFileForTesting, AlienConstants.GLOBAL_WORKSPACE_ID);
        imageLoader.importImages(csarFileForTesting, result.getResult(), result.getContext().getParsingErrors());

        // we expect to have warning issues due to missing files or invalid formats.
        Assert.assertFalse(result.hasError(ParsingErrorLevel.ERROR));
        Assert.assertTrue(result.hasError(ParsingErrorLevel.WARNING));
    }
}