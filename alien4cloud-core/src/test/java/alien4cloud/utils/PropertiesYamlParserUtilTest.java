package alien4cloud.utils;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.NoSuchFileException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PropertiesYamlParserUtilTest {

    @Test
    public void testParsingValidFile() throws IOException {
        SimpleYaml simpleYaml = YamlParserUtil.parseFromUTF8File("src/test/resources/alien/utils/validfile.yaml", SimpleYaml.class);
        assertNotNull(simpleYaml);
        assertEquals("id", simpleYaml.getId());
        assertEquals("value", simpleYaml.getValue());
        assertNotNull(simpleYaml.getInnerYaml());
        assertEquals("id", simpleYaml.getInnerYaml().getId());
        assertEquals("toto", simpleYaml.getInnerYaml().getToto());
    }

    @Test(expected = NoSuchFileException.class)
    public void testParsingInexistantFile() throws IOException {
        YamlParserUtil.parseFromUTF8File("donotexists.yaml", SimpleYaml.class);
    }

    @Test(expected = UnrecognizedPropertyException.class)
    public void testParsingInValidFile() throws IOException {
        YamlParserUtil.parseFromUTF8File("src/test/resources/alien/utils/invalidfile.yaml", SimpleYaml.class);
    }
}
