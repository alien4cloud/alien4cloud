package alien4cloud.utils;

import org.junit.Assert;
import org.junit.Test;

import alien4cloud.utils.VersionUtil;
import alien4cloud.utils.version.InvalidVersionException;

public class VersionUtilTest {

    @Test
    public void testValidVersion() {
        // Good versions
        Assert.assertTrue(VersionUtil.isValid("1"));
        Assert.assertTrue(VersionUtil.isValid("1.0"));
        Assert.assertTrue(VersionUtil.isValid("10.0.11"));
        Assert.assertTrue(VersionUtil.isValid("10.0.1.1"));
        Assert.assertTrue(VersionUtil.isValid("10.0.11-SNAPSHOT"));
        Assert.assertTrue(VersionUtil.isValid("10.0.11.alpha1"));
        Assert.assertTrue(VersionUtil.isValid("10.0.11-alpha1"));
        // Bad versions
        Assert.assertFalse(VersionUtil.isValid("toto11"));
        Assert.assertFalse(VersionUtil.isValid("11toto"));
        Assert.assertFalse(VersionUtil.isValid("10.0.11)alpha1"));
    }

    @Test
    public void testParseVersionSuccess() throws InvalidVersionException {
        Assert.assertNotNull(VersionUtil.parseVersion("1.0"));
    }

    @Test(expected = InvalidVersionException.class)
    public void testParseVersionFailed() throws InvalidVersionException {
        VersionUtil.parseVersion("BIG-RELEASE");
    }
}
