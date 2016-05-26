package alien4cloud.tosca.parser;

import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.impl.ErrorCode;

import com.google.common.collect.Lists;

/**
 * Test tosca parsing for Tosca Simple profile in YAML alien_dsl_1_2_0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:tosca/parser-application-context.xml")
public class ToscaParserSimpleProfileAlien120Test extends AbstractToscaParserSimpleProfileTest {

    @Override
    protected String getRootDirectory() {
        return "src/test/resources/tosca/SimpleProfil_alien120/parsing/";
    }

    @Override
    protected String getToscaVersion() {
        return "alien_dsl_1_2_0";
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBadOccurrence() throws FileNotFoundException, ParsingException {
        Mockito.reset(repositorySearchService);
        IndexedNodeType mockedResult = Mockito.mock(IndexedNodeType.class);
        Mockito.when(
                repositorySearchService.getElementInDependencies(Mockito.eq(IndexedNodeType.class), Mockito.eq("tosca.nodes.SoftwareComponent"),
                        Mockito.any(List.class))).thenReturn(mockedResult);
        Mockito.when(mockedResult.getDerivedFrom()).thenReturn(Lists.newArrayList("tosca.nodes.Root"));
        Mockito.when(
                repositorySearchService.getElementInDependencies(Mockito.eq(IndexedNodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(List.class)))
                .thenReturn(mockedResult);

        Mockito.when(
                repositorySearchService.getElementInDependencies(Mockito.eq(IndexedNodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(List.class)))
                .thenReturn(mockedResult);
        IndexedRelationshipType hostedOn = new IndexedRelationshipType();
        Mockito.when(
                repositorySearchService.getElementInDependencies(Mockito.eq(IndexedRelationshipType.class), Mockito.eq("tosca.relationships.HostedOn"),
                        Mockito.any(List.class))).thenReturn(hostedOn);
        IndexedCapabilityType mockedCapabilityResult = Mockito.mock(IndexedCapabilityType.class);
        Mockito.when(
                repositorySearchService.getElementInDependencies(Mockito.eq(IndexedCapabilityType.class), Mockito.eq("tosca.capabilities.Endpoint"),
                        Mockito.any(List.class))).thenReturn(mockedCapabilityResult);
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-node-type-invalid-occurrence.yml"));

        Assert.assertEquals(2, countErrorByLevelAndCode(parsingResult, ParsingErrorLevel.ERROR, ErrorCode.SYNTAX_ERROR));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRelationshipType() throws FileNotFoundException, ParsingException {
        Mockito.reset(repositorySearchService);
        IndexedRelationshipType mockedResult = Mockito.mock(IndexedRelationshipType.class);
        Mockito.when(
                repositorySearchService.getElementInDependencies(Mockito.eq(IndexedRelationshipType.class), Mockito.eq("tosca.relationships.Relationship"),
                        Mockito.any(List.class))).thenReturn(mockedResult);
        Mockito.when(mockedResult.getDerivedFrom()).thenReturn(Lists.newArrayList("tosca.capabilities.Root"));

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-relationship-type.yml"));
        assertNoBlocker(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        Assert.assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        Assert.assertEquals(1, archiveRoot.getRelationshipTypes().size());
        Entry<String, IndexedRelationshipType> entry = archiveRoot.getRelationshipTypes().entrySet().iterator().next();
        Assert.assertEquals("mycompany.mytypes.myapplication.MyRelationship", entry.getKey());
        IndexedRelationshipType relationship = entry.getValue();
        Assert.assertEquals(Lists.newArrayList("tosca.relationships.Relationship", "tosca.capabilities.Root"), relationship.getDerivedFrom());
        Assert.assertEquals("a custom relationship", relationship.getDescription());

        // properties
        Assert.assertEquals(2, relationship.getProperties().size());
        Assert.assertTrue(relationship.getProperties().containsKey("my_feature_setting"));
        PropertyDefinition pd = relationship.getProperties().get("my_feature_setting");
        Assert.assertEquals("string", pd.getType());
        Assert.assertTrue(relationship.getProperties().containsKey("my_feature_value"));
        pd = relationship.getProperties().get("my_feature_value");
        Assert.assertEquals("integer", pd.getType());

        // valid targets
        Assert.assertEquals(2, relationship.getValidTargets().length);
        Assert.assertEquals("tosca.capabilities.Feature1", relationship.getValidTargets()[0]);
        Assert.assertEquals("tosca.capabilities.Feature2", relationship.getValidTargets()[1]);

    }

}
