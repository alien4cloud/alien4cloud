package alien4cloud.tosca.parser;

import java.nio.file.Paths;
import java.util.Set;

import javax.annotation.Resource;

import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.tosca.ArchiveParserTest;
import alien4cloud.tosca.model.ArchiveRoot;

/**
 * Test tosca parsing for Tosca Simple profile in YAML alien_dsl_1_1_0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:tosca/parser-application-context.xml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ToscaParserSimpleProfileAlien110Test extends AbstractToscaParserSimpleProfileTest {

    @Override
    protected String getRootDirectory() {
        return "src/test/resources/tosca/SimpleProfile_alien110/parsing/";
    }

    @Override
    protected String getToscaVersion() {
        return "alien_dsl_1_1_0";
    }

    @Resource
    private ToscaParser parser;

    @Resource
    private ICSARRepositorySearchService repositorySearchService;

    @Test
    public void testCapabilities() throws ParsingException {
        NodeType mockedResult = Mockito.mock(NodeType.class);
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.SoftwareComponent"),
                Mockito.any(Set.class))).thenReturn(mockedResult);
        Mockito.when(mockedResult.getDerivedFrom()).thenReturn(Lists.newArrayList("tosca.nodes.Root"));
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);

        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);
        CapabilityType mockedCapabilityResult = Mockito.mock(CapabilityType.class);
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq("tosca.capabilities.Endpoint"),
                Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq("tosca.capabilities.Container"),
                Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);

        RelationshipType connectsTo = new RelationshipType();
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(RelationshipType.class), Mockito.eq("tosca.relationships.ConnectsTo"),
                Mockito.any(Set.class))).thenReturn(connectsTo);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "requirement_capabilities.yaml"));
        ArchiveParserTest.displayErrors(parsingResult);
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
                    Assert.assertNotNull(capabilityDefinition.getDescription());
                }
            });
        });
    }
}
