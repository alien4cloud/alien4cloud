package alien4cloud.tosca.parser;

import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.Set;

import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

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
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(Mockito.mock(NodeType.class));
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
}