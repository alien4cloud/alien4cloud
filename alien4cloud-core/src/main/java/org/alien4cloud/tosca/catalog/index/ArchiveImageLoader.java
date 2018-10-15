package org.alien4cloud.tosca.catalog.index;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import alien4cloud.images.IImageDAO;
import alien4cloud.images.ImageData;
import alien4cloud.images.exception.ImageUploadException;
import alien4cloud.model.common.Tag;
import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.impl.ErrorCode;

import static alien4cloud.utils.AlienUtils.safe;

/**
 * Import images from CloudServiceArchive to ElasticSearch
 */
@Component
public class ArchiveImageLoader {
    private static final String ALIEN_ICON_TAG = "icon";
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    @Inject
    private IImageDAO imageDAO;

    /**
     * Import all images from the artifacts types in an archive.
     *
     * @param archiveFile The path to the root of the archive file.
     * @param archiveRoot The parsed archive object that contains all the types and topologies.
     * @param parsingErrors The list of parsing error in which to add errors in case there are (format error, file not found etc.)
     */
    public void importImages(Path archiveFile, ArchiveRoot archiveRoot, List<ParsingError> parsingErrors) {
        // Import archive icons
        importImages(archiveFile, archiveRoot.getNodeTypes(), parsingErrors);
        importImages(archiveFile, archiveRoot.getRelationshipTypes(), parsingErrors);
        importImages(archiveFile, archiveRoot.getCapabilityTypes(), parsingErrors);
        importImages(archiveFile, archiveRoot.getArtifactTypes(), parsingErrors);
        importImages(archiveFile, archiveRoot.getPolicyTypes(), parsingErrors);
        // Import topology icon
        importImages(archiveFile, archiveRoot.getArchive().getTags(), parsingErrors);
        if (archiveRoot.getArchive().isHasTopology()) {
            importImages(archiveFile, archiveRoot.getTopology().getTags(), parsingErrors);
        }
    }

    private void importImages(Path archiveFile, Map<String, ? extends AbstractInheritableToscaType> toscaInheritableElement, List<ParsingError> parsingErrors) {
        for (Map.Entry<String, ? extends AbstractInheritableToscaType> element : safe(toscaInheritableElement).entrySet()) {
            importImages(archiveFile, element.getValue().getTags(), parsingErrors);
        }
    }

    private void importImages(Path archiveFile, List<Tag> tags, List<ParsingError> parsingErrors) {
        if (tags == null) {
            return;
        }
        Tag iconTag = ArchiveImageLoader.getIconTag(tags);
        if (iconTag != null && !UUID_PATTERN.matcher(iconTag.getValue()).matches()) {
            importImage(archiveFile, parsingErrors, iconTag);
        }
    }

    private void importImage(Path archiveFile, List<ParsingError> parsingErrors, Tag iconTag) {
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
                parsingErrors.add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.INVALID_ICON_FORMAT, "Icon loading", null,
                        "Invalid icon format at path <" + iconPath + ">", null, safeToString(iconPath)));
            }
        } catch (NoSuchFileException | InvalidPathException e) {
            parsingErrors.add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.MISSING_FILE, "Icon loading", null,
                    "No icon file found at path <" + iconPath + ">", null, safeToString(iconPath)));
        } catch (ImageUploadException e) {
            parsingErrors.add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.INVALID_ICON_FORMAT, "Icon loading", null,
                    "Invalid icon format at path <" + iconPath + ">", null, safeToString(iconPath)));
        } catch (IOException e) {
            parsingErrors.add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.FAILED_TO_READ_FILE, "Icon loading", null,
                    "IO error while loading icon at path <" + iconPath + ">", null, safeToString(iconPath)));
        }
    }

    private String safeToString(Path path) {
        return path == null ? "" : path.toString();
    }

    /**
     * Get the icon tag from a tag list.
     * 
     * @param tags The list of tags in which to search for the icon tag.
     * @return The icon tag or null if the tag cannot be found.
     */
    public static Tag getIconTag(List<Tag> tags) {
        if (tags == null) {
            return null;
        }
        int iconTagIndex = tags.indexOf(new Tag(ALIEN_ICON_TAG, null));
        if (iconTagIndex < 0) {
            return null;
        }
        return tags.get(iconTagIndex);
    }
}