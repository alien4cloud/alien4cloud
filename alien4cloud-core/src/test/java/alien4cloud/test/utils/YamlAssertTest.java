package alien4cloud.test.utils;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.Test;

import alien4cloud.test.utils.YamlJsonAssert.DocumentType;
import alien4cloud.test.utils.YamlJsonAssert.YamlJsonNotEqualsException;
import alien4cloud.utils.FileUtil;

import com.google.common.collect.Sets;

public class YamlAssertTest {

    @Test
    public void testEquals() throws IOException {
        String expected = FileUtil.readTextFile(Paths.get("src/test/resources/alien/test/utils/valid/expected.yaml"));
        String actual = FileUtil.readTextFile(Paths.get("src/test/resources/alien/test/utils/valid/actual.yaml"));
        YamlJsonAssert.assertEquals(expected, actual, DocumentType.YAML);
    }

    @Test(expected = YamlJsonNotEqualsException.class)
    public void testValueNotEquals() throws IOException {
        String expected = FileUtil.readTextFile(Paths.get("src/test/resources/alien/test/utils/valueNotEquals/expected.yaml"));
        String actual = FileUtil.readTextFile(Paths.get("src/test/resources/alien/test/utils/valueNotEquals/actual.yaml"));
        YamlJsonAssert.assertEquals(expected, actual, DocumentType.YAML);
    }

    @Test(expected = YamlJsonNotEqualsException.class)
    public void testMissingInActual() throws IOException {
        String expected = FileUtil.readTextFile(Paths.get("src/test/resources/alien/test/utils/missingInActual/expected.yaml"));
        String actual = FileUtil.readTextFile(Paths.get("src/test/resources/alien/test/utils/missingInActual/actual.yaml"));
        YamlJsonAssert.assertEquals(expected, actual, DocumentType.YAML);
    }

    @Test(expected = YamlJsonNotEqualsException.class)
    public void testMissingInExpected() throws IOException {
        String expected = FileUtil.readTextFile(Paths.get("src/test/resources/alien/test/utils/missingInExpected/expected.yaml"));
        String actual = FileUtil.readTextFile(Paths.get("src/test/resources/alien/test/utils/missingInExpected/actual.yaml"));
        YamlJsonAssert.assertEquals(expected, actual, DocumentType.YAML);
    }

    @Test
    public void testMissingInExpectedButIgnored() throws IOException {
        String expected = FileUtil.readTextFile(Paths.get("src/test/resources/alien/test/utils/missingInExpected/expected.yaml"));
        String actual = FileUtil.readTextFile(Paths.get("src/test/resources/alien/test/utils/missingInExpected/actual.yaml"));
        YamlJsonAssert.assertEquals(expected, actual, Sets.newHashSet("/persons/\\[1\\]/brad_pitt/profession"), DocumentType.YAML);
    }
}
