package alien4cloud.utils;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Utility to resize images.
 * 
 * @author luc boutier
 */
public final class ImageResizeUtil {
    private ImageResizeUtil() {
    }

    /**
     * Resize an image with default quality settings.
     * 
     * @param originalImage The image to resize.
     * @param width The target width.
     * @param height The target height.
     * @param preserveDimensions Flag to know if we should preserve original image dimensions.
     * @return The resized image.
     */
    public static BufferedImage resizeImage(final BufferedImage originalImage, final int width, final int height,
            final boolean preserveDimensions) {
        return resizeImage(originalImage, width, height, preserveDimensions, false);
    }

    /**
     * <p>
     * Resize an image with high quality settings.
     * </p>
     * <ul>
     * <li>g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);</li>
     * <li>g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);</li>
     * <li>g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);</li>
     * </ul>
     * 
     * @param originalImage The image to resize.
     * @param width The target width.
     * @param height The target height.
     * @param preserveDimensions Flag to know if we should preserve original image dimensions.
     * @return The resized image.
     */
    public static BufferedImage resizeImageWithHint(BufferedImage originalImage, final int width, final int height,
            final boolean preserveDimensions) {
        return resizeImage(originalImage, width, height, preserveDimensions, true);
    }

    private static BufferedImage resizeImage(BufferedImage originalImage, final int width, final int height,
            final boolean preserveDimensions, final boolean enableHighQuality) {
        int type = originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType();

        int targetWidth = width;
        int targetHeight = height;

        if (preserveDimensions) {
            int[] targetDimentions = computeDimensions(width, height, originalImage.getWidth(), originalImage.getHeight());
            targetWidth = targetDimentions[0];
            targetHeight = targetDimentions[1];
        }

        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, type);

        Graphics2D g = resizedImage.createGraphics();
        if (enableHighQuality) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }

        g.drawImage(originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH), 0, 0, null);
        g.dispose();

        return resizedImage;
    }

    /**
     * Compute target width and height based on requested width and height but making sure the original dimensions of the image will be preserved.
     * 
     * @param width The ideal (and max) target width.
     * @param height The ideal (and max) target height.
     * @param originalWidth The original width.
     * @param originalHeight The original height.
     * @return An array of int that contains the ideal width and height to preserve dimensions.
     */
    public static int[] computeDimensions(final int width, final int height, final int originalWidth, final int originalHeight) {
        int targetWidth = width;
        int targetHeight = height;

        float targetDimensions = Float.valueOf(width).floatValue() / Float.valueOf(height).floatValue();
        float sourceDimensions = Float.valueOf(originalWidth).floatValue() / Float.valueOf(originalHeight).floatValue();
        if (targetDimensions > sourceDimensions) {
            targetWidth = Float.valueOf(width * sourceDimensions / targetDimensions).intValue();
        } else {
            targetHeight = Float.valueOf(height * targetDimensions / sourceDimensions).intValue();
        }

        return new int[] { targetWidth, targetHeight };
    }
}
