package alien4cloud.tosca.parser;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import alien4cloud.component.repository.ArtifactRepositoryConstants;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.AttributeDefinition;
import org.alien4cloud.tosca.model.definitions.CapabilityDefinition;
import org.alien4cloud.tosca.model.definitions.ConcatPropertyValue;
import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.IValue;
import org.alien4cloud.tosca.model.definitions.ImplementationArtifact;
import org.alien4cloud.tosca.model.definitions.Interface;
import org.alien4cloud.tosca.model.definitions.Operation;
import org.alien4cloud.tosca.model.definitions.PropertyConstraint;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.RequirementDefinition;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.definitions.constraints.GreaterThanConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.LessThanConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.MaxLengthConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.MinLengthConstraint;
import org.alien4cloud.tosca.model.types.AbstractInstantiableToscaType;
import org.alien4cloud.tosca.model.types.ArtifactType;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.normative.constants.NormativeCapabilityTypes;
import org.alien4cloud.tosca.normative.constants.NormativeCredentialConstant;
import org.alien4cloud.tosca.normative.constants.NormativeTypesConstant;
import org.elasticsearch.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.Lists;

import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.utils.AlienConstants;
import alien4cloud.utils.MapUtil;

public class ToscaParserSimpleProfileAlien130Test extends AbstractToscaParserSimpleProfileTest {

    @Override
    protected String getRootDirectory() {
        return "src/test/resources/tosca/alien_dsl_1_3_0/";
    }

    @Override
    protected String getToscaVersion() {
        return "alien_dsl_1_3_0";
    }

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
        assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        assertNotNull(archiveRoot.getArchive().getDescription());
        Assert.assertEquals("This is an example of a single line description (no folding).", archiveRoot.getArchive().getDescription());
    }

    @Test
    public void testDescriptionMultiLine() throws FileNotFoundException, ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "description-multi-line.yml"));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
        ArchiveRoot archiveRoot = parsingResult.getResult();
        assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        assertNotNull(archiveRoot.getArchive().getDescription());
        Assert.assertEquals(
                "This is an example of a multi-line description using YAML. It permits for line breaks for easier readability...\nif needed.  However, (multiple) line breaks are folded into a single space character when processed into a single string value.",
                archiveRoot.getArchive().getDescription());
    }

    @Test
    public void testRootCategories() throws FileNotFoundException, ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-root-categories.yml"));
        assertNoBlocker(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        Assert.assertEquals("Tosca default namespace value", archiveRoot.getArchive().getToscaDefaultNamespace());
        Assert.assertEquals("Template name value", archiveRoot.getArchive().getName());
        Assert.assertEquals("Temlate author value", archiveRoot.getArchive().getTemplateAuthor());
        Assert.assertEquals("1.0.0-SNAPSHOT", archiveRoot.getArchive().getVersion());
        Assert.assertEquals("This is an example of a single line description (no folding).", archiveRoot.getArchive().getDescription());
    }

    @Test
    public void testImportDependency() throws FileNotFoundException, ParsingException {
        Csar csar = new Csar("tosca-normative-types", "1.0.0-SNAPSHOT-wd03");
        Mockito.when(csarRepositorySearchService.getArchive(csar.getName(), csar.getVersion())).thenReturn(csar);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-import-dependency.yml"));

        Mockito.verify(csarRepositorySearchService).getArchive(csar.getName(), csar.getVersion());

        assertNoBlocker(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        assertNotNull(archiveRoot.getArchive());
        assertNotNull(archiveRoot.getArchive().getDependencies());
        Assert.assertEquals(1, archiveRoot.getArchive().getDependencies().size());
        Assert.assertEquals(new CSARDependency(csar.getName(), csar.getVersion()), archiveRoot.getArchive().getDependencies().iterator().next());
    }

    @Test
    public void testImportDependencyMissing() throws FileNotFoundException, ParsingException {
        Csar csar = new Csar("tosca-normative-types", "1.0.0-SNAPSHOT-wd03");
        Mockito.when(csarRepositorySearchService.getArchive(csar.getName(), csar.getVersion())).thenReturn(null);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-import-dependency.yml"));

        Mockito.verify(csarRepositorySearchService).getArchive(csar.getName(), csar.getVersion());

        assertNoBlocker(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        assertNotNull(archiveRoot.getArchive());
        assertNotNull(archiveRoot.getArchive().getDependencies());
        Assert.assertEquals(0, archiveRoot.getArchive().getDependencies().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testArtifactType() throws FileNotFoundException, ParsingException {
        ArtifactType mockedResult = Mockito.mock(ArtifactType.class);
        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(ArtifactType.class), Mockito.eq("tosca.artifact.Root"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);
        List<String> derivedFromSet = Lists.newArrayList();
        Mockito.when(mockedResult.getDerivedFrom()).thenReturn(derivedFromSet);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-artifact-type.yml"));

        assertNoBlocker(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        Assert.assertEquals(1, archiveRoot.getArtifactTypes().size());
        Entry<String, ArtifactType> entry = archiveRoot.getArtifactTypes().entrySet().iterator().next();
        Assert.assertEquals("my_artifact_type", entry.getKey());
        ArtifactType artifact = entry.getValue();
        Assert.assertEquals(Lists.newArrayList("tosca.artifact.Root"), artifact.getDerivedFrom());
        Assert.assertEquals("Java Archive artifact type", artifact.getDescription());
        Assert.assertEquals("application/java-archive", artifact.getMimeType());
        Assert.assertEquals(Lists.newArrayList("jar"), artifact.getFileExt());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCapabilityType() throws FileNotFoundException, ParsingException {
        CapabilityType mockedResult = Mockito.mock(CapabilityType.class);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq("tosca.capabilities.Feature"),
                Mockito.any(Set.class))).thenReturn(mockedResult);
        Mockito.when(mockedResult.getDerivedFrom()).thenReturn(Lists.newArrayList("tosca.capabilities.Root"));

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-capability-type.yml"));
        assertNoBlocker(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        Assert.assertEquals(1, archiveRoot.getCapabilityTypes().size());
        Entry<String, CapabilityType> entry = archiveRoot.getCapabilityTypes().entrySet().iterator().next();
        Assert.assertEquals("mycompany.mytypes.myapplication.MyFeature", entry.getKey());
        CapabilityType capability = entry.getValue();
        Assert.assertEquals(Lists.newArrayList("tosca.capabilities.Feature", "tosca.capabilities.Root"), capability.getDerivedFrom());
        Assert.assertEquals("a custom feature of my company’s application", capability.getDescription());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNodeType() throws FileNotFoundException, ParsingException {
        NodeType mockedResult = Mockito.mock(NodeType.class);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.SoftwareComponent"),
                Mockito.any(Set.class))).thenReturn(mockedResult);
        Mockito.when(mockedResult.getDerivedFrom()).thenReturn(Lists.newArrayList("tosca.nodes.Root"));
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);

        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);
        CapabilityType mockedCapabilityResult = Mockito.mock(CapabilityType.class);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class),
                Mockito.eq("mytypes.mycapabilities.MyCapabilityTypeName"), Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class),
                Mockito.eq("mytypes.mycapabilities.MyCapabilityTypeName"), Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);

        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq("tosca.capabilities.Endpoint"),
                Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);
        RelationshipType hostedOn = new RelationshipType();
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(RelationshipType.class), Mockito.eq("tosca.relationships.HostedOn"),
                Mockito.any(Set.class))).thenReturn(hostedOn);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-node-type.yml"));
        ParserTestUtil.displayErrors(parsingResult);
        assertNoBlocker(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        Assert.assertEquals(1, archiveRoot.getNodeTypes().size());
        // check node type.
        Entry<String, NodeType> entry = archiveRoot.getNodeTypes().entrySet().iterator().next();

        Assert.assertEquals("my_company.my_types.MyAppNodeType", entry.getKey());
        NodeType nodeType = entry.getValue();

        Assert.assertEquals(Lists.newArrayList("tosca.nodes.SoftwareComponent", "tosca.nodes.Root"), nodeType.getDerivedFrom());
        Assert.assertEquals("My company’s custom applicaton", nodeType.getDescription());

        // validate properties parsing
        Assert.assertEquals(5, nodeType.getProperties().size());

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

        PropertyDefinition def5 = new PropertyDefinition();
        def5.setType("string");
        def5.setDefault(new ScalarPropertyValue(""));
        def5.setDescription("a prop with an empty string as default value");

        Assert.assertEquals(MapUtil.newHashMap(new String[] { "my_app_password", "my_app_duration", "my_app_size", "my_app_port", "my_empty_default_prop" },
                new PropertyDefinition[] { def1, def4, def3, def2, def5 }), nodeType.getProperties());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNodeTypeWithCutomInterface() throws FileNotFoundException, ParsingException {
        NodeType mockedResult = Mockito.mock(NodeType.class);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.SoftwareComponent"),
                Mockito.any(Set.class))).thenReturn(mockedResult);
        Mockito.when(mockedResult.getDerivedFrom()).thenReturn(Lists.newArrayList("tosca.nodes.Root"));
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);

        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);
        CapabilityType mockedCapabilityResult = Mockito.mock(CapabilityType.class);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class),
                Mockito.eq("mytypes.mycapabilities.MyCapabilityTypeName"), Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq("tosca.capabilities.Container"),
                Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);

        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq("tosca.capabilities.Endpoint"),
                Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);
        RelationshipType hostedOn = new RelationshipType();
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(RelationshipType.class), Mockito.eq("tosca.relationships.HostedOn"),
                Mockito.any(Set.class))).thenReturn(hostedOn);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-node-type-interface-operations.yml"));

        assertNoBlocker(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        Assert.assertEquals(1, archiveRoot.getNodeTypes().size());
        // check node type.
        Entry<String, NodeType> entry = archiveRoot.getNodeTypes().entrySet().iterator().next();

        Assert.assertEquals("my_company.my_types.MyAppNodeType", entry.getKey());
        NodeType nodeType = entry.getValue();

        assertNotNull(nodeType.getInterfaces());
        Assert.assertEquals(2, nodeType.getInterfaces().size());
        assertNotNull(nodeType.getInterfaces().get(ToscaNodeLifecycleConstants.STANDARD));
        Interface customInterface = nodeType.getInterfaces().get("custom");
        assertNotNull(customInterface);
        Assert.assertEquals("this is a sample interface used to execute custom operations.", customInterface.getDescription());
        Assert.assertEquals(1, customInterface.getOperations().size());
        Operation operation = customInterface.getOperations().get("do_something");
        assertNotNull(operation);
        Assert.assertEquals(3, operation.getInputParameters().size());
        Assert.assertEquals(ScalarPropertyValue.class, operation.getInputParameters().get("value_input").getClass());
        Assert.assertEquals(PropertyDefinition.class, operation.getInputParameters().get("definition_input").getClass());
        Assert.assertEquals(FunctionPropertyValue.class, operation.getInputParameters().get("function_input").getClass());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAttributesConcatValid() throws Throwable {
        Csar csar = new Csar("tosca-normative-types", "1.0.0-SNAPSHOT-wd03");
        // Mockito.when(csarRepositorySearchService.getArchive(csar.getId())).thenReturn(csar);

        NodeType mockedResult = Mockito.mock(NodeType.class);
        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);
        Mockito.when(mockedResult.getId()).thenReturn("tosca.nodes.Compute:1.0.0-SNAPSHOT-wd03");

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-node-type-inputs.yml"));

        Mockito.verify(csarRepositorySearchService).getArchive(csar.getName(), csar.getVersion());

        assertNoBlocker(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());

        // check nodetype elements
        Entry<String, NodeType> entry = archiveRoot.getNodeTypes().entrySet().iterator().next();
        Assert.assertEquals("alien.test.TestComputeConcat", entry.getKey());
        NodeType nodeType = entry.getValue();
        nodeType.setWorkspace(AlienConstants.GLOBAL_WORKSPACE_ID);
        Map<String, IValue> attributes = nodeType.getAttributes();

        IValue simpleDefinition = attributes.get("simple_definition");
        IValue ipAddressDefinition = attributes.get("ip_address");
        IValue simpleConcat = attributes.get("simple_concat");
        IValue complexConcat = attributes.get("complex_concat");

        // check attributes types
        assertTrue(simpleDefinition.getClass().equals(AttributeDefinition.class));
        assertTrue(ipAddressDefinition.getClass().equals(AttributeDefinition.class));
        assertTrue(simpleConcat.getClass().equals(ConcatPropertyValue.class));
        assertTrue(complexConcat.getClass().equals(ConcatPropertyValue.class));

        // Test nodeType serialization
        String nodeTypeJson = JsonUtil.toString(nodeType);
        // recover node from serialized string
        NodeType nodeTypeDeserialized = JsonUtil.readObject(nodeTypeJson, NodeType.class);
        assertNotNull(nodeTypeDeserialized);

        attributes = nodeTypeDeserialized.getAttributes();
        simpleDefinition = attributes.get("simple_definition");
        ipAddressDefinition = attributes.get("ip_address");
        simpleConcat = attributes.get("simple_concat");
        complexConcat = attributes.get("complex_concat");

        // check attributes types after deserialization
        assertTrue(simpleDefinition.getClass().equals(AttributeDefinition.class));
        assertTrue(ipAddressDefinition.getClass().equals(AttributeDefinition.class));
        assertTrue(simpleConcat.getClass().equals(ConcatPropertyValue.class));
        assertTrue(complexConcat.getClass().equals(ConcatPropertyValue.class));
    }

    @Test
    public void testCapabilities() throws ParsingException {
        NodeType mockedResult = Mockito.mock(NodeType.class);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.SoftwareComponent"),
                Mockito.any(Set.class))).thenReturn(mockedResult);
        Mockito.when(mockedResult.getDerivedFrom()).thenReturn(Lists.newArrayList("tosca.nodes.Root"));
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);

        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);
        CapabilityType mockedCapabilityResult = Mockito.mock(CapabilityType.class);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq("tosca.capabilities.Endpoint"),
                Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq("tosca.capabilities.Container"),
                Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);

        RelationshipType connectsTo = new RelationshipType();
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(RelationshipType.class), Mockito.eq("tosca.relationships.ConnectsTo"),
                Mockito.any(Set.class))).thenReturn(connectsTo);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "requirement_capabilities.yaml"));
        ParserTestUtil.displayErrors(parsingResult);
        parsingResult.getResult().getNodeTypes().values().forEach(nodeType -> {
            nodeType.getRequirements().forEach(requirementDefinition -> {
                switch (requirementDefinition.getId()) {
                case "host":
                    Assert.assertEquals("tosca.capabilities.Container", requirementDefinition.getType());
                    break;
                case "endpoint":
                case "another_endpoint":
                    Assert.assertEquals("tosca.capabilities.Endpoint", requirementDefinition.getType());
                    Assert.assertEquals(0, requirementDefinition.getLowerBound());
                    Assert.assertEquals(Integer.MAX_VALUE, requirementDefinition.getUpperBound());
                    Assert.assertEquals("tosca.relationships.ConnectsTo", requirementDefinition.getRelationshipType());
                    break;
                }
            });
            nodeType.getCapabilities().forEach(capabilityDefinition -> {
                switch (capabilityDefinition.getId()) {
                case "host":
                    Assert.assertEquals("tosca.capabilities.Container", capabilityDefinition.getType());
                    break;
                case "endpoint":
                case "another_endpoint":
                    Assert.assertEquals("tosca.capabilities.Endpoint", capabilityDefinition.getType());
                    assertNotNull(capabilityDefinition.getDescription());
                }
            });
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetOperationOutputFunction() throws Throwable {
        Csar csar = new Csar("tosca-normative-types", "1.0.0-SNAPSHOT-wd03");
        // Mockito.when(csarRepositorySearchService.getArchive(csar.getId())).thenReturn(csar);

        NodeType mockedResult = Mockito.mock(NodeType.class);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.SoftwareComponent"),
                Mockito.any(Set.class))).thenReturn(mockedResult);
        Mockito.when(mockedResult.getDerivedFrom()).thenReturn(Lists.newArrayList("tosca.nodes.Root"));
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);

        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);
        CapabilityType mockedCapabilityResult = Mockito.mock(CapabilityType.class);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class),
                Mockito.eq("mytypes.mycapabilities.MyCapabilityTypeName"), Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq("tosca.capabilities.Container"),
                Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);

        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq("tosca.capabilities.Endpoint"),
                Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);
        RelationshipType hostedOn = new RelationshipType();
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(RelationshipType.class), Mockito.eq("tosca.relationships.HostedOn"),
                Mockito.any(Set.class))).thenReturn(hostedOn);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-functions.yml"));

        Mockito.verify(csarRepositorySearchService).getArchive(csar.getName(), csar.getVersion());

        assertNoBlocker(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());

        // check nodetype elements
        Entry<String, NodeType> entry = archiveRoot.getNodeTypes().entrySet().iterator().next();
        Assert.assertEquals("my_company.my_types.MyAppNodeType", entry.getKey());
        NodeType nodeType = entry.getValue();

        // on input level
        Map<String, Interface> interfaces = nodeType.getInterfaces();
        Interface customInterface = interfaces.get("custom");
        Map<String, IValue> doSomethingInputs = customInterface.getOperations().get("do_something").getInputParameters();
        assertNotNull(doSomethingInputs);
        Assert.assertFalse(doSomethingInputs.isEmpty());
        IValue operationOutput_input = doSomethingInputs.get("operationOutput_input");
        assertTrue(operationOutput_input instanceof FunctionPropertyValue);
        FunctionPropertyValue function = (FunctionPropertyValue) operationOutput_input;
        Assert.assertEquals("get_operation_output", function.getFunction());
        Assert.assertEquals(4, function.getParameters().size());

        Map<String, IValue> attributes = nodeType.getAttributes();

        IValue operationOutputAttr = attributes.get("url");

        // check attributes types
        assertTrue(operationOutputAttr instanceof FunctionPropertyValue);
        function = (FunctionPropertyValue) operationOutputAttr;
        Assert.assertEquals("get_operation_output", function.getFunction());
        Assert.assertEquals(4, function.getParameters().size());
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
        ParserTestUtil.displayErrors(parsingResult);
        Assert.assertEquals(4, parsingResult.getResult().getDataTypes().size());
        Assert.assertEquals(2, parsingResult.getResult().getNodeTypes().size());
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testDataTypesWithError1() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-data-types-error1.yml"));
        ParserTestUtil.displayErrors(parsingResult);
        Assert.assertEquals(1, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testDataTypesWithError2() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-data-types-error2.yml"));
        ParserTestUtil.displayErrors(parsingResult);
        Assert.assertEquals(1, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testDataTypesWithError3() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-data-types-error3.yml"));
        ParserTestUtil.displayErrors(parsingResult);
        Assert.assertEquals(1, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testDataTypesWithError4() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-data-types-error4.yml"));
        ParserTestUtil.displayErrors(parsingResult);
        Assert.assertEquals(1, parsingResult.getContext().getParsingErrors().size());
    }

    private NodeType getMockedCompute() {
        NodeType mockedCompute = new NodeType();
        mockedCompute.setArchiveName("tosca-normative-types");
        mockedCompute.setArchiveVersion("1.0.0.wd03-SNAPSHOT");
        CapabilityDefinition capabilityDefinition = new CapabilityDefinition("host", "tosca.capabilities.Container", Integer.MAX_VALUE);
        mockedCompute.setCapabilities(Lists.newArrayList(capabilityDefinition));
        mockedCompute.setElementId("tosca.nodes.Compute");
        return mockedCompute;
    }

    private NodeType getMockedSoftwareComponent() {
        NodeType mockedSoftware = new NodeType();
        mockedSoftware.setArchiveName("tosca-normative-types");
        mockedSoftware.setArchiveVersion("1.0.0.wd03-SNAPSHOT");
        RequirementDefinition hostRequirement = new RequirementDefinition("host", "tosca.capabilities.Container", null, "", "tosca.relationships.HostedOn",
                "host", 1, Integer.MAX_VALUE, null);
        mockedSoftware.setRequirements(Lists.<RequirementDefinition> newArrayList(hostRequirement));
        mockedSoftware.setElementId("tosca.nodes.SoftwareComponent");
        return mockedSoftware;
    }

    @Test
    public void testParseTopologyReferenceToNormative() throws FileNotFoundException, ParsingException {
        Csar csar = new Csar("tosca-normative-types", "1.0.0.wd03-SNAPSHOT");

        NodeType mockedCompute = getMockedCompute();
        NodeType mockedSoftware = getMockedSoftwareComponent();

        CapabilityType mockedContainer = Mockito.mock(CapabilityType.class);
        Mockito.when(mockedContainer.getElementId()).thenReturn("tosca.capabilities.Container");

        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.SoftwareComponent"),
                Mockito.any(Set.class))).thenReturn(mockedSoftware);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq("tosca.capabilities.Container"),
                Mockito.any(Set.class))).thenReturn(mockedContainer);
        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(Set.class)))
                .thenReturn(mockedCompute);
        RelationshipType hostedOn = new RelationshipType();
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(RelationshipType.class), Mockito.eq("tosca.relationships.HostedOn"),
                Mockito.any(Set.class))).thenReturn(hostedOn);

        ParsingResult<ArchiveRoot> parsingResult = parser
                .parseFile(Paths.get(getRootDirectory(), "tosca-topology-template-node-from-derived-type-from-import.yml"));

        Mockito.verify(csarRepositorySearchService).getArchive(csar.getName(), csar.getVersion());

        assertNoBlocker(parsingResult);
    }

    @Test
    public void testParseTopologyWithWorkflows() throws FileNotFoundException, ParsingException {
        Csar csar = new Csar("tosca-normative-types", "1.0.0.wd03-SNAPSHOT");

        NodeType mockedCompute = getMockedCompute();
        NodeType mockedSoftware = getMockedSoftwareComponent();
        CapabilityType mockedContainer = Mockito.mock(CapabilityType.class);
        Mockito.when(mockedContainer.getElementId()).thenReturn("tosca.capabilities.Container");

        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.SoftwareComponent"),
                Mockito.any(Set.class))).thenReturn(mockedSoftware);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq("tosca.capabilities.Container"),
                Mockito.any(Set.class))).thenReturn(mockedContainer);
        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(Set.class)))
                .thenReturn(mockedCompute);
        RelationshipType hostedOn = Mockito.mock(RelationshipType.class);
        Mockito.when(hostedOn.getElementId()).thenReturn("tosca.relationships.HostedOn");
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(RelationshipType.class), Mockito.eq("tosca.relationships.HostedOn"),
                Mockito.any(Set.class))).thenReturn(hostedOn);
        Mockito.when(csarRepositorySearchService.getArchive(csar.getName(), csar.getVersion())).thenReturn(csar);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-topology-template-workflows.yml"));

        Mockito.verify(csarRepositorySearchService).getArchive(csar.getName(), csar.getVersion());

        assertNoBlocker(parsingResult);

        Assert.assertEquals(3, parsingResult.getContext().getParsingErrors().size());
        assertNotNull(parsingResult.getResult().getTopology());
        Assert.assertEquals(5, parsingResult.getResult().getTopology().getWorkflows().size());

        // check invalid names were renamed
        assertTrue(parsingResult.getResult().getTopology().getWorkflows().containsKey("invalidName_"));
        assertTrue(parsingResult.getResult().getTopology().getWorkflows().containsKey("invalid_Name"));
        assertTrue(parsingResult.getResult().getTopology().getWorkflows().containsKey("invalid_Name_1"));
    }

    @Test
    public void testDerivedFromNothing() throws ParsingException {
        ParserTestUtil.mockNormativeTypes(csarRepositorySearchService);
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "derived_from_nothing/template.yml"));
        List<ParsingError> errors = parsingResult.getContext().getParsingErrors();
        Assert.assertEquals(5, errors.size());
        assertTrue(errors.stream()
                .allMatch(error -> error.getErrorLevel() == ParsingErrorLevel.WARNING && error.getErrorCode() == ErrorCode.DERIVED_FROM_NOTHING));
        assertTrue(parsingResult.getResult().getNodeTypes().values().stream()
                .allMatch(nodeType -> nodeType.getDerivedFrom() != null && nodeType.getDerivedFrom().contains(NormativeTypesConstant.ROOT_NODE_TYPE)));
        assertTrue(parsingResult.getResult().getDataTypes().values().stream()
                .allMatch(dataType -> dataType.getDerivedFrom() != null && dataType.getDerivedFrom().contains(NormativeTypesConstant.ROOT_DATA_TYPE)));
        assertTrue(parsingResult.getResult().getCapabilityTypes().values().stream().allMatch(
                capabilityType -> capabilityType.getDerivedFrom() != null && capabilityType.getDerivedFrom().contains(NormativeCapabilityTypes.ROOT)));
        assertTrue(parsingResult.getResult().getRelationshipTypes().values().stream().allMatch(relationshipType -> relationshipType.getDerivedFrom() != null
                && relationshipType.getDerivedFrom().contains(NormativeTypesConstant.ROOT_RELATIONSHIP_TYPE)));
        assertTrue(parsingResult.getResult().getArtifactTypes().values().stream().allMatch(
                artifactType -> artifactType.getDerivedFrom() != null && artifactType.getDerivedFrom().contains(NormativeTypesConstant.ROOT_ARTIFACT_TYPE)));
    }

    @Test
    public void testNodeTypeMissingRequirementType() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-node-type-missing-requirement-type.yml"));
        Assert.assertEquals(2, parsingResult.getContext().getParsingErrors().size());
        Assert.assertEquals(ErrorCode.TYPE_NOT_FOUND, parsingResult.getContext().getParsingErrors().get(0).getErrorCode());
        Assert.assertEquals(ErrorCode.TYPE_NOT_FOUND, parsingResult.getContext().getParsingErrors().get(1).getErrorCode());
    }

    @Test
    public void testNodeTypeMissingCapabilityType() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-node-type-missing-capability-type.yml"));
        Assert.assertEquals(1, parsingResult.getContext().getParsingErrors().size());
        Assert.assertEquals(ErrorCode.TYPE_NOT_FOUND, parsingResult.getContext().getParsingErrors().get(0).getErrorCode());
    }

    private void validateHttpArtifact(NodeType httpComponent) {
        ImplementationArtifact httpComponentCreateArtifact = getImplementationArtifact(httpComponent, "create");
        Assert.assertEquals("https://otherCompany/script/short_notation.sh", httpComponentCreateArtifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Implementation.Bash", httpComponentCreateArtifact.getArtifactType());
        assertNull(httpComponentCreateArtifact.getRepositoryCredential());
        assertNull(httpComponentCreateArtifact.getRepositoryName());
        assertNull(httpComponentCreateArtifact.getArtifactRepository());
        assertNull(httpComponentCreateArtifact.getRepositoryURL());

        ImplementationArtifact httpComponentStartArtifact = getImplementationArtifact(httpComponent, "start");
        Assert.assertEquals("myScript.abc", httpComponentStartArtifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Implementation.Bash", httpComponentStartArtifact.getArtifactType());
        Assert.assertEquals(
                ImmutableMap.<String, Object> builder().put(NormativeCredentialConstant.USER_KEY, "good_user")
                        .put(NormativeCredentialConstant.TOKEN_KEY, "real_secured_password").put(NormativeCredentialConstant.TOKEN_TYPE, "password").build(),
                httpComponentStartArtifact.getRepositoryCredential());
        Assert.assertEquals("script_repo", httpComponentStartArtifact.getRepositoryName());
        assertNull(httpComponentStartArtifact.getArtifactRepository());
        Assert.assertEquals("https://myCompany/script", httpComponentStartArtifact.getRepositoryURL());
    }

    @Test
    public void testParseImplementationArtifactWithRepository() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "implementation_artifact.yml"));
        ParserTestUtil.displayErrors(parsingResult);
        assertTrue(parsingResult.getContext().getParsingErrors().isEmpty());
        ArchiveRoot archiveRoot = parsingResult.getResult();
        assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        Assert.assertEquals(2, archiveRoot.getArtifactTypes().size());
        Assert.assertEquals(4, archiveRoot.getNodeTypes().size());
        Assert.assertEquals(3, archiveRoot.getRepositories().size());
        Assert.assertEquals(3, archiveRoot.getRelationshipTypes().size());

        NodeType httpComponent = archiveRoot.getNodeTypes().get("my.http.component");
        validateHttpArtifact(httpComponent);

        NodeType httpComponentExtended = archiveRoot.getNodeTypes().get("my.http.component.extended");
        validateHttpArtifact(httpComponentExtended);

        NodeType gitComponent = archiveRoot.getNodeTypes().get("my.git.component");
        ImplementationArtifact gitComponentCreateArtifact = getImplementationArtifact(gitComponent, "create");
        Assert.assertEquals("master:myGitScript.xyz", gitComponentCreateArtifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Implementation.Bash", gitComponentCreateArtifact.getArtifactType());
        assertNull(gitComponentCreateArtifact.getRepositoryCredential());
        Assert.assertEquals("git_repo", gitComponentCreateArtifact.getRepositoryName());
        Assert.assertEquals("git", gitComponentCreateArtifact.getArtifactRepository());
        Assert.assertEquals("https://github.com/myId/myRepo.git", gitComponentCreateArtifact.getRepositoryURL());

        RelationshipType httpRelationship = archiveRoot.getRelationshipTypes().get("my.http.relationship");
        ImplementationArtifact httpRelationshipCreateArtifact = getImplementationArtifact(httpRelationship, "create");
        Assert.assertEquals("https://otherCompany/script/short_notation.sh", httpRelationshipCreateArtifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Implementation.Bash", httpRelationshipCreateArtifact.getArtifactType());
        assertNull(httpRelationshipCreateArtifact.getRepositoryCredential());
        assertNull(httpRelationshipCreateArtifact.getRepositoryName());
        assertNull(httpRelationshipCreateArtifact.getArtifactRepository());
        assertNull(httpRelationshipCreateArtifact.getRepositoryURL());

        ImplementationArtifact httpRelationshipStartArtifact = getImplementationArtifact(httpRelationship, "start");
        Assert.assertEquals("myScript.abc", httpRelationshipStartArtifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Implementation.Bash", httpRelationshipStartArtifact.getArtifactType());
        Assert.assertEquals(
                ImmutableMap.<String, Object> builder().put(NormativeCredentialConstant.USER_KEY, "good_user")
                        .put(NormativeCredentialConstant.TOKEN_KEY, "real_secured_password").put(NormativeCredentialConstant.TOKEN_TYPE, "password").build(),
                httpRelationshipStartArtifact.getRepositoryCredential());
        Assert.assertEquals("script_repo", httpRelationshipStartArtifact.getRepositoryName());
        assertNull(httpRelationshipStartArtifact.getArtifactRepository());
        Assert.assertEquals("https://myCompany/script", httpRelationshipStartArtifact.getRepositoryURL());
    }

    private void validateSimpleWar(DeploymentArtifact artifact) {
        Assert.assertEquals("binary/myWar.war", artifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Deployment.War", artifact.getArtifactType());
        assertNull(artifact.getRepositoryCredential());
        assertNull(artifact.getRepositoryName());
        assertNull(artifact.getArtifactRepository());
        assertNull(artifact.getRepositoryURL());
    }

    private void validateRemoteWar(DeploymentArtifact repositoryArtifact) {
        Assert.assertEquals("alien4cloud:alien4cloud-ui:1.3.0-SM3", repositoryArtifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Deployment.War", repositoryArtifact.getArtifactType());
        Assert.assertEquals(
                ImmutableMap.<String, Object> builder().put(NormativeCredentialConstant.USER_KEY, "good_user")
                        .put(NormativeCredentialConstant.TOKEN_KEY, "real_secured_password").put(NormativeCredentialConstant.TOKEN_TYPE, "password").build(),
                repositoryArtifact.getRepositoryCredential());
        Assert.assertEquals("maven_repo", repositoryArtifact.getRepositoryName());
        Assert.assertEquals("maven", repositoryArtifact.getArtifactRepository());
        Assert.assertEquals("https://fastconnect.org/maven/content/repositories/fastconnect", repositoryArtifact.getRepositoryURL());
    }

    private void validateMavenDeploymentArtifact(NodeType mavenComponent) {
        DeploymentArtifact artifact = getDeploymentArtifact(mavenComponent, "simple_war");
        validateSimpleWar(artifact);
        DeploymentArtifact repositoryArtifact = getDeploymentArtifact(mavenComponent, "remote_war");
        validateRemoteWar(repositoryArtifact);
    }

    @Test
    public void testParseDeploymentArtifactWithRepository() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "deployment_artifact.yml"));
        ParserTestUtil.displayErrors(parsingResult);
        assertTrue(parsingResult.getContext().getParsingErrors().isEmpty());
        ArchiveRoot archiveRoot = parsingResult.getResult();
        assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        Assert.assertEquals(1, archiveRoot.getRepositories().size());
        Assert.assertEquals(2, archiveRoot.getArtifactTypes().size());
        Assert.assertEquals(3, archiveRoot.getNodeTypes().size());
        Assert.assertEquals(3, archiveRoot.getNodeTypes().size());
        Assert.assertEquals(3, archiveRoot.getRelationshipTypes().size());

        NodeType mavenComponent = archiveRoot.getNodeTypes().get("my.maven.component");
        validateMavenDeploymentArtifact(mavenComponent);

        NodeType mavenExtendedComponent = archiveRoot.getNodeTypes().get("my.maven.component.extended");
        validateMavenDeploymentArtifact(mavenExtendedComponent);

        DeploymentArtifact artifact = archiveRoot.getTopology().getInputArtifacts().get("simple_war");
        validateSimpleWar(artifact);

        DeploymentArtifact repositoryArtifact = archiveRoot.getTopology().getInputArtifacts().get("remote_war");
        validateRemoteWar(repositoryArtifact);

        artifact = archiveRoot.getTopology().getNodeTemplates().get("my_node").getArtifacts().get("simple_war");
        Assert.assertEquals("binary/myWar.war", artifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Deployment.War", artifact.getArtifactType());
        assertNull(artifact.getRepositoryCredential());
        assertNull(artifact.getRepositoryName());
        Assert.assertEquals(ArtifactRepositoryConstants.ALIEN_TOPOLOGY_REPOSITORY, artifact.getArtifactRepository());

        assertNull(artifact.getRepositoryURL());

        repositoryArtifact = archiveRoot.getTopology().getNodeTemplates().get("my_node").getArtifacts().get("remote_war");
        Assert.assertEquals("alien4cloud:alien4cloud-ui:1.3.0-SM3", repositoryArtifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Deployment.War", repositoryArtifact.getArtifactType());
        Assert.assertEquals(
                ImmutableMap.<String, Object> builder().put(NormativeCredentialConstant.USER_KEY, "good_user")
                        .put(NormativeCredentialConstant.TOKEN_KEY, "real_secured_password").put(NormativeCredentialConstant.TOKEN_TYPE, "password").build(),
                repositoryArtifact.getRepositoryCredential());
        Assert.assertEquals("maven_repo", repositoryArtifact.getRepositoryName());
        Assert.assertEquals("maven", repositoryArtifact.getArtifactRepository());
        Assert.assertEquals("https://fastconnect.org/maven/content/repositories/fastconnect", repositoryArtifact.getRepositoryURL());
    }

    private ImplementationArtifact getImplementationArtifact(AbstractInstantiableToscaType component, String operation) {
        return component.getInterfaces().values().iterator().next().getOperations().get(operation).getImplementationArtifact();
    }

    private DeploymentArtifact getDeploymentArtifact(AbstractInstantiableToscaType component, String artifactName) {
        return component.getArtifacts().get(artifactName);
    }

    @Test
    public void testRangeType() throws ParsingException {
        ParserTestUtil.mockNormativeTypes(csarRepositorySearchService);
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "range_type.yml"));
        List<ParsingError> errors = parsingResult.getContext().getParsingErrors();
        Assert.assertEquals(0, errors.size());
    }

    @Test
    public void testRangeTypeConstraint() throws ParsingException {
        ParserTestUtil.mockNormativeTypes(csarRepositorySearchService);
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "range_type_constraint.yml"));
        List<ParsingError> errors = parsingResult.getContext().getParsingErrors();
        Assert.assertEquals(0, errors.size());
    }

    @Test
    public void testRangeTypeConstraintFailMin() throws ParsingException {
        ParserTestUtil.mockNormativeTypes(csarRepositorySearchService);
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "range_type_constraint_fail_min.yml"));
        List<ParsingError> errors = parsingResult.getContext().getParsingErrors();
        Assert.assertEquals(0, errors.size());
    }

    @Test
    public void testRangeTypeConstraintFailMax() throws ParsingException {
        ParserTestUtil.mockNormativeTypes(csarRepositorySearchService);
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "range_type_constraint_fail_max.yml"));
        List<ParsingError> errors = parsingResult.getContext().getParsingErrors();
        Assert.assertEquals(0, errors.size());
    }

}
