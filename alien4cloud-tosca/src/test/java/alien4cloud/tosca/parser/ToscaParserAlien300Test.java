package alien4cloud.tosca.parser;

import alien4cloud.services.PropertyDefaultValueService;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.context.ToscaContextual;
import alien4cloud.tosca.model.ArchiveRoot;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.ComplexPropertyValue;
import org.alien4cloud.tosca.model.definitions.ListPropertyValue;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.types.DataType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.junit.Test;
import org.mockito.Mockito;

import javax.annotation.Resource;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;

public class ToscaParserAlien300Test extends AbstractToscaParserSimpleProfileTest {
    @Override
    protected String getRootDirectory() {
        return "src/test/resources/tosca/alien_dsl_3_0_0/";
    }

    @Override
    protected String getToscaVersion() {
        return "alien_dsl_3_0_0";
    }

    @Resource
    private PropertyDefaultValueService defaultValueService;

    private void mockNormativeTypes() {
        Mockito.reset(csarRepositorySearchService);
        Mockito.when(csarRepositorySearchService.getArchive("tosca-normative-types", "1.0.0-ALIEN20")).thenReturn(Mockito.mock(Csar.class));
        NodeType nodeType = new NodeType();
        nodeType.setElementId("tosca.nodes.Root");
        nodeType.setArchiveName("tosca-normative-types");
        nodeType.setArchiveVersion("1.0.0-ALIEN20");
        nodeType.setAbstract(true);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(nodeType);

        DataType dataType = new DataType();
        nodeType.setElementId("tosca.datatypes.Root");
        nodeType.setArchiveName("tosca-normative-types");
        nodeType.setArchiveVersion("1.0.0-ALIEN20");
        nodeType.setAbstract(true);

        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(DataType.class), Mockito.eq("tosca.datatypes.Root"), Mockito.any(Set.class)))
                .thenReturn(dataType);
    }

    private void mockArchive(ArchiveRoot archiveRoot) {
        for (NodeType nodeType : archiveRoot.getNodeTypes().values()) {
            Mockito.when(
                    csarRepositorySearchService.getElementInDependencies(
                            Mockito.eq(NodeType.class),
                            Mockito.eq(nodeType.getElementId()),
                            Mockito.any(Set.class)
                    )).thenReturn(nodeType);
        }
        for (DataType dataType : archiveRoot.getDataTypes().values()) {
            Mockito.when(
                    csarRepositorySearchService.getElementInDependencies(
                            Mockito.eq(DataType.class),
                            Mockito.eq(dataType.getElementId()),
                            Mockito.any(Set.class)
                    )).thenReturn(dataType);
        }
    }

    private <T> T checkedConversion(Class<T> clazz,Object v) {
        assertThat(v,instanceOf(clazz));
        return (T) v;
    }

    private void assertScalarStringProp(Map<String,?> map, String key, String value) {
        assertThat(map,hasKey(key));
        assertThat(checkedConversion(ScalarPropertyValue.class,map.get(key)).getValue(),equalTo(value));
    }

    private void assertStringProp(Map<String,?> map, String key, String value) {
        assertThat(map,hasKey(key));
        assertThat(checkedConversion(String.class,map.get(key)),equalTo(value));
    }

    @Test
    public void parsingTest() throws ParsingException {
        ComplexPropertyValue cv0;
        ListPropertyValue lv0;
        Map<String,Object> m0;

        mockNormativeTypes();
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "default-props.yml"));
        assertEquals(0, parsingResult.getContext().getParsingErrors().size());

        ArchiveRoot archiveRoot = parsingResult.getResult();

        NodeTemplate mock1 = archiveRoot.getTopology().getNodeTemplates().get("Mock1");
        // Check that mock1 contains default value for properties initialized at root level
        // (ie. no recursion inside datatypes)

        // Empty mandatory property with default values must be accepted even if the parser
        // does not feed the property
        assertThat(mock1.getProperties(), hasKey("prop2"));
        assertThat(mock1.getProperties(), hasKey("prop3"));
        assertThat(mock1.getProperties(), hasKey("prop4"));
        assertThat(mock1.getProperties(), hasKey("prop5"));
        assertThat(mock1.getProperties(), hasKey("prop6"));
        assertThat(mock1.getProperties(), hasKey("prop7"));
        assertThat(mock1.getProperties(), hasKey("prop8"));

        cv0 = checkedConversion(ComplexPropertyValue.class, mock1.getProperties().get("prop2"));
        assertStringProp(cv0.getValue(), "field1", "Simple_Value1");
        assertStringProp(cv0.getValue(), "field2", "Simple_Value2");

        cv0 = checkedConversion(ComplexPropertyValue.class, mock1.getProperties().get("prop3"));
        assertThat(cv0.getValue(), not(hasKey("field1")));
        assertStringProp(cv0.getValue(), "field2", "Mock_Value2");

        cv0 = checkedConversion(ComplexPropertyValue.class, mock1.getProperties().get("prop4"));
        m0 = checkedConversion(Map.class, cv0.getValue().get("k0"));
        assertThat(m0.keySet(),hasSize(1));
        assertStringProp(m0, "field1", "prop4_k0_value1");
        m0 = checkedConversion(Map.class, cv0.getValue().get("k1"));
        assertThat(m0.keySet(),hasSize(1));
        assertStringProp(m0, "field1", "prop4_k1_value1");

        cv0 = checkedConversion(ComplexPropertyValue.class, mock1.getProperties().get("prop5"));
        m0 = checkedConversion(Map.class, cv0.getValue().get("k0"));
        assertThat(m0.keySet(),hasSize(1));
        assertStringProp(m0, "field1", "prop5_k0_value1");
        m0 = checkedConversion(Map.class, cv0.getValue().get("k1"));
        assertThat(m0.keySet(),hasSize(1));
        assertStringProp(m0, "field1", "prop5_k1_value1");

        lv0 = checkedConversion(ListPropertyValue.class, mock1.getProperties().get("prop6"));
        m0 = checkedConversion(Map.class, lv0.getValue().get(0));
        assertThat(m0.keySet(),hasSize(1));
        assertStringProp(m0, "field1", "prop6_0_value1");
        m0 = checkedConversion(Map.class, lv0.getValue().get(1));
        assertThat(m0.keySet(),hasSize(1));
        assertStringProp(m0, "field1", "prop6_1_value1");

        lv0 = checkedConversion(ListPropertyValue.class, mock1.getProperties().get("prop7"));
        m0 = checkedConversion(Map.class, lv0.getValue().get(0));
        assertThat(m0.keySet(),hasSize(1));
        assertStringProp(m0, "field1", "prop7_0_value1");
        m0 = checkedConversion(Map.class, lv0.getValue().get(1));
        assertThat(m0.keySet(),hasSize(1));
        assertStringProp(m0, "field1", "prop7_1_value1");

        cv0 = checkedConversion(ComplexPropertyValue.class, mock1.getProperties().get("prop8"));
        assertThat(cv0.getValue().keySet(),hasSize(1));
        assertStringProp(cv0.getValue(), "nested1", "Nested_Value1");
    }

    @Test
    public void defaultTest() throws ParsingException {
        ComplexPropertyValue cv0;
        ListPropertyValue lv0;
        Map<String,Object> m0;

        mockNormativeTypes();
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "default-props.yml"));
        assertEquals(0, parsingResult.getContext().getParsingErrors().size());

        ArchiveRoot archiveRoot = parsingResult.getResult();

        mockArchive(archiveRoot);

        ToscaContext.init(new HashSet<>());
        ToscaContext.setCsarRepositorySearchService(csarRepositorySearchService);

        try {
            NodeTemplate mock1 = archiveRoot.getTopology().getNodeTemplates().get("Mock1");

            var fedProperties = defaultValueService.feedDefaultValues(mock1);

            // Check that the properties has been fed with their defaults
            assertThat(fedProperties, hasKey("prop3"));
            assertThat(fedProperties, hasKey("prop4"));
            assertThat(fedProperties, hasKey("prop5"));

            cv0 = checkedConversion(ComplexPropertyValue.class, fedProperties.get("prop3"));
            assertStringProp(cv0.getValue(), "field1", "Simple_Value1"); // Injected default
            assertStringProp(cv0.getValue(), "field2", "Mock_Value2"); // Coming from Node Template

            cv0 = checkedConversion(ComplexPropertyValue.class, fedProperties.get("prop4"));
            m0 = checkedConversion(Map.class, cv0.getValue().get("k0"));
            assertThat(m0.keySet(),hasSize(2));
            assertStringProp(m0, "field1", "prop4_k0_value1"); // Coming from NodeType
            assertStringProp(m0, "field2", "Simple_Value2");  // Injected default
            m0 = checkedConversion(Map.class, cv0.getValue().get("k1"));
            assertThat(m0.keySet(),hasSize(2));
            assertStringProp(m0, "field1", "prop4_k1_value1"); // Coming from NodeType
            assertStringProp(m0, "field2", "Simple_Value2");  // Injected default

            cv0 = checkedConversion(ComplexPropertyValue.class, fedProperties.get("prop5"));
            m0 = checkedConversion(Map.class, cv0.getValue().get("k0"));
            assertThat(m0.keySet(),hasSize(2));
            assertStringProp(m0, "field1", "prop5_k0_value1"); // Coming from NodeTemplate
            assertStringProp(m0, "field2", "Simple_Value2");  // Injected default
            m0 = checkedConversion(Map.class, cv0.getValue().get("k1"));
            assertThat(m0.keySet(),hasSize(2));
            assertStringProp(m0, "field1", "prop5_k1_value1"); // Coming from NodeTemplate
            assertStringProp(m0, "field2", "Simple_Value2");  // Injected default

            lv0 = checkedConversion(ListPropertyValue.class, fedProperties.get("prop6"));
            m0 = checkedConversion(Map.class, lv0.getValue().get(0));
            assertThat(m0.keySet(),hasSize(2));
            assertStringProp(m0, "field1", "prop6_0_value1"); // Coming from NodeType
            assertStringProp(m0, "field2", "Simple_Value2");  // Injected default
            m0 = checkedConversion(Map.class, lv0.getValue().get(1));
            assertThat(m0.keySet(),hasSize(2));
            assertStringProp(m0, "field1", "prop6_1_value1"); // Coming from NodeType
            assertStringProp(m0, "field2", "Simple_Value2");  // Injected default

            lv0 = checkedConversion(ListPropertyValue.class, fedProperties.get("prop7"));
            m0 = checkedConversion(Map.class, lv0.getValue().get(0));
            assertThat(m0.keySet(),hasSize(2));
            assertStringProp(m0, "field1", "prop7_0_value1"); // Coming from NodeTemplate
            assertStringProp(m0, "field2", "Simple_Value2");  // Injected default
            m0 = checkedConversion(Map.class, lv0.getValue().get(1));
            assertThat(m0.keySet(),hasSize(2));
            assertStringProp(m0, "field1", "prop7_1_value1"); // Coming from NodeTemplate
            assertStringProp(m0, "field2", "Simple_Value2");  // Injected default

            cv0 = checkedConversion(ComplexPropertyValue.class, fedProperties.get("prop8"));
            assertThat(cv0.getValue().keySet(),hasSize(3)); // Coming from NodeTemplate (injected by parser)
            assertStringProp(cv0.getValue(), "nested1", "Nested_Value1");
            m0 = checkedConversion(Map.class, cv0.getValue().get("nested2"));
            assertStringProp(m0, "field1", "Simple_Value1"); // Injected default by Simple DT
            assertStringProp(m0, "field2", "Simple_Value2"); // Injected default by Simple DT
            m0 = checkedConversion(Map.class, cv0.getValue().get("nested3"));
            assertStringProp(m0, "field1", "Nested3_Default_value1"); // Injected default by Nested DT
            assertStringProp(m0, "field2", "Simple_Value2"); // Injected default by Simple DT
        } finally {
            ToscaContext.destroy();
        }
    }
}
