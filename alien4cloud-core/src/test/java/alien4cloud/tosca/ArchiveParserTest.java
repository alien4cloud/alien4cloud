package alien4cloud.tosca;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.git.RepositoryManager;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.utils.FileUtil;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:tosca/archive-parser-application-context.xml")
public class ArchiveParserTest {
    private Path artifactsDirectory = Paths.get("../target/it-artifacts");
    private RepositoryManager repositoryManager = new RepositoryManager();

    @Resource
    private ArchiveParser archiveParser;

    @Test
    public void parseNormativeTypesWd03() throws ParsingException, IOException {
        String localName = "tosca-normative-types-1.0.0.wd03";
        repositoryManager.cloneOrCheckout(artifactsDirectory, "https://github.com/alien4cloud/tosca-normative-types.git", "1.0.0.wd03", localName);

        Path normativeTypesPath = artifactsDirectory.resolve(localName);
        Path normativeTypesZipPath = artifactsDirectory.resolve(localName + ".zip");
        // Update zip
        FileUtil.zip(normativeTypesPath, normativeTypesZipPath);

        ParsingResult<ArchiveRoot> parsingResult = archiveParser.parse(normativeTypesZipPath);

        displayErrors(parsingResult);

        Assert.assertFalse(ArchiveUploadService.hasError(parsingResult, ParsingErrorLevel.ERROR));
    }

    private void displayErrors(ParsingResult<?> parsingResult) {
        for (int i = 0; i < parsingResult.getContext().getParsingErrors().size(); i++) {
            ParsingError error = parsingResult.getContext().getParsingErrors().get(i);
            if (error.getErrorLevel().equals(ParsingErrorLevel.ERROR)) {
                System.out.println(parsingResult.getContext().getFileName() + "\n" + error);
            }
        }

        for (ParsingResult<?> child : parsingResult.getContext().getSubResults()) {
            displayErrors(child);
        }
    }
}