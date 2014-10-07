package alien4cloud.tosca.container.archive;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.images.IImageDAO;
import alien4cloud.images.ImageData;
import alien4cloud.images.exception.ImageUploadException;
import alien4cloud.tosca.container.exception.CSARIOException;
import alien4cloud.tosca.container.exception.CSARImportImageException;
import alien4cloud.tosca.container.model.CloudServiceArchive;
import alien4cloud.tosca.container.model.ToscaInheritableElement;
import alien4cloud.tosca.container.validation.CSARErrorCode;

/**
 *
 * Import impages from CloudServiceArchive to ElasticSearch
 *
 * @author mourouvi
 *
 */
@Component
public class ArchiveImageLoader {

    private static final String ALIEN_ICON_TAG = "icon";

    @Resource
    private IImageDAO imageDAO;

    public void importImages(Path archiveFile, CloudServiceArchive cloudServiceARchive) throws CSARImportImageException {

        // iterate over node types
        Map<String, ToscaInheritableElement> toscaInheritableElements = cloudServiceARchive.getArchiveInheritableElements();

        // look for icon tag
        Boolean elementHasTags = false;
        for (Map.Entry<String, ToscaInheritableElement> toscaInheritableElement : toscaInheritableElements.entrySet()) {
            elementHasTags = toscaInheritableElement.getValue().getTags() != null;

            if (elementHasTags && toscaInheritableElement.getValue().getTags().containsKey(ALIEN_ICON_TAG)) {

                FileSystem csarFS = null;
                Path iconPath = null;
                String alienIconTagPath = toscaInheritableElement.getValue().getTags().get(ALIEN_ICON_TAG);

                try {
                    csarFS = FileSystems.newFileSystem(archiveFile, null);
                    iconPath = csarFS.getPath(alienIconTagPath);
                    if (!Files.isDirectory(iconPath)) {
                        String iconId = UUID.randomUUID().toString();
                        // Saving the image
                        ImageData imageData = new ImageData();
                        imageData.setData(Files.readAllBytes(iconPath));
                        imageData.setId(iconId);
                        imageDAO.writeImage(imageData);
                        // Replace the image uri by the indexed image ID
                        toscaInheritableElement.getValue().getTags().put(ALIEN_ICON_TAG, iconId);
                    } else {
                        throw new CSARImportImageException(alienIconTagPath, CSARErrorCode.MISSING_ICON_FILE, "Icon not found in tag <" + ALIEN_ICON_TAG
                                + "> at path <" + iconPath + ">");
                    }
                } catch (NoSuchFileException | InvalidPathException e) {
                    throw new CSARImportImageException(alienIconTagPath, CSARErrorCode.MISSING_ICON_FILE, "Icon not found in tag <" + ALIEN_ICON_TAG
                            + "> at path <" + iconPath + ">", e);
                } catch (ImageUploadException e) {
                    throw new CSARImportImageException(alienIconTagPath, CSARErrorCode.ERRONEOUS_ICON_FILE, "Invalid archiveFile or icon path", e);
                } catch (IOException e) {
                    throw new CSARIOException("Invalid archiveFile or icon path");
                }
            }
        }
    }
}
