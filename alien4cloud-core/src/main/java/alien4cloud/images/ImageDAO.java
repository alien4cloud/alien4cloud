package alien4cloud.images;

import java.awt.image.BufferedImage;
import java.beans.IntrospectionException;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.imageio.ImageIO;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.mapping.MappingBuilder;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import alien4cloud.dao.ESGenericIdDAO;
import alien4cloud.exception.IndexingServiceException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.images.exception.ImageUploadException;
import alien4cloud.utils.ImageQuality;
import alien4cloud.utils.ImageResizeUtil;

/**
 * A dao to store/load images.
 */
@Slf4j
@Component("image-dao")
public class ImageDAO extends ESGenericIdDAO implements IImageDAO {
    @Resource
    private MappingBuilder mappingBuilder;
    private Path rootPath;

    @Required
    @Value("${directories.alien}/${directories.images}")
    public void setRootPath(String path) throws IOException {
        this.rootPath = Paths.get(path).toAbsolutePath();
        if (!Files.exists(rootPath)) {
            Files.createDirectories(rootPath);
        }
    }

    @PostConstruct
    public void initEnvironment() {
        // init ES annotation scanning
        try {
            mappingBuilder.initialize(ImageData.class.getPackage().getName());
        } catch (IntrospectionException | IOException e) {
            throw new IndexingServiceException("Could not initialize elastic search mapping builder", e);
        }
        // init indexes and mapped classes
        initIndices(ImageData.class.getSimpleName().toLowerCase(), null, ImageData.class);
        initCompleted();
    }

    @Override
    public String writeImage(byte[] imageBytes) {
        String iconId = UUID.randomUUID().toString();
        ImageData imageData = new ImageData();
        imageData.setData(imageBytes);
        imageData.setId(iconId);
        writeImage(imageData);
        return iconId;
    }

    @Override
    public void writeImage(final ImageData imageData) {
        // resize the image to store the different available qualities.
        InputStream is = new ByteArrayInputStream(imageData.getData());
        try {
            BufferedImage original = ImageIO.read(is);
            if (original == null) {
                throw new ImageUploadException("The image is not valid and cannot be read");
            }
            resizeAndWrite(getImageId(ImageQuality.QUALITY_16, imageData.getId()), original, ImageQuality.QUALITY_16.getSize());
            resizeAndWrite(getImageId(ImageQuality.QUALITY_32, imageData.getId()), original, ImageQuality.QUALITY_32.getSize());
            resizeAndWrite(getImageId(ImageQuality.QUALITY_64, imageData.getId()), original, ImageQuality.QUALITY_64.getSize());
            resizeAndWrite(getImageId(ImageQuality.QUALITY_128, imageData.getId()), original, ImageQuality.QUALITY_128.getSize());
            // resizeAndWrite(getImageId(ImageQuality.QUALITY_512, imageData.getId()), original, ImageQuality.QUALITY_512.getSize());

            saveAsPng(imageData.getId(), original);
        } catch (IOException e) {
            throw new ImageUploadException("Unable to write uploaded image to data source", e);
        }
    }

    private void resizeAndWrite(final String imageId, final BufferedImage original, final int size) throws IOException {
        BufferedImage target = ImageResizeUtil.resizeImageWithHint(original, size, size, true);
        saveAsPng(imageId, target);
    }

    private void saveAsPng(String imageId, BufferedImage target) throws IOException {
        FileOutputStream fos = new FileOutputStream(rootPath.resolve(imageId + ".png").toFile());
        try {
            ImageIO.write(target, "png", fos);
            fos.flush();
        } finally {
            fos.close();
        }
        // save in elastic search
        ImageData imageData = new ImageData();
        imageData.setId(imageId);
        imageData.setMime("image/png");
        this.save(imageData);
    }

    @Override
    public ImageData readImage(final String id, ImageQuality imageQuality) {
        ImageData imageData = findById(ImageData.class, getImageId(imageQuality, id));
        if (imageData == null) {
            throw new NotFoundException("Unable to find image.");
        }
        if (imageData.getData() == null) {
            try {
                imageData.setData(Files.readAllBytes(rootPath.resolve(imageData.getId() + ".png")));
            } catch (IOException e) {
                throw new NotFoundException("Unable to find image on disk.");
            }
        }
        return imageData;
    }

    @Override
    public void delete(String id) {
        delete(ImageData.class, id);
    }

    private String getImageId(final ImageQuality imageQuality, final String id) {
        switch (imageQuality) {
        case QUALITY_16:
            return imageQuality.name() + id;
        case QUALITY_32:
            return imageQuality.name() + id;
        case QUALITY_64:
            return imageQuality.name() + id;
        case QUALITY_128:
            return imageQuality.name() + id;
        case QUALITY_512:
            return imageQuality.name() + id;
        default:
            return id;
        }
    }
}