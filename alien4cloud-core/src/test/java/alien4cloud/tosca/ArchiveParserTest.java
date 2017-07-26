package alien4cloud.tosca;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.Resource;

import alien4cloud.tosca.parser.ParserTestUtil;
import org.alien4cloud.tosca.catalog.ArchiveParser;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.git.RepositoryManager;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.utils.AlienConstants;
import alien4cloud.utils.FileUtil;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:tosca/archive-parser-application-context.xml")
@DirtiesContext
public class ArchiveParserTest {
    private Path artifactsDirectory = Paths.get("../target/it-artifacts");
    private RepositoryManager repositoryManager = new RepositoryManager();

    @Resource
    private ArchiveParser archiveParser;

    @Test
    public void parseNormativeTypes() throws ParsingException, IOException {
        String localName = "tosca-normative-types";
        repositoryManager.cloneOrCheckout(artifactsDirectory, "https://github.com/alien4cloud/tosca-normative-types.git", "master", localName);

        Path normativeTypesPath = artifactsDirectory.resolve(localName);
        Path normativeTypesZipPath = artifactsDirectory.resolve(localName + ".zip");
        // Update zip
        FileUtil.zip(normativeTypesPath, normativeTypesZipPath);

        ParsingResult<ArchiveRoot> parsingResult = archiveParser.parse(normativeTypesZipPath, AlienConstants.GLOBAL_WORKSPACE_ID);

        ParserTestUtil.displayErrors(parsingResult);

        Assert.assertFalse(parsingResult.hasError(ParsingErrorLevel.ERROR));
    }
}
