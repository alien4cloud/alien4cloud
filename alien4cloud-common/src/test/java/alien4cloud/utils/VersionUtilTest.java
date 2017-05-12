package alien4cloud.utils;

import org.junit.Assert;
import org.junit.Test;

import alien4cloud.utils.version.InvalidVersionException;

/**
 * Created by igor on 12/05/17.
 */
public class VersionUtilTest {

    @Test
    public void isQualifierValid() throws Exception {
        Assert.assertTrue(VersionUtil.isQualifierValid("toto"));
        Assert.assertTrue(VersionUtil.isQualifierValid("123"));
        Assert.assertTrue(VersionUtil.isQualifierValid("toto123"));
        Assert.assertTrue(VersionUtil.isQualifierValid("toto-233"));
        Assert.assertTrue(VersionUtil.isQualifierValid("toto_233"));

        // failure
        Assert.assertFalse(VersionUtil.isQualifierValid("1cd&&"));
        Assert.assertFalse(VersionUtil.isQualifierValid("snapshot"));
        Assert.assertFalse(VersionUtil.isQualifierValid("DEV-snapshot"));
        Assert.assertFalse(VersionUtil.isQualifierValid("DEV-SNAPSHOT"));
        Assert.assertFalse(VersionUtil.isQualifierValid("Dev-SnAPSHoT-toto"));
    }

    @Test(expected = InvalidVersionException.class)
    public void isQualifierValidOrFail() throws Exception {
        VersionUtil.isQualifierValidOrFail("Dev-SnAPSHoT-toto");
    }

}