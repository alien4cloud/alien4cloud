package alien4cloud.tosca.parser;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.Resource;

import org.alien4cloud.tosca.catalog.ArchiveParser;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.common.AlienConstants;
import alien4cloud.tosca.ArchiveParserTest;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.utils.FileUtil;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:tosca/archive-parser-application-context.xml")
public class ArchiveParserAlien120Test {
    private Path artifactsDirectory = Paths.get("../target/it-artifacts");

    @Resource
    private ArchiveParser archiveParser;

    private static final String ROOT_DIRECTORY = "src/test/resources/tosca/SimpleProfil_alien120/parsing/";

    private ParsingResult<ArchiveRoot> parse(String fileName) throws IOException, ParsingException {
        Path source = Paths.get(ROOT_DIRECTORY, fileName);
        Path target = artifactsDirectory.resolve(fileName + ".zip");
        FileUtil.zip(source, target);

        ParsingResult<ArchiveRoot> parsingResult = archiveParser.parse(target, AlienConstants.GLOBAL_WORKSPACE_ID);

        return parsingResult;
    }

    @Test
    public void testDataTypesVeryComplexWithDefaultError1() throws ParsingException, IOException {
        ParsingResult<ArchiveRoot> parsingResult = parse("tosca-data-types-very-complex-default-error1.yml");
        ArchiveParserTest.displayErrors(parsingResult);
        Assert.assertEquals(2, parsingResult.getResult().getDataTypes().size());
        Assert.assertEquals(1, parsingResult.getResult().getNodeTypes().size());
        Assert.assertEquals(1, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testDataTypesVeryComplexWithDefaultError2() throws ParsingException, IOException {
        ParsingResult<ArchiveRoot> parsingResult = parse("tosca-data-types-very-complex-default-error2.yml");
        ArchiveParserTest.displayErrors(parsingResult);
        Assert.assertEquals(2, parsingResult.getResult().getDataTypes().size());
        Assert.assertEquals(1, parsingResult.getResult().getNodeTypes().size());
        Assert.assertEquals(1, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testDataTypesVeryComplexWithDefaultError3() throws ParsingException, IOException {
        ParsingResult<ArchiveRoot> parsingResult = parse("tosca-data-types-very-complex-default-error3.yml");
        ArchiveParserTest.displayErrors(parsingResult);
        Assert.assertEquals(2, parsingResult.getResult().getDataTypes().size());
        Assert.assertEquals(1, parsingResult.getResult().getNodeTypes().size());
        Assert.assertEquals(1, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testDataTypesVeryComplexWithDefaultError4() throws ParsingException, IOException {
        ParsingResult<ArchiveRoot> parsingResult = parse("tosca-data-types-very-complex-default-error4.yml");
        ArchiveParserTest.displayErrors(parsingResult);
        Assert.assertEquals(2, parsingResult.getResult().getDataTypes().size());
        Assert.assertEquals(1, parsingResult.getResult().getNodeTypes().size());
        Assert.assertEquals(2, parsingResult.getContext().getParsingErrors().size());
    }

}