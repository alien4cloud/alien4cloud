package alien4cloud.tosca.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.ComplexPropertyValue;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.IValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.DataType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.Lists;

import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.tosca.model.ArchiveRoot;

/**
 * Created by lucboutier on 12/04/2017.
 */
public class ToscaParserAlien140Test extends AbstractToscaParserSimpleProfileTest {
    @Override
    protected String getRootDirectory() {
        return "src/test/resources/tosca/alien_dsl_1_4_0/";
    }

    @Override
    protected String getToscaVersion() {
        return "alien_dsl_1_4_0";
    }

    @Test
    public void testServiceRelationshipSubstitution() throws FileNotFoundException, ParsingException {
        Mockito.reset(csarRepositorySearchService);
        Mockito.when(csarRepositorySearchService.getArchive("tosca-normative-types", "1.0.0-ALIEN14")).thenReturn(Mockito.mock(Csar.class));
        NodeType mockRoot = Mockito.mock(NodeType.class);
        Mockito.when(mockRoot.isAbstract()).thenReturn(true);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(mockRoot);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq("tosca.capabilities.Root"),
                Mockito.any(Set.class))).thenReturn(Mockito.mock(CapabilityType.class));
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(RelationshipType.class), Mockito.eq("tosca.relationships.Root"),
                Mockito.any(Set.class))).thenReturn(Mockito.mock(RelationshipType.class));

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "substitution_mapping_service_relationship.yml"));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());

        ArchiveRoot archiveRoot = parsingResult.getResult();
        Assert.assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        Assert.assertEquals("org.alien4cloud.relationships.test.MyRelationship",
                archiveRoot.getTopology().getSubstitutionMapping().getCapabilities().get("subst_capa").getServiceRelationshipType());
    }

    @Test
    public void testCapabilitiesComplexProperty() throws ParsingException {
        Mockito.reset(csarRepositorySearchService);

        Csar csar = new Csar("tosca-normative-types", "1.0.0-ALIEN14");
        Mockito.when(csarRepositorySearchService.getArchive(csar.getName(), csar.getVersion())).thenReturn(csar);
        NodeType mockedResult = Mockito.mock(NodeType.class);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);
        CapabilityType mockedCapabilityResult = Mockito.mock(CapabilityType.class);

        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq("tosca.capabilities.Root"),
                Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);

        DataType mockedDataType = new DataType();
        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(DataType.class), Mockito.eq("tosca.datatypes.Root"), Mockito.any(Set.class)))
                .thenReturn(mockedDataType);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "capa_complex_props.yml"));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());

        ArchiveRoot archiveRoot = parsingResult.getResult();

        // check the capabilityType
        //////////////
        CapabilityType capaType = archiveRoot.getCapabilityTypes().values().stream().findFirst().get();
        assertNotNull(capaType.getProperties());
        Assert.assertEquals(3, capaType.getProperties().size());

        // map property
        String map = "map";
        PropertyDefinition propertyDefinition = capaType.getProperties().get(map);

        assertNotNull(propertyDefinition.getDefault());
        assertTrue(propertyDefinition.getDefault() instanceof ComplexPropertyValue);

        Map<String, Object> propertyMapValue = ((ComplexPropertyValue) propertyDefinition.getDefault()).getValue();
        assertNotNull(propertyMapValue);
        Assert.assertEquals(2, propertyMapValue.size());
        Assert.assertEquals("toto_value", propertyMapValue.get("toto"));
        Assert.assertEquals("tata_value", propertyMapValue.get("tata"));

        // custom property
        String custom = "custom";
        propertyDefinition = capaType.getProperties().get(custom);

        assertEquals("alien.test.datatypes.Custom", propertyDefinition.getType());
        assertNull(propertyDefinition.getDefault());

        // custom_with_default property
        String custom_with_default = "custom_with_default";
        propertyDefinition = capaType.getProperties().get(custom_with_default);
        assertNotNull(propertyDefinition.getDefault());
        assertTrue(propertyDefinition.getDefault() instanceof ComplexPropertyValue);

        propertyMapValue = ((ComplexPropertyValue) propertyDefinition.getDefault()).getValue();
        assertNotNull(propertyMapValue);
        assertEquals(2, propertyMapValue.size());
        assertEquals("defaultName", propertyMapValue.get("name"));

        Object list = propertyMapValue.get("groups");
        assertTrue(list instanceof List);
        assertEquals(2, ((List) list).size());
        assertTrue(CollectionUtils.containsAll((List) list, Lists.newArrayList("alien", "fastconnect")));

        // check the node template capability
        //////////////

        NodeTemplate nodeTemplate = archiveRoot.getTopology().getNodeTemplates().values().stream().findFirst().get();
        Capability capability = nodeTemplate.getCapabilities().values().stream().findFirst().get();
        assertNotNull(capability);
        Assert.assertEquals(3, capability.getProperties().size());

        // map property
        AbstractPropertyValue propertyValue = capability.getProperties().get(map);
        assertNotNull(propertyValue);
        assertTrue(propertyValue instanceof ComplexPropertyValue);

        propertyMapValue = ((ComplexPropertyValue) propertyValue).getValue();
        assertNotNull(propertyMapValue);
        Assert.assertEquals(2, propertyMapValue.size());
        Assert.assertEquals("toto_value", propertyMapValue.get("toto"));
        Assert.assertEquals("tata_value", propertyMapValue.get("tata"));

        // custom property
        propertyValue = capability.getProperties().get(custom);
        assertNotNull(propertyValue);
        assertTrue(propertyValue instanceof ComplexPropertyValue);
        propertyMapValue = ((ComplexPropertyValue) propertyValue).getValue();
        assertNotNull(propertyMapValue);
        assertEquals(2, propertyMapValue.size());
        assertEquals("manual", propertyMapValue.get("name"));

        list = propertyMapValue.get("groups");
        assertTrue(list instanceof List);
        assertEquals(2, ((List) list).size());
        assertTrue(CollectionUtils.containsAll((List) list, Lists.newArrayList("manual_alien", "manual_fastconnect")));

        // custom_with_default property
        propertyValue = capability.getProperties().get(custom_with_default);
        assertNotNull(propertyValue);
        assertTrue(propertyValue instanceof ComplexPropertyValue);

        propertyMapValue = ((ComplexPropertyValue) propertyValue).getValue();
        assertNotNull(propertyMapValue);
        assertEquals(2, propertyMapValue.size());
        assertEquals("defaultName", propertyMapValue.get("name"));

        list = propertyMapValue.get("groups");
        assertTrue(list instanceof List);
        assertEquals(2, ((List) list).size());
        assertTrue(CollectionUtils.containsAll((List) list, Lists.newArrayList("alien", "fastconnect")));

    }

    @Test
    public void testInterfaceInputs() throws ParsingException {
        Mockito.reset(csarRepositorySearchService);
        Mockito.when(csarRepositorySearchService.getArchive("tosca-normative-types", "1.0.0-ALIEN14")).thenReturn(Mockito.mock(Csar.class));
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(Mockito.mock(NodeType.class));

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "interface-inputs.yml"));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
        Map<String, IValue> createInputs = parsingResult.getResult().getNodeTypes().get("org.alien4cloud.test.parsing.InterfaceInputsTestNode").getInterfaces()
                .get(ToscaNodeLifecycleConstants.STANDARD).getOperations().get("create").getInputParameters();
        assertNotNull(createInputs);
        assertEquals(2, createInputs.size());
        assertNotNull(createInputs.get("prop_definition"));
        assertTrue(createInputs.get("prop_definition") instanceof PropertyDefinition);
        assertNotNull(createInputs.get("prop_assignment"));
        assertTrue(createInputs.get("prop_assignment") instanceof FunctionPropertyValue);

        Map<String, IValue> startInputs = parsingResult.getResult().getNodeTypes().get("org.alien4cloud.test.parsing.InterfaceInputsTestNode").getInterfaces()
                .get(ToscaNodeLifecycleConstants.STANDARD).getOperations().get("start").getInputParameters();
        assertNotNull(startInputs);
        assertEquals(3, startInputs.size());
        assertNotNull(startInputs.get("prop_definition"));
        assertTrue(startInputs.get("prop_definition") instanceof PropertyDefinition);
        assertNotNull(startInputs.get("prop_assignment"));
        assertTrue(startInputs.get("prop_assignment") instanceof PropertyValue);
        assertNotNull(startInputs.get("new_input"));
        assertTrue(startInputs.get("new_input") instanceof PropertyValue);
    }

    @Test
    public void testDuplicateNodeTemplate() throws ParsingException {
        Mockito.reset(csarRepositorySearchService);
        Mockito.when(csarRepositorySearchService.getArchive("tosca-normative-types", "1.0.0-ALIEN14")).thenReturn(Mockito.mock(Csar.class));
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(Mockito.mock(NodeType.class));

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "topo-duplicate-node-template.yml"));
        Assert.assertEquals(1, parsingResult.getContext().getParsingErrors().size());

    }
}