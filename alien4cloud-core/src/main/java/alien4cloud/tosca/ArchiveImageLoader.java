package alien4cloud.tosca;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.component.model.IndexedInheritableToscaElement;
import alien4cloud.component.model.Tag;
import alien4cloud.images.IImageDAO;
import alien4cloud.images.ImageData;
import alien4cloud.images.exception.ImageUploadException;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingResult;

/**
 * Import images from CloudServiceArchive to ElasticSearch
 */
@Component
public class ArchiveImageLoader {
    private static final String ALIEN_ICON_TAG = "icon";

    @Resource
    private IImageDAO imageDAO;

    /**
     * Import all images from the artifacts types in an archive.
     * 
     * @param archiveFile The path to the archive root.
     * @param archiveRoot The archive root object.
     */
    public void importImages(Path archiveFile, ParsingResult<ArchiveRoot> parsingResult) {
        importImages(archiveFile, parsingResult, parsingResult.getResult().getNodeTypes());
        importImages(archiveFile, parsingResult, parsingResult.getResult().getRelationshipTypes());
        importImages(archiveFile, parsingResult, parsingResult.getResult().getCapabilityTypes());
        importImages(archiveFile, parsingResult, parsingResult.getResult().getArtifactTypes());
    }

    private void importImages(Path archiveFile, ParsingResult<ArchiveRoot> parsingResult,
            Map<String, ? extends IndexedInheritableToscaElement> toscaInheritableElement) {
        for (Map.Entry<String, ? extends IndexedInheritableToscaElement> element : toscaInheritableElement.entrySet()) {
            if (element.getValue().getTags() != null) {
                List<Tag> tags = element.getValue().getTags();
                Tag iconTag = tags.get(tags.indexOf(new Tag(ALIEN_ICON_TAG, null)));
                if (iconTag != null) {
                    FileSystem csarFS = null;
                    Path iconPath = null;

                    try {
                        csarFS = FileSystems.newFileSystem(archiveFile, null);
                        iconPath = csarFS.getPath(iconTag.getValue());
                        if (!Files.isDirectory(iconPath)) {
                            String iconId = UUID.randomUUID().toString();
                            // Saving the image
                            ImageData imageData = new ImageData();
                            imageData.setData(Files.readAllBytes(iconPath));
                            imageData.setId(iconId);
                            imageDAO.writeImage(imageData);
                            // Replace the image uri by the indexed image ID
                            iconTag.setValue(iconId);
                        } else {
                            parsingResult
                                    .getContext()
                                    .getParsingErrors()
                                    .add(new ParsingError("Icons loading", null, "Icon not found in tag <" + ALIEN_ICON_TAG + "> at path <" + iconPath + ">",
                                            null, parsingResult.getContext().getFileName()));
                        }
                    } catch (NoSuchFileException | InvalidPathException e) {
                        parsingResult
                                .getContext()
                                .getParsingErrors()
                                .add(new ParsingError("Icons loading", null, "Icon not found in tag <" + ALIEN_ICON_TAG + "> at path <" + iconPath + ">", null,
                                        parsingResult.getContext().getFileName()));
                    } catch (ImageUploadException e) {
                        parsingResult
                                .getContext()
                                .getParsingErrors()
                                .add(new ParsingError("Icons loading", null, "Icon not found in tag <" + ALIEN_ICON_TAG + "> at path <" + iconPath + ">", null,
                                        parsingResult.getContext().getFileName()));
                        // CSARErrorCode.ERRONEOUS_ICON_FILE
                    } catch (IOException e) {
                        parsingResult
                                .getContext()
                                .getParsingErrors()
                                .add(new ParsingError("Icons loading", null, "Icon not found in tag <" + ALIEN_ICON_TAG + "> at path <" + iconPath + ">", null,
                                        parsingResult.getContext().getFileName()));
                        // throw new CSARIOException("Invalid archiveFile or icon path");
                    }
                }
            }
        }
    }
}