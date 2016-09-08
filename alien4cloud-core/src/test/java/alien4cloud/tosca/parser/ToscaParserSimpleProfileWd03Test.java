package alien4cloud.tosca.parser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.model.components.*;
import alien4cloud.model.components.constraints.GreaterThanConstraint;
import alien4cloud.model.components.constraints.LessThanConstraint;
import alien4cloud.model.components.constraints.MaxLengthConstraint;
import alien4cloud.model.components.constraints.MinLengthConstraint;
import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.tosca.ArchiveParserTest;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.utils.MapUtil;

/**
 * Test tosca parsing for Tosca Simple profile in YAML wd03
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:tosca/parser-application-context.xml")
public class ToscaParserSimpleProfileWd03Test extends AbstractToscaParserSimpleProfileTest {

    @Override
    protected String getRootDirectory() {
        return "src/test/resources/tosca/SimpleProfile_wd03/parsing/";
    }

    @Override
    protected String getToscaVersion() {
        return "tosca_simple_yaml_1_0_0_wd03";
    }

    @Resource
    private ToscaParser parser;

    @Resource
    private ICSARRepositorySearchService repositorySearchService;

    @Test(expected = ParsingException.class)
    public void testDefinitionVersionInvalidYaml() throws FileNotFoundException, ParsingException {
        parser.parseFile(Paths.get(getRootDirectory(), "tosca-definition-version-invalid.yml"));
    }

    @Test(expected = ParsingException.class)
    public void testDefinitionVersionUnknown() throws FileNotFoundException, ParsingException {
        parser.parseFile(Paths.get(getRootDirectory(), "tosca-definition-version-unknown.yml"));
    }

    @Test
    public void testDescriptionSingleLine() throws FileNotFoundException, ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "description-single-line.yml"));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
        ArchiveRoot archiveRoot = parsingResult.getResult();
        Assert.assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        Assert.assertNotNull(archiveRoot.getArchive().getDescription());
        Assert.assertEquals("This is an example of a single line description (no folding).", archiveRoot.getArchive().getDescription());
    }

    @Test
    public void testDescriptionMultiLine() throws FileNotFoundException, ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "description-multi-line.yml"));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
        ArchiveRoot archiveRoot = parsingResult.getResult();
        Assert.assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        Assert.assertNotNull(archiveRoot.getArchive().getDescription());
        Assert.assertEquals(
                "This is an example of a multi-line description using YAML. It permits for line breaks for easier readability...\nif needed.  However, (multiple) line breaks are folded into a single space character when processed into a single string value.",
                archiveRoot.getArchive().getDescription());
    }

    @Test
    public void testRootCategories() throws FileNotFoundException, ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-root-categories.yml"));
        assertNoBlocker(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        Assert.assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        Assert.assertEquals("Tosca default namespace value", archiveRoot.getArchive().getToscaDefaultNamespace());
        Assert.assertEquals("Template name value", archiveRoot.getArchive().getName());
        Assert.assertEquals("Temlate author value", archiveRoot.getArchive().getTemplateAuthor());
        Assert.assertEquals("1.0.0-SNAPSHOT", archiveRoot.getArchive().getVersion());
        Assert.assertEquals("This is an example of a single line description (no folding).", archiveRoot.getArchive().getDescription());
    }

    @Ignore
    @Test
    public void testMissingNameFails() throws FileNotFoundException, ParsingException {
        // ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), ""));
        // Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
        // ArchiveRoot archiveRoot = parsingResult.getResult();
        // Assert.assertNotNull(archiveRoot.getArchive());
    }

    @Ignore
    @Test
    public void testImportRelative() throws FileNotFoundException, ParsingException {
        // ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-import-relative.yml"));
        // Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
        // ArchiveRoot archiveRoot = parsingResult.getResult();
        // Assert.assertNotNull(archiveRoot.getArchive());
    }

    @Ignore
    @Test
    public void testImportRelativeMissing() throws FileNotFoundException, ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-import-relative-missing.yml"));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
        ArchiveRoot archiveRoot = parsingResult.getResult();
        Assert.assertNotNull(archiveRoot.getArchive());
    }

    @Test
    public void testImportDependency() throws FileNotFoundException, ParsingException {
        Mockito.reset(repositorySearchService);
        Csar csar = new Csar("tosca-normative-types", "1.0.0-SNAPSHOT-wd03");
        // Mockito.when(repositorySearchService.getArchive(csar.getId())).thenReturn(csar);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-import-dependency.yml"));

        Mockito.verify(repositorySearchService).getArchive(csar.getId());

        assertNoBlocker(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        Assert.assertNotNull(archiveRoot.getArchive());
        Assert.assertNotNull(archiveRoot.getArchive().getDependencies());
        Assert.assertEquals(1, archiveRoot.getArchive().getDependencies().size());
        Assert.assertEquals(new CSARDependency(csar.getName(), csar.getVersion()), archiveRoot.getArchive().getDependencies().iterator().next());
    }

    @Test
    public void testImportDependencyMissing() throws FileNotFoundException, ParsingException {
        Mockito.reset(repositorySearchService);
        Csar csar = new Csar("tosca-normative-types", "1.0.0-SNAPSHOT-wd03");
        // Mockito.when(repositorySearchService.getArchive(csar.getId())).thenReturn(null);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-import-dependency.yml"));

        Mockito.verify(repositorySearchService).getArchive(csar.getId());

        assertNoBlocker(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        Assert.assertNotNull(archiveRoot.getArchive());
        Assert.assertNotNull(archiveRoot.getArchive().getDependencies());
        Assert.assertEquals(1, archiveRoot.getArchive().getDependencies().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testArtifactType() throws FileNotFoundException, ParsingException {
        Mockito.reset(repositorySearchService);
        IndexedArtifactType mockedResult = Mockito.mock(IndexedArtifactType.class);
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(IndexedArtifactType.class), Mockito.eq("tosca.artifact.Root"),
                Mockito.any(List.class))).thenReturn(mockedResult);
        List<String> derivedFromSet = Lists.newArrayList();
        Mockito.when(mockedResult.getDerivedFrom()).thenReturn(derivedFromSet);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-artifact-type.yml"));

        assertNoBlocker(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        Assert.assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        Assert.assertEquals(1, archiveRoot.getArtifactTypes().size());
        Entry<String, IndexedArtifactType> entry = archiveRoot.getArtifactTypes().entrySet().iterator().next();
        Assert.assertEquals("my_artifact_type", entry.getKey());
        IndexedArtifactType artifact = entry.getValue();
        Assert.assertEquals(Lists.newArrayList("tosca.artifact.Root"), artifact.getDerivedFrom());
        Assert.assertEquals("Java Archive artifact type", artifact.getDescription());
        Assert.assertEquals("application/java-archive", artifact.getMimeType());
        Assert.assertEquals(Lists.newArrayList("jar"), artifact.getFileExt());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCapabilityType() throws FileNotFoundException, ParsingException {
        Mockito.reset(repositorySearchService);
        IndexedCapabilityType mockedResult = Mockito.mock(IndexedCapabilityType.class);
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(IndexedCapabilityType.class), Mockito.eq("tosca.capabilities.Feature"),
                Mockito.any(List.class))).thenReturn(mockedResult);
        Mockito.when(mockedResult.getDerivedFrom()).thenReturn(Lists.newArrayList("tosca.capabilities.Root"));

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-capability-type.yml"));
        assertNoBlocker(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        Assert.assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        Assert.assertEquals(1, archiveRoot.getCapabilityTypes().size());
        Entry<String, IndexedCapabilityType> entry = archiveRoot.getCapabilityTypes().entrySet().iterator().next();
        Assert.assertEquals("mycompany.mytypes.myapplication.MyFeature", entry.getKey());
        IndexedCapabilityType capability = entry.getValue();
        Assert.assertEquals(Lists.newArrayList("tosca.capabilities.Feature", "tosca.capabilities.Root"), capability.getDerivedFrom());
        Assert.assertEquals("a custom feature of my company’s application", capability.getDescription());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNodeType() throws FileNotFoundException, ParsingException {
        Mockito.reset(repositorySearchService);
        IndexedNodeType mockedResult = Mockito.mock(IndexedNodeType.class);
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(IndexedNodeType.class), Mockito.eq("tosca.nodes.SoftwareComponent"),
                Mockito.any(List.class))).thenReturn(mockedResult);
        Mockito.when(mockedResult.getDerivedFrom()).thenReturn(Lists.newArrayList("tosca.nodes.Root"));
        Mockito.when(
                repositorySearchService.getElementInDependencies(Mockito.eq(IndexedNodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(List.class)))
                .thenReturn(mockedResult);

        Mockito.when(
                repositorySearchService.getElementInDependencies(Mockito.eq(IndexedNodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(List.class)))
                .thenReturn(mockedResult);
        IndexedCapabilityType mockedCapabilityResult = Mockito.mock(IndexedCapabilityType.class);
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(IndexedCapabilityType.class),
                Mockito.eq("mytypes.mycapabilities.MyCapabilityTypeName"), Mockito.any(List.class))).thenReturn(mockedCapabilityResult);
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(IndexedCapabilityType.class),
                Mockito.eq("mytypes.mycapabilities.MyCapabilityTypeName"), Mockito.any(List.class))).thenReturn(mockedCapabilityResult);

        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(IndexedCapabilityType.class), Mockito.eq("tosca.capabilities.Endpoint"),
                Mockito.any(List.class))).thenReturn(mockedCapabilityResult);
        IndexedRelationshipType hostedOn = new IndexedRelationshipType();
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(IndexedRelationshipType.class), Mockito.eq("tosca.relationships.HostedOn"),
                Mockito.any(List.class))).thenReturn(hostedOn);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-node-type.yml"));
        ArchiveParserTest.displayErrors(parsingResult);
        assertNoBlocker(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        Assert.assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        Assert.assertEquals(1, archiveRoot.getNodeTypes().size());
        // check node type.
        Entry<String, IndexedNodeType> entry = archiveRoot.getNodeTypes().entrySet().iterator().next();

        Assert.assertEquals("my_company.my_types.MyAppNodeType", entry.getKey());
        IndexedNodeType nodeType = entry.getValue();

        Assert.assertEquals(Lists.newArrayList("tosca.nodes.SoftwareComponent", "tosca.nodes.Root"), nodeType.getDerivedFrom());
        Assert.assertEquals("My company’s custom applicaton", nodeType.getDescription());

        // validate properties parsing
        Assert.assertEquals(4, nodeType.getProperties().size());

        PropertyDefinition def1 = new PropertyDefinition();
        def1.setType("string");
        def1.setDefault(new ScalarPropertyValue("default"));
        def1.setDescription("application password");
        List<PropertyConstraint> constraints = Lists.newArrayList();
        constraints.add(new MinLengthConstraint(6));
        constraints.add(new MaxLengthConstraint(10));
        def1.setConstraints(constraints);

        PropertyDefinition def2 = new PropertyDefinition();
        def2.setType("integer");
        def2.setDescription("application port number");

        PropertyDefinition def3 = new PropertyDefinition();
        def3.setType("scalar-unit.size");
        def3.setDefault(new ScalarPropertyValue("1 GB"));
        LessThanConstraint ltConstraint = new LessThanConstraint();
        ltConstraint.setLessThan("1 TB");
        constraints = Lists.<PropertyConstraint> newArrayList(ltConstraint);
        def3.setConstraints(constraints);

        PropertyDefinition def4 = new PropertyDefinition();
        def4.setType("scalar-unit.time");
        def4.setDefault(new ScalarPropertyValue("1 d"));
        GreaterThanConstraint gtConstraint = new GreaterThanConstraint();
        gtConstraint.setGreaterThan("1 h");
        constraints = Lists.<PropertyConstraint> newArrayList(gtConstraint);
        def4.setConstraints(constraints);

        Assert.assertEquals(MapUtil.newHashMap(new String[] { "my_app_password", "my_app_duration", "my_app_size", "my_app_port" },
                new PropertyDefinition[] { def1, def4, def3, def2 }), nodeType.getProperties());

        // validate attributes parsing

        // nodeType.getAttributes()
        // nodeType.getInterfaces()
        // nodeType.getCapabilities()
        // nodeType.get
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNodeTypeWithCutomInterface() throws FileNotFoundException, ParsingException {
        Mockito.reset(repositorySearchService);
        IndexedNodeType mockedResult = Mockito.mock(IndexedNodeType.class);
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(IndexedNodeType.class), Mockito.eq("tosca.nodes.SoftwareComponent"),
                Mockito.any(List.class))).thenReturn(mockedResult);
        Mockito.when(mockedResult.getDerivedFrom()).thenReturn(Lists.newArrayList("tosca.nodes.Root"));
        Mockito.when(
                repositorySearchService.getElementInDependencies(Mockito.eq(IndexedNodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(List.class)))
                .thenReturn(mockedResult);

        Mockito.when(
                repositorySearchService.getElementInDependencies(Mockito.eq(IndexedNodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(List.class)))
                .thenReturn(mockedResult);
        IndexedCapabilityType mockedCapabilityResult = Mockito.mock(IndexedCapabilityType.class);
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(IndexedCapabilityType.class),
                Mockito.eq("mytypes.mycapabilities.MyCapabilityTypeName"), Mockito.any(List.class))).thenReturn(mockedCapabilityResult);
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(IndexedCapabilityType.class),
                Mockito.eq("mytypes.mycapabilities.MyCapabilityTypeName"), Mockito.any(List.class))).thenReturn(mockedCapabilityResult);

        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(IndexedCapabilityType.class), Mockito.eq("tosca.capabilities.Endpoint"),
                Mockito.any(List.class))).thenReturn(mockedCapabilityResult);
        IndexedRelationshipType hostedOn = new IndexedRelationshipType();
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(IndexedRelationshipType.class), Mockito.eq("tosca.relationships.HostedOn"),
                Mockito.any(List.class))).thenReturn(hostedOn);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-node-type-interface-operations.yml"));

        assertNoBlocker(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        Assert.assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        Assert.assertEquals(1, archiveRoot.getNodeTypes().size());
        // check node type.
        Entry<String, IndexedNodeType> entry = archiveRoot.getNodeTypes().entrySet().iterator().next();

        Assert.assertEquals("my_company.my_types.MyAppNodeType", entry.getKey());
        IndexedNodeType nodeType = entry.getValue();

        Assert.assertNotNull(nodeType.getInterfaces());
        Assert.assertEquals(2, nodeType.getInterfaces().size());
        Assert.assertNotNull(nodeType.getInterfaces().get(ToscaNodeLifecycleConstants.STANDARD));
        Interface customInterface = nodeType.getInterfaces().get("custom");
        Assert.assertNotNull(customInterface);
        Assert.assertEquals("this is a sample interface used to execute custom operations.", customInterface.getDescription());
        Assert.assertEquals(1, customInterface.getOperations().size());
        Operation operation = customInterface.getOperations().get("do_something");
        Assert.assertNotNull(operation);
        Assert.assertEquals(3, operation.getInputParameters().size());
        Assert.assertEquals(ScalarPropertyValue.class, operation.getInputParameters().get("value_input").getClass());
        Assert.assertEquals(PropertyDefinition.class, operation.getInputParameters().get("definition_input").getClass());
        Assert.assertEquals(FunctionPropertyValue.class, operation.getInputParameters().get("function_input").getClass());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAttributesConcatValid() throws Throwable {
        Mockito.reset(repositorySearchService);
        Csar csar = new Csar("tosca-normative-types", "1.0.0-SNAPSHOT-wd03");
        // Mockito.when(repositorySearchService.getArchive(csar.getId())).thenReturn(csar);

        IndexedNodeType mockedResult = Mockito.mock(IndexedNodeType.class);
        Mockito.when(
                repositorySearchService.getElementInDependencies(Mockito.eq(IndexedNodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(List.class)))
                .thenReturn(mockedResult);
        Mockito.when(mockedResult.getId()).thenReturn("tosca.nodes.Compute:1.0.0-SNAPSHOT-wd03");

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-node-type-inputs.yml"));

        Mockito.verify(repositorySearchService).getArchive(csar.getId());

        assertNoBlocker(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        Assert.assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());

        // check nodetype elements
        Entry<String, IndexedNodeType> entry = archiveRoot.getNodeTypes().entrySet().iterator().next();
        Assert.assertEquals("alien.test.TestComputeConcat", entry.getKey());
        IndexedNodeType nodeType = entry.getValue();

        Map<String, IValue> attributes = nodeType.getAttributes();

        IValue simpleDefinition = attributes.get("simple_definition");
        IValue ipAddressDefinition = attributes.get("ip_address");
        IValue simpleConcat = attributes.get("simple_concat");
        IValue complexConcat = attributes.get("complex_concat");

        // check attributes types
        Assert.assertTrue(simpleDefinition.getClass().equals(AttributeDefinition.class));
        Assert.assertTrue(ipAddressDefinition.getClass().equals(AttributeDefinition.class));
        Assert.assertTrue(simpleConcat.getClass().equals(ConcatPropertyValue.class));
        Assert.assertTrue(complexConcat.getClass().equals(ConcatPropertyValue.class));

        // Test nodeType serialization
        String nodeTypeJson = JsonUtil.toString(nodeType);
        // recover node from serialized string
        IndexedNodeType nodeTypeDeserialized = JsonUtil.readObject(nodeTypeJson, IndexedNodeType.class);
        Assert.assertNotNull(nodeTypeDeserialized);

        attributes = nodeTypeDeserialized.getAttributes();
        simpleDefinition = attributes.get("simple_definition");
        ipAddressDefinition = attributes.get("ip_address");
        simpleConcat = attributes.get("simple_concat");
        complexConcat = attributes.get("complex_concat");

        // check attributes types after deserialization
        Assert.assertTrue(simpleDefinition.getClass().equals(AttributeDefinition.class));
        Assert.assertTrue(ipAddressDefinition.getClass().equals(AttributeDefinition.class));
        Assert.assertTrue(simpleConcat.getClass().equals(ConcatPropertyValue.class));
        Assert.assertTrue(complexConcat.getClass().equals(ConcatPropertyValue.class));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetOperationOutputFunction() throws Throwable {
        Mockito.reset(repositorySearchService);
        Csar csar = new Csar("tosca-normative-types", "1.0.0-SNAPSHOT-wd03");
        // Mockito.when(repositorySearchService.getArchive(csar.getId())).thenReturn(csar);

        IndexedNodeType mockedResult = Mockito.mock(IndexedNodeType.class);
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(IndexedNodeType.class), Mockito.eq("tosca.nodes.SoftwareComponent"),
                Mockito.any(List.class))).thenReturn(mockedResult);
        Mockito.when(mockedResult.getDerivedFrom()).thenReturn(Lists.newArrayList("tosca.nodes.Root"));
        Mockito.when(
                repositorySearchService.getElementInDependencies(Mockito.eq(IndexedNodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(List.class)))
                .thenReturn(mockedResult);

        Mockito.when(
                repositorySearchService.getElementInDependencies(Mockito.eq(IndexedNodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(List.class)))
                .thenReturn(mockedResult);
        IndexedCapabilityType mockedCapabilityResult = Mockito.mock(IndexedCapabilityType.class);
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(IndexedCapabilityType.class),
                Mockito.eq("mytypes.mycapabilities.MyCapabilityTypeName"), Mockito.any(List.class))).thenReturn(mockedCapabilityResult);
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(IndexedCapabilityType.class),
                Mockito.eq("mytypes.mycapabilities.MyCapabilityTypeName"), Mockito.any(List.class))).thenReturn(mockedCapabilityResult);

        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(IndexedCapabilityType.class), Mockito.eq("tosca.capabilities.Endpoint"),
                Mockito.any(List.class))).thenReturn(mockedCapabilityResult);
        IndexedRelationshipType hostedOn = new IndexedRelationshipType();
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(IndexedRelationshipType.class), Mockito.eq("tosca.relationships.HostedOn"),
                Mockito.any(List.class))).thenReturn(hostedOn);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-functions.yml"));

        Mockito.verify(repositorySearchService).getArchive(csar.getId());

        assertNoBlocker(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        Assert.assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());

        // check nodetype elements
        Entry<String, IndexedNodeType> entry = archiveRoot.getNodeTypes().entrySet().iterator().next();
        Assert.assertEquals("my_company.my_types.MyAppNodeType", entry.getKey());
        IndexedNodeType nodeType = entry.getValue();

        // on input level
        Map<String, Interface> interfaces = nodeType.getInterfaces();
        Interface customInterface = interfaces.get("custom");
        Map<String, IValue> doSomethingInputs = customInterface.getOperations().get("do_something").getInputParameters();
        Assert.assertNotNull(doSomethingInputs);
        Assert.assertFalse(doSomethingInputs.isEmpty());
        IValue operationOutput_input = doSomethingInputs.get("operationOutput_input");
        Assert.assertTrue(operationOutput_input instanceof FunctionPropertyValue);
        FunctionPropertyValue function = (FunctionPropertyValue) operationOutput_input;
        Assert.assertEquals("get_operation_output", function.getFunction());
        Assert.assertEquals(4, function.getParameters().size());

        Map<String, IValue> attributes = nodeType.getAttributes();

        IValue operationOutputAttr = attributes.get("url");

        // check attributes types
        Assert.assertTrue(operationOutputAttr instanceof FunctionPropertyValue);
        function = (FunctionPropertyValue) operationOutputAttr;
        Assert.assertEquals("get_operation_output", function.getFunction());
        Assert.assertEquals(4, function.getParameters().size());
    }

    @Test
    public void testNodeTypeNodeFilter() throws ParsingException {
        Mockito.reset(repositorySearchService);
        Csar csar = new Csar("tosca-normative-types", "1.0.0.wd03-SNAPSHOT");
        // Mockito.when(repositorySearchService.getArchive(csar.getId())).thenReturn(csar);

        IndexedNodeType mockedResult = Mockito.mock(IndexedNodeType.class);
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(IndexedNodeType.class), Mockito.eq("tosca.nodes.SoftwareComponent"),
                Mockito.any(List.class))).thenReturn(mockedResult);
        Mockito.when(
                repositorySearchService.getElementInDependencies(Mockito.eq(IndexedNodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(List.class)))
                .thenReturn(mockedResult);
        Mockito.when(mockedResult.getDerivedFrom()).thenReturn(Lists.newArrayList("tosca.nodes.Root"));

        Mockito.when(
                repositorySearchService.getElementInDependencies(Mockito.eq(IndexedNodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(List.class)))
                .thenReturn(mockedResult);
        IndexedCapabilityType mockedCapabilityResult = Mockito.mock(IndexedCapabilityType.class);
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(IndexedCapabilityType.class), Mockito.eq("tosca.capabilities.Endpoint"),
                Mockito.any(List.class))).thenReturn(mockedCapabilityResult);
        IndexedRelationshipType hostedOn = new IndexedRelationshipType();
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(IndexedRelationshipType.class), Mockito.eq("tosca.relationships.HostedOn"),
                Mockito.any(List.class))).thenReturn(hostedOn);

        // parse the node define with node_filter
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-node-type-nodefilter.yml"));

        Mockito.verify(repositorySearchService).getArchive(csar.getId());

        // check the node_filter parsing
        IndexedNodeType nodeType = parsingResult.getResult().getNodeTypes().get("my_company.my_types.MyAppNodeType");
        // requirements for this snapshot
        List<RequirementDefinition> requirements = nodeType.getRequirements();
        RequirementDefinition requirementHost = requirements.get(0); // requirement host

        NodeFilter nodeFilter = requirementHost.getNodeFilter();
        Map<String, List<PropertyConstraint>> properties = nodeFilter.getProperties();
        Assert.assertEquals(3, properties.size());
        Map<String, FilterDefinition> capabilities = nodeFilter.getCapabilities();
        Assert.assertEquals(2, capabilities.size());

        // check constraints on properties & capabilities
        Assert.assertTrue(properties.containsKey("os_type"));
        List<PropertyConstraint> constraints = properties.get("os_type");
        Assert.assertEquals(1, constraints.size());

        Assert.assertTrue(properties.containsKey("os_mix"));
        constraints = properties.get("os_mix");
        Assert.assertEquals(2, constraints.size());

        Assert.assertTrue(properties.containsKey("os_arch"));
        constraints = properties.get("os_arch");
        Assert.assertEquals(2, constraints.size());

        Assert.assertTrue(capabilities.containsKey("host"));
        properties = capabilities.get("host").getProperties();
        Assert.assertEquals(2, properties.size());

        Assert.assertTrue(properties.containsKey("num_cpus"));
        constraints = properties.get("num_cpus");
        Assert.assertEquals(1, constraints.size());
        Assert.assertTrue(properties.containsKey("mem_size"));
        constraints = properties.get("mem_size");
        Assert.assertEquals(1, constraints.size());

        Assert.assertTrue(capabilities.containsKey("mytypes.capabilities.compute.encryption"));
        properties = capabilities.get("mytypes.capabilities.compute.encryption").getProperties();
        Assert.assertEquals(2, properties.size());
        Assert.assertTrue(properties.containsKey("algorithm"));
        constraints = properties.get("algorithm");
        Assert.assertEquals(1, constraints.size());
        Assert.assertTrue(properties.containsKey("keylength"));
        constraints = properties.get("keylength");
        Assert.assertEquals(2, constraints.size());
    }

    @Test
    public void parseTopologyTemplateWithGetInputErrors() throws ParsingException, IOException {
        // parse the node define with node_filter
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-topology-template-badinputs.yml"));
        // there are 2 MISSING INPUT errors
        Assert.assertEquals(2, countErrorByLevelAndCode(parsingResult, ParsingErrorLevel.ERROR, ErrorCode.MISSING_TOPOLOGY_INPUT));
        // check 2 errors content
        List<ParsingError> errors = parsingResult.getContext().getParsingErrors();
        for (Iterator iterator = errors.iterator(); iterator.hasNext();) {
            ParsingError parsingError = (ParsingError) iterator.next();
            if (parsingError.getErrorLevel().equals(ParsingErrorLevel.ERROR) && parsingError.getErrorCode().equals(ErrorCode.MISSING_TOPOLOGY_INPUT)) {
                if (parsingError.getProblem().equals("toto")) {
                    Assert.assertEquals("os_distribution", parsingError.getNote());
                }
                if (parsingError.getProblem().equals("greatsize")) {
                    Assert.assertEquals("size", parsingError.getNote());
                }
            }
        }
    }

    @Test
    public void testDataTypes() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-data-types.yml"));
        ArchiveParserTest.displayErrors(parsingResult);
        Assert.assertEquals(3, parsingResult.getResult().getDataTypes().size());
        Assert.assertEquals(1, parsingResult.getResult().getNodeTypes().size());
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testDataTypesWithError1() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-data-types-error1.yml"));
        ArchiveParserTest.displayErrors(parsingResult);
        Assert.assertEquals(1, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testDataTypesWithError2() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-data-types-error2.yml"));
        ArchiveParserTest.displayErrors(parsingResult);
        Assert.assertEquals(1, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testDataTypesWithError3() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-data-types-error3.yml"));
        ArchiveParserTest.displayErrors(parsingResult);
        Assert.assertEquals(1, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testDataTypesWithError4() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-data-types-error4.yml"));
        ArchiveParserTest.displayErrors(parsingResult);
        Assert.assertEquals(1, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testParseTopologyReferenceToNormative() throws FileNotFoundException, ParsingException {
        Mockito.reset(repositorySearchService);
        Csar csar = new Csar("tosca-normative-types", "1.0.0.wd03-SNAPSHOT");
        // Mockito.when(repositorySearchService.getArchive(csar.getId())).thenReturn(csar);

        IndexedNodeType mockedCompute = Mockito.mock(IndexedNodeType.class);
        IndexedNodeType mockedSoftware = Mockito.mock(IndexedNodeType.class);
        IndexedCapabilityType mockedContainer  = Mockito.mock(IndexedCapabilityType.class);
        RequirementDefinition hostRequirement = new RequirementDefinition("host", "tosca.capabilities.Container", "", "tosca.relationships.HostedOn", "host", 1,
                Integer.MAX_VALUE, null);
        Mockito.when(mockedSoftware.getRequirements()).thenReturn(Lists.<RequirementDefinition> newArrayList(hostRequirement));
        Mockito.when(mockedSoftware.getElementId()).thenReturn("tosca.nodes.SoftwareComponent");
        CapabilityDefinition capabilityDefinition = new CapabilityDefinition("host", "tosca.capabilities.Container", Integer.MAX_VALUE);
        Mockito.when(mockedCompute.getCapabilities()).thenReturn(Lists.newArrayList(capabilityDefinition));
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(IndexedNodeType.class), Mockito.eq("tosca.nodes.SoftwareComponent"),
                Mockito.any(List.class))).thenReturn(mockedSoftware);
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(IndexedCapabilityType.class), Mockito.eq("tosca.capabilities.Container"),
                Mockito.any(List.class))).thenReturn(mockedContainer);
        Mockito.when(
                repositorySearchService.getElementInDependencies(Mockito.eq(IndexedNodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(List.class)))
                .thenReturn(mockedCompute);
        IndexedRelationshipType hostedOn = new IndexedRelationshipType();
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(IndexedRelationshipType.class), Mockito.eq("tosca.relationships.HostedOn"),
                Mockito.any(List.class))).thenReturn(hostedOn);

        ParsingResult<ArchiveRoot> parsingResult = parser
                .parseFile(Paths.get(getRootDirectory(), "tosca-topology-template-node-from-derived-type-from-import.yml"));

        Mockito.verify(repositorySearchService).getArchive(csar.getId());

        assertNoBlocker(parsingResult);
    }
}
