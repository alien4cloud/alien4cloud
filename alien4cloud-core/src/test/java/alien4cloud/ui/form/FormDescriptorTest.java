package alien4cloud.ui.form;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.test.utils.YamlJsonAssert;
import alien4cloud.test.utils.YamlJsonAssert.DocumentType;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import alien4cloud.utils.FileUtil;

public class FormDescriptorTest {

    private PojoFormDescriptorGenerator generator = new PojoFormDescriptorGenerator();

    @Before
    public void setUp() throws Exception {
        generator.setPropertyDefinitionConverter(new PropertyDefinitionConverter());
    }

    @Test
    public void testPropertyDefinition() throws IOException {
        Map<String, Object> metaModel = generator.generateDescriptor(PropertyDefinition.class);
        String actual = JsonUtil.toString(metaModel);
        String expected = FileUtil.readTextFile(Paths.get("./src/test/resources/alien/ui/form/PropertyDefinition.json"));
        YamlJsonAssert.assertEquals(expected, actual, DocumentType.JSON);
    }

    @Test
    public void testAllPossibleManagedTypes() throws IOException {
        Map<String, Object> metaModel = generator.generateDescriptor(FormExampleObject.class);
        String actual = JsonUtil.toString(metaModel);
        String expected = FileUtil.readTextFile(Paths.get("./src/test/resources/alien/ui/form/FormExampleObject.json"));
        YamlJsonAssert.assertEquals(expected, actual, DocumentType.JSON);
    }

    @Test
    public void testFormPropertiesAnnotation() throws IOException {
        Map<String, Object> metaModel = generator.generateDescriptor(FormPropertiesExampleObject.class);
        String actual = JsonUtil.toString(metaModel);
        String expected = FileUtil.readTextFile(Paths.get("./src/test/resources/alien/ui/form/FormPropertiesExampleObject.json"));
        YamlJsonAssert.assertEquals(expected, actual, DocumentType.JSON);
    }

    @Test
    public void testTypeExtendBaseClass() throws IOException {
        Map<String, Object> metaModel = generator.generateDescriptor(FormExtendBaseClassExampleObject.class);
        String actual = JsonUtil.toString(metaModel);
        String expected = FileUtil.readTextFile(Paths.get("./src/test/resources/alien/ui/form/FormExtendBaseClassExampleObject.json"));
        YamlJsonAssert.assertEquals(expected, actual, DocumentType.JSON);
    }

    @Test
    public void testSpecificType() throws IOException {
        Map<String, Object> metaModel = generator.generateDescriptor(FormSpecificTypeExampleObject.class);
        String actual = JsonUtil.toString(metaModel);
        String expected = FileUtil.readTextFile(Paths.get("./src/test/resources/alien/ui/form/FormSpecificTypeExampleObject.json"));
        YamlJsonAssert.assertEquals(expected, actual, DocumentType.JSON);
    }

    @Test
    public void testToscaType() throws IOException {
        Map<String, Object> metaModel = generator.generateDescriptor(FormToscaTypeExampleObject.class);
        String actual = JsonUtil.toString(metaModel);
        String expected = FileUtil.readTextFile(Paths.get("./src/test/resources/alien/ui/form/FormToscaTypeExampleObject.json"));
        YamlJsonAssert.assertEquals(expected, actual, DocumentType.JSON);
    }
}
