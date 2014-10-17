package alien4cloud.utils;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import alien4cloud.utils.ImageResizeUtil;

public class ImageResizeUtilTest {
    @Test
    public void testComputeDimensions() throws IOException {
        int[] results = ImageResizeUtil.computeDimensions(64, 64, 500, 250);
        Assert.assertEquals(64, results[0]);
        Assert.assertEquals(32, results[1]);
        results = ImageResizeUtil.computeDimensions(64, 64, 250, 500);
        Assert.assertEquals(32, results[0]);
        Assert.assertEquals(64, results[1]);
    }
}
