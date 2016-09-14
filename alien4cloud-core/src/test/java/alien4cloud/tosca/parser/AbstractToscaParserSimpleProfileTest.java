package alien4cloud.tosca.parser;

import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Resource;

import alien4cloud.tosca.ArchiveParserTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.component.ICSARRepositorySearchService;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.model.definitions.PropertyConstraint;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.RequirementDefinition;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.definitions.constraints.GreaterThanConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.LessThanConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.MaxLengthConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.MinLengthConstraint;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.utils.MapUtil;

import com.google.common.collect.Lists;

/**
 * Test tosca parsing for Tosca Simple profile in YAML wd03
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:tosca/parser-application-context.xml")
public abstract class AbstractToscaParserSimpleProfileTest {

    protected abstract String getRootDirectory();

    protected abstract String getToscaVersion();

    @Resource
    protected ToscaParser parser;

    @Resource
    protected ICSARRepositorySearchService repositorySearchService;

    @Test
    public void testDefinitionVersionValid() throws FileNotFoundException, ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-definition-version.yml"));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
        ArchiveRoot archiveRoot = parsingResult.getResult();
        Assert.assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNodeType() throws FileNotFoundException, ParsingException {
        Mockito.reset(repositorySearchService);
        NodeType mockedResult = Mockito.mock(NodeType.class);
        Mockito.when(
                repositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.SoftwareComponent"),
                        Mockito.any(Set.class))).thenReturn(mockedResult);
        Mockito.when(mockedResult.getDerivedFrom()).thenReturn(Lists.newArrayList("tosca.nodes.Root"));
        Mockito.when(
                repositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);

        Mockito.when(
                repositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);
        CapabilityType mockedCapabilityResult = Mockito.mock(CapabilityType.class);
        Mockito.when(
                repositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class),
                        Mockito.eq("mytypes.mycapabilities.MyCapabilityTypeName"), Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);
        Mockito.when(
                repositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class),
                        Mockito.eq("mytypes.mycapabilities.MyCapabilityTypeName"), Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);

        Mockito.when(
                repositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq("tosca.capabilities.Endpoint"),
                        Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);
        RelationshipType hostedOn = new RelationshipType();
        Mockito.when(
                repositorySearchService.getElementInDependencies(Mockito.eq(RelationshipType.class), Mockito.eq("tosca.relationships.HostedOn"),
                        Mockito.any(Set.class))).thenReturn(hostedOn);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-node-type.yml"));

        ArchiveParserTest.displayErrors(parsingResult);
        assertNoBlocker(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        Assert.assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        Assert.assertEquals(1, archiveRoot.getNodeTypes().size());
        // check node type.
        Entry<String, NodeType> entry = archiveRoot.getNodeTypes().entrySet().iterator().next();

        Assert.assertEquals("my_company.my_types.MyAppNodeType", entry.getKey());
        NodeType nodeType = entry.getValue();

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

        Assert.assertEquals(
                MapUtil.newHashMap(new String[] { "my_app_password", "my_app_duration", "my_app_size", "my_app_port" }, new PropertyDefinition[] { def1, def4,
                        def3, def2 }), nodeType.getProperties());

        // check requirements
        Assert.assertEquals(2, nodeType.getRequirements().size());
        RequirementDefinition rd0 = nodeType.getRequirements().get(0);
        Assert.assertEquals("host", rd0.getId());
        Assert.assertEquals(1, rd0.getLowerBound());
        Assert.assertEquals(1, rd0.getUpperBound());

        RequirementDefinition rd1 = nodeType.getRequirements().get(1);
        Assert.assertEquals("other", rd1.getId());
        Assert.assertEquals(0, rd1.getLowerBound());
        Assert.assertEquals(Integer.MAX_VALUE, rd1.getUpperBound());

        // validate attributes parsing

        // nodeType.getAttributes()
        // nodeType.getInterfaces()
        // nodeType.getCapabilities()
        // nodeType.get
    }

    public static int countErrorByLevelAndCode(ParsingResult<?> parsingResult, ParsingErrorLevel errorLevel, ErrorCode errorCode) {
        int finalCount = 0;
        for (int i = 0; i < parsingResult.getContext().getParsingErrors().size(); i++) {
            ParsingError error = parsingResult.getContext().getParsingErrors().get(i);
            if (error.getErrorLevel().equals(errorLevel) && (error.getErrorCode().equals(errorCode) || errorCode == null)) {
                finalCount++;
            }
        }
        return finalCount;
    }

    public static void assertNoBlocker(ParsingResult<?> parsingResult) {
        Assert.assertFalse(countErrorByLevelAndCode(parsingResult, ParsingErrorLevel.ERROR, null) > 0);
    }

}
