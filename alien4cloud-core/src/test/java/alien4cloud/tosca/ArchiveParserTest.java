package alien4cloud.tosca;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import javax.annotation.Resource;

import org.alien4cloud.tosca.catalog.ArchiveParser;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.types.ArtifactType;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.DataType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.git.RepositoryManager;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.normative.NormativeTypesConstant;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.utils.AlienConstants;
import alien4cloud.utils.FileUtil;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:tosca/archive-parser-application-context.xml")
public class ArchiveParserTest {
    private Path artifactsDirectory = Paths.get("../target/it-artifacts");
    private RepositoryManager repositoryManager = new RepositoryManager();

    @Resource
    private ArchiveParser archiveParser;

    public static void mockNormativeTypes(ICSARRepositorySearchService repositorySearchService) {
        Csar csar = new Csar("tosca-normative-types", "1.0.0-ALIEN12");
        Mockito.when(repositorySearchService.getArchive(csar.getName(), csar.getVersion())).thenReturn(csar);
        NodeType mockedNodeRoot = new NodeType();
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq(NormativeTypesConstant.ROOT_NODE_TYPE),
                Mockito.any(Set.class))).thenReturn(mockedNodeRoot);
        RelationshipType mockedRelationshipRoot = new RelationshipType();
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(RelationshipType.class),
                Mockito.eq(NormativeTypesConstant.ROOT_RELATIONSHIP_TYPE), Mockito.any(Set.class))).thenReturn(mockedRelationshipRoot);
        CapabilityType mockedCapabilityType = new CapabilityType();
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq(NormativeTypesConstant.ROOT_CAPABILITY_TYPE),
                Mockito.any(Set.class))).thenReturn(mockedCapabilityType);
        DataType mockedDataType = new DataType();
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(DataType.class), Mockito.eq(NormativeTypesConstant.ROOT_DATA_TYPE),
                Mockito.any(Set.class))).thenReturn(mockedDataType);
        ArtifactType mockedArtifactType = new ArtifactType();
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(ArtifactType.class), Mockito.eq(NormativeTypesConstant.ROOT_ARTIFACT_TYPE),
                Mockito.any(Set.class))).thenReturn(mockedArtifactType);
    }

    @Test
    public void parseNormativeTypesWd03() throws ParsingException, IOException {
        String localName = "tosca-normative-types";
        repositoryManager.cloneOrCheckout(artifactsDirectory, "https://github.com/alien4cloud/tosca-normative-types.git", "master", localName);

        Path normativeTypesPath = artifactsDirectory.resolve(localName);
        Path normativeTypesZipPath = artifactsDirectory.resolve(localName + ".zip");
        // Update zip
        FileUtil.zip(normativeTypesPath, normativeTypesZipPath);

        // Path normativeTypesZipPath = Paths.get("../target/it-artifacts/zipped/apache-lb-types-0.1.csar");
        ParsingResult<ArchiveRoot> parsingResult = archiveParser.parse(normativeTypesZipPath, AlienConstants.GLOBAL_WORKSPACE_ID);

        displayErrors(parsingResult);

        Assert.assertFalse(parsingResult.hasError(ParsingErrorLevel.ERROR));
    }

    public static void displayErrors(ParsingResult<?> parsingResult) {
        System.out.println("\n\nERRORS: \n");
        for (int i = 0; i < parsingResult.getContext().getParsingErrors().size(); i++) {
            ParsingError error = parsingResult.getContext().getParsingErrors().get(i);
            if (error.getErrorLevel().equals(ParsingErrorLevel.ERROR)) {
                System.out.println(parsingResult.getContext().getFileName() + "\n" + error);
            }
        }
        System.out.println("\n\nWARNING: \n");
        for (int i = 0; i < parsingResult.getContext().getParsingErrors().size(); i++) {
            ParsingError error = parsingResult.getContext().getParsingErrors().get(i);
            if (error.getErrorLevel().equals(ParsingErrorLevel.WARNING)) {
                System.out.println(parsingResult.getContext().getFileName() + "\n" + error);
            }
        }
        System.out.println("\n\nINFO: \n");
        for (int i = 0; i < parsingResult.getContext().getParsingErrors().size(); i++) {
            ParsingError error = parsingResult.getContext().getParsingErrors().get(i);
            if (error.getErrorLevel().equals(ParsingErrorLevel.INFO)) {
                System.out.println(parsingResult.getContext().getFileName() + "\n" + error);
            }
        }
    }
}