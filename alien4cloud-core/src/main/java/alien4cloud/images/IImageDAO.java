package alien4cloud.images;

import alien4cloud.utils.ImageQuality;

/**
 * DAO to manage image upload and retrieval.
 * 
 * @author luc boutier
 */
public interface IImageDAO {

    /**
     * Save an image in the DAO layer.
     * 
     * @param imageBytes
     */
    String writeImage(byte[] imageBytes);

    /**
     * Save an image in the DAO layer.
     * 
     * @param imageData
     */
    void writeImage(ImageData imageData);

    /**
     * Get an image as a byte array based on the image id.
     * 
     * @param id The id of the image to read.
     * @param imageQuality The level of quality of the image to get.
     * @return The image as a byte array.
     */
    ImageData readImage(String id, ImageQuality imageQuality);

    /**
     * Delete the given image.
     * 
     * @param id Id of the image to delete.
     */
    void delete(String id);

    /**
     * Delete all images build from original
     *
     * @param id Id of the ORIGINAL image to delete
     */
    void deleteAll(String id);
}