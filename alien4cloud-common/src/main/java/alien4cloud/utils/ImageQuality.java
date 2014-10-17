package alien4cloud.utils;

/**
 * Available qualities for images in Alien 4 Cloud.
 *
 * @author luc boutier
 */
public enum ImageQuality {
    QUALITY_16(16), QUALITY_32(32), QUALITY_64(64), QUALITY_128(128), QUALITY_512(512), QUALITY_BEST(-1);

    private final int size;

    private ImageQuality(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }
}
