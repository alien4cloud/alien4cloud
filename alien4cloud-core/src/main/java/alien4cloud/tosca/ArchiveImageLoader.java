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
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.tosca.parser.impl.ErrorCode;

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
    @SuppressWarnings("unchecked")
    public void importImages(Path archiveFile, ParsingResult<ArchiveRoot> parsingResult) {
        importImages(archiveFile, parsingResult, parsingResult.getResult().getNodeTypes());
        importImages(archiveFile, parsingResult, parsingResult.getResult().getRelationshipTypes());
        importImages(archiveFile, parsingResult, parsingResult.getResult().getCapabilityTypes());
        importImages(archiveFile, parsingResult, parsingResult.getResult().getArtifactTypes());

        for (ParsingResult<?> subResult : parsingResult.getContext().getSubResults()) {
            if (subResult.getResult() instanceof ArchiveRoot) {
                importImages(archiveFile, (ParsingResult<ArchiveRoot>) subResult);
            }
        }
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
                                    .add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.INVALID_ICON_FORMAT, "Icon loading", null,
                                            "Invalid icon format at path <" + iconPath + ">", null, iconPath.toString()));
                        }
                    } catch (NoSuchFileException | InvalidPathException e) {
                        parsingResult
                                .getContext()
                                .getParsingErrors()
                                .add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.MISSING_FILE, "Icon loading", null, "No icon file found at path <"
                                        + iconPath + ">", null, iconPath.toString()));
                    } catch (ImageUploadException e) {
                        parsingResult
                                .getContext()
                                .getParsingErrors()
                                .add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.INVALID_ICON_FORMAT, "Icon loading", null,
                                        "Invalid icon format at path <" + iconPath + ">", null, iconPath.toString()));
                    } catch (IOException e) {
                        parsingResult
                                .getContext()
                                .getParsingErrors()
                                .add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.FAILED_TO_READ_FILE, "Icon loading", null,
                                        "IO error while loading icon at path <" + iconPath + ">", null, iconPath.toString()));
                    }
                }
            }
        }
    }
}