package alien4cloud.utils;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;

import alien4cloud.test.utils.YamlJsonAssert;
import alien4cloud.test.utils.YamlJsonAssert.DocumentType;
import alien4cloud.tosca.container.model.Definitions;
import alien4cloud.utils.FileUtil;
import alien4cloud.utils.YamlParserUtil;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.google.common.collect.Sets;

@Slf4j
public class YamlParserUtilTest {

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

    @Test
    public void loadingToscaBaseTypes() throws IOException {
        String toscaBaseType = "src/main/resources/tosca-base-types/1.0/Definitions/tosca-base-types.yaml";
        validateToscaYamlDefinitions(toscaBaseType);
    }

    @Test
    public void loadingToscaJavaType() throws IOException {
        String toscaBaseType = "src/main/resources/java-types/1.0/Definitions/java-types.yaml";
        validateToscaYamlDefinitions(toscaBaseType);
    }

    private void validateToscaYamlDefinitions(String file) throws IOException {
        Path filePath = Paths.get(file);
        log.info("Begin validating deserialization + serialization process for file at path [" + file + "]");
        String expected = FileUtil.readTextFile(filePath);
        String actual = YamlParserUtil.toYaml(YamlParserUtil.parseFromUTF8File(filePath, Definitions.class));
        YamlJsonAssert.assertEquals(expected, actual, Sets.newHashSet(".+/abstract", ".+/final", ".+/max_instances", ".+/min_instances", ".+/lower_bound",
                ".+/upper_bound", ".+interfaces/lifecycle/operations/.+", ".+/properties/.+/required", ".+/properties/.+/password",
                ".+/constraints/.+/range_min_value", ".+/constraints/.+/range_max_value"), DocumentType.YAML);
        log.info("Succeeded validating deserialization + serialization process for file at path [" + file + "]");
    }
}
